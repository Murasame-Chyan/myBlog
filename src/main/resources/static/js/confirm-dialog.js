// 统一确认对话框：首次调用时惰性创建 DOM，返回 Promise<boolean>
// 使用方式：const ok = await showConfirmDialog('确定要执行此操作吗？');
// 或：showConfirmDialog('确认？').then(function(ok) { if (ok) { ... } });
var _confirmOverlay = null;
var _confirmMsgEl = null;
var _confirmResolve = null;

function showConfirmDialog(message) {
    return new Promise(function(resolve) {
        _confirmResolve = resolve;
        if (!_confirmOverlay) buildConfirmDialog();
        _confirmMsgEl.textContent = message;
        _confirmOverlay.classList.add('active');
        var confirmBtn = _confirmOverlay.querySelector('.confirm-dialog-btn-confirm');
        if (confirmBtn) setTimeout(function() { confirmBtn.focus(); }, 50);
    });
}

function buildConfirmDialog() {
    _confirmOverlay = document.createElement('div');
    _confirmOverlay.className = 'confirm-dialog-overlay';
    _confirmOverlay.addEventListener('click', function(e) {
        if (e.target === _confirmOverlay) resolveConfirm(false);
    });

    var modal = document.createElement('div');
    modal.className = 'confirm-dialog-modal';
    modal.innerHTML =
        '<div class="confirm-dialog-header">' +
            '<i class="bi bi-exclamation-triangle confirm-dialog-icon"></i>' +
            '<span>确认操作</span>' +
        '</div>' +
        '<div class="confirm-dialog-body">' +
            '<p class="confirm-dialog-message"></p>' +
        '</div>' +
        '<div class="confirm-dialog-footer">' +
            '<button type="button" class="confirm-dialog-btn confirm-dialog-btn-cancel">取消</button>' +
            '<button type="button" class="confirm-dialog-btn confirm-dialog-btn-confirm">确定</button>' +
        '</div>';

    _confirmOverlay.appendChild(modal);
    document.body.appendChild(_confirmOverlay);

    _confirmMsgEl = modal.querySelector('.confirm-dialog-message');

    modal.querySelector('.confirm-dialog-btn-cancel').addEventListener('click', function() {
        resolveConfirm(false);
    });
    modal.querySelector('.confirm-dialog-btn-confirm').addEventListener('click', function() {
        resolveConfirm(true);
    });

    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape' && _confirmOverlay.classList.contains('active')) {
            resolveConfirm(false);
        }
    });
}

function resolveConfirm(result) {
    if (_confirmOverlay) _confirmOverlay.classList.remove('active');
    if (_confirmResolve) {
        var resolve = _confirmResolve;
        _confirmResolve = null;
        resolve(result);
    }
}
