// 轻量级 toast 通知系统：自动消失、支持四种类型、防双击关闭
function showToast(msg, type) {
    type = type || 'info';
    var icons = { success: 'bi-check-circle-fill', error: 'bi-x-circle-fill', info: 'bi-info-circle-fill', warning: 'bi-exclamation-triangle-fill' };
    var icon = icons[type] || icons.info;

    // 惰性创建容器：首次调用 showToast 时才创建 #toastContainer，避免 DOM 闲置节点
    var container = document.getElementById('toastContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toastContainer';
        container.className = 'toast-container';
        document.body.appendChild(container);
    }

    var item = document.createElement('div');
    item.className = 'toast-item toast-' + type;
    item.innerHTML =
        '<i class="bi ' + icon + '"></i>' +
        '<span class="toast-msg">' + escapeHtmlToast(msg) + '</span>' +
        '<span class="toast-close" onclick="dismissToast(this.parentElement)">&times;</span>';

    container.appendChild(item);

    // error 类型 toast 存留时间更长 (2500ms vs 1000ms)，给予用户足够时间阅读错误信息
    var autoClose = type === 'error' ? 2500 : 1000;
    var timer = setTimeout(function() { dismissToast(item); }, autoClose);
    item._timer = timer;
}

// _dismissing 守卫：防止定时器到期自动关闭与用户点击关闭按钮同时触发（双重关闭）
function dismissToast(el) {
    if (el._dismissing) return;
    el._dismissing = true;
    clearTimeout(el._timer);
    el.classList.add('toast-out');
    setTimeout(function() {
        if (el.parentElement) el.parentElement.removeChild(el);
    }, 300);
}

// XSS 防护：利用 DOM textContent 赋值进行 HTML 编码，防止用户输入中的恶意脚本注入
function escapeHtmlToast(str) {
    var div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}
