// 全局图表实例
let trendChart, hotBlogsChart, tagChart;
// 缓存完整数据引用
let allTagAnalytics = [];

// 页面加载
document.addEventListener('DOMContentLoaded', function() {
    // 初始化Bootstrap tooltip
    try {
        var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
        tooltipTriggerList.map(function(el) { return new bootstrap.Tooltip(el); });
    } catch (e) {}

    fetchAnalyticsData();
    loadUserTags();
    initDateFilter();
});

// 获取数据
function fetchAnalyticsData(days, startDate, endDate) {
    var url = '/creator/analytics/data';
    var params = [];
    if (days) params.push('days=' + days);
    if (startDate && endDate) {
        params.push('startDate=' + startDate);
        params.push('endDate=' + endDate);
    }
    if (params.length > 0) url += '?' + params.join('&');

    fetch(url)
        .then(function(res) { return res.json(); })
        .then(function(data) {
            if (data.code === 200) {
                renderData(data.data);
            } else {
                console.error('数据加载失败:', data.msg);
            }
        })
        .catch(function(err) {
            console.error('数据加载失败', err);
        });
}

// 渲染所有图表
function renderData(data) {
    if (!data) return;

    // 1. 核心数据看板
    setText('totalBlogs', formatNumber(data.totalBlogs));
    setText('totalReads', formatNumber(data.totalReads));
    setText('totalLikes', formatNumber(data.totalLikes));
    setText('totalComments', formatNumber(data.totalComments));

    // 2. 趋势折线图
    if (data.trendDates && data.trendDates.length > 0) {
        renderTrendChart(data);
    }

    // 3. 热门文章排行
    if (data.hotBlogs && data.hotBlogs.length > 0) {
        renderHotBlogsChart(data.hotBlogs);
    }

    // 4. 标签分析
    renderTagChart(data.tagAnalytics || []);

    // 5. 互动率
    renderInteractionChart(data.likeRate || 0, data.commentRate || 0);

    // 6. 发文热力图
    if (data.publishHeatmap && data.publishHeatmap.length > 0) {
        renderHeatmapChart(data.publishHeatmap);
    }
}

function setText(id, val) {
    var el = document.getElementById(id);
    if (el) el.textContent = val;
}

// 格式化数字
function formatNumber(num) {
    if (num == null) return '0';
    if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
    if (num >= 1000) return (num / 1000).toFixed(1) + 'k';
    return num.toString();
}

// ===== 日期筛选 =====
function initDateFilter() {
    var btns = document.querySelectorAll('.date-preset-btn');
    btns.forEach(function(btn) {
        btn.addEventListener('click', function() {
            btns.forEach(function(b) { b.classList.remove('active'); });
            this.classList.add('active');
            var sd = document.getElementById('startDate');
            var ed = document.getElementById('endDate');
            if (sd) sd.value = '';
            if (ed) ed.value = '';
            fetchAnalyticsData(parseInt(this.dataset.days));
        });
    });

    var applyBtn = document.getElementById('dateApplyBtn');
    if (applyBtn) {
        applyBtn.addEventListener('click', function() {
            var sv = document.getElementById('startDate').value;
            var ev = document.getElementById('endDate').value;
            if (sv && ev) {
                btns.forEach(function(b) { b.classList.remove('active'); });
                fetchAnalyticsData(null, sv, ev);
            }
        });
    }
}

// ===== 标签选择 =====
function loadUserTags() {
    fetch('/creator/analytics/tags')
        .then(function(res) { return res.json(); })
        .then(function(data) {
            if (data.code === 200 && data.data && data.data.length > 0) {
                renderTagChips(data.data);
            }
        })
        .catch(function() {});
}

function renderTagChips(tags) {
    var container = document.getElementById('tagSelectChips');
    if (!container) return;
    container.innerHTML = '';

    tags.forEach(function(tag) {
        var chip = document.createElement('label');
        chip.className = 'tag-select-chip selected';
        chip.innerHTML = '<input type="checkbox" value="' + tag.tagId + '" checked> ' + tag.tagName;
        chip.querySelector('input').addEventListener('change', updateTagChartBySelection);
        container.appendChild(chip);
    });

    // 绑定展开/收起 — 放在 tag-select-row 的 hint 旁边
    var area = document.getElementById('tagSelectArea');
    var hint = document.querySelector('#tagSelectArea .tag-select-hint');
    var expandBtn = document.getElementById('tagExpandBtn');
    if (!expandBtn) {
        expandBtn = document.createElement('span');
        expandBtn.id = 'tagExpandBtn';
        expandBtn.className = 'tag-expand-btn';
        expandBtn.textContent = '展开';
        expandBtn.addEventListener('click', function() {
            var isExpanded = area.classList.toggle('expanded');
            expandBtn.textContent = isExpanded ? '收起' : '展开';
        });
        if (hint) {
            hint.parentNode.insertBefore(expandBtn, hint);
        } else {
            area.appendChild(expandBtn);
        }
    }

    updateTagChartBySelection();

    document.getElementById('tagSelectAll').onclick = function() {
        container.querySelectorAll('input').forEach(function(cb) {
            cb.checked = true;
            cb.parentElement.classList.add('selected');
        });
        updateTagChartBySelection();
    };

    document.getElementById('tagSelectNone').onclick = function() {
        container.querySelectorAll('input').forEach(function(cb) {
            cb.checked = false;
            cb.parentElement.classList.remove('selected');
        });
        updateTagChartBySelection();
    };
}

function updateTagChartBySelection() {
    var selectedIds = [];
    document.querySelectorAll('#tagSelectChips input:checked').forEach(function(cb) {
        selectedIds.push(cb.value);
    });

    document.querySelectorAll('.tag-select-chip').forEach(function(chip) {
        var cb = chip.querySelector('input');
        if (cb && cb.checked) {
            chip.classList.add('selected');
        } else {
            chip.classList.remove('selected');
        }
    });

    if (selectedIds.length === 0) {
        renderTagChart([]);
        return;
    }

    fetch('/creator/analytics/data?tagIds=' + selectedIds.join(','))
        .then(function(res) { return res.json(); })
        .then(function(data) {
            if (data.code === 200) {
                renderTagChart(data.data.tagAnalytics || []);
            }
        });
}

// ===== 互动率进度条 =====
function renderInteractionChart(likeRate, commentRate) {
    var likeVal = (likeRate || 0).toFixed(2);
    var commentVal = (commentRate || 0).toFixed(2);

    setText('likeRateValue', likeVal);
    setText('commentRateValue', commentVal);

    var likeBar = document.getElementById('likeRateBar');
    var commentBar = document.getElementById('commentRateBar');
    if (likeBar) likeBar.style.width = Math.min(likeRate, 100) + '%';
    if (commentBar) commentBar.style.width = Math.min(commentRate, 100) + '%';
}

// ===== 趋势折线图 =====
function renderTrendChart(data) {
    var chartDom = document.getElementById('trendChart');
    if (!chartDom) return;
    if (!trendChart) {
        trendChart = echarts.init(chartDom);
    }

    var option = {
        backgroundColor: 'rgba(255, 255, 255, 0.05)',
        tooltip: {
            trigger: 'axis',
            backgroundColor: 'rgba(30, 35, 50, 0.95)',
            borderColor: 'rgba(135, 206, 235, 0.3)',
            textStyle: { color: '#fff' }
        },
        legend: {
            data: ['阅读量', '点赞数', '评论数'],
            textStyle: { color: '#ccc' },
            top: 10
        },
        grid: {
            left: '3%',
            right: '8%',
            bottom: '3%',
            top: 55,
            containLabel: true
        },
        xAxis: {
            type: 'category',
            data: data.trendDates,
            axisLine: { lineStyle: { color: 'rgba(255,255,255,0.2)' } },
            axisLabel: { color: 'rgba(255,255,255,0.5)', fontSize: 10, rotate: data.trendDates.length > 30 ? 30 : 0 }
        },
        yAxis: [
            {
                type: 'value',
                name: '阅读量',
                nameTextStyle: { color: '#f5576c', fontSize: 11 },
                splitLine: { lineStyle: { color: 'rgba(255,255,255,0.08)' } },
                axisLabel: { color: 'rgba(255,255,255,0.5)' }
            },
            {
                type: 'value',
                name: '互动',
                nameTextStyle: { color: '#00f2fe', fontSize: 11 },
                splitLine: { show: false },
                axisLabel: { color: 'rgba(255,255,255,0.5)' }
            }
        ],
        series: [
            {
                name: '阅读量',
                type: 'bar',
                data: data.trendReads,
                itemStyle: {
                    color: 'rgba(245, 87, 108, 0.6)',
                    borderRadius: [4, 4, 0, 0]
                },
                barWidth: '60%'
            },
            {
                name: '点赞数',
                type: 'line',
                yAxisIndex: 1,
                data: data.trendLikes,
                itemStyle: { color: '#00f2fe' },
                symbol: 'circle',
                symbolSize: 5,
                lineStyle: { width: 2.5 }
            },
            {
                name: '评论数',
                type: 'line',
                yAxisIndex: 1,
                data: data.trendComments,
                itemStyle: { color: '#fee140' },
                symbol: 'circle',
                symbolSize: 5,
                lineStyle: { width: 2.5 }
            }
        ]
    };

    trendChart.setOption(option, true);
    window.addEventListener('resize', function() { trendChart.resize(); });
}

// ===== 热门文章排行 =====
function renderHotBlogsChart(hotBlogs) {
    var chartDom = document.getElementById('hotBlogsChart');
    if (!chartDom) return;
    if (!hotBlogsChart) {
        hotBlogsChart = echarts.init(chartDom);
    }

    // 反转数组使得分高的在上面（ECharts bar chart的y轴从下到上）
    var reversed = hotBlogs.slice().reverse();
    var titles = reversed.map(function(b) {
        return b.title.length > 20 ? b.title.substring(0, 20) + '...' : b.title;
    });
    var scores = reversed.map(function(b) { return b.score; });

    var option = {
        backgroundColor: 'rgba(255, 255, 255, 0.05)',
        tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'shadow' },
            backgroundColor: 'rgba(30, 35, 50, 0.95)',
            borderColor: 'rgba(135, 206, 235, 0.3)',
            textStyle: { color: '#fff' },
            formatter: function(params) {
                var blog = reversed[params[0].dataIndex];
                if (!blog) return '';
                return '<strong>' + blog.title + '</strong><br/>' +
                       '阅读: ' + blog.readCount + '<br/>' +
                       '点赞: ' + blog.likeCount + '<br/>' +
                       '得分: ' + blog.score.toFixed(1);
            }
        },
        grid: {
            left: '3%',
            right: '4%',
            bottom: '3%',
            top: 20,
            containLabel: true
        },
        xAxis: {
            type: 'value',
            splitLine: { lineStyle: { color: 'rgba(255,255,255,0.08)' } },
            axisLabel: { color: 'rgba(255,255,255,0.5)' }
        },
        yAxis: {
            type: 'category',
            data: titles,
            axisLabel: { color: 'rgba(255,255,255,0.7)', fontSize: 11 }
        },
        series: [{
            name: '综合得分',
            type: 'bar',
            data: scores,
            itemStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                    { offset: 0, color: '#667eea' },
                    { offset: 1, color: '#764ba2' }
                ]),
                borderRadius: [0, 4, 4, 0]
            },
            barWidth: '55%'
        }]
    };

    hotBlogsChart.setOption(option, true);
    window.addEventListener('resize', function() { hotBlogsChart.resize(); });
}

// ===== 标签分析 =====
function renderTagChart(tagAnalytics) {
    var chartDom = document.getElementById('tagChart');
    if (!chartDom) return;
    if (!tagChart) {
        tagChart = echarts.init(chartDom);
    }

    if (!tagAnalytics || tagAnalytics.length === 0) {
        tagChart.setOption({
            backgroundColor: 'rgba(255, 255, 255, 0.05)',
            graphic: {
                type: 'text',
                left: 'center',
                top: 'center',
                style: {
                    text: '请选择标签以查看分析',
                    fill: 'rgba(255,255,255,0.3)',
                    fontSize: 14
                }
            }
        }, true);
        return;
    }

    var pieData = tagAnalytics.map(function(t) {
        return { name: t.tagName, value: t.blogCount, avgReads: t.avgReads };
    });

    var option = {
        backgroundColor: 'rgba(255, 255, 255, 0.05)',
        tooltip: {
            trigger: 'item',
            backgroundColor: 'rgba(30, 35, 50, 0.95)',
            borderColor: 'rgba(135, 206, 235, 0.3)',
            textStyle: { color: '#fff' },
            formatter: function(params) {
                var item = pieData[params.dataIndex];
                if (!item) return '';
                return item.name + '<br/>文章数: ' + item.value + '<br/>平均阅读: ' + item.avgReads.toFixed(1);
            }
        },
        legend: {
            type: 'scroll',
            orient: 'horizontal',
            bottom: 5,
            textStyle: { color: '#ccc', fontSize: 10 }
        },
        series: [{
            name: '标签分布',
            type: 'pie',
            radius: ['45%', '70%'],
            center: ['50%', '45%'],
            itemStyle: { borderRadius: 6, borderColor: 'rgba(20, 24, 38, 1)', borderWidth: 2 },
            label: {
                color: 'rgba(255,255,255,0.6)',
                fontSize: 10,
                formatter: '{b} ({c}, {d}%)'
            },
            data: pieData,
            emphasis: {
                itemStyle: {
                    shadowBlur: 10,
                    shadowOffsetX: 0,
                    shadowColor: 'rgba(0, 0, 0, 0.5)'
                }
            }
        }]
    };

    tagChart.setOption(option, true);
    window.addEventListener('resize', function() { tagChart.resize(); });
}

// ===== 发文热力图 =====
function renderHeatmapChart(publishHeatmap) {
    var svg = document.getElementById('heatmapChart');
    var tooltipEl = document.getElementById('heatmapTooltip');
    if (!svg) return;

    var CELL = 11;
    var GAP = 3;
    var ROWS = 7;
    var MONTHS = ['1月','2月','3月','4月','5月','6月','7月','8月','9月','10月','11月','12月'];
    var DAYS = ['', '一', '', '三', '', '五', ''];

    var today = new Date();
    var oneYearAgo = new Date(today);
    oneYearAgo.setDate(today.getDate() - 364);

    var dataMap = {};
    if (publishHeatmap) {
        publishHeatmap.forEach(function(item) {
            dataMap[item.date] = item.publishCount;
        });
    }

    var contributions = [];
    for (var d = new Date(oneYearAgo); d <= today; d.setDate(d.getDate() + 1)) {
        var ds = d.toISOString().split('T')[0];
        contributions.push({ date: ds, count: dataMap[ds] || 0 });
    }

    var weeks = Math.ceil(contributions.length / ROWS);
    var labelOffset = 28;
    var headerOffset = 16;
    var svgWidth = labelOffset + weeks * (CELL + GAP);
    var svgHeight = headerOffset + ROWS * (CELL + GAP);

    svg.setAttribute('viewBox', '0 0 ' + svgWidth + ' ' + svgHeight);
    svg.innerHTML = '';

    var ns = 'http://www.w3.org/2000/svg';

    // 月份标签
    var monthLabels = [];
    var lastMonth = -1;
    for (var w = 0; w < weeks; w++) {
        var idx = w * ROWS;
        if (idx < contributions.length) {
            var month = new Date(contributions[idx].date).getMonth();
            if (month !== lastMonth) {
                monthLabels.push({ label: MONTHS[month], x: labelOffset + w * (CELL + GAP) });
                lastMonth = month;
            }
        }
    }
    monthLabels.forEach(function(m) {
        var text = document.createElementNS(ns, 'text');
        text.setAttribute('x', m.x);
        text.setAttribute('y', 11);
        text.setAttribute('fill', 'rgba(255,255,255,0.5)');
        text.setAttribute('font-size', '10');
        text.setAttribute('font-family', 'sans-serif');
        text.textContent = m.label;
        svg.appendChild(text);
    });

    // 星期标签
    DAYS.forEach(function(d, i) {
        if (d) {
            var text = document.createElementNS(ns, 'text');
            text.setAttribute('x', 0);
            text.setAttribute('y', headerOffset + i * (CELL + GAP) + CELL - 1);
            text.setAttribute('fill', 'rgba(255,255,255,0.4)');
            text.setAttribute('font-size', '9');
            text.setAttribute('font-family', 'sans-serif');
            text.textContent = d;
            svg.appendChild(text);
        }
    });

    // 格子
    contributions.forEach(function(day, idx) {
        var col = Math.floor(idx / ROWS);
        var row = idx % ROWS;
        var rect = document.createElementNS(ns, 'rect');

        rect.setAttribute('x', labelOffset + col * (CELL + GAP));
        rect.setAttribute('y', headerOffset + row * (CELL + GAP));
        rect.setAttribute('width', CELL);
        rect.setAttribute('height', CELL);
        rect.setAttribute('rx', 2);
        rect.setAttribute('ry', 2);

        var opacity;
        if (day.count === 0) opacity = 0.1;
        else if (day.count === 1) opacity = 0.3;
        else if (day.count <= 3) opacity = 0.6;
        else if (day.count <= 5) opacity = 0.85;
        else opacity = 1;

        rect.setAttribute('fill', 'rgba(135, 206, 235, ' + opacity + ')');
        rect.style.transition = 'all 150ms ease';
        rect.style.cursor = 'pointer';
        rect.setAttribute('data-date', day.date);
        rect.setAttribute('data-count', day.count);
        svg.appendChild(rect);
    });

    // 事件委托
    var currentHoverRect = null;
    svg.removeEventListener('mouseover', svg._hoverHandler);
    svg.removeEventListener('mouseout', svg._outHandler);

    svg._hoverHandler = function(e) {
        var rect = e.target;
        if (rect.tagName === 'rect' && rect.hasAttribute('data-date')) {
            if (currentHoverRect === rect) return;
            currentHoverRect = rect;
            rect.style.opacity = '1';
            rect.style.filter = 'drop-shadow(0 2px 6px rgba(135, 206, 235, 0.6))';
            showTooltip(rect.getAttribute('data-date'), parseInt(rect.getAttribute('data-count')), rect);
        }
    };

    svg._outHandler = function(e) {
        var rect = e.target;
        if (rect.tagName === 'rect' && rect.hasAttribute('data-date')) {
            rect.style.opacity = '';
            rect.style.filter = '';
            currentHoverRect = null;
            hideTooltip();
        }
    };

    svg.addEventListener('mouseover', svg._hoverHandler);
    svg.addEventListener('mouseout', svg._outHandler);

    function showTooltip(date, count, element) {
        if (!tooltipEl) return;
        var d = new Date(date);
        var ds = d.getFullYear() + '年' + (d.getMonth() + 1) + '月' + d.getDate() + '日';
        var text = count > 0 ? ds + ': ' + count + ' 篇文章' : ds + ': 无发文';
        tooltipEl.textContent = text;
        tooltipEl.style.display = 'block';

        requestAnimationFrame(function() {
            var rect = element.getBoundingClientRect();
            var ttRect = tooltipEl.getBoundingClientRect();
            var x = rect.left + rect.width / 2 - ttRect.width / 2;
            var y = rect.top - ttRect.height - 8;
            x = Math.max(10, Math.min(x, window.innerWidth - ttRect.width - 10));
            if (y < 10) y = rect.bottom + 8;
            tooltipEl.style.left = x + 'px';
            tooltipEl.style.top = y + 'px';
        });
    }

    function hideTooltip() {
        if (tooltipEl) tooltipEl.style.display = 'none';
    }
}
