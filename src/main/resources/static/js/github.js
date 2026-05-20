// GitHub Profile 渲染：热力图、成就徽章、仓库列表（分页）
var githubUsername = '';
var MAX_RETRIES = 3;

// 带重试的 fetch 包装：遇到 502/503/504 等瞬时错误自动重试，含用户友好提示
function fetchWithRetry(url, maxRetries) {
    return new Promise(function(resolve, reject) {
        var attempt = 0;
        function tryOnce() {
            attempt++;
            fetch(url)
                .then(function(res) { return res.json(); })
                .then(function(data) {
                    if (data.code === 200) {
                        resolve(data);
                        return;
                    }
                    var retryable = /502|503|504|timeout|timed out|connection|refused/i.test(data.msg || '');
                    if (retryable && attempt < maxRetries) {
                        showToast('获取 GitHub 数据失败，正在重试 (' + attempt + '/' + maxRetries + ')...', 'info');
                        setTimeout(tryOnce, 800 * attempt);
                    } else if (attempt < maxRetries && !retryable) {
                        showToast('正在重试...', 'info');
                        setTimeout(tryOnce, 600);
                    } else {
                        resolve(data);
                    }
                })
                .catch(function(err) {
                    if (attempt < maxRetries) {
                        showToast('网络不稳定，正在重试 (' + attempt + '/' + maxRetries + ')...', 'info');
                        setTimeout(tryOnce, 800 * attempt);
                    } else {
                        reject(err);
                    }
                });
        }
        tryOnce();
    });
}

function loadGitHubProfile(username) {
    githubUsername = username;
    var loadingEl = document.getElementById('github-loading');
    var errorEl = document.getElementById('github-error');

    loadingEl.style.display = 'block';
    errorEl.style.display = 'none';

    fetchWithRetry('/api/github/' + encodeURIComponent(username) + '/profile', MAX_RETRIES)
    .then(function(data) {
        loadingEl.style.display = 'none';

        if (data.code !== 200) {
            errorEl.style.display = 'block';
            document.getElementById('github-error-msg').textContent = data.msg || '获取 GitHub 数据失败';
            showToast(data.msg || '获取 GitHub 数据失败', 'error');
            return;
        }

        var profile = data.data;

        // 热力图
        renderHeatmap(profile.heatmap, profile.totalContributions);
        // 成就
        renderAchievements(profile.achievements);
        // 仓库（分页加载）
        var card = document.getElementById('github-repos-card');
        card.style.display = 'block';
        document.getElementById('repos-skeleton').style.display = 'block';
        loadRepos(1);
    })
    .catch(function(err) {
        loadingEl.style.display = 'none';
        errorEl.style.display = 'block';
        document.getElementById('github-error-msg').textContent = '网络错误，请稍后再试';
        showToast('获取 GitHub 数据失败，请稍后重试', 'error');
    });
}

function retryGitHub() {
    if (githubUsername) {
        loadGitHubProfile(githubUsername);
    }
}

var DAY_LABELS = ['', 'Mon', '', 'Wed', '', 'Fri', ''];

function renderHeatmap(weeks, totalContributions) {
    var card = document.getElementById('github-heatmap-card');
    var skeleton = document.getElementById('heatmap-skeleton');
    var content = document.getElementById('heatmap-content');
    var error = document.getElementById('heatmap-error');
    var empty = document.getElementById('heatmap-empty');

    card.style.display = 'block';
    skeleton.style.display = 'none';

    if (!weeks || weeks.length === 0) {
        content.style.display = 'none';
        error.style.display = 'none';
        empty.style.display = 'block';
        return;
    }

    error.style.display = 'none';
    empty.style.display = 'none';
    content.style.display = 'block';

    // Hide total (estimated count is inaccurate)
    document.getElementById('github-total-contributions').style.display = 'none';

    var grid = document.getElementById('heatmap-grid');
    grid.innerHTML = '';

    var table = document.createElement('table');
    table.className = 'heatmap-table';

    // --- 月份标签行：遍历每周取首个有效日期的月份，通过与 prevMonth 比对检测月份边界 ---
    var months = ['1月','2月','3月','4月','5月','6月','7月','8月','9月','10月','11月','12月'];
    var monthSpans = [];
    var prevMonth = -1;
    for (var w = 0; w < weeks.length; w++) {
        var month = -1;
        for (var d = 0; d < 7; d++) {
            var dateStr = weeks[w] && weeks[w][d] ? weeks[w][d].date : '';
            if (dateStr) {
                month = parseInt(dateStr.split('-')[1]) - 1;
                break;
            }
        }
        if (month >= 0 && month !== prevMonth) {
            monthSpans.push({label: months[month], start: w});
            prevMonth = month;
        }
    }

    var monthRow = document.createElement('tr');
    var cornerCell = document.createElement('td');
    cornerCell.className = 'heatmap-corner';
    monthRow.appendChild(cornerCell);

    var monthCell = document.createElement('td');
    monthCell.colSpan = weeks.length;
    monthCell.className = 'heatmap-month-labels';
    var labelHtml = '';
    for (var i = 0; i < monthSpans.length; i++) {
        var span = monthSpans[i];
        var spanWeeks = (i + 1 < monthSpans.length ? monthSpans[i + 1].start : weeks.length) - span.start;
        labelHtml += '<span style="width:' + (spanWeeks * 14) + 'px; display: inline-block;">' + span.label + '</span>';
    }
    monthCell.innerHTML = labelHtml;
    monthRow.appendChild(monthCell);
    table.appendChild(monthRow);

    // --- Day rows ---
    for (var day = 0; day < 7; day++) {
        var tr = document.createElement('tr');
        var labelCell = document.createElement('td');
        labelCell.className = 'heatmap-day-label';
        labelCell.textContent = DAY_LABELS[day];
        tr.appendChild(labelCell);

        for (var w = 0; w < weeks.length; w++) {
            var td = document.createElement('td');
            td.className = 'heatmap-cell';
            if (weeks[w] && weeks[w][day]) {
                var dayData = weeks[w][day];
                var tip = dayData.date ? (dayData.count + ' contributions on ' + dayData.date) : '';
                td.title = tip;
                td.setAttribute('data-level', dayData.level);
                if (dayData.level > 0) {
                    td.style.backgroundColor = getHeatColor(dayData.level);
                }
            }
            tr.appendChild(td);
        }
        table.appendChild(tr);
    }

    grid.appendChild(table);

    // Legend inside grid so it flows with the table, not the card
    var legend = document.createElement('div');
    legend.className = 'heatmap-legend';
    legend.innerHTML =
        '<span style="font-size:0.72rem;color:rgba(255,255,255,0.4);">Less</span>' +
        '<span class="heatmap-cell" style="background:rgba(255,255,255,0.06);"></span>' +
        '<span class="heatmap-cell" style="background:#9be9a8;"></span>' +
        '<span class="heatmap-cell" style="background:#40c463;"></span>' +
        '<span class="heatmap-cell" style="background:#30a14e;"></span>' +
        '<span class="heatmap-cell" style="background:#216e39;"></span>' +
        '<span style="font-size:0.72rem;color:rgba(255,255,255,0.4);">More</span>';
    grid.appendChild(legend);
}

function getHeatColor(level) {
    switch(level) {
        case 1: return '#9be9a8';
        case 2: return '#40c463';
        case 3: return '#30a14e';
        case 4: return '#216e39';
        default: return 'rgba(255,255,255,0.06)';
    }
}

function renderAchievements(achievements) {
    var card = document.getElementById('github-achievements-card');
    var skeleton = document.getElementById('achievements-skeleton');
    var grid = document.getElementById('achievements-grid');
    var error = document.getElementById('achievements-error');
    var empty = document.getElementById('achievements-empty');

    card.style.display = 'block';
    skeleton.style.display = 'none';

    if (!achievements || achievements.length === 0) {
        grid.style.display = 'none';
        error.style.display = 'none';
        empty.style.display = 'block';
        return;
    }

    error.style.display = 'none';
    empty.style.display = 'none';
    grid.style.display = 'flex';
    grid.innerHTML = '';

    achievements.forEach(function(a) {
        var item = document.createElement('div');
        item.className = 'achievement-item';
        item.title = a.description || a.name;
        if (a.iconUrl) {
            var img = document.createElement('img');
            img.src = a.iconUrl;
            img.alt = a.name;
            img.className = 'achievement-icon';
            item.appendChild(img);
        }
        var nameSpan = document.createElement('span');
        nameSpan.className = 'achievement-name';
        nameSpan.textContent = a.name;
        item.appendChild(nameSpan);
        grid.appendChild(item);
    });
}

var repoPage = 1;
var repoPerPage = 10;
var repoHasMore = false;

function loadRepos(page) {
    repoPage = page;
    var list = document.getElementById('repos-list');
    var pagination = document.getElementById('repos-pagination');
    var skeleton = document.getElementById('repos-skeleton');
    var error = document.getElementById('repos-error');
    var empty = document.getElementById('repos-empty');

    skeleton.style.display = 'block';
    list.style.display = 'none';
    error.style.display = 'none';
    empty.style.display = 'none';

    fetch('/api/github/' + encodeURIComponent(githubUsername) + '/repos?page=' + page + '&perPage=' + repoPerPage)
    .then(function(res) { return res.json(); })
    .then(function(data) {
        skeleton.style.display = 'none';

        if (data.code !== 200) {
            error.style.display = 'block';
            list.style.display = 'none';
            empty.style.display = 'none';
            pagination.innerHTML = '';
            return;
        }

        var repos = data.data;
        repoHasMore = repos.length === repoPerPage;

        if (!repos || repos.length === 0) {
            empty.style.display = 'block';
            list.style.display = 'none';
            error.style.display = 'none';
            pagination.innerHTML = '';
            return;
        }

        error.style.display = 'none';
        empty.style.display = 'none';
        list.style.display = 'block';
        list.innerHTML = '';

        repos.forEach(function(repo) {
            var item = document.createElement('a');
            item.href = repo.htmlUrl;
            item.target = '_blank';
            item.rel = 'noopener';
            item.className = 'repo-item';
            item.innerHTML =
                '<div class="repo-header">' +
                    '<i class="bi bi-journal-code" style="color: #87CEEB; margin-right: 6px;"></i>' +
                    '<span class="repo-name">' + escapeHtml(repo.name) + '</span>' +
                    '<span class="repo-meta">' +
                        (repo.language ? '<span class="repo-lang-dot"></span><span class="repo-lang-text">' + escapeHtml(repo.language) + '</span>' : '') +
                        '<span><i class="bi bi-star-fill"></i> ' + repo.stargazersCount + '</span>' +
                        '<span><i class="bi bi-git"></i> ' + repo.forksCount + '</span>' +
                    '</span>' +
                '</div>' +
                (repo.description ? '<p class="repo-desc">' + escapeHtml(repo.description) + '</p>' : '');
            list.appendChild(item);
        });

        renderRepoPagination();
    })
    .catch(function() {
        skeleton.style.display = 'none';
        error.style.display = 'block';
        list.style.display = 'none';
        empty.style.display = 'none';
    });
}

function renderRepoPagination() {
    var pagination = document.getElementById('repos-pagination');
    if (!repoHasMore && repoPage <= 1) {
        pagination.innerHTML = '';
        return;
    }
    var html = '';
    html += '<button class="repo-page-btn" ' + (repoPage <= 1 ? 'disabled' : '') + ' onclick="loadRepos(' + (repoPage - 1) + ')">‹ 上一页</button>';
    html += '<span class="repo-page-info">第 ' + repoPage + ' 页</span>';
    html += '<button class="repo-page-btn" ' + (!repoHasMore ? 'disabled' : '') + ' onclick="loadRepos(' + (repoPage + 1) + ')">下一页 ›</button>';
    pagination.innerHTML = html;
}

// XSS 防护：通过 DOM textContent 赋值进行 HTML 编码，防止仓库名/描述中的恶意标签注入
function escapeHtml(str) {
    var div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}
