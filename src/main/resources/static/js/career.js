/**
 * 求职进度模块（MovingOn）前端交互
 *
 * 依赖（均由 header 片段 / 页面引入的全局函数）：
 *   - authFetch(url, options)     401 自动刷新并重试（来自 auth.js）
 *   - showToast(msg, type)        轻提示（来自 toast.js）
 *   - showConfirmDialog(message)  二次确认，返回 Promise<boolean>（来自 confirm-dialog.js）
 *
 * 约定：
 *   - 枚举中文标签从页面里服务端渲染好的 select 选项读取，前端不硬编码枚举；
 *   - 所有用户文本都经 escapeHtml 转义后再拼 HTML；
 *   - 任何写操作成功后统一 refreshAll()（列表 + 统计）。
 */
(function () {
    'use strict';

    // ==================== 状态 ====================
    var state = {
        page: 1,
        pageSize: 10,
        keyword: '',
        status: '',
        channel: '',
        overdueOnly: false,
        sortBy: '',
        sortOrder: 'desc',
        total: 0,
        list: [],
        currentId: null,
        detail: null,
        view: 'list',
        dragId: null
    };

    var lastStats = null;
    var statusChartInstance = null;
    var trendChartInstance = null;

    var STATUS_LABELS = {};
    var CHANNEL_LABELS = {};
    var PRIORITY_LABELS = {};
    var EVENT_LABELS = {};

    // ==================== 小工具 ====================
    function $(id) {
        return document.getElementById(id);
    }

    function on(el, type, handler) {
        if (el) {
            el.addEventListener(type, handler);
        }
    }

    function noop() {
    }

    function escapeHtml(value) {
        if (value === null || value === undefined) {
            return '';
        }
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function valueOf(id) {
        var el = $(id);
        return el ? el.value : '';
    }

    function trimmedValueOf(id) {
        return String(valueOf(id) || '').trim();
    }

    function setValue(id, value) {
        var el = $(id);
        if (el) {
            el.value = (value === null || value === undefined) ? '' : value;
        }
    }

    function setText(id, value) {
        var el = $(id);
        if (el) {
            el.textContent = (value === null || value === undefined) ? '0' : String(value);
        }
    }

    function toast(message, type) {
        if (typeof showToast === 'function') {
            showToast(message, type || 'success');
        }
    }

    function confirmAction(message) {
        if (typeof showConfirmDialog === 'function') {
            return showConfirmDialog(message);
        }
        return Promise.resolve(window.confirm(message));
    }

    function setBusy(el, busy) {
        if (!el) {
            return;
        }
        el.disabled = !!busy;
    }

    // ==================== URL 处理 ====================
    /** 只允许 http/https，其余（含 javascript: 等）一律不渲染成链接 */
    function safeUrl(url) {
        if (!url) {
            return '';
        }
        var value = String(url).trim();
        return /^https?:\/\//i.test(value) ? value : '';
    }

    /** 保存前归一化：没写协议的自动补 https:// */
    function normalizeUrl(url) {
        var value = String(url === null || url === undefined ? '' : url).trim();
        if (!value) {
            return null;
        }
        return /^https?:\/\//i.test(value) ? value : ('https://' + value);
    }

    /** 渠道展示：填了 JD 链接就渲染成可点击的外链，否则纯文本 */
    function channelHtml(app, withIcon) {
        var text = escapeHtml(app.channelLabel || labelOf(CHANNEL_LABELS, app.channel));
        var url = safeUrl(app.jobUrl);
        if (!url) {
            return text;
        }
        return '<a class="career-channel-link" href="' + escapeHtml(url) + '" target="_blank" rel="noopener noreferrer"' +
            ' title="打开 JD 链接">' + text + (withIcon === false ? '' : ' <i class="bi bi-box-arrow-up-right"></i>') + '</a>';
    }

    // ==================== 新增表单草稿（localStorage） ====================
    var DRAFT_KEY = 'career_application_draft';
    var draftTimer = null;

    function readDraft() {
        try {
            var raw = window.localStorage.getItem(DRAFT_KEY);
            if (!raw) {
                return null;
            }
            var data = JSON.parse(raw);
            return (data && typeof data === 'object') ? data : null;
        } catch (e) {
            return null;
        }
    }

    function collectDraft() {
        return {
            company: valueOf('companyInput'),
            position: valueOf('positionInput'),
            channel: valueOf('channelSelect'),
            location: valueOf('locationInput'),
            salaryRange: valueOf('salaryRangeInput'),
            priority: valueOf('prioritySelect'),
            appliedAt: valueOf('appliedAtInput'),
            followUpAt: valueOf('followUpAtInput'),
            nextAction: valueOf('nextActionInput'),
            contact: valueOf('contactInput'),
            resumeVersion: valueOf('resumeVersionInput'),
            jobUrl: valueOf('jobUrlInput'),
            notes: valueOf('notesInput'),
            status: valueOf('statusInput'),
            savedAt: Date.now()
        };
    }

    function showDraftBar() {
        var bar = $('draftBar');
        if (bar) {
            bar.classList.add('is-visible');
        }
    }

    function hideDraftBar() {
        var bar = $('draftBar');
        if (bar) {
            bar.classList.remove('is-visible');
        }
    }

    function saveDraft() {
        // 只在"新增"模式保存；编辑已有记录时不覆盖草稿
        if (state.currentId !== null) {
            return;
        }
        try {
            window.localStorage.setItem(DRAFT_KEY, JSON.stringify(collectDraft()));
        } catch (e) {
            return;
        }
        showDraftBar();
    }

    function saveDraftDebounced() {
        if (draftTimer) {
            clearTimeout(draftTimer);
        }
        draftTimer = setTimeout(saveDraft, 300);
    }

    function clearDraft() {
        try {
            window.localStorage.removeItem(DRAFT_KEY);
        } catch (e) {
            // localStorage 不可用时忽略
        }
        hideDraftBar();
    }

    function hasDraftContent(draft) {
        if (!draft) {
            return false;
        }
        var fields = ['company', 'position', 'jobUrl', 'nextAction', 'contact',
            'resumeVersion', 'notes', 'salaryRange', 'location'];
        for (var i = 0; i < fields.length; i++) {
            var value = draft[fields[i]];
            if (value !== null && value !== undefined && String(value).trim()) {
                return true;
            }
        }
        return false;
    }

    function applyDraft(draft) {
        setValue('companyInput', draft.company);
        setValue('positionInput', draft.position);
        setValue('channelSelect', draft.channel || 'other');
        setValue('locationInput', draft.location);
        setValue('salaryRangeInput', draft.salaryRange);
        setValue('prioritySelect', draft.priority || 'medium');
        setValue('appliedAtInput', draft.appliedAt);
        setValue('followUpAtInput', draft.followUpAt);
        setValue('nextActionInput', draft.nextAction);
        setValue('contactInput', draft.contact);
        setValue('resumeVersionInput', draft.resumeVersion);
        setValue('jobUrlInput', draft.jobUrl);
        setValue('notesInput', draft.notes);
        setValue('statusInput', draft.status || 'applied');
    }

    // ==================== 日期 ====================
    function fmtDate(value) {
        if (!value) {
            return '';
        }
        return String(value).slice(0, 10);
    }

    function fmtDateTime(value) {
        if (!value) {
            return '';
        }
        var text = String(value).replace('T', ' ');
        return text.length >= 16 ? text.slice(5, 16) : text;
    }

    function todayIso() {
        var now = new Date();
        var month = ('0' + (now.getMonth() + 1)).slice(-2);
        var day = ('0' + now.getDate()).slice(-2);
        return now.getFullYear() + '-' + month + '-' + day;
    }

    // ==================== 枚举标签（从服务端渲染的 select 读取） ====================
    function buildLabelMap(selectId) {
        var map = {};
        var select = $(selectId);
        if (!select) {
            return map;
        }
        Array.prototype.forEach.call(select.options, function (option) {
            if (option.value) {
                map[option.value] = String(option.textContent || '').trim();
            }
        });
        return map;
    }

    function fillLabelMaps() {
        STATUS_LABELS = buildLabelMap('statusFilter');
        CHANNEL_LABELS = buildLabelMap('channelFilter');
        PRIORITY_LABELS = buildLabelMap('prioritySelect');
        EVENT_LABELS = buildLabelMap('eventTypeSelect');
    }

    function labelOf(map, code) {
        if (!code) {
            return '';
        }
        return map[code] || code;
    }

    // ==================== 请求 ====================
    function request(path, options) {
        var opts = Object.assign({ headers: { 'Content-Type': 'application/json' } }, options || {});
        var doFetch = (typeof authFetch === 'function') ? authFetch : window.fetch.bind(window);
        return doFetch(path, opts).then(function (response) {
            return response.json().catch(function () {
                return {};
            }).then(function (body) {
                if (response.status === 401 || body.code === 401) {
                    toast('登录已过期，请重新登录', 'error');
                    throw new Error('unauthorized');
                }
                if (body.code !== 200) {
                    toast(body.msg || '操作失败', 'error');
                    throw new Error(body.msg || 'request failed');
                }
                return body.data;
            });
        });
    }

    function httpGet(path) {
        return request(path, { method: 'GET' });
    }

    function httpSend(path, method, payload) {
        var options = { method: method };
        if (payload) {
            options.body = JSON.stringify(payload);
        }
        return request(path, options);
    }

    // ==================== 统计卡 ====================
    function countOf(items, code) {
        if (!items || !items.length) {
            return 0;
        }
        for (var i = 0; i < items.length; i++) {
            if (items[i].code === code) {
                return items[i].count || 0;
            }
        }
        return 0;
    }

    function loadStats() {
        return httpGet('/api/career/stats').then(function (data) {
            setText('statTotal', data.total || 0);
            setText('statCommunicating', countOf(data.byStatus, 'communicating'));
            setText('statInterviewing', countOf(data.byStatus, 'interviewing'));
            setText('statOffer', countOf(data.byStatus, 'offer'));
            setText('statRejected', countOf(data.byStatus, 'rejected'));
            setText('statOverdue', data.overdue || 0);
            renderCharts(data);
        }).catch(noop);
    }

    // ==================== 看板视图 ====================
    var BOARD_STATUSES = ['to_apply', 'applied', 'communicating', 'written_test', 'interviewing', 'offer'];

    function applyView(view) {
        state.view = (view === 'board') ? 'board' : 'list';
        try {
            window.localStorage.setItem('career_view', state.view);
        } catch (e) {
            // localStorage 不可用时忽略
        }
        document.body.classList.toggle('career-board-mode', state.view === 'board');
        Array.prototype.forEach.call(document.querySelectorAll('#viewSwitch button[data-view]'), function (btn) {
            btn.classList.toggle('is-active', btn.getAttribute('data-view') === state.view);
        });
        var boardSection = $('boardSection');
        var listSection = $('listSection');
        var pagination = $('pagination');
        var totalText = $('listTotalText');
        if (boardSection) {
            boardSection.hidden = state.view !== 'board';
        }
        if (listSection) {
            listSection.hidden = state.view === 'board';
        }
        if (pagination) {
            pagination.style.display = state.view === 'board' ? 'none' : '';
        }
        if (totalText) {
            totalText.style.display = state.view === 'board' ? 'none' : '';
        }
        if (state.view === 'board') {
            loadBoard();
            requestAnimationFrame(updateBoardScrollbar);
        } else {
            loadList(state.page);
        }
    }

    function loadBoard() {
        syncFilters();
        var params = new URLSearchParams();
        params.set('page', '1');
        params.set('pageSize', '100');
        if (state.keyword) {
            params.set('keyword', state.keyword);
        }
        if (state.channel) {
            params.set('channel', state.channel);
        }
        if (state.overdueOnly) {
            params.set('overdue', '1');
        }
        return httpGet('/api/career/applications?' + params.toString()).then(function (data) {
            renderBoard(data.list || [], data.total || 0);
        }).catch(noop);
    }

    function buildBoardCard(app) {
        var card = document.createElement('div');
        card.className = 'career-board-card';
        card.setAttribute('draggable', 'true');
        card.setAttribute('data-id', String(app.id));
        card.setAttribute('data-status', app.status || '');
        var flags = '';
        if (app.overdue) {
            flags += '<span class="career-chip is-overdue">逾期</span>';
        } else if (app.dueToday) {
            flags += '<span class="career-chip is-today">今日</span>';
        }
        if (app.priority === 'high') {
            flags += '<span class="career-chip is-priority-high">高优</span>';
        }
        var days = (app.appliedDays === 0) ? '今天' : (app.appliedDays + ' 天');
        card.innerHTML =
            '<div class="career-board-company">' + escapeHtml(app.company) + '</div>' +
            '<div class="career-board-position">' + escapeHtml(app.position) + '</div>' +
            '<div class="career-board-meta">' +
            '<span>' + escapeHtml(fmtDate(app.appliedAt)) + ' · ' + escapeHtml(days) + '</span>' +
            (app.followUpAt ? '<span>跟进 ' + escapeHtml(fmtDate(app.followUpAt)) + '</span>' : '') +
            '</div>' +
            (flags ? '<div class="career-board-flags">' + flags + '</div>' : '');
        return card;
    }

    function renderBoard(list, total) {
        var box = $('boardColumns');
        if (!box) {
            return;
        }
        box.innerHTML = '';
        BOARD_STATUSES.forEach(function (status) {
            var column = document.createElement('div');
            column.className = 'career-board-col';
            column.setAttribute('data-status', status);
            column.innerHTML =
                '<div class="career-board-col-head"><span>' + escapeHtml(labelOf(STATUS_LABELS, status)) +
                '</span><span class="career-board-count">' +
                list.filter(function (app) { return app.status === status; }).length + '</span></div>' +
                '<div class="career-board-col-body" data-status="' + status + '"></div>';
            var body = column.querySelector('.career-board-col-body');
            var cards = list.filter(function (app) { return app.status === status; });
            if (!cards.length) {
                var empty = document.createElement('div');
                empty.className = 'career-board-empty';
                empty.textContent = '拖到这里';
                body.appendChild(empty);
            } else {
                cards.forEach(function (app) {
                    body.appendChild(buildBoardCard(app));
                });
            }
            box.appendChild(column);
        });
        var hint = $('boardHint');
        if (hint) {
            hint.textContent = '共 ' + total + ' 条未删除记录' +
                (total > 100 ? '（看板仅展示前 100 条）' : '') +
                '；已结束（拒绝 / 放弃 / 归档）的记录请在列表视图查看。拖拽卡片可修改状态。';
        }
        requestAnimationFrame(updateBoardScrollbar);
    }

    function moveCardToStatus(id, toStatus) {
        if (!id || !toStatus) {
            return Promise.resolve();
        }
        return httpSend('/api/career/applications/' + id + '/status', 'POST', {
            toStatus: toStatus,
            note: null
        }).then(function () {
            toast('已移动到「' + labelOf(STATUS_LABELS, toStatus) + '」');
            loadBoard();
            loadStats();
        }).catch(function () {
            loadBoard();
        });
    }

    // 看板自定义横向滚动条（隐藏原生滚动条，提供显式拖动条）
    var boardThumbLeft = 0;
    var boardDrag = null;

    function updateBoardScrollbar() {
        var wrap = $('boardColumns');
        var bar = $('boardScrollbar');
        var thumb = $('boardThumb');
        var boardSection = $('boardSection');
        if (!wrap || !bar || !thumb) {
            return;
        }
        if (boardSection && boardSection.hidden) {
            bar.hidden = true;
            return;
        }
        var maxScroll = wrap.scrollWidth - wrap.clientWidth;
        if (maxScroll <= 2) {
            bar.hidden = true;
            return;
        }
        bar.hidden = false;
        var trackWidth = bar.clientWidth;
        if (trackWidth <= 0) {
            return;
        }
        var thumbWidth = Math.max(36, Math.round(trackWidth * wrap.clientWidth / wrap.scrollWidth));
        thumbWidth = Math.min(thumbWidth, trackWidth);
        var maxLeft = Math.max(1, trackWidth - thumbWidth);
        boardThumbLeft = Math.round((wrap.scrollLeft / maxScroll) * maxLeft);
        thumb.style.width = thumbWidth + 'px';
        thumb.style.transform = 'translateX(' + boardThumbLeft + 'px)';
    }

    function scrollBoardToThumb() {
        var wrap = $('boardColumns');
        var bar = $('boardScrollbar');
        var thumb = $('boardThumb');
        if (!wrap || !bar || !thumb) {
            return;
        }
        var maxLeft = Math.max(1, bar.clientWidth - thumb.offsetWidth);
        var maxScroll = wrap.scrollWidth - wrap.clientWidth;
        wrap.scrollLeft = (boardThumbLeft / maxLeft) * maxScroll;
    }

    function onBoardThumbMove(event) {
        if (!boardDrag) {
            return;
        }
        var bar = $('boardScrollbar');
        var thumb = $('boardThumb');
        if (!bar || !thumb) {
            return;
        }
        var maxLeft = Math.max(1, bar.clientWidth - thumb.offsetWidth);
        boardThumbLeft = Math.max(0, Math.min(maxLeft, boardDrag.startLeft + (event.clientX - boardDrag.startX)));
        thumb.style.transform = 'translateX(' + boardThumbLeft + 'px)';
        scrollBoardToThumb();
    }

    function stopBoardThumbDrag() {
        boardDrag = null;
        document.removeEventListener('pointermove', onBoardThumbMove);
        document.removeEventListener('pointerup', stopBoardThumbDrag);
        document.removeEventListener('pointercancel', stopBoardThumbDrag);
    }

    function bindBoardScrollbar() {
        var wrap = $('boardColumns');
        var bar = $('boardScrollbar');
        var thumb = $('boardThumb');
        if (!wrap || !bar || !thumb) {
            return;
        }
        on(wrap, 'scroll', updateBoardScrollbar);
        on(window, 'resize', updateBoardScrollbar);
        on(bar, 'click', function (event) {
            if (event.target === thumb) {
                return;
            }
            var rect = bar.getBoundingClientRect();
            var maxLeft = Math.max(1, bar.clientWidth - thumb.offsetWidth);
            boardThumbLeft = Math.max(0, Math.min(maxLeft,
                event.clientX - rect.left - thumb.offsetWidth / 2));
            thumb.style.transform = 'translateX(' + boardThumbLeft + 'px)';
            scrollBoardToThumb();
        });
        on(thumb, 'pointerdown', function (event) {
            event.preventDefault();
            boardDrag = { startX: event.clientX, startLeft: boardThumbLeft };
            document.addEventListener('pointermove', onBoardThumbMove);
            document.addEventListener('pointerup', stopBoardThumbDrag);
            document.addEventListener('pointercancel', stopBoardThumbDrag);
        });
    }

    // ==================== 统计图表（ECharts，复用 creatorAnalytics 的同一 CDN） ====================
    function cssVar(name, fallback) {
        try {
            var value = window.getComputedStyle(document.documentElement).getPropertyValue(name).trim();
            return value || fallback;
        } catch (e) {
            return fallback;
        }
    }

    function chartTextColor() {
        return cssVar('--text-primary', '#e5e7eb');
    }

    function chartGridColor() {
        return cssVar('--divider', 'rgba(255,255,255,0.12)');
    }

    function emptyChartOption() {
        return {
            graphic: [{
                type: 'text',
                left: 'center',
                top: 'middle',
                style: { text: '暂无数据', fill: cssVar('--text-disabled', '#999999'), fontSize: 12 }
            }],
            series: []
        };
    }

    function renderStatusChart(stats) {
        var el = $('statusChart');
        if (!el || !window.echarts || typeof window.echarts.init !== 'function') {
            return;
        }
        if (!statusChartInstance) {
            statusChartInstance = window.echarts.init(el);
        }
        var items = (stats.byStatus || []).filter(function (item) {
            return item.count > 0;
        });
        if (!items.length) {
            statusChartInstance.setOption(emptyChartOption(), true);
            return;
        }
        var data = items.map(function (item) {
            return {
                name: item.label || item.code,
                value: item.count,
                itemStyle: { color: cssVar('--career-status-' + item.code, '#87CEEB') }
            };
        });
        statusChartInstance.setOption({
            tooltip: { trigger: 'item' },
            legend: {
                bottom: 0,
                icon: 'circle',
                itemWidth: 8,
                itemHeight: 8,
                textStyle: { color: chartTextColor(), fontSize: 11 }
            },
            series: [{
                type: 'pie',
                radius: ['42%', '66%'],
                center: ['50%', '44%'],
                avoidLabelOverlap: true,
                label: { show: false },
                data: data
            }]
        }, true);
    }

    function renderTrendChart(stats) {
        var el = $('trendChart');
        if (!el || !window.echarts || typeof window.echarts.init !== 'function') {
            return;
        }
        if (!trendChartInstance) {
            trendChartInstance = window.echarts.init(el);
        }
        var trend = stats.trend || [];
        if (!trend.length) {
            trendChartInstance.setOption(emptyChartOption(), true);
            return;
        }
        var accent = cssVar('--accent', '#87CEEB');
        var textColor = chartTextColor();
        var gridColor = chartGridColor();
        trendChartInstance.setOption({
            tooltip: { trigger: 'axis' },
            grid: { left: 8, right: 14, top: 16, bottom: 24, containLabel: true },
            xAxis: {
                type: 'category',
                boundaryGap: false,
                data: trend.map(function (item) {
                    return String(item.date).slice(5);
                }),
                axisLine: { lineStyle: { color: gridColor } },
                axisTick: { show: false },
                axisLabel: { color: textColor, fontSize: 10 }
            },
            yAxis: {
                type: 'value',
                minInterval: 1,
                splitLine: { lineStyle: { color: gridColor } },
                axisLabel: { color: textColor, fontSize: 10 }
            },
            series: [{
                type: 'line',
                smooth: true,
                symbol: 'circle',
                symbolSize: 4,
                data: trend.map(function (item) {
                    return item.count;
                }),
                lineStyle: { color: accent, width: 2 },
                itemStyle: { color: accent },
                areaStyle: { color: accent, opacity: 0.12 }
            }]
        }, true);
    }

    function markChartsUnavailable() {
        ['statusChart', 'trendChart'].forEach(function (id) {
            var el = $(id);
            if (!el || el.getAttribute('data-fallback')) {
                return;
            }
            el.setAttribute('data-fallback', '1');
            el.style.display = 'flex';
            el.style.alignItems = 'center';
            el.style.justifyContent = 'center';
            el.style.color = 'var(--text-disabled)';
            el.style.fontSize = '0.78rem';
            el.textContent = '图表库加载失败（请检查能否访问 cdn.jsdelivr.net）';
        });
    }

    function renderCharts(stats) {
        if (!stats) {
            return;
        }
        if (!window.echarts || typeof window.echarts.init !== 'function') {
            markChartsUnavailable();
            return;
        }
        lastStats = stats;
        renderStatusChart(stats);
        renderTrendChart(stats);
    }

    function watchChartTheme() {
        window.addEventListener('resize', function () {
            if (statusChartInstance) {
                statusChartInstance.resize();
            }
            if (trendChartInstance) {
                trendChartInstance.resize();
            }
        });
        if (!window.MutationObserver) {
            return;
        }
        new MutationObserver(function () {
            if (lastStats) {
                renderCharts(lastStats);
            }
        }).observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });
    }

    // ==================== 表头排序 / 筛选联动 ====================
    function updateSortIndicators() {
        var headers = document.querySelectorAll(".career-th-sort");
        Array.prototype.forEach.call(headers, function (th) {
            var field = th.getAttribute("data-sort");
            var icon = th.querySelector("i");
            th.classList.remove("sort-asc", "sort-desc");
            if (!icon) {
                return;
            }
            if (state.sortBy && field === state.sortBy) {
                var isAsc = state.sortOrder === "asc";
                th.classList.add(isAsc ? "sort-asc" : "sort-desc");
                icon.className = isAsc ? "bi bi-arrow-up" : "bi bi-arrow-down";
            } else {
                icon.className = "bi bi-arrow-down-up";
            }
        });
    }

    function handleSortClick(field) {
        if (!field) {
            return;
        }
        if (state.sortBy === field) {
            state.sortOrder = state.sortOrder === "asc" ? "desc" : "asc";
        } else {
            state.sortBy = field;
            state.sortOrder = "asc";
        }
        updateSortIndicators();
        loadList(1);
    }

    function syncFilterSelects() {
        var status = valueOf("statusFilter");
        var channel = valueOf("channelFilter");
        var statusMobile = $("statusFilterMobile");
        var channelMobile = $("channelFilterMobile");
        if (statusMobile) {
            statusMobile.value = status || "";
        }
        if (channelMobile) {
            channelMobile.value = channel || "";
        }
    }

    // ==================== 列表 ====================
    function syncFilters() {
        state.keyword = trimmedValueOf('keywordInput');
        state.status = valueOf('statusFilter') || '';
        state.channel = valueOf('channelFilter') || '';
        var overdueEl = $('overdueOnly');
        state.overdueOnly = !!(overdueEl && overdueEl.checked);
    }

    function buildQuery() {
        var params = new URLSearchParams();
        params.set('page', String(state.page));
        params.set('pageSize', String(state.pageSize));
        if (state.keyword) {
            params.set('keyword', state.keyword);
        }
        if (state.status) {
            params.set('status', state.status);
        }
        if (state.channel) {
            params.set('channel', state.channel);
        }
        if (state.overdueOnly) {
            params.set('overdue', '1');
        }
        if (state.sortBy) {
            params.set('sortBy', state.sortBy);
            params.set('sortOrder', state.sortOrder || 'desc');
        }
        return params.toString();
    }

    function setLoading(loading) {
        var el = $('listLoading');
        if (el) {
            el.hidden = !loading;
        }
    }

    function loadList(page) {
        if (state.view === 'board') {
            return loadBoard();
        }
        state.page = page || state.page || 1;
        syncFilters();
        setLoading(true);
        return httpGet('/api/career/applications?' + buildQuery()).then(function (data) {
            state.list = data.list || [];
            state.total = data.total || 0;
            renderTable();
            renderCards();
            renderGroups(data.dueTodayList || [], data.overdueList || []);
            renderPagination();
            renderTotalText();
            // 末页最后一条被删除后，自动回退到上一页
            if (!state.list.length && state.total > 0 && state.page > 1) {
                return loadList(state.page - 1);
            }
        }).catch(noop).then(function () {
            setLoading(false);
        });
    }

    function renderTotalText() {
        var el = $('listTotalText');
        if (!el) {
            return;
        }
        if (!state.total) {
            el.textContent = '';
            return;
        }
        var totalPages = Math.max(1, Math.ceil(state.total / state.pageSize));
        el.textContent = '共 ' + state.total + ' 条记录 · 第 ' + state.page + ' / ' + totalPages + ' 页';
    }

    function statusBadgeHtml(app) {
        var status = escapeHtml(app.status || '');
        var label = escapeHtml(app.statusLabel || labelOf(STATUS_LABELS, app.status));
        var html = '<span class="career-status" data-status="' + status + '">' + label + '</span>';
        if (app.overdue) {
            html += ' <span class="career-chip is-overdue">逾期</span>';
        } else if (app.dueToday) {
            html += ' <span class="career-chip is-today">今日</span>';
        }
        if (app.priority === 'high') {
            html += ' <span class="career-chip is-priority-high">高优</span>';
        }
        return html;
    }

    function followDateClass(app) {
        if (app.overdue) {
            return 'career-cell-date is-overdue';
        }
        if (app.dueToday) {
            return 'career-cell-date is-today';
        }
        return 'career-cell-date';
    }

    function rowHtml(app) {
        var rowClass = app.overdue ? 'career-row-overdue' : (app.dueToday ? 'career-row-duetoday' : '');
        var days = (app.appliedDays === 0) ? '今天' : (app.appliedDays + ' 天');
        var location = app.location ? ' · ' + escapeHtml(app.location) : '';
        return '<td class="career-cell-main">' +
            '<div class="career-cell-company">' + escapeHtml(app.company) + '</div>' +
            '<div class="career-cell-position">' + escapeHtml(app.position) + location + '</div>' +
            '</td>' +
            '<td>' + channelHtml(app) + '</td>' +
            '<td>' + statusBadgeHtml(app) + '</td>' +
            '<td class="career-cell-date">' + escapeHtml(fmtDate(app.appliedAt)) + '</td>' +
            '<td class="' + followDateClass(app) + '">' + (app.followUpAt ? escapeHtml(fmtDate(app.followUpAt)) : '—') + '</td>' +
            '<td class="career-days">' + escapeHtml(days) + '</td>' +
            '<td class="career-td-actions">' +
            '<button type="button" class="career-link-btn" data-action="detail">查看</button>' +
            '<button type="button" class="career-link-btn" data-action="follow">+7天</button>' +
            '<button type="button" class="career-link-btn is-danger" data-action="delete">删除</button>' +
            '</td>';
    }

    function renderTable() {
        var tbody = $('applicationTableBody');
        if (!tbody) {
            return;
        }
        Array.prototype.slice.call(tbody.querySelectorAll('tr[data-id]')).forEach(function (tr) {
            tr.parentNode.removeChild(tr);
        });
        var emptyRow = $('tableEmptyRow');
        if (!state.list.length) {
            if (emptyRow) {
                emptyRow.style.display = '';
            }
            return;
        }
        if (emptyRow) {
            emptyRow.style.display = 'none';
        }
        state.list.forEach(function (app) {
            var tr = document.createElement('tr');
            if (app.overdue) {
                tr.className = 'career-row-overdue';
            } else if (app.dueToday) {
                tr.className = 'career-row-duetoday';
            }
            tr.setAttribute('data-id', String(app.id));
            tr.innerHTML = rowHtml(app);
            tbody.appendChild(tr);
        });
    }

    function cardHtml(app) {
        var rowClass = app.overdue ? 'career-row-overdue' : (app.dueToday ? 'career-row-duetoday' : '');
        var days = (app.appliedDays === 0) ? '今天投递' : ('已投 ' + app.appliedDays + ' 天');
        return '<div class="career-card-item ' + rowClass + '" data-id="' + escapeHtml(app.id) + '">' +
            '<div class="career-card-top">' +
            '<div>' +
            '<div class="career-card-company">' + escapeHtml(app.company) + '</div>' +
            '<div class="career-card-position">' + escapeHtml(app.position) + '</div>' +
            '</div>' +
            '<div>' + statusBadgeHtml(app) + '</div>' +
            '</div>' +
            '<div class="career-card-meta">' +
            '<span>' + escapeHtml(fmtDate(app.appliedAt)) + '</span>' +
            '<span>' + escapeHtml(days) + '</span>' +
            '<span>' + channelHtml(app, false) + '</span>' +
            (app.followUpAt ? '<span>跟进 ' + escapeHtml(fmtDate(app.followUpAt)) + '</span>' : '') +
            '</div>' +
            '</div>';
    }

    function renderCards() {
        var box = $('applicationCardList');
        if (!box) {
            return;
        }
        Array.prototype.slice.call(box.querySelectorAll('.career-card-item')).forEach(function (item) {
            item.parentNode.removeChild(item);
        });
        var cardEmpty = $('cardEmpty');
        if (!state.list.length) {
            if (cardEmpty) {
                cardEmpty.style.display = '';
            }
            return;
        }
        if (cardEmpty) {
            cardEmpty.style.display = 'none';
        }
        state.list.forEach(function (app) {
            var wrap = document.createElement('div');
            wrap.innerHTML = cardHtml(app);
            box.appendChild(wrap.firstChild);
        });
    }

    function miniItemHtml(app) {
        return '<div class="career-mini-item" data-id="' + escapeHtml(app.id) + '">' +
            '<div class="career-mini-main">' +
            '<div class="career-mini-title">' + escapeHtml(app.company) + ' · ' + escapeHtml(app.position) + '</div>' +
            '<div class="career-mini-sub">' + escapeHtml(app.channelLabel || labelOf(CHANNEL_LABELS, app.channel)) +
            (app.nextAction ? ' · ' + escapeHtml(app.nextAction) : '') + '</div>' +
            '</div>' +
            '<div class="career-mini-date">' + escapeHtml(fmtDate(app.followUpAt)) + '</div>' +
            '</div>';
    }

    function renderGroups(dueTodayList, overdueList) {
        setText('dueTodayCount', dueTodayList.length);
        setText('overdueCount', overdueList.length);
        renderMiniList('dueTodayList', dueTodayList, 'dueTodayEmpty');
        renderMiniList('overdueList', overdueList, 'overdueEmpty');
    }

    function renderMiniList(containerId, items, emptyId) {
        var box = $(containerId);
        if (!box) {
            return;
        }
        box.innerHTML = '';
        var empty = $(emptyId);
        if (!items || !items.length) {
            if (empty) {
                empty.style.display = '';
            }
            return;
        }
        if (empty) {
            empty.style.display = 'none';
        }
        items.forEach(function (app) {
            var wrap = document.createElement('div');
            wrap.innerHTML = miniItemHtml(app);
            box.appendChild(wrap.firstChild);
        });
    }

    function renderPagination() {
        var box = $('pagination');
        if (!box) {
            return;
        }
        box.innerHTML = '';
        if (!state.total) {
            return;
        }
        var totalPages = Math.max(1, Math.ceil(state.total / state.pageSize));
        if (totalPages <= 1) {
            return;
        }
        var pageButton = function (label, page, disabled, active) {
            var btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'career-page-btn' + (active ? ' is-active' : '');
            btn.textContent = label;
            btn.setAttribute('data-page', String(page));
            btn.disabled = !!disabled;
            return btn;
        };
        box.appendChild(pageButton('上一页', state.page - 1, state.page <= 1, false));
        var start = Math.max(1, state.page - 2);
        var end = Math.min(totalPages, start + 4);
        start = Math.max(1, end - 4);
        for (var page = start; page <= end; page++) {
            box.appendChild(pageButton(String(page), page, false, page === state.page));
        }
        box.appendChild(pageButton('下一页', state.page + 1, state.page >= totalPages, false));
    }

    // ==================== 回收站（软删恢复） ====================
    function loadDeleted() {
        return httpGet('/api/career/applications/deleted').then(function (data) {
            renderDeleted(data || []);
        }).catch(noop);
    }

    function renderDeleted(items) {
        var box = $('deletedList');
        var empty = $('deletedEmpty');
        if (!box) {
            return;
        }
        box.innerHTML = '';
        setText('deletedCount', items.length);
        if (empty) {
            empty.style.display = items.length ? 'none' : '';
        }
        if (!items.length) {
            return;
        }
        items.forEach(function (app) {
            var item = document.createElement('div');
            item.className = 'career-mini-item';
            item.setAttribute('data-id', String(app.id));
            item.innerHTML =
                '<div class="career-mini-main">' +
                '<div class="career-mini-title">' + escapeHtml(app.company) + ' · ' + escapeHtml(app.position) + '</div>' +
                '<div class="career-mini-sub">删除于 ' + escapeHtml(fmtDateTime(app.deletedAt)) +
                ' · ' + escapeHtml(app.statusLabel || labelOf(STATUS_LABELS, app.status)) + '</div>' +
                '</div>' +
                '<button type="button" class="career-link-btn" data-action="restore">恢复</button>';
            box.appendChild(item);
        });
    }

    function restoreById(id) {
        confirmAction('恢复这条投递记录？').then(function (ok) {
            if (!ok) {
                return;
            }
            return httpSend('/api/career/applications/' + id + '/restore', 'POST').then(function () {
                toast('已恢复');
                refreshAll();
                loadDeleted();
            });
        }).catch(noop);
    }

    // ==================== 抽屉 ====================
    function measureNavOffset() {
        var nav = document.querySelector('.navbar-custom');
        var offset = nav ? Math.max(0, Math.round(nav.getBoundingClientRect().bottom)) : 72;
        document.documentElement.style.setProperty('--career-nav-offset', offset + 'px');
    }

    function openDrawer(mode, title) {
        var drawer = $('detailDrawer');
        var mask = $('drawerMask');
        if (!drawer || !mask) {
            return;
        }
        measureNavOffset();
        drawer.classList.remove('mode-create', 'mode-detail');
        drawer.classList.add('mode-' + mode);
        if ($('drawerTitle')) {
            $('drawerTitle').textContent = title || '';
        }
        drawer.hidden = false;
        mask.hidden = false;
        requestAnimationFrame(function () {
            drawer.classList.add('is-open');
            mask.classList.add('is-open');
        });
        document.body.classList.add('career-drawer-open');
    }

    function closeDrawer() {
        var drawer = $('detailDrawer');
        var mask = $('drawerMask');
        if (!drawer || !mask || drawer.hidden) {
            return;
        }
        drawer.classList.remove('is-open');
        mask.classList.remove('is-open');
        document.body.classList.remove('career-drawer-open');
        setTimeout(function () {
            drawer.hidden = true;
            mask.hidden = true;
        }, 280);
        state.currentId = null;
        state.detail = null;
    }

    function resetForm() {
        ['companyInput', 'positionInput', 'locationInput', 'salaryRangeInput', 'nextActionInput',
            'contactInput', 'resumeVersionInput', 'jobUrlInput', 'notesInput', 'appliedAtInput',
            'followUpAtInput'].forEach(function (id) {
            setValue(id, '');
        });
        setValue('channelSelect', 'other');
        setValue('prioritySelect', 'medium');
        setValue('statusInput', 'applied');
    }

    function openCreate() {
        state.currentId = null;
        state.detail = null;
        resetForm();
        var meta = $('detailMeta');
        if (meta) {
            meta.innerHTML = '';
        }
        // 优先恢复上次草稿；没有草稿才默认投递日期=今天
        var draft = readDraft();
        if (hasDraftContent(draft)) {
            applyDraft(draft);
            showDraftBar();
        } else {
            setValue('appliedAtInput', todayIso());
            hideDraftBar();
        }
        openDrawer('create', '新增投递');
        setTimeout(function () {
            var company = $('companyInput');
            if (company) {
                company.focus();
            }
        }, 320);
    }

    function fillForm(app) {
        setValue('companyInput', app.company);
        setValue('positionInput', app.position);
        setValue('channelSelect', app.channel || 'other');
        setValue('appliedAtInput', app.appliedAt || '');
        setValue('statusInput', app.status || 'applied');
        setValue('locationInput', app.location);
        setValue('salaryRangeInput', app.salaryRange);
        setValue('prioritySelect', app.priority || 'medium');
        setValue('followUpAtInput', app.followUpAt || '');
        setValue('nextActionInput', app.nextAction);
        setValue('contactInput', app.contact);
        setValue('resumeVersionInput', app.resumeVersion);
        setValue('jobUrlInput', app.jobUrl);
        setValue('notesInput', app.notes);
    }

    function readForm() {
        return {
            company: trimmedValueOf('companyInput'),
            position: trimmedValueOf('positionInput'),
            channel: valueOf('channelSelect') || 'other',
            appliedAt: valueOf('appliedAtInput') || null,
            priority: valueOf('prioritySelect') || 'medium',
            location: trimmedValueOf('locationInput') || null,
            salaryRange: trimmedValueOf('salaryRangeInput') || null,
            resumeVersion: trimmedValueOf('resumeVersionInput') || null,
            contact: trimmedValueOf('contactInput') || null,
            nextAction: trimmedValueOf('nextActionInput') || null,
            followUpAt: valueOf('followUpAtInput') || null,
            jobUrl: normalizeUrl(valueOf('jobUrlInput')),
            notes: trimmedValueOf('notesInput') || null
        };
    }

    function renderMeta(app) {
        var box = $('detailMeta');
        if (!box) {
            return;
        }
        var metaItem = function (label, value) {
            return '<div class="career-meta-item"><span>' + escapeHtml(label) + '</span><strong>' +
                escapeHtml(value) + '</strong></div>';
        };
        var metaItemHtml = function (label, valueHtml) {
            return '<div class="career-meta-item"><span>' + escapeHtml(label) + '</span><strong>' +
                valueHtml + '</strong></div>';
        };
        box.innerHTML =
            '<div class="career-meta-head">' +
            '<span class="career-meta-company">' + escapeHtml(app.company) + '</span>' +
            '<span class="career-meta-position">' + escapeHtml(app.position) + '</span>' +
            statusBadgeHtml(app) +
            '</div>' +
            '<div class="career-meta-grid">' +
            metaItem('投递日期', fmtDate(app.appliedAt)) +
            metaItem('已投天数', (app.appliedDays || 0) + ' 天') +
            metaItem('当前状态停留', (app.statusDays || 0) + ' 天') +
            metaItem('跟进日期', app.followUpAt ? fmtDate(app.followUpAt) : '未设置') +
            metaItemHtml('渠道', channelHtml(app)) +
            metaItem('优先级', app.priorityLabel || labelOf(PRIORITY_LABELS, app.priority)) +
            metaItem('薪资', app.salaryRange || '—') +
            metaItem('下一步', app.nextAction || '—') +
            '</div>';
    }

    function renderTimeline(events) {
        var box = $('timelineList');
        var empty = $('timelineEmpty');
        if (!box) {
            return;
        }
        box.innerHTML = '';
        if (!events || !events.length) {
            if (empty) {
                empty.style.display = '';
            }
            return;
        }
        if (empty) {
            empty.style.display = 'none';
        }
        events.forEach(function (event) {
            var item = document.createElement('div');
            item.className = 'career-timeline-item';
            item.innerHTML =
                '<span class="career-timeline-dot" data-type="' + escapeHtml(event.type) + '"></span>' +
                '<div class="career-timeline-head">' +
                '<span class="career-timeline-title">' + escapeHtml(event.title) + '</span>' +
                '<span class="career-timeline-type">' + escapeHtml(event.typeLabel || labelOf(EVENT_LABELS, event.type)) + '</span>' +
                '<span class="career-timeline-time">' + escapeHtml(fmtDateTime(event.occurredAt)) + '</span>' +
                '</div>' +
                (event.content ? '<div class="career-timeline-content">' + escapeHtml(event.content) + '</div>' : '');
            box.appendChild(item);
        });
    }

    function renderDetail(app) {
        state.detail = app;
        fillForm(app);
        renderMeta(app);
        renderTimeline(app.events || []);
        setValue('statusActionSelect', app.status || '');
        setValue('statusNoteInput', '');
        resetEventForm();
    }

    function loadDetail(id) {
        return httpGet('/api/career/applications/' + id).then(function (data) {
            renderDetail(data);
            return data;
        });
    }

    function openDetail(id) {
        state.currentId = id;
        loadDetail(id).then(function (data) {
            openDrawer('detail', (data.company || '') + ' · ' + (data.position || ''));
        }).catch(function () {
            state.currentId = null;
        });
    }

    function refreshDetail() {
        if (!state.currentId) {
            return Promise.resolve();
        }
        return loadDetail(state.currentId).catch(noop);
    }

    function resetEventForm() {
        setValue('eventTitleInput', '');
        setValue('eventContentInput', '');
        setValue('eventMetaInput', '');
        setValue('eventOccurredAtInput', '');
        setValue('eventTypeSelect', 'note');
    }

    // ==================== 写入操作 ====================
    function handleSave(event) {
        if (event) {
            event.preventDefault();
        }
        var payload = readForm();
        if (!payload.company || !payload.position || !payload.appliedAt) {
            toast('公司、岗位、投递日期为必填项', 'warning');
            return;
        }
        var isCreate = state.currentId === null;
        if (isCreate) {
            payload.status = valueOf('statusInput') || 'applied';
        }
        var button = $('btnSaveApplication');
        setBusy(button, true);
        var promise = isCreate
            ? httpSend('/api/career/applications', 'POST', payload)
            : httpSend('/api/career/applications/' + state.currentId, 'PUT', payload);
        promise.then(function () {
            toast(isCreate ? '投递记录已创建' : '已保存');
            closeDrawer();
            refreshAll();
        }).catch(noop).then(function () {
            setBusy(button, false);
        });
    }

    function handleChangeStatus() {
        if (!state.currentId) {
            return;
        }
        var toStatus = valueOf('statusActionSelect');
        if (!toStatus) {
            toast('请选择目标状态', 'warning');
            return;
        }
        if (state.detail && toStatus === state.detail.status) {
            toast('状态未变化', 'info');
            return;
        }
        var button = $('btnChangeStatus');
        setBusy(button, true);
        httpSend('/api/career/applications/' + state.currentId + '/status', 'POST', {
            toStatus: toStatus,
            note: trimmedValueOf('statusNoteInput') || null
        }).then(function () {
            toast('状态已更新');
            return refreshDetail();
        }).then(function () {
            refreshAll();
        }).catch(noop).then(function () {
            setBusy(button, false);
        });
    }

    function applyStatus(toStatus, confirmMessage) {
        if (!state.currentId) {
            return;
        }
        confirmAction(confirmMessage).then(function (ok) {
            if (!ok) {
                return;
            }
            return httpSend('/api/career/applications/' + state.currentId + '/status', 'POST', {
                toStatus: toStatus,
                note: null
            }).then(function () {
                toast('状态已更新为 ' + labelOf(STATUS_LABELS, toStatus));
                return refreshDetail();
            }).then(function () {
                refreshAll();
            });
        }).catch(noop);
    }

    function handleFollowUp() {
        if (!state.currentId) {
            return;
        }
        var button = $('btnFollowUp');
        setBusy(button, true);
        httpSend('/api/career/applications/' + state.currentId + '/follow-up?days=7', 'POST')
            .then(function (nextDate) {
                toast('已设置跟进提醒：' + (nextDate ? fmtDate(nextDate) : '7 天后'));
                return refreshDetail();
            }).then(function () {
                refreshAll();
            }).catch(noop).then(function () {
                setBusy(button, false);
            });
    }

    function deleteById(id) {
        return confirmAction('删除后列表不再显示该记录（事件仍会保留），确认删除？').then(function (ok) {
            if (!ok) {
                return;
            }
            return httpSend('/api/career/applications/' + id, 'DELETE').then(function () {
                toast('已删除');
                if (state.currentId === id) {
                    closeDrawer();
                }
                refreshAll();
            });
        }).catch(noop);
    }

    function handleDelete() {
        if (state.currentId) {
            deleteById(state.currentId);
        }
    }

    function handleAddEvent(event) {
        if (event) {
            event.preventDefault();
        }
        if (!state.currentId) {
            return;
        }
        var title = trimmedValueOf('eventTitleInput');
        if (!title) {
            toast('事件标题不能为空', 'warning');
            return;
        }
        var payload = {
            type: valueOf('eventTypeSelect') || 'note',
            title: title,
            content: trimmedValueOf('eventContentInput') || null,
            meta: trimmedValueOf('eventMetaInput') || null
        };
        var occurredAt = valueOf('eventOccurredAtInput');
        if (occurredAt) {
            payload.occurredAt = occurredAt.length === 16 ? occurredAt + ':00' : occurredAt;
        }
        var button = $('btnAddEvent');
        setBusy(button, true);
        httpSend('/api/career/applications/' + state.currentId + '/events', 'POST', payload)
            .then(function () {
                toast('事件已添加');
                resetEventForm();
                return refreshDetail();
            }).then(function () {
                refreshAll();
            }).catch(noop).then(function () {
                setBusy(button, false);
            });
    }

    function handleRowAction(id, action) {
        if (action === 'detail') {
            openDetail(id);
        } else if (action === 'follow') {
            httpSend('/api/career/applications/' + id + '/follow-up?days=7', 'POST').then(function (nextDate) {
                toast('已设置跟进提醒：' + (nextDate ? fmtDate(nextDate) : '7 天后'));
                refreshAll();
            }).catch(noop);
        } else if (action === 'delete') {
            deleteById(id);
        }
    }

    function refreshAll() {
        loadStats();
        loadList(state.page);
        loadDeleted();
    }

    // ==================== 事件绑定 ====================
    function bindEvents() {
        // 抽屉开合
        on($('btnNewApplication'), 'click', openCreate);
        on($('btnCloseDrawer'), 'click', closeDrawer);
        on($('btnCancelDrawer'), 'click', closeDrawer);
        on($('drawerMask'), 'click', closeDrawer);
        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape') {
                closeDrawer();
            }
        });

        // 分组折叠
        [['toggleDueToday', 'groupDueToday'], ['toggleOverdue', 'groupOverdue']].forEach(function (pair) {
            var button = $(pair[0]);
            var group = $(pair[1]);
            if (!button || !group) {
                return;
            }
            on(button, 'click', function () {
                group.classList.toggle('collapsed');
                button.setAttribute('aria-expanded', String(!group.classList.contains('collapsed')));
            });
        });

        // 筛选
        on($('btnSearch'), 'click', function () {
            loadList(1);
        });
        on($('btnReset'), 'click', function () {
            setValue('keywordInput', '');
            setValue('statusFilter', '');
            setValue('channelFilter', '');
            var overdueEl = $('overdueOnly');
            if (overdueEl) {
                overdueEl.checked = false;
            }
            setValue('sortByMobile', '');
            state.sortBy = '';
            state.sortOrder = 'desc';
            syncFilterSelects();
            updateSortIndicators();
            loadList(1);
        });
        on($('keywordInput'), 'keydown', function (event) {
            if (event.key === 'Enter') {
                event.preventDefault();
                loadList(1);
            }
        });
        ['statusFilter', 'channelFilter'].forEach(function (id) {
            on($(id), 'change', function () {
                syncFilterSelects();
                loadList(1);
            });
        });
        ['statusFilterMobile', 'channelFilterMobile'].forEach(function (id) {
            on($(id), 'change', function () {
                var sourceId = (id === 'statusFilterMobile') ? 'statusFilter' : 'channelFilter';
                var source = $(sourceId);
                var current = $(id);
                if (source && current) {
                    source.value = current.value;
                }
                loadList(1);
            });
        });
        on($('sortByMobile'), 'change', function () {
            var value = valueOf('sortByMobile') || '';
            if (!value) {
                state.sortBy = '';
                state.sortOrder = 'desc';
            } else {
                var parts = value.split(':');
                state.sortBy = parts[0];
                state.sortOrder = parts[1] || 'desc';
            }
            updateSortIndicators();
            loadList(1);
        });
        Array.prototype.forEach.call(document.querySelectorAll('.career-th-sort'), function (th) {
            on(th, 'click', function () {
                handleSortClick(th.getAttribute('data-sort'));
            });
        });
        on($('overdueOnly'), 'change', function () {
            loadList(1);
        });

        // 视图切换
        on($('viewSwitch'), 'click', function (event) {
            var button = event.target.closest ? event.target.closest('button[data-view]') : null;
            if (button) {
                applyView(button.getAttribute('data-view'));
            }
        });

        // 看板拖拽
        var boardBox = $('boardColumns');
        if (boardBox) {
            on(boardBox, 'dragstart', function (event) {
                var card = event.target.closest ? event.target.closest('.career-board-card') : null;
                if (!card) {
                    return;
                }
                state.dragId = Number(card.getAttribute('data-id'));
                try {
                    event.dataTransfer.setData('text/plain', String(state.dragId));
                    event.dataTransfer.effectAllowed = 'move';
                } catch (e) {
                    // 忽略浏览器差异
                }
                card.classList.add('is-dragging');
            });
            on(boardBox, 'dragend', function () {
                Array.prototype.forEach.call(boardBox.querySelectorAll('.is-dragging'), function (card) {
                    card.classList.remove('is-dragging');
                });
                Array.prototype.forEach.call(boardBox.querySelectorAll('.is-over'), function (col) {
                    col.classList.remove('is-over');
                });
            });
            on(boardBox, 'dragover', function (event) {
                var body = event.target.closest ? event.target.closest('.career-board-col-body') : null;
                if (!body || !state.dragId) {
                    return;
                }
                event.preventDefault();
                try {
                    event.dataTransfer.dropEffect = 'move';
                } catch (e) {
                    // 忽略
                }
                body.parentNode.classList.add('is-over');
            });
            on(boardBox, 'dragleave', function (event) {
                var body = event.target.closest ? event.target.closest('.career-board-col-body') : null;
                if (body) {
                    body.parentNode.classList.remove('is-over');
                }
            });
            on(boardBox, 'drop', function (event) {
                var body = event.target.closest ? event.target.closest('.career-board-col-body') : null;
                if (!body) {
                    return;
                }
                event.preventDefault();
                Array.prototype.forEach.call(boardBox.querySelectorAll('.is-over'), function (col) {
                    col.classList.remove('is-over');
                });
                var toStatus = body.getAttribute('data-status');
                var id = state.dragId;
                state.dragId = null;
                moveCardToStatus(id, toStatus);
            });
            on(boardBox, 'click', function (event) {
                var card = event.target.closest ? event.target.closest('.career-board-card') : null;
                if (card) {
                    openDetail(Number(card.getAttribute('data-id')));
                }
            });
        }

        // 表格行操作（事件委托）
        on($('applicationTableBody'), 'click', function (event) {
            var button = event.target.closest ? event.target.closest('button[data-action]') : null;
            if (!button) {
                return;
            }
            var row = button.closest('tr[data-id]');
            if (!row) {
                return;
            }
            handleRowAction(Number(row.getAttribute('data-id')), button.getAttribute('data-action'));
        });

        // 移动端卡片点击
        on($('applicationCardList'), 'click', function (event) {
            var card = event.target.closest ? event.target.closest('.career-card-item[data-id]') : null;
            if (!card) {
                return;
            }
            openDetail(Number(card.getAttribute('data-id')));
        });

        // 今日/逾期分组点击
        ['dueTodayList', 'overdueList'].forEach(function (id) {
            on($(id), 'click', function (event) {
                var item = event.target.closest ? event.target.closest('.career-mini-item[data-id]') : null;
                if (item) {
                    openDetail(Number(item.getAttribute('data-id')));
                }
            });
        });

        // 分页
        on($('pagination'), 'click', function (event) {
            var button = event.target.closest ? event.target.closest('button[data-page]') : null;
            if (!button || button.disabled) {
                return;
            }
            loadList(Number(button.getAttribute('data-page')));
        });

        // 表单
        on($('applicationForm'), 'submit', handleSave);
        on($('eventForm'), 'submit', handleAddEvent);

        // 新增表单草稿：输入即保存（防抖），编辑模式不保存
        ['companyInput', 'positionInput', 'channelSelect', 'locationInput', 'salaryRangeInput',
            'prioritySelect', 'appliedAtInput', 'followUpAtInput', 'nextActionInput', 'contactInput',
            'resumeVersionInput', 'jobUrlInput', 'notesInput', 'statusInput'].forEach(function (id) {
            var el = $(id);
            if (!el) {
                return;
            }
            on(el, 'input', saveDraftDebounced);
            on(el, 'change', saveDraftDebounced);
        });
        on($('btnClearDraft'), 'click', function () {
            clearDraft();
            resetForm();
            setValue('appliedAtInput', todayIso());
            toast('草稿已清空', 'info');
        });

        // 回收站：折叠 + 恢复
        on($('toggleDeleted'), 'click', function () {
            var group = $('groupDeleted');
            if (!group) {
                return;
            }
            group.classList.toggle('collapsed');
            var button = $('toggleDeleted');
            if (button) {
                button.setAttribute('aria-expanded', String(!group.classList.contains('collapsed')));
            }
        });
        on($('deletedList'), 'click', function (event) {
            var button = event.target.closest ? event.target.closest('button[data-action="restore"]') : null;
            if (!button) {
                return;
            }
            var item = button.closest('.career-mini-item[data-id]');
            if (item) {
                restoreById(Number(item.getAttribute('data-id')));
            }
        });

        // 详情操作
        on($('btnChangeStatus'), 'click', handleChangeStatus);
        on($('btnFollowUp'), 'click', handleFollowUp);
        on($('btnReject'), 'click', function () {
            applyStatus('rejected', '确认把该投递标记为「拒绝」吗？');
        });
        on($('btnArchive'), 'click', function () {
            applyStatus('archived', '确认归档该投递吗？归档后不再参与今日/逾期跟进。');
        });
        on($('btnDelete'), 'click', handleDelete);
    }

    // ==================== 启动 ====================
    function init() {
        fillLabelMaps();
        bindEvents();
        bindBoardScrollbar();
        syncFilterSelects();
        updateSortIndicators();
        watchChartTheme();
        loadStats();
        var storedView = 'list';
        try {
            storedView = window.localStorage.getItem('career_view') || 'list';
        } catch (e) {
            storedView = 'list';
        }
        applyView(storedView);
        loadDeleted();
        window.CAREER_MODULE_READY = true;
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
