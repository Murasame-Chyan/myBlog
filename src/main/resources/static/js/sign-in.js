(function () {
    "use strict";

    var widget = document.getElementById("signInWidget");
    if (!widget) return;

    var state = null;  // current status from server

    // Load status on page load
    fetch("/sign-in/status")
        .then(function (r) { return r.json(); })
        .then(function (d) {
            if (d.code === 200 && d.data) {
                state = d.data;
                render();
            }
        })
        .catch(function () {
            widget.querySelectorAll(".sign-in-loading").forEach(function (el) { el.remove(); });
            var errEl = document.createElement("div");
            errEl.className = "sign-in-error";
            errEl.textContent = "签到加载失败";
            widget.appendChild(errEl);
        });

    // 等级颜色映射
    function getLevelColor(level, isMax) {
        var colors = {
            expBar: ['', '#8BC34A', '#64B5F6', '#FFD54F', '#FFB74D', '#EF5350', '#F48FB1'],
            bg:    ['', 'linear-gradient(135deg, #8BC34A, #7CB342)', 'linear-gradient(135deg, #64B5F6, #42A5F5)',
                         'linear-gradient(135deg, #FFD54F, #FFCA28)', 'linear-gradient(135deg, #FFB74D, #FFA726)',
                         'linear-gradient(135deg, #EF5350, #F44336)', 'linear-gradient(135deg, #F48FB1, #EC407A)']
        };
        if (isMax) return { bar: '#CE93D8', bg: 'linear-gradient(135deg, #CE93D8, #AB47BC)' };
        var lv = Math.min(level, 6);
        return { bar: colors.expBar[lv], bg: colors.bg[lv] };
    }

    function render() {
        if (!state) return;

        // 移除加载中占位
        widget.querySelectorAll(".sign-in-loading, .sign-in-error").forEach(function (el) { el.remove(); });

        var titleEl = widget.querySelector(".sidebar-title");
        var headerHtml = '<i class="bi bi-calendar-check"></i> 每日签到';
        if (titleEl) titleEl.innerHTML = headerHtml;

        if (!state.loggedIn) {
            widget.querySelectorAll(".sign-in-body, .sign-in-error").forEach(function (el) { el.remove(); });
            var body = document.createElement("div");
            body.className = "sign-in-body sign-in-unauth";
            body.innerHTML = '<p class="sign-in-hint">登录后即可签到</p>' +
                '<button class="sign-in-btn" id="signInLoginBtn">登录</button>';
            widget.appendChild(body);
            document.getElementById("signInLoginBtn").addEventListener("click", function () {
                if (typeof openAuthModal === "function") openAuthModal("login");
            });
            return;
        }

        var levelColors = getLevelColor(state.currentLevel, state.isMaxLevel);
        var range = state.expForNextLevel - state.currentLevelExp;
        var pct = range > 0 ? Math.min(100, Math.round(state.expProgressInLevel / range * 100)) : 100;
        // 极小进度也给个 2% 最小可见宽度
        if (pct === 0 && state.expProgressInLevel > 0) pct = 2;

        var levelLabel = state.isMaxLevel ? 'LV6+' : ('LV.' + state.currentLevel);

        var html = '<div class="sign-in-body">' +
            '<span class="sign-in-lv-badge" style="background:' + levelColors.bg + '">' + levelLabel + '</span>' +
            '<div class="sign-in-progress-bar"><div class="sign-in-progress-fill" style="width:' + pct + '%;background:' + levelColors.bar + '"></div></div>' +
            '<div class="sign-in-progress-label">';

        if (state.isMaxLevel) {
            html += '已满级';
        } else {
            html += state.currentExp + ' / ' + state.expForNextLevel;
        }
        html += '</div>';

        if (state.signedToday) {
            html += '<div class="sign-in-done">' +
                '<span class="sign-in-letter">' + (state.todayLetter || "?") + '</span>' +
                '<div class="sign-in-consecutive">连续签到 <strong>' + state.consecutiveDays + '</strong> 天</div>' +
                '</div>';
        } else {
            html += '<button class="sign-in-btn" id="signInBtn">' +
                '<i class="bi bi-gift"></i> 立即签到' +
                '</button>';
            if (state.consecutiveDays > 1) {
                html += '<div class="sign-in-consecutive">连续 <strong>' + state.consecutiveDays + '</strong> 天</div>';
            }
        }

        if (state.canMakeup) {
            html += '<div class="sign-in-makeup">' +
                '<a href="/game" class="sign-in-makeup-link">' +
                '<i class="bi bi-controller"></i> 游玩小游戏可补签 (+3 EXP)' +
                '</a></div>';
        }

        html += '</div>';

        // 移除旧 body 再插入新的
        widget.querySelectorAll(".sign-in-body, .sign-in-error").forEach(function (el) { el.remove(); });
        var div = document.createElement("div");
        div.innerHTML = html;
        while (div.firstChild) widget.appendChild(div.firstChild);

        var btn = document.getElementById("signInBtn");
        if (btn) btn.addEventListener("click", handleSignIn);
    }

    function handleSignIn() {
        var btn = document.getElementById("signInBtn");
        if (btn) {
            btn.disabled = true;
            btn.textContent = "签到中...";
        }

        authFetch("/sign-in", { method: "POST" })
            .then(function (r) { return r.json(); })
            .then(function (d) {
                if (d.code === 200 && d.data) {
                    showSignInResult(d.data);
                } else {
                    if (d.code === 400 && d.msg) {
                        if (typeof showToast === "function") showToast(d.msg, "warning");
                    }
                    fetchStatus();
                }
            })
            .catch(function () {
                if (typeof showToast === "function") showToast("签到失败", "error");
                fetchStatus();
            });
    }

    function showSignInResult(data) {
        var overlay = document.createElement("div");
        overlay.className = "sign-in-result-overlay";
        overlay.innerHTML =
            '<div class="sign-in-result-card">' +
            '<div class="sign-in-result-letter" id="signInResultLetter">' + data.letter + '</div>' +
            '<div class="sign-in-result-exp">+' + data.totalExp + ' EXP</div>' +
            (data.bonusExp > 0 ? '<div class="sign-in-result-bonus">含连击加成 +' + data.bonusExp + '</div>' : '') +
            (data.leveledUp ? '<div class="sign-in-result-levelup">升级! LV.' + data.newLevel + '</div>' : '') +
            '<div class="sign-in-result-consec">连续签到 ' + data.consecutiveDays + ' 天</div>' +
            '<button class="sign-in-result-close" id="signInResultClose">确定</button>' +
            '</div>';
        document.body.appendChild(overlay);

        requestAnimationFrame(function () {
            var letterEl = document.getElementById("signInResultLetter");
            if (letterEl) letterEl.classList.add("bounce-in");
        });

        document.getElementById("signInResultClose").addEventListener("click", function () {
            overlay.classList.add("fade-out");
            setTimeout(function () { overlay.remove(); }, 300);
        });
        overlay.addEventListener("click", function (e) {
            if (e.target === overlay) {
                overlay.classList.add("fade-out");
                setTimeout(function () { overlay.remove(); }, 300);
            }
        });

        fetchStatus();
    }

    function fetchStatus() {
        fetch("/sign-in/status")
            .then(function (r) { return r.json(); })
            .then(function (d) {
                if (d.code === 200 && d.data) {
                    state = d.data;
                    render();
                }
            });
    }
})();
