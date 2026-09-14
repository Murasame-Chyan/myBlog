// ===== 通知小铃铛 =====

// 加载未读通知数，更新徽标
function loadUnreadCount() {
    authFetch('/user/notification/unread-count')
        .then(function (r) { return r.json(); })
        .then(function (d) {
            if (d.code === 200) updateBadge(d.data);
        })
        .catch(function () {});
}

// 更新未读数徽标
function updateBadge(count) {
    var badge = document.getElementById('notificationBadge');
    if (!badge) return;
    if (count > 0) {
        badge.textContent = count > 99 ? '99+' : count;
        badge.style.display = 'flex';
    } else {
        badge.style.display = 'none';
    }
}

// 切换通知面板
function toggleNotification(e) {
    e.stopPropagation();
    var panel = document.getElementById('notificationPanel');
    if (panel.classList.contains('show')) {
        panel.classList.remove('show');
    } else {
        panel.classList.add('show');
        loadNotifications();
    }
}

// 加载通知列表
function loadNotifications() {
    var listEl = document.getElementById('notificationList');
    if (!listEl) return;
    authFetch('/user/notification/list')
        .then(function (r) { return r.json(); })
        .then(function (d) {
            if (d.code !== 200) return;
            var list = d.data || [];
            if (list.length === 0) {
                listEl.innerHTML = '<div class="notification-empty">暂无通知</div>';
                return;
            }
            listEl.innerHTML = list.map(function (n) {
                var cls = 'notification-item' + (n.isRead === 1 ? ' read' : '');
                var content = n.content ? '<div class="notification-content">' + escapeHtml(n.content) + '</div>' : '';
                return '<div class="' + cls + '" onclick="markNotificationRead(' + n.id + ')">' +
                    '<div class="notification-title">' + escapeHtml(n.title) + '</div>' +
                    content +
                    '<div class="notification-time">' + formatTime(n.createTime) + '</div>' +
                    '</div>';
            }).join('');
        })
        .catch(function () {});
}

// 标记单条已读
function markNotificationRead(id) {
    authFetch('/user/notification/read/' + id, { method: 'POST' })
        .then(function (r) { return r.json(); })
        .then(function () {
            loadUnreadCount();
            loadNotifications();
        })
        .catch(function () {});
}

// 全部已读
function markAllNotificationsRead() {
    authFetch('/user/notification/read-all', { method: 'POST' })
        .then(function (r) { return r.json(); })
        .then(function () {
            loadUnreadCount();
            loadNotifications();
        })
        .catch(function () {});
}

// 转义 HTML，防止通知内容注入 XSS
function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

// 格式化时间：后端 LocalDateTime 形如 "2026-09-06T15:30:00"
function formatTime(time) {
    if (!time) return '';
    return time.replace('T', ' ').substring(0, 16);
}

// 点击外部关闭通知面板
document.addEventListener('click', function (e) {
    var panel = document.getElementById('notificationPanel');
    var bell = document.getElementById('notificationBell');
    if (panel && panel.classList.contains('show') &&
        !panel.contains(e.target) && bell && !bell.contains(e.target)) {
        panel.classList.remove('show');
    }
});

// 页面加载时拉取未读数
document.addEventListener('DOMContentLoaded', loadUnreadCount);
