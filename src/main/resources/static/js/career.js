/**
 * 求职进度模块（MovingOn）前端脚本 —— P0-4 占位版本。
 *
 * 现在只做三件事，便于先验收页面样式：
 * 1. 「新增投递」打开抽屉（create 模式）；
 * 2. 关闭按钮 / 取消 / 遮罩 / ESC 关闭抽屉；
 * 3. 今日跟进 / 逾期跟进分组折叠。
 *
 * P0-5 会用完整的列表加载、筛选、分页、详情、时间线、快捷操作逻辑替换本文件。
 */
(function () {
    'use strict';

    var drawer = document.getElementById('detailDrawer');
    var mask = document.getElementById('drawerMask');
    var drawerTitle = document.getElementById('drawerTitle');

    function openDrawer(mode, title) {
        if (!drawer || !mask) return;
        if (drawerTitle) drawerTitle.textContent = title;
        drawer.classList.remove('mode-create', 'mode-detail');
        drawer.classList.add('mode-' + mode);
        drawer.hidden = false;
        mask.hidden = false;
        requestAnimationFrame(function () {
            drawer.classList.add('is-open');
            mask.classList.add('is-open');
        });
        document.body.classList.add('career-drawer-open');
    }

    function closeDrawer() {
        if (!drawer || !mask || drawer.hidden) return;
        drawer.classList.remove('is-open');
        mask.classList.remove('is-open');
        document.body.classList.remove('career-drawer-open');
        setTimeout(function () {
            drawer.hidden = true;
            mask.hidden = true;
        }, 280);
    }

    var btnNew = document.getElementById('btnNewApplication');
    if (btnNew) {
        btnNew.addEventListener('click', function () {
            openDrawer('create', '新增投递');
        });
    }

    ['btnCloseDrawer', 'btnCancelDrawer'].forEach(function (id) {
        var el = document.getElementById(id);
        if (el) el.addEventListener('click', closeDrawer);
    });
    if (mask) mask.addEventListener('click', closeDrawer);
    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape') closeDrawer();
    });

    // 分组折叠
    [['toggleDueToday', 'groupDueToday'], ['toggleOverdue', 'groupOverdue']].forEach(function (pair) {
        var btn = document.getElementById(pair[0]);
        var group = document.getElementById(pair[1]);
        if (!btn || !group) return;
        btn.addEventListener('click', function () {
            group.classList.toggle('collapsed');
            btn.setAttribute('aria-expanded', String(!group.classList.contains('collapsed')));
        });
    });

    window.CAREER_MODULE_READY = true;
})();
