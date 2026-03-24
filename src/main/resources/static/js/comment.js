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

function renderComment(comment, level) {
	const commentDiv = document.createElement('div');
	commentDiv.className = 'comment-item' + (level > 0 ? ' comment-reply' : '');
	commentDiv.style.marginLeft = (level * 20) + 'px';

	const authorName = comment.author_name || '用户' + comment.author_id;
	const timeStr = formatTime(comment.created_at);
	
	let contentHtml = comment.content;
	if (comment.parent_cid && comment.parent_cid !== comment.id) {
		const parentComment = findCommentById(comment.parent_cid);
		if (parentComment) {
			const parentAuthor = parentComment.author_name || '用户' + parentComment.author_id;
			contentHtml = `<span class="reply-to">回复 @${parentAuthor}：</span>${contentHtml}`;
		}
	}

	commentDiv.innerHTML = `
		<div class="card mb-3">
			<div class="card-body">
				<div class="d-flex justify-content-between align-items-start">
					<div>
						<h6 class="comment-author">${authorName}</h6>
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
		alert('请输入评论内容');
		return;
	}

	const params = new URLSearchParams();
	params.append('blogId', currentBlogId);
	params.append('authorId', 1);
	params.append('content', content);
	if (replyToCommentId) {
		params.append('parentCid', replyToCommentId);
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
		if (data.code === 200) {
			alert('评论成功！');
			document.getElementById('commentContent').value = '';
			cancelReply();
			loadComments();
		} else {
			alert('评论失败：' + data.msg);
		}
	})
	.catch(error => {
		console.error('评论出错:', error);
		alert('评论失败，请稍后重试');
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
			alert('博客已成功移入回收箱！');
			window.location.href = '/';
		} else {
			alert('删除失败：' + data);
		}
	})
	.catch(error => {
		console.error('删除博客出错:', error);
		alert('删除失败，请稍后重试');
	});
}
