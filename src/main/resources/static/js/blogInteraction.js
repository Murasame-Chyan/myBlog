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
    authFetch(`/blogs/isLiked/${blogId}`)
        .then(function(response) { return response.json(); })
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
        method: 'POST',
        credentials: 'same-origin'
    })
    .then(function(response) { return response.json(); })
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
    
    authFetch(url, {
        method: 'POST'
    })
    .then(function(response) { return response.json(); })
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
        showToast('操作失败，请重试', 'error');
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
        likeButton.classList.add('liked');
    } else {
        likeIcon.classList.remove('bi-hand-thumbs-up-fill');
        likeIcon.classList.add('bi-hand-thumbs-up');
        likeButton.classList.remove('liked');
    }
}

// 分享卡片弹窗
var _shareUrl = '';

function shareBlog() {
    var authorEl = document.querySelector('.author-link span');
    var avatarEl = document.querySelector('.author-link .author-avatar-mini');
    var timeEl = document.querySelector('.blog-meta-row span:nth-child(3) span');
    var titleEl = document.getElementById('textTitle');
    var contentEl = document.getElementById('textContent');
    var readsEl = document.querySelector('.blog-meta-row .bi-eye + span');
    var likesEl = document.getElementById('likeCount');

    document.getElementById('shareCardAvatar').src = avatarEl ? avatarEl.src : '/images/default-avatar.png';
    document.getElementById('shareCardAuthor').textContent = authorEl ? authorEl.textContent : '';
    document.getElementById('shareCardTime').textContent = timeEl ? timeEl.textContent : '';

    var titleDiv = document.getElementById('shareCardTitle');
    titleDiv.textContent = titleEl ? titleEl.textContent : '';
    titleDiv.style.display = '';

    var snippet = '';
    if (contentEl) {
        var text = contentEl.textContent.replace(/\s+/g, ' ').trim();
        snippet = text.length > 60 ? text.substring(0, 60) + '...' : text;
    }
    document.getElementById('shareCardSnippet').textContent = snippet || '暂无内容预览';

    document.getElementById('shareCardTypeLabel').textContent = '发布了';

    var stats = document.querySelector('.share-card-stats');
    stats.style.display = '';
    document.getElementById('shareCardReads').textContent = readsEl ? readsEl.textContent : '0';
    document.getElementById('shareCardLikes').textContent = likesEl ? likesEl.textContent : '0';

    _shareUrl = window.location.href;
    document.getElementById('shareCardLinkUrl').textContent = _shareUrl;
    generateQrCode(_shareUrl);
    document.getElementById('shareOverlay').classList.add('active');
    document.body.style.overflow = 'hidden';
}

function shareComment(data) {
    document.getElementById('shareCardAvatar').src = data.avatarUrl || '/images/default-avatar.png';
    document.getElementById('shareCardAuthor').textContent = data.authorName || '';
    document.getElementById('shareCardTime').textContent = data.time || '';

    document.getElementById('shareCardTitle').style.display = 'none';

    var div = document.createElement('div');
    div.textContent = data.content || '';
    var text = (div.textContent || div.innerText || '').replace(/\s+/g, ' ').trim();
    text = text.length > 80 ? text.substring(0, 80) + '...' : text;
    document.getElementById('shareCardSnippet').textContent = text || '暂无内容';

    document.getElementById('shareCardTypeLabel').textContent = '评论了';

    document.querySelector('.share-card-stats').style.display = 'none';

    _shareUrl = window.location.href;
    document.getElementById('shareCardLinkUrl').textContent = _shareUrl;
    generateQrCode(_shareUrl);
    document.getElementById('shareOverlay').classList.add('active');
    document.body.style.overflow = 'hidden';
}

function closeShareCard() {
    document.getElementById('shareOverlay').classList.remove('active');
    document.body.style.overflow = '';
}

function generateQrCode(url) {
    var container = document.getElementById('shareCardQr');
    container.innerHTML = '';
    if (typeof QRCode !== 'undefined') {
        new QRCode(container, {
            text: url,
            width: 80,
            height: 80,
            colorDark: '#1a1e2b',
            colorLight: '#ffffff',
            correctLevel: QRCode.CorrectLevel.L
        });
    }
}

function copyShareImage() {
    var card = document.querySelector('.share-card');
    if (!card) return;

    var imgs = card.querySelectorAll('img');
    var swaps = [];
    var loadPromises = [];

    imgs.forEach(function(img) {
        var src = img.src;
        if (src && (src.startsWith('http://') || src.startsWith('https://'))) {
            swaps.push({ img: img, orig: src });
            // 先注册监听器，再改 src，防止 load 事件在监听器就位前触发
            loadPromises.push(new Promise(function(resolve) {
                img.addEventListener('load', resolve, { once: true });
                img.addEventListener('error', resolve, { once: true });
            }));
        }
    });
    // 监听器全部就位后，统一切换 src
    swaps.forEach(function(item) {
        item.img.src = '/api/proxy-image?url=' + encodeURIComponent(item.orig);
    });

    Promise.all(loadPromises).then(function() {
        return html2canvas(card, {
            backgroundColor: null,
            scale: 2,
            onclone: function(clonedDoc) {
                var clonedOverlay = clonedDoc.getElementById('shareOverlay');
                var clonedCard = clonedDoc.querySelector('.share-card');
                if (clonedOverlay) {
                    clonedOverlay.style.setProperty('background', 'transparent', 'important');
                    clonedOverlay.style.setProperty('backdrop-filter', 'none', 'important');
                    clonedOverlay.style.setProperty('-webkit-backdrop-filter', 'none', 'important');
                }
                if (clonedCard) {
                    clonedCard.style.setProperty('background', 'linear-gradient(160deg, #1e2235 0%, #252838 100%)', 'important');
                    clonedCard.style.setProperty('backdrop-filter', 'none', 'important');
                    clonedCard.style.setProperty('-webkit-backdrop-filter', 'none', 'important');
                    clonedCard.style.setProperty('border', 'none', 'important');
                    clonedCard.style.setProperty('box-shadow', 'none', 'important');
                    var hdr = clonedCard.querySelector('.share-card-header');
                    var ftr = clonedCard.querySelector('.share-card-footer');
                    if (hdr) hdr.style.display = 'none';
                    if (ftr) ftr.style.display = 'none';
                    var includeLink = clonedDoc.getElementById('shareCardIncludeLink');
                    var linkRow = clonedCard.querySelector('.share-card-link-row');
                    if (linkRow && includeLink && !includeLink.checked) {
                        linkRow.style.display = 'none';
                    }
                }
            }
        }).then(function(canvas) {
            canvas.toBlob(function(blob) {
                if (!blob) { showToast('图片生成失败', 'error'); return; }
                try {
                    navigator.clipboard.write([
                        new ClipboardItem({ 'image/png': blob })
                    ]).then(function() {
                        showToast('分享图已复制到剪贴板！', 'success');
                    }).catch(function() {
                        showToast('复制失败，请长按保存', 'error');
                    });
                } catch(e) {
                    var a = document.createElement('a');
                    a.href = URL.createObjectURL(blob);
                    a.download = 'share-card.png';
                    a.click();
                    URL.revokeObjectURL(a.href);
                    showToast('已下载分享图', 'success');
                }
            }, 'image/png');
        }).catch(function() {
            showToast('图片生成失败', 'error');
        }).finally(function() {
            swaps.forEach(function(item) { item.img.src = item.orig; });
        });
    });
}

function copyShareLink() {
    var url = _shareUrl || window.location.href;
    if (navigator.clipboard) {
        navigator.clipboard.writeText(url).then(function() {
            showToast('链接已复制！', 'success');
        }).catch(function() {
            showToast('复制失败', 'error');
        });
    } else {
        var ta = document.createElement('textarea');
        ta.value = url;
        document.body.appendChild(ta);
        ta.select();
        try { document.execCommand('copy'); showToast('链接已复制！', 'success'); }
        catch(e) { showToast('复制失败', 'error'); }
        document.body.removeChild(ta);
    }
}

document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') closeShareCard();
});
document.getElementById('shareOverlay').addEventListener('click', function(e) {
    if (e.target === this) closeShareCard();
});