// 博客评论交互：加载、渲染嵌套评论、提交、内联回复、删除
let currentBlogId = 0;
let allComments = [];

// HTML 实体转义（防止 XSS）
function escapeHtml(str) {
    if (!str) return '';
    var div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

document.addEventListener('DOMContentLoaded', function() {
	currentBlogId = document.getElementById('getBlogId').value;
	loadComments();
});

function loadComments() {
	fetch(`/user/comment/list/${currentBlogId}`)
		.then(response => response.json())
		.then(data => {
			if (data.code === 200) {
				allComments = data.data;
				renderComments();
				updateCommentCount();
			} else {
				console.error('加载评论失败:', data.msg);
			}
		})
		.catch(error => console.error('加载评论出错:', error));
}

function renderComments() {
	const commentList = document.getElementById('commentList');
	commentList.innerHTML = '';

	if (allComments.length === 0) {
		commentList.innerHTML = '<div class="alert alert-info">暂无评论，快来抢沙发吧~</div>';
		return;
	}

	allComments.forEach(comment => {
		const commentHtml = renderComment(comment, 0);
		commentList.appendChild(commentHtml);
	});
}

// 递归渲染嵌套评论：通过 level 参数控制缩进深度 (marginLeft = level * 20px)，逐级嵌套 children
function renderComment(comment, level) {
	const commentDiv = document.createElement('div');
	commentDiv.className = 'comment-item' + (level > 0 ? ' comment-reply' : '');
	commentDiv.style.marginLeft = (level * 20) + 'px';
	commentDiv.setAttribute('data-comment-id', comment.id);

	const authorName = escapeHtml(comment.author_name || ('用户' + comment.u_id));
	const avatarUrl = comment.author_avatar || '/images/default-avatar.png';
	// 防御 avatar URL 注入，只允许 https:// 或 / 开头的合法 URL
	var safeAvatarUrl = (avatarUrl && (avatarUrl.startsWith('https://') || avatarUrl.startsWith('/'))) ? avatarUrl : '/images/default-avatar.png';
	const avatarImg = '<img src="' + safeAvatarUrl + '" class="author-avatar-mini" alt="" style="width:22px;height:22px;object-fit:cover;">';

	// LV 徽章
	var lvBadge = '';
	if (comment.authorLevel != null && comment.authorLevel > 0) {
		var lv = comment.authorLevel >= 6 ? 7 : comment.authorLevel;
		lvBadge = '<span class="comment-lv-badge" data-level="' + lv + '">LV.' + comment.authorLevel + '</span>';
	}

	// 成就徽章图标（最多3个）
	var badgeIcons = '';
	if (comment.authorAchievementIds && comment.authorAchievementIds.length > 0) {
		var displayIds = comment.authorAchievementIds.slice(0, 3);
		badgeIcons = displayIds.map(function(bid) {
			return '<span class="comment-badge-icon" title="成就徽章"><img src="/images/badge/icon/' + bid + '-icon.jpg" alt="" loading="lazy" onerror="this.style.display=\'none\'" style="width:18px;height:18px;object-fit:cover;border-radius:3px;"></span>';
		}).join('');
	}

	var authorLinkHtml = '<a href="/user/profile?id=' + comment.u_id + '" class="author-link comment-author-link">' + avatarImg + authorName + '</a>' + lvBadge + badgeIcons;
	var fullTime = formatFullTime(comment.created_at);
	var relTime = formatTime(comment.created_at);
	var timeStr = fullTime + ' - ' + relTime;

	var contentHtml = escapeHtml(comment.content);
	if (comment.parent_cid && comment.parent_cid !== comment.id) {
		var parentComment = findCommentById(comment.parent_cid);
		if (parentComment) {
			var parentAuthor = escapeHtml(parentComment.author_name || ('用户' + parentComment.u_id));
			contentHtml = '<span class="reply-to">回复 @' + parentAuthor + '：</span>' + contentHtml;
		}
	}

	commentDiv.innerHTML =
		'<div class="card mb-3">' +
			'<div class="card-body">' +
				'<div class="comment-header-row">' +
					'<div class="comment-author-left">' +
						authorLinkHtml +
					'</div>' +
					'<div class="comment-time-right">' +
						'<span class="text-muted comment-time-text">' + timeStr + '</span>' +
					'</div>' +
				'</div>' +
				'<p class="comment-content mb-2">' + contentHtml + '</p>' +
				'<div class="comment-actions">' +
					'<button class="btn-forward" onclick="forwardComment(' + comment.id + ')" title="转发/打印此评论">' +
						'<i class="bi bi-share"></i> 转发' +
					'</button>' +
					'<button class="btn-reply" onclick="replyTo(' + comment.id + ', \'' + authorName.replace(/'/g, "\\'") + '\')">' +
						'回复' +
					'</button>' +
				'</div>' +
			'</div>' +
		'</div>';

	if (comment.children && comment.children.length > 0) {
		comment.children.forEach(child => {
			const childHtml = renderComment(child, level + 1);
			commentDiv.appendChild(childHtml);
		});
	}

	return commentDiv;
}

// 两层树搜索：先查顶层评论列表，再深入每个顶层评论的 children 数组查找
function findCommentById(id) {
	for (const comment of allComments) {
		if (comment.id === id) {
			return comment;
		}
		if (comment.children) {
			for (const child of comment.children) {
				if (child.id === id) {
					return child;
				}
			}
		}
	}
	return null;
}

// 将时间戳转换为绝对时间：YYYY年MM月DD日HH时MM分
function formatFullTime(timeStr) {
	var date = new Date(timeStr);
	var y = date.getFullYear();
	var M = ('0' + (date.getMonth() + 1)).slice(-2);
	var d = ('0' + date.getDate()).slice(-2);
	var h = ('0' + date.getHours()).slice(-2);
	var m = ('0' + date.getMinutes()).slice(-2);
	return y + '年' + M + '月' + d + '日' + h + '时' + m + '分';
}

// 将时间戳转换为相对时间显示（刚刚/N分钟前/N小时前/N天前），超过7天显示完整日期
function formatTime(timeStr) {
	const date = new Date(timeStr);
	const now = new Date();
	const diff = now - date;

	const minute = 60 * 1000;
	const hour = 60 * minute;
	const day = 24 * hour;

	if (diff < minute) {
		return '刚刚';
	} else if (diff < hour) {
		return Math.floor(diff / minute) + '分钟前';
	} else if (diff < day) {
		return Math.floor(diff / hour) + '小时前';
	} else if (diff < 7 * day) {
		return Math.floor(diff / day) + '天前';
	} else {
		return date.toLocaleDateString('zh-CN');
	}
}

// 转发评论：复用分享卡片弹窗
function forwardComment(commentId) {
	var comment = findCommentById(commentId);
	if (!comment) return;
	shareComment({
		authorName: comment.author_name || ('用户' + comment.u_id),
		avatarUrl: comment.author_avatar || '/images/default-avatar.png',
		content: comment.content,
		time: formatFullTime(comment.created_at)
	});
}

// 发表顶层评论（不含回复，回复通过内联表单提交）
function submitComment() {
	const content = document.getElementById('commentContent').value.trim();

	if (!content) {
		showToast('请输入评论内容', 'warning');
		return;
	}
	if (content.length > 65535) {
		showToast('评论内容过长，请精简后再提交', 'warning');
		return;
	}

	const params = new URLSearchParams();
	params.append('blogId', currentBlogId);
	params.append('content', content);

	authFetch('/user/comment/add', {
		method: 'POST',
		headers: {
			'Content-Type': 'application/x-www-form-urlencoded',
		},
		body: params
	})
	.then(response => response.json())
	.then(data => {
		if (data.code === 200) {
			showToast('评论成功！', 'success');
			document.getElementById('commentContent').value = '';
			loadComments();
		} else {
			showToast('评论失败：' + data.msg, 'error');
		}
	})
	.catch(error => {
		console.error('评论出错:', error);
		showToast('评论失败，请稍后重试', 'error');
	});
}

// 内联回复：在父评论下方展开回复表单
function replyTo(commentId, authorName) {
	removeAllInlineReplyForms();

	var targetItems = document.querySelectorAll('.comment-item[data-comment-id="' + commentId + '"]');
	if (targetItems.length === 0) return;
	var targetItem = targetItems[0];
	var card = targetItem.querySelector('.card');
	if (!card) return;

	var uniqueId = 'inlineReplyContent_' + commentId;
	var formDiv = document.createElement('div');
	formDiv.className = 'inline-reply-form';
	formDiv.id = 'inlineReplyForm_' + commentId;

	var safeAuthorName = authorName.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
	formDiv.innerHTML =
		'<div class="inline-reply-body">' +
			'<textarea class="form-control inline-reply-textarea" id="' + uniqueId + '" rows="2" placeholder="回复 @' + safeAuthorName + '：" maxlength="65535"></textarea>' +
			'<div class="inline-reply-actions">' +
				'<button type="button" class="btn btn-sm btn-outline-light btn-emoji-inline" data-target="' + uniqueId + '" title="插入表情">😊</button>' +
				'<button type="button" class="btn btn-accent btn-accent-sm" onclick="submitInlineReply(' + commentId + ')">回复</button>' +
				'<button type="button" class="btn btn-ghost btn-ghost-sm" onclick="cancelInlineReply()">取消</button>' +
			'</div>' +
		'</div>';

	card.parentNode.insertBefore(formDiv, card.nextSibling);

	var ta = document.getElementById(uniqueId);
	if (ta) setTimeout(function() { ta.focus(); }, 50);
}

// 清除所有内联回复表单
function removeAllInlineReplyForms() {
	var forms = document.querySelectorAll('.inline-reply-form');
	forms.forEach(function(f) { f.remove(); });
}

// 取消内联回复
function cancelInlineReply() {
	removeAllInlineReplyForms();
}

// 提交内联回复
function submitInlineReply(commentId) {
	var textarea = document.getElementById('inlineReplyContent_' + commentId);
	if (!textarea) return;
	var content = textarea.value.trim();

	if (!content) {
		showToast('请输入回复内容', 'warning');
		return;
	}
	if (content.length > 65535) {
		showToast('回复内容过长，请精简后再提交', 'warning');
		return;
	}

	var params = new URLSearchParams();
	params.append('blogId', currentBlogId);
	params.append('content', content);
	params.append('parentId', commentId);

	authFetch('/user/comment/add', {
		method: 'POST',
		headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
		body: params
	})
	.then(function(r) { return r.json(); })
	.then(function(data) {
		if (data.code === 200) {
			showToast('回复成功！', 'success');
			removeAllInlineReplyForms();
			loadComments();
		} else {
			showToast('回复失败：' + data.msg, 'error');
		}
	})
	.catch(function() {
		showToast('回复失败，请稍后重试', 'error');
	});
}

function updateCommentCount() {
	let count = allComments.length;
	allComments.forEach(comment => {
		if (comment.children) {
			count += comment.children.length;
		}
	});
	document.getElementById('commentCount').textContent = count;
}

// ---- Emoji 表情选择器 ----
(function() {
    var EMOJIS = [
        '😀','😃','😄','😁','😅','😂','🤣','😊','😇','🙂','😉','😍','😘','🤗','🤩','😎','😏','😒','😞','😔','😣','😖','😫','😩','😢','😭','😤','😡','🤬','😱','😨','😰','😳','🤔','🤨','😴','🤤','😋','😛','😝','🤪','😷','🤒','🤕','🥴','🥺','🧐',
        '👍','👎','👌','✌','🤞','🤘','👋','✋','👏','🙌','🤝','💪','🙏','✍','👀','🧠','👑','💍',
        '❤','🧡','💛','💚','💙','💜','🖤','🤍','💔','❣','💕','💞','💓','💗','💖','💘','💝',
        '💯','🔥','⭐','🌟','✨','💫','🎉','🎊','🎈','🎂','🎁','🏆','🥇','✅','❌','💡','💻','📱','⌨','🎵','🎶','📚','📝','✏','✂','🔒','🔑','🔨','🔧','💊','🛁',
        '☀','☁','🌧','⛈','❄','🌈','🌙','⚡','💧','🌊','🌍','🌸','🌹','🌻','🍀','🎄','⭐',
        '🍎','🍕','🍔','🍟','🍩','🍰','🍺','☕','🍿','🍦','🎂','🍬','🍭','🍪','🍷','🍹'
    ];
    var targetId = null;
    var popup = null;

    function build() {
        if (popup) return;
        popup = document.createElement('div');
        popup.className = 'emoji-popup';
        popup.style.display = 'none';
        var html = '<div class="emoji-grid">';
        EMOJIS.forEach(function(e) {
            html += '<span class="emoji-item" data-emoji="' + e + '">' + e + '</span>';
        });
        html += '</div>';
        popup.innerHTML = html;
        document.body.appendChild(popup);
        // 点击表情项插入
        popup.querySelectorAll('.emoji-item').forEach(function(item) {
            item.addEventListener('click', function() {
                insertEmoji(this.getAttribute('data-emoji'));
            });
        });
        // 点击外部关闭
        document.addEventListener('click', function(e) {
            if (!popup || popup.style.display !== 'block') return;
            if (!popup.contains(e.target) && !e.target.closest('.btn-emoji-inline, .btn-emoji')) {
                popup.style.display = 'none';
            }
        });
    }

    function insertEmoji(emoji) {
        if (!targetId) return;
        var el = document.getElementById(targetId);
        if (!el) return;
        var s = el.selectionStart, end = el.selectionEnd;
        el.value = el.value.substring(0, s) + emoji + el.value.substring(end);
        el.selectionStart = el.selectionEnd = s + emoji.length;
        el.focus();
        if (popup) popup.style.display = 'none';
    }

    // 等 DOM 就绪后构建 emoji 弹窗，使用事件委托支持动态创建的按钮
    document.addEventListener('DOMContentLoaded', function() {
        build();
    });
    document.addEventListener('click', function(e) {
        var btn = e.target.closest('.btn-emoji-inline, .btn-emoji');
        if (!btn) return;
        e.stopPropagation();
        targetId = btn.getAttribute('data-target');
        if (popup.style.display === 'block') {
            popup.style.display = 'none';
            return;
        }
        var rect = btn.getBoundingClientRect();
        popup.style.top = (rect.bottom + window.scrollY + 4) + 'px';
        popup.style.left = Math.max(0, rect.left + window.scrollX - 100) + 'px';
        popup.style.display = 'block';
        popup.style.zIndex = 9999;
    });
})();

// 软删除（移入回收箱），使用自定义确认对话框
async function deleteBlog() {
	const confirmed = await showConfirmDialog('确定要将此博客移入回收箱吗？');
	if (!confirmed) return;

	authFetch(`/blogs/delete/${currentBlogId}`, {
		method: 'POST'
	})
	.then(response => response.text())
	.then(data => {
		if (data.includes('已移入回收箱')) {
			showToast('博客已成功移入回收箱！', 'success');
			window.location.href = '/';
		} else {
			showToast('删除失败：' + data, 'error');
		}
	})
	.catch(error => {
		console.error('删除博客出错:', error);
		showToast('删除失败，请稍后重试', 'error');
	});
}
