// ===== 认证模态窗交互 =====

function openAuthModal(tab) {
    var overlay = document.getElementById('authOverlay');
    if (overlay) {
        overlay.classList.add('show');
        switchAuthTab(tab || 'login');
    }
}

function closeAuthModal() {
    var overlay = document.getElementById('authOverlay');
    if (overlay) {
        overlay.classList.remove('show');
    }
    clearAuthErrors();
    clearAuthForms();
}

function clearAuthErrors() {
    document.querySelectorAll('.auth-error-msg').forEach(function(el) {
        el.style.display = 'none';
        el.textContent = '';
    });
}

function clearAuthForms() {
    document.querySelectorAll('.auth-form-input').forEach(function(el) {
        el.value = '';
    });
}

function showAuthError(panel, fieldId, message) {
    var panelEl = document.getElementById(panel);
    if (!panelEl) return;
    var errorEl = panelEl.querySelector('.auth-error-msg[data-field="' + fieldId + '"]');
    if (errorEl) {
        errorEl.textContent = message;
        errorEl.style.display = 'block';
    }
}

function switchAuthTab(tab) {
    var loginTab = document.getElementById('authTabLogin');
    var registerTab = document.getElementById('authTabRegister');
    var loginPanel = document.getElementById('loginPanel');
    var registerPanel = document.getElementById('registerPanel');

    if (tab === 'login') {
        loginTab.classList.add('active');
        registerTab.classList.remove('active');
        loginPanel.classList.add('active');
        registerPanel.classList.remove('active');
    } else {
        registerTab.classList.add('active');
        loginTab.classList.remove('active');
        registerPanel.classList.add('active');
        loginPanel.classList.remove('active');
    }
    clearAuthErrors();
}

function handleLogin() {
    clearAuthErrors();
    var email = document.getElementById('loginEmail').value.trim();
    var password = document.getElementById('loginPassword').value;

    if (!email) {
        showAuthError('loginPanel', 'loginEmail', '请输入邮箱');
        return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        showAuthError('loginPanel', 'loginEmail', '邮箱格式不正确');
        return;
    }
    if (!password) {
        showAuthError('loginPanel', 'loginPassword', '请输入密码');
        return;
    }

    var formData = new URLSearchParams();
    formData.append('email', email);
    formData.append('password', password);

    fetch('/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: formData.toString()
    })
    .then(function(res) { return res.json(); })
    .then(function(data) {
        if (data.code === 200) {
            location.reload();
        } else {
            showAuthError('loginPanel', 'loginPassword', data.msg || '登录失败');
        }
    })
    .catch(function() {
        showAuthError('loginPanel', 'loginPassword', '网络错误，请稍后重试');
    });
}

function handleRegister() {
    clearAuthErrors();
    var nickname = document.getElementById('registerNickname').value.trim();
    var email = document.getElementById('registerEmail').value.trim();
    var password = document.getElementById('registerPassword').value;
    var confirmPassword = document.getElementById('registerConfirmPassword').value;

    var hasError = false;
    if (!nickname) {
        showAuthError('registerPanel', 'registerNickname', '请输入昵称');
        hasError = true;
    }
    if (!email) {
        showAuthError('registerPanel', 'registerEmail', '请输入邮箱');
        hasError = true;
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        showAuthError('registerPanel', 'registerEmail', '邮箱格式不正确');
        hasError = true;
    }
    if (!password) {
        showAuthError('registerPanel', 'registerPassword', '请输入密码');
        hasError = true;
    } else if (password.length < 6) {
        showAuthError('registerPanel', 'registerPassword', '密码至少6位');
        hasError = true;
    }
    if (password !== confirmPassword) {
        showAuthError('registerPanel', 'registerConfirmPassword', '两次密码不一致');
        hasError = true;
    }
    if (hasError) return;

    var formData = new URLSearchParams();
    formData.append('email', email);
    formData.append('nickname', nickname);
    formData.append('password', password);

    fetch('/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: formData.toString()
    })
    .then(function(res) { return res.json(); })
    .then(function(data) {
        if (data.code === 200) {
            location.reload();
        } else {
            showAuthError('registerPanel', 'registerEmail', data.msg || '注册失败');
        }
    })
    .catch(function() {
        showAuthError('registerPanel', 'registerEmail', '网络错误，请稍后重试');
    });
}

// 点击遮罩层关闭
document.addEventListener('click', function(e) {
    if (e.target.id === 'authOverlay') {
        closeAuthModal();
    }
});

// 按 ESC 关闭
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        var overlay = document.getElementById('authOverlay');
        if (overlay && overlay.classList.contains('show')) {
            closeAuthModal();
        }
    }
});
