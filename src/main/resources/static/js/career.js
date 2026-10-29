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
        total: 0,
        list: [],
        currentId: null,
        detail: null
    };

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
        }).catch(noop);
    }

    // ==================== 列表 ====================
    function syncFilters() {
        state.keyword = trimmedValueOf('keywordInput');
        state.status = valueOf('statusFilter') || '';
        state.channel = valueOf('channelFilter') || '';
        var overdueEl = $('overdueOnly');
        state.overdueOnly = !!(overdueEl && overdueEl.checked);
        state.sortBy = valueOf('sortBy') || '';
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
            '<td>' + escapeHtml(app.channelLabel || labelOf(CHANNEL_LABELS, app.channel)) + '</td>' +
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
            '<span>' + escapeHtml(app.channelLabel || labelOf(CHANNEL_LABELS, app.channel)) + '</span>' +
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
        setValue('appliedAtInput', todayIso());
        var meta = $('detailMeta');
        if (meta) {
            meta.innerHTML = '';
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
            metaItem('渠道', app.channelLabel || labelOf(CHANNEL_LABELS, app.channel)) +
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
            setValue('sortBy', '');
            var overdueEl = $('overdueOnly');
            if (overdueEl) {
                overdueEl.checked = false;
            }
            loadList(1);
        });
        on($('keywordInput'), 'keydown', function (event) {
            if (event.key === 'Enter') {
                event.preventDefault();
                loadList(1);
            }
        });
        ['statusFilter', 'channelFilter', 'sortBy'].forEach(function (id) {
            on($(id), 'change', function () {
                loadList(1);
            });
        });
        on($('overdueOnly'), 'change', function () {
            loadList(1);
        });

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
        loadStats();
        loadList(1);
        window.CAREER_MODULE_READY = true;
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
