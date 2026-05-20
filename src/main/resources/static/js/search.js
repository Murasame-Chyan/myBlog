// 筛选菜单交互
var activeTimeSpan = 'all';
var activeSort = 'newest';

function toggleFilter() {
    var dropdown = document.getElementById('filterDropdown');
    dropdown.classList.toggle('show');
    updateActiveChips();
}

// Click outside to close
document.addEventListener('click', function(e) {
    var dropdown = document.getElementById('filterDropdown');
    var btn = document.getElementById('filterBtn');
    if (dropdown && !dropdown.contains(e.target) && e.target !== btn && !btn.contains(e.target)) {
        dropdown.classList.remove('show');
    }
});

function setTimeSpan(span) {
    activeTimeSpan = span;
    var from = document.getElementById('filterDateFrom');
    var to = document.getElementById('filterDateTo');
    from.value = '';
    to.value = '';

    var now = new Date();
    if (span === 'week') {
        to.value = formatDate(now);
        var weekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
        from.value = formatDate(weekAgo);
    } else if (span === 'month') {
        to.value = formatDate(now);
        var monthAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
        from.value = formatDate(monthAgo);
    } else if (span === 'year') {
        to.value = formatDate(now);
        var yearAgo = new Date(now.getTime() - 365 * 24 * 60 * 60 * 1000);
        from.value = formatDate(yearAgo);
    }
    updateActiveChips();
}

function applyCustomDate() {
    activeTimeSpan = 'custom';
    updateActiveChips();
}

function setSortBy(sort) {
    activeSort = sort;
    updateActiveChips();
}

function updateActiveChips() {
    document.querySelectorAll('.filter-chip[data-span]').forEach(function(el) {
        el.classList.toggle('active', el.dataset.span === activeTimeSpan);
    });
    document.querySelectorAll('.filter-chip[data-sort]').forEach(function(el) {
        el.classList.toggle('active', el.dataset.sort === activeSort);
    });
}

// 双层输入模式：visible 筛选控件 (filterDateFrom/filterDateTo) 仅用于 UI 交互，
// 实际表单提交数据写入隐藏的 searchDateFrom/searchDateTo/searchSortBy，分离 UI 层与表单数据层
function applyFilters() {
    var dateFrom = document.getElementById('filterDateFrom').value;
    var dateTo = document.getElementById('filterDateTo').value;

    document.getElementById('searchDateFrom').value = dateFrom;
    document.getElementById('searchDateTo').value = dateTo;
    document.getElementById('searchSortBy').value = activeSort === 'newest' ? '' : activeSort;
    document.getElementById('searchForm').submit();
}

function formatDate(date) {
    return date.getFullYear() + '-' +
        String(date.getMonth() + 1).padStart(2, '0') + '-' +
        String(date.getDate()).padStart(2, '0');
}

// 搜索框按回车时提交
document.getElementById('searchForm').addEventListener('submit', function(e) {
    var dateFrom = document.getElementById('filterDateFrom').value;
    var dateTo = document.getElementById('filterDateTo').value;
    document.getElementById('searchDateFrom').value = dateFrom || '';
    document.getElementById('searchDateTo').value = dateTo || '';
    document.getElementById('searchSortBy').value = activeSort === 'newest' ? '' : activeSort;
});

// 初始化：根据后端传回的值回显
(function() {
    var existingFrom = document.getElementById('searchDateFrom').value;
    var existingTo = document.getElementById('searchDateTo').value;
    var existingSort = document.getElementById('searchSortBy').value;

    if (existingFrom || existingTo) {
        document.getElementById('filterDateFrom').value = existingFrom;
        document.getElementById('filterDateTo').value = existingTo;
        activeTimeSpan = 'custom';
    }
    if (existingSort) {
        activeSort = existingSort;
    }
    updateActiveChips();
})();
