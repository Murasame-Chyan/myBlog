let isLiked = false;
let isProcessing = false;
let readCounted = false;

document.addEventListener('DOMContentLoaded', function() {
    const blogId = document.getElementById('getBlogId').value;
    if (blogId) {
        checkLikeStatus(blogId);
        
        setTimeout(() => {
            if (!readCounted) {
                incrementReadCount(blogId);
                readCounted = true;
            }
        }, 3000);
    }
});

function checkLikeStatus(blogId) {
    fetch(`/blogs/isLiked/${blogId}?userId=1`)
        .then(response => response.json())
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
    .then(response => response.json())
    .then(data => {
        if (data.code === 200) {
            console.log('阅读量已更新');
        }
    })
    .catch(error => console.error('更新阅读量失败:', error));
}

function handleLike() {
    if (isProcessing) {
        return;
    }
    
    const blogId = document.getElementById('getBlogId').value;
    if (!blogId) {
        alert('无法获取博客ID');
        return;
    }
    
    isProcessing = true;
    const likeButton = document.getElementById('likeButton');
    likeButton.disabled = true;
    
    const url = isLiked ? `/blogs/unlike/${blogId}` : `/blogs/like/${blogId}`;
    
    fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: `userId=1`
    })
    .then(response => response.json())
    .then(data => {
        if (data.code === 200) {
            isLiked = !isLiked;
            if (data.data !== undefined) {
                document.getElementById('likeCount').textContent = data.data;
            }
            updateLikeButton();
        } else {
            alert(data.message || '操作失败');
        }
    })
    .catch(error => {
        console.error('点赞操作失败:', error);
        alert('操作失败，请重试');
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
                alert('链接已复制到剪贴板！');
            })
            .catch(error => {
                console.error('复制失败:', error);
                alert('复制失败，请手动复制链接');
            });
    } else {
        const textarea = document.createElement('textarea');
        textarea.value = url;
        document.body.appendChild(textarea);
        textarea.select();
        try {
            document.execCommand('copy');
            alert('链接已复制到剪贴板！');
        } catch (error) {
            console.error('复制失败:', error);
            alert('复制失败，请手动复制链接');
        }
        document.body.removeChild(textarea);
    }
}