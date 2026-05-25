// 博客评论交互：加载、渲染嵌套评论、提交、回复、删除
let currentBlogId = 0;
let allComments = [];

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

	const authorName = comment.author_name || '用户' + comment.u_id;
	const avatarUrl = comment.author_avatar || '/images/default-avatar.png';
	const avatarImg = `<img src="${avatarUrl}" class="author-avatar-mini" alt="" style="width:22px;height:22px;object-fit:cover;">`;
	const authorLink = `<a href="/user/profile?id=${comment.u_id}" class="author-link">${avatarImg}${authorName}</a>`;
	const timeStr = formatTime(comment.created_at);

	let contentHtml = comment.content;
	if (comment.parent_cid && comment.parent_cid !== comment.id) {
		const parentComment = findCommentById(comment.parent_cid);
		if (parentComment) {
			const parentAuthor = parentComment.author_name || '用户' + parentComment.u_id;
			contentHtml = `<span class="reply-to">回复 @${parentAuthor}：</span>${contentHtml}`;
		}
	}

	commentDiv.innerHTML = `
		<div class="card mb-3">
			<div class="card-body">
				<div class="d-flex justify-content-between align-items-start">
					<div>
						<h6 class="comment-author">${authorLink}</h6>
						<p class="comment-content mb-2">${contentHtml}</p>
						<small class="text-muted">${timeStr}</small>
					</div>
					<button class="btn btn-sm btn-link text-decoration-none" onclick="replyTo(${comment.id}, '${authorName}')">
						回复
					</button>
				</div>
			</div>
		</div>
	`;

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

function submitComment() {
	const content = document.getElementById('commentContent').value.trim();
	const replyToCommentId = document.getElementById('replyToCommentId').value;

	if (!content) {
		showToast('请输入评论内容', 'warning');
		return;
	}
	// 客户端长度限制 65535，防止超长内容写入数据库
	if (content.length > 65535) {
		showToast('评论内容过长，请精简后再提交', 'warning');
		return;
	}

	const params = new URLSearchParams();
	params.append('blogId', currentBlogId);
	params.append('content', content);
	if (replyToCommentId) {
		params.append('parentId', replyToCommentId);
	}

	fetch('/user/comment/add', {
		method: 'POST',
		headers: {
			'Content-Type': 'application/x-www-form-urlencoded',
		},
		body: params
	})
	.then(response => response.json())
	.then(data => {
		if (data.code === 401) {
			openAuthModal('login');
			showToast('请先登录后再评论', 'warning');
			return;
		}
		if (data.code === 200) {
			showToast('评论成功！', 'success');
			document.getElementById('commentContent').value = '';
			cancelReply();
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

function replyTo(commentId, authorName) {
	document.getElementById('replyToCommentId').value = commentId;
	document.getElementById('replyToAuthor').value = authorName;
	document.getElementById('cancelReply').style.display = 'inline-block';
	document.getElementById('commentContent').placeholder = `回复 @${authorName}：`;
	document.getElementById('commentContent').focus();
}

function cancelReply() {
	document.getElementById('replyToCommentId').value = '';
	document.getElementById('replyToAuthor').value = '';
	document.getElementById('cancelReply').style.display = 'none';
	document.getElementById('commentContent').placeholder = '请输入评论内容';
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
            var btn = document.querySelector('.btn-emoji-inline, .btn-outline-light');
            if (!popup.contains(e.target) && (!btn || !btn.contains(e.target))) {
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

    // 等 DOM 就绪后给所有 emoji 按钮绑定事件
    document.addEventListener('DOMContentLoaded', function() {
        build();
        document.querySelectorAll('.btn-emoji-inline, .btn-emoji').forEach(function(btn) {
            btn.addEventListener('click', function(e) {
                e.stopPropagation();
                targetId = this.getAttribute('data-target');
                if (popup.style.display === 'block') {
                    popup.style.display = 'none';
                    return;
                }
                var rect = this.getBoundingClientRect();
                popup.style.top = (rect.bottom + window.scrollY + 4) + 'px';
                popup.style.left = Math.max(0, rect.left + window.scrollX - 100) + 'px';
                popup.style.display = 'block';
                popup.style.zIndex = 9999;
            });
        });
    });
})();

// 软删除（移入回收箱），通过检查后端返回字符串判断结果
// 注意：data.includes('已移入回收箱') 依赖后端响应消息格式，属于脆弱匹配
function deleteBlog() {
	if (!confirm('确定要将此博客移入回收箱吗？')) {
		return;
	}

	fetch(`/blogs/delete/${currentBlogId}`, {
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
