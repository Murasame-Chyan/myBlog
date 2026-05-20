// 博客交互：点赞、阅读计数、分享
let isLiked = false;
let isProcessing = false;
let readCounted = false;

document.addEventListener('DOMContentLoaded', function() {
    const blogId = document.getElementById('getBlogId').value;
    if (blogId) {
        checkLikeStatus(blogId);

        // 延迟 3 秒计数阅读量：防抖机制，避免用户快速跳出时误计为有效阅读
        setTimeout(() => {
            if (!readCounted) {
                incrementReadCount(blogId);
                readCounted = true;
            }
        }, 3000);
    }
});

function checkLikeStatus(blogId) {
    fetch(`/blogs/isLiked/${blogId}`)
        .then(function(response) {
        if (response.status === 401) {
            openAuthModal('login');
            showToast('请先登录后再操作', 'warning');
            throw new Error('unauthorized');
        }
        return response.json();
    })
        .then(data => {
            if (data.code === 200) {
                isLiked = data.data;
                updateLikeButton();
            }
        })
        .catch(error => console.error('检查点赞状态失败:', error));
}

function incrementReadCount(blogId) {
    fetch(`/blogs/incrementRead/${blogId}`, {
        method: 'POST'
    })
    .then(function(response) {
        if (response.status === 401) {
            openAuthModal('login');
            showToast('请先登录后再操作', 'warning');
            throw new Error('unauthorized');
        }
        return response.json();
    })
    .then(data => {
        if (data.code === 200) {
            console.log('阅读量已更新');
        }
    })
    .catch(error => console.error('更新阅读量失败:', error));
}

// isProcessing 防抖守卫：防止异步请求期间重复点击导致重复提交
function handleLike() {
    if (isProcessing) {
        return;
    }
    
    const blogId = document.getElementById('getBlogId').value;
    if (!blogId) {
        showToast('无法获取博客ID', 'error');
        return;
    }
    
    isProcessing = true;
    const likeButton = document.getElementById('likeButton');
    likeButton.disabled = true;
    
    const url = isLiked ? `/blogs/unlike/${blogId}` : `/blogs/like/${blogId}`;
    
    fetch(url, {
        method: 'POST'
    })
    .then(function(response) {
        if (response.status === 401) {
            openAuthModal('login');
            showToast('请先登录后再操作', 'warning');
            throw new Error('unauthorized');
        }
        return response.json();
    })
    .then(data => {
        if (data.code === 200) {
            isLiked = !isLiked;
            if (data.data !== undefined) {
                document.getElementById('likeCount').textContent = data.data;
            }
            updateLikeButton();
        } else {
            showToast(data.message || '操作失败', 'error');
        }
    })
    .catch(error => {
        console.error('点赞操作失败:', error);
        if (error.message !== 'unauthorized') {
            showToast('操作失败，请重试', 'error');
        }
    })
    .finally(() => {
        isProcessing = false;
        likeButton.disabled = false;
    });
}

function updateLikeButton() {
    const likeIcon = document.getElementById('likeIcon');
    const likeButton = document.getElementById('likeButton');
    
    if (isLiked) {
        likeIcon.classList.remove('bi-hand-thumbs-up');
        likeIcon.classList.add('bi-hand-thumbs-up-fill');
        likeButton.classList.remove('btn-outline-primary');
        likeButton.classList.add('btn-primary');
    } else {
        likeIcon.classList.remove('bi-hand-thumbs-up-fill');
        likeIcon.classList.add('bi-hand-thumbs-up');
        likeButton.classList.remove('btn-primary');
        likeButton.classList.add('btn-outline-primary');
    }
}

// 三级分享降级策略：Web Share API → Clipboard API → execCommand('copy') 临时 textarea 兜底
function shareBlog() {
    const blogId = document.getElementById('getBlogId').value;
    const title = document.getElementById('textTitle').textContent;
    const shareUrl = window.location.href;
    
    if (navigator.share) {
        navigator.share({
            title: title,
            url: shareUrl
        }).catch(error => {
            console.log('分享失败:', error);
            fallbackShare(shareUrl);
        });
    } else {
        fallbackShare(shareUrl);
    }
}

function fallbackShare(url) {
    if (navigator.clipboard) {
        navigator.clipboard.writeText(url)
            .then(() => {
                showToast('链接已复制到剪贴板！', 'success');
            })
            .catch(error => {
                console.error('复制失败:', error);
                showToast('复制失败，请手动复制链接', 'error');
            });
    } else {
        const textarea = document.createElement('textarea');
        textarea.value = url;
        document.body.appendChild(textarea);
        textarea.select();
        try {
            document.execCommand('copy');
            showToast('链接已复制到剪贴板！', 'success');
        } catch (error) {
            console.error('复制失败:', error);
            showToast('复制失败，请手动复制链接', 'error');
        }
        document.body.removeChild(textarea);
    }
}