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
    resetSendCodeBtn('sendCodeBtn');
    resetSendCodeBtn('resetSendCodeBtn');
}

function resetSendCodeBtn(btnId) {
    var btn = document.getElementById(btnId);
    if (btn) {
        btn.disabled = false;
        btn.textContent = '发送验证码';
        btn.classList.remove('counting');
    }
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
    var resetTab = document.getElementById('authTabReset');
    var loginPanel = document.getElementById('loginPanel');
    var registerPanel = document.getElementById('registerPanel');
    var resetPanel = document.getElementById('resetPanel');

    [loginTab, registerTab, resetTab].forEach(function(t) { if (t) t.classList.remove('active'); });
    [loginPanel, registerPanel, resetPanel].forEach(function(p) { if (p) p.classList.remove('active'); });

    if (tab === 'login') {
        if (loginTab) loginTab.classList.add('active');
        if (loginPanel) loginPanel.classList.add('active');
        refreshCaptcha();
    } else if (tab === 'register') {
        if (registerTab) registerTab.classList.add('active');
        if (registerPanel) registerPanel.classList.add('active');
    } else if (tab === 'reset') {
        if (resetTab) resetTab.classList.add('active');
        if (resetPanel) resetPanel.classList.add('active');
    }
    clearAuthErrors();
}

// 注意：登录验证码错误匹配依赖后端 msg 字符串关键字（'图形验证码'/'验证码'），与后端紧耦合
function handleLogin() {
    clearAuthErrors();
    var email = document.getElementById('loginEmail').value.trim();
    var password = document.getElementById('loginPassword').value;
    var captcha = document.getElementById('loginCaptcha').value.trim();

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
    if (!captcha) {
        showAuthError('loginPanel', 'loginCaptcha', '请输入图形验证码');
        return;
    }
    if (captcha.length !== 4) {
        showAuthError('loginPanel', 'loginCaptcha', '验证码为4位字符');
        return;
    }
    if (email.length > 255) {
        showAuthError('loginPanel', 'loginEmail', '邮箱不能超过255个字符');
        return;
    }
    if (password.length > 255) {
        showAuthError('loginPanel', 'loginPassword', '密码不能超过255个字符');
        return;
    }

    var formData = new URLSearchParams();
    formData.append('email', email);
    formData.append('password', password);
    formData.append('captchaCode', captcha);
    formData.append('captchaToken', currentCaptchaToken);

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
            var msg = data.msg || '登录失败';
            if (msg.indexOf('图形验证码') !== -1 || msg.indexOf('验证码') !== -1) {
                showAuthError('loginPanel', 'loginCaptcha', msg);
                refreshCaptcha();
            } else {
                showAuthError('loginPanel', 'loginPassword', msg);
            }
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
    var emailCode = document.getElementById('registerEmailCode').value.trim();

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
    if (!emailCode) {
        showAuthError('registerPanel', 'registerEmailCode', '请输入邮箱验证码');
        hasError = true;
    } else if (!/^\d{6}$/.test(emailCode)) {
        showAuthError('registerPanel', 'registerEmailCode', '验证码为6位数字');
        hasError = true;
    }
    if (nickname.length > 32) {
        showAuthError('registerPanel', 'registerNickname', '昵称不能超过32个字符');
        hasError = true;
    }
    if (email.length > 255) {
        showAuthError('registerPanel', 'registerEmail', '邮箱不能超过255个字符');
        hasError = true;
    }
    if (password.length > 255) {
        showAuthError('registerPanel', 'registerPassword', '密码不能超过255个字符');
        hasError = true;
    }
    if (hasError) return;

    var formData = new URLSearchParams();
    formData.append('email', email);
    formData.append('nickname', nickname);
    formData.append('password', password);
    formData.append('emailCode', emailCode);

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
            var msg = data.msg || '注册失败';
            if (msg.indexOf('邮箱验证码') !== -1 || msg.indexOf('验证码') !== -1) {
                showAuthError('registerPanel', 'registerEmailCode', msg);
            } else {
                showAuthError('registerPanel', 'registerEmail', msg);
            }
        }
    })
    .catch(function() {
        showAuthError('registerPanel', 'registerEmail', '网络错误，请稍后重试');
    });
}

// ===== 忘记密码 =====

function handleResetPassword() {
    clearAuthErrors();
    var email = document.getElementById('resetEmail').value.trim();
    var emailCode = document.getElementById('resetEmailCode').value.trim();
    var newPassword = document.getElementById('resetNewPassword').value;
    var confirmPassword = document.getElementById('resetConfirmPassword').value;

    var hasError = false;
    if (!email) {
        showAuthError('resetPanel', 'resetEmail', '请输入邮箱');
        hasError = true;
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        showAuthError('resetPanel', 'resetEmail', '邮箱格式不正确');
        hasError = true;
    }
    if (!emailCode) {
        showAuthError('resetPanel', 'resetEmailCode', '请输入邮箱验证码');
        hasError = true;
    } else if (!/^\d{6}$/.test(emailCode)) {
        showAuthError('resetPanel', 'resetEmailCode', '验证码为6位数字');
        hasError = true;
    }
    if (!newPassword) {
        showAuthError('resetPanel', 'resetNewPassword', '请输入新密码');
        hasError = true;
    } else if (newPassword.length < 6) {
        showAuthError('resetPanel', 'resetNewPassword', '密码至少6位');
        hasError = true;
    }
    if (newPassword !== confirmPassword) {
        showAuthError('resetPanel', 'resetConfirmPassword', '两次密码不一致');
        hasError = true;
    }
    if (hasError) return;

    var formData = new URLSearchParams();
    formData.append('email', email);
    formData.append('newPassword', newPassword);
    formData.append('emailCode', emailCode);

    fetch('/auth/reset-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: formData.toString()
    })
    .then(function(res) { return res.json(); })
    .then(function(data) {
        if (data.code === 200) {
            if (typeof showToast === 'function') {
                showToast('密码重置成功，请登录', 'success');
            }
            switchAuthTab('login');
        } else {
            var msg = data.msg || '重置失败';
            if (msg.indexOf('邮箱验证码') !== -1 || msg.indexOf('验证码') !== -1) {
                showAuthError('resetPanel', 'resetEmailCode', msg);
            } else {
                showAuthError('resetPanel', 'resetEmail', msg);
            }
        }
    })
    .catch(function() {
        showAuthError('resetPanel', 'resetEmail', '网络错误，请稍后重试');
    });
}

function sendResetCode() {
    var email = document.getElementById('resetEmail').value.trim();
    var btn = document.getElementById('resetSendCodeBtn');

    if (!email) {
        showAuthError('resetPanel', 'resetEmailCode', '请先输入邮箱地址');
        return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        showAuthError('resetPanel', 'resetEmailCode', '邮箱格式不正确');
        return;
    }

    btn.disabled = true;
    btn.textContent = '发送中...';

    var formData = new URLSearchParams();
    formData.append('email', email);

    fetch('/auth/send-reset-code', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: formData.toString()
    })
    .then(function(res) { return res.json(); })
    .then(function(data) {
        if (data.code === 200) {
            if (typeof showToast === 'function') {
                showToast('验证码已发送', 'success');
            }
            startResetSendCodeCountdown(60);
        } else {
            showAuthError('resetPanel', 'resetEmailCode', data.msg || '发送失败');
            btn.disabled = false;
            btn.textContent = '发送验证码';
        }
    })
    .catch(function() {
        showAuthError('resetPanel', 'resetEmailCode', '网络错误，请稍后重试');
        btn.disabled = false;
        btn.textContent = '发送验证码';
    });
}

function startResetSendCodeCountdown(seconds) {
    var btn = document.getElementById('resetSendCodeBtn');
    if (!btn) return;
    btn.classList.add('counting');
    var remaining = seconds;
    btn.textContent = remaining + 's 后重发';
    var timer = setInterval(function() {
        remaining--;
        if (remaining <= 0) {
            clearInterval(timer);
            btn.classList.remove('counting');
            btn.disabled = false;
            btn.textContent = '发送验证码';
        } else {
            btn.textContent = remaining + 's 后重发';
        }
    }, 1000);
}

// 注意：以下两套发送验证码/倒计时函数逻辑相同但分别服务于注册(/auth/send-code)和重置密码(/auth/send-reset-code)，无法合并因为它们请求不同 API 端点
// ===== 图形验证码（登录用） =====

var currentCaptchaToken = '';

function refreshCaptcha() {
    var img = document.getElementById('captchaImg');
    if (!img) return;
    fetch('/auth/captcha?t=' + Date.now())
    .then(function(res) {
        currentCaptchaToken = res.headers.get('X-Captcha-Token') || '';
        return res.blob();
    })
    .then(function(blob) {
        var oldUrl = img.src;
        img.src = URL.createObjectURL(blob);
        if (oldUrl && oldUrl.startsWith('blob:')) URL.revokeObjectURL(oldUrl);
    });
}

// ===== 邮箱验证码（注册用） =====

function sendVerificationCode() {
    var email = document.getElementById('registerEmail').value.trim();
    var btn = document.getElementById('sendCodeBtn');

    if (!email) {
        showAuthError('registerPanel', 'registerEmailCode', '请先输入邮箱地址');
        return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        showAuthError('registerPanel', 'registerEmailCode', '邮箱格式不正确');
        return;
    }

    btn.disabled = true;
    btn.textContent = '发送中...';

    var formData = new URLSearchParams();
    formData.append('email', email);

    fetch('/auth/send-code', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: formData.toString()
    })
    .then(function(res) { return res.json(); })
    .then(function(data) {
        if (data.code === 200) {
            if (typeof showToast === 'function') {
                showToast('验证码已发送', 'success');
            }
            startSendCodeCountdown(60);
        } else {
            showAuthError('registerPanel', 'registerEmailCode', data.msg || '发送失败');
            btn.disabled = false;
            btn.textContent = '发送验证码';
        }
    })
    .catch(function() {
        showAuthError('registerPanel', 'registerEmailCode', '网络错误，请稍后重试');
        btn.disabled = false;
        btn.textContent = '发送验证码';
    });
}

function startSendCodeCountdown(seconds) {
    var btn = document.getElementById('sendCodeBtn');
    if (!btn) return;
    btn.classList.add('counting');
    var remaining = seconds;
    btn.textContent = remaining + 's 后重发';
    var timer = setInterval(function() {
        remaining--;
        if (remaining <= 0) {
            clearInterval(timer);
            btn.classList.remove('counting');
            btn.disabled = false;
            btn.textContent = '发送验证码';
        } else {
            btn.textContent = remaining + 's 后重发';
        }
    }, 1000);
}

// 点击遮罩层关闭
document.addEventListener('click', function(e) {
    if (e.target.id === 'authOverlay') {
        closeAuthModal();
    }
});

function handleLogout() {
    fetch('/auth/logout', { method: 'POST', credentials: 'same-origin' })
    .then(function(res) { return res.json(); })
    .then(function() { location.reload(); });
}

// 401 自动刷新：access_token 过期时自动调用 /auth/refresh 并重试原请求
// HttpOnly Cookie 自动携带，无需手动传 token
var _isRefreshing = false;
var _pendingRequests = [];

function authFetch(url, options) {
    options = options || {};
    options.credentials = 'same-origin';
    var isRetry = options._isRetry === true;
    delete options._isRetry;

    return fetch(url, options).then(function(response) {
        if (response.status !== 401 || isRetry) {
            return response;
        }
        // 收到 401，触发刷新
        if (_isRefreshing) {
            // 已有刷新进行中，加入队列等待
            return new Promise(function(resolve) {
                _pendingRequests.push(function(success) {
                    if (success) {
                        var retryOpts = Object.assign({}, options, { _isRetry: true });
                        resolve(authFetch(url, retryOpts));
                    } else {
                        resolve(response);
                    }
                });
            });
        }
        _isRefreshing = true;
        return fetch('/auth/refresh', { method: 'POST', credentials: 'same-origin' })
            .then(function(refreshResp) { return refreshResp.json(); })
            .then(function(data) {
                _isRefreshing = false;
                var success = data && data.code === 200;
                _pendingRequests.forEach(function(cb) { cb(success); });
                _pendingRequests = [];
                if (success) {
                    var retryOpts = Object.assign({}, options, { _isRetry: true });
                    return authFetch(url, retryOpts);
                }
                // 刷新失败：弹出登录框
                if (typeof openAuthModal === 'function') {
                    openAuthModal('login');
                }
                return response;
            })
            .catch(function() {
                _isRefreshing = false;
                _pendingRequests.forEach(function(cb) { cb(false); });
                _pendingRequests = [];
                return response;
            });
    });
}

// 按 ESC 关闭
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        var overlay = document.getElementById('authOverlay');
        if (overlay && overlay.classList.contains('show')) {
            closeAuthModal();
        }
    }
});
