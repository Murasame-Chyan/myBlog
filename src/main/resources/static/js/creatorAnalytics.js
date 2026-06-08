// 全局图表实例（热力图改为原生SVG，不再使用echarts）
let trendChart, hotBlogsChart, tagChart;
// 缓存完整数据引用
let fullData = null;
let allTagAnalytics = [];

// 页面加载时获取数据
document.addEventListener('DOMContentLoaded', function() {
    // 初始化Bootstrap tooltip
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function(el) { return new bootstrap.Tooltip(el); });

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
        .then(res => res.json())
        .then(data => {
            if (data.code === 200) {
                fullData = data.data;
                renderData(data.data);
            } else {
                console.error('数据加载失败:', data.msg);
            }
        })
        .catch(err => {
            console.error('数据加载失败', err);
        });
}

// 渲染所有图表
function renderData(data) {
    // 1. 核心数据看板
    document.getElementById('totalBlogs').textContent = formatNumber(data.totalBlogs);
    document.getElementById('totalReads').textContent = formatNumber(data.totalReads);
    document.getElementById('totalLikes').textContent = formatNumber(data.totalLikes);
    document.getElementById('totalComments').textContent = formatNumber(data.totalComments);

    // 2. 趋势折线图
    renderTrendChart(data);

    // 3. 热门文章排行
    renderHotBlogsChart(data.hotBlogs);

    // 4. 标签分析（使用缓存的全部标签数据首次渲染）
    renderTagChart(data.tagAnalytics);

    // 5. 互动率分析
    renderInteractionChart(data.likeRate, data.commentRate);

    // 6. 发文热力图
    renderHeatmapChart(data.publishHeatmap);
}

// 格式化数字（1000 -> 1k）
function formatNumber(num) {
    if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
    if (num >= 1000) return (num / 1000).toFixed(1) + 'k';
    return num.toString();
}

// ===== 日期筛选 =====
function initDateFilter() {
    document.querySelectorAll('.date-preset-btn').forEach(function(btn) {
        btn.addEventListener('click', function() {
            document.querySelectorAll('.date-preset-btn').forEach(function(b) { b.classList.remove('active'); });
            this.classList.add('active');
            document.getElementById('startDate').value = '';
            document.getElementById('endDate').value = '';
            var days = parseInt(this.dataset.days);
            fetchAnalyticsData(days);
        });
    });

    document.getElementById('dateApplyBtn').addEventListener('click', function() {
        var startVal = document.getElementById('startDate').value;
        var endVal = document.getElementById('endDate').value;
        if (startVal && endVal) {
            document.querySelectorAll('.date-preset-btn').forEach(function(b) { b.classList.remove('active'); });
            fetchAnalyticsData(null, startVal, endVal);
        }
    });
}

// ===== 标签选择交互 =====
function loadUserTags() {
    fetch('/creator/analytics/tags')
        .then(function(res) { return res.json(); })
        .then(function(data) {
            if (data.code === 200 && data.data && data.data.length > 0) {
                renderTagChips(data.data);
            }
        })
        .catch(function(err) {
            console.error('加载标签列表失败', err);
        });
}

function renderTagChips(tags) {
    var container = document.getElementById('tagSelectChips');
    container.innerHTML = '';

    tags.forEach(function(tag) {
        var chip = document.createElement('label');
        chip.className = 'tag-select-chip selected';
        chip.innerHTML = '<input type="checkbox" value="' + tag.tagId + '" checked> ' + tag.tagName;
        chip.querySelector('input').addEventListener('change', updateTagChartBySelection);
        container.appendChild(chip);
    });

    updateTagChartBySelection();

    document.getElementById('tagSelectAll').addEventListener('click', function() {
        container.querySelectorAll('input').forEach(function(cb) {
            cb.checked = true;
            cb.parentElement.classList.add('selected');
        });
        updateTagChartBySelection();
    });

    document.getElementById('tagSelectNone').addEventListener('click', function() {
        container.querySelectorAll('input').forEach(function(cb) {
            cb.checked = false;
            cb.parentElement.classList.remove('selected');
        });
        updateTagChartBySelection();
    });
}

function updateTagChartBySelection() {
    var checked = document.querySelectorAll('#tagSelectChips input:checked');
    var selectedIds = Array.from(checked).map(function(cb) { return cb.value; });

    var chips = document.querySelectorAll('.tag-select-chip');
    chips.forEach(function(chip) {
        var cb = chip.querySelector('input');
        if (cb.checked) {
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
                renderTagChart(data.data.tagAnalytics);
            }
        });
}

// ===== 互动率进度条 =====
function renderInteractionChart(likeRate, commentRate) {
    var likeVal = likeRate.toFixed(2);
    var commentVal = commentRate.toFixed(2);

    document.getElementById('likeRateValue').textContent = likeVal;
    document.getElementById('commentRateValue').textContent = commentVal;

    document.getElementById('likeRateBar').style.width = Math.min(likeRate, 100) + '%';
    document.getElementById('commentRateBar').style.width = Math.min(commentRate, 100) + '%';
}

// 趋势折线图
function renderTrendChart(data) {
    const chartDom = document.getElementById('trendChart');
    if (!trendChart) {
        trendChart = echarts.init(chartDom);
    }

    const option = {
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
            top: 15
        },
        grid: {
            left: '3%',
            right: '4%',
            bottom: '3%',
            top: 60,
            containLabel: true
        },
        xAxis: {
            type: 'category',
            data: data.trendDates,
            axisLine: { lineStyle: { color: 'rgba(255,255,255,0.2)' } },
            axisLabel: { color: 'rgba(255,255,255,0.6)' }
        },
        yAxis: {
            type: 'value',
            splitLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } },
            axisLabel: { color: 'rgba(255,255,255,0.6)' }
        },
        series: [
            {
                name: '阅读量',
                type: 'line',
                smooth: true,
                data: data.trendReads,
                itemStyle: { color: '#f5576c' },
                areaStyle: { color: 'rgba(245, 87, 108, 0.2)' }
            },
            {
                name: '点赞数',
                type: 'line',
                smooth: true,
                data: data.trendLikes,
                itemStyle: { color: '#00f2fe' },
                areaStyle: { color: 'rgba(0, 242, 254, 0.2)' }
            },
            {
                name: '评论数',
                type: 'line',
                smooth: true,
                data: data.trendComments,
                itemStyle: { color: '#fee140' },
                areaStyle: { color: 'rgba(254, 225, 64, 0.2)' }
            }
        ]
    };

    trendChart.setOption(option);
    window.addEventListener('resize', () => trendChart.resize());
}

// 热门文章排行（横向条形图）
function renderHotBlogsChart(hotBlogs) {
    const chartDom = document.getElementById('hotBlogsChart');
    if (!hotBlogsChart) {
        hotBlogsChart = echarts.init(chartDom);
    }

    const titles = hotBlogs.map(b => b.title.length > 20 ? b.title.substring(0, 20) + '...' : b.title);
    const scores = hotBlogs.map(b => b.score);

    const option = {
        backgroundColor: 'rgba(255, 255, 255, 0.05)',
        tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'shadow' },
            backgroundColor: 'rgba(30, 35, 50, 0.95)',
            borderColor: 'rgba(135, 206, 235, 0.3)',
            textStyle: { color: '#fff' },
            formatter: function(params) {
                const blog = hotBlogs[params[0].dataIndex];
                return `${blog.title}<br/>阅读: ${blog.readCount}<br/>点赞: ${blog.likeCount}<br/>得分: ${blog.score.toFixed(1)}`;
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
            splitLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } },
            axisLabel: { color: 'rgba(255,255,255,0.6)' }
        },
        yAxis: {
            type: 'category',
            data: titles.reverse(),
            axisLabel: { color: 'rgba(255,255,255,0.7)' }
        },
        series: [{
            name: '综合得分',
            type: 'bar',
            data: scores.reverse(),
            itemStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                    { offset: 0, color: '#667eea' },
                    { offset: 1, color: '#764ba2' }
                ])
            },
            barWidth: '60%'
        }]
    };

    hotBlogsChart.setOption(option);
    window.addEventListener('resize', () => hotBlogsChart.resize());
}

// 标签分析（饼图）
function renderTagChart(tagAnalytics) {
    const chartDom = document.getElementById('tagChart');
    if (!tagChart) {
        tagChart = echarts.init(chartDom);
    }

    const data = tagAnalytics.map(t => ({
        name: t.tagName,
        value: t.blogCount
    }));

    const option = {
        backgroundColor: 'rgba(255, 255, 255, 0.05)',
        tooltip: {
            trigger: 'item',
            backgroundColor: 'rgba(30, 35, 50, 0.95)',
            borderColor: 'rgba(135, 206, 235, 0.3)',
            textStyle: { color: '#fff' },
            formatter: function(params) {
                const tag = tagAnalytics[params.dataIndex];
                return `${tag.tagName}<br/>文章数: ${tag.blogCount}<br/>平均阅读: ${tag.avgReads.toFixed(1)}`;
            }
        },
        legend: {
            orient: 'vertical',
            left: 'left',
            textStyle: { color: '#ccc' }
        },
        series: [{
            name: '标签分布',
            type: 'pie',
            radius: '60%',
            center: ['60%', '50%'],
            data: data,
            emphasis: {
                itemStyle: {
                    shadowBlur: 10,
                    shadowOffsetX: 0,
                    shadowColor: 'rgba(0, 0, 0, 0.5)'
                }
            }
        }]
    };

    tagChart.setOption(option);
    window.addEventListener('resize', () => tagChart.resize());
}

// 互动率仪表盘
function renderInteractionChart(likeRate, commentRate) {
    const chartDom = document.getElementById('interactionChart');
    if (!interactionChart) {
        interactionChart = echarts.init(chartDom);
    }

    const option = {
        backgroundColor: 'rgba(255, 255, 255, 0.05)',
        series: [
            {
                type: 'gauge',
                center: ['25%', '60%'],
                radius: '70%',
                startAngle: 200,
                endAngle: -20,
                min: 0,
                max: 100,
                detail: {
                    formatter: '{value}%',
                    color: '#00f2fe',
                    fontSize: 20
                },
                data: [{ value: likeRate.toFixed(2), name: '点赞率' }],
                title: {
                    offsetCenter: [0, '80%'],
                    color: '#ccc'
                },
                itemStyle: { color: '#00f2fe' },
                axisLine: {
                    lineStyle: {
                        color: [[1, 'rgba(0, 242, 254, 0.2)']]
                    }
                }
            },
            {
                type: 'gauge',
                center: ['75%', '60%'],
                radius: '70%',
                startAngle: 200,
                endAngle: -20,
                min: 0,
                max: 100,
                detail: {
                    formatter: '{value}%',
                    color: '#fee140',
                    fontSize: 20
                },
                data: [{ value: commentRate.toFixed(2), name: '评论率' }],
                title: {
                    offsetCenter: [0, '80%'],
                    color: '#ccc'
                },
                itemStyle: { color: '#fee140' },
                axisLine: {
                    lineStyle: {
                        color: [[1, 'rgba(254, 225, 64, 0.2)']]
                    }
                }
            }
        ]
    };

    interactionChart.setOption(option);
    window.addEventListener('resize', () => interactionChart.resize());
}

// 发文热力图（原生SVG GitHub风格）
function renderHeatmapChart(publishHeatmap) {
    const svg = document.getElementById('heatmapChart');
    const tooltipEl = document.getElementById('heatmapTooltip');
    if (!svg) return;

    const CELL = 11;
    const GAP = 3;
    const ROWS = 7;
    const MONTHS = ['1月','2月','3月','4月','5月','6月','7月','8月','9月','10月','11月','12月'];
    const DAYS = ['', '一', '', '三', '', '五', ''];

    // 构造过去一年的数据（从今天往前推365天）
    const today = new Date();
    const oneYearAgo = new Date(today);
    oneYearAgo.setDate(today.getDate() - 364);

    const dataMap = {};
    publishHeatmap.forEach(item => {
        dataMap[item.date] = item.publishCount;
    });

    const contributions = [];
    for (let d = new Date(oneYearAgo); d <= today; d.setDate(d.getDate() + 1)) {
        const dateStr = d.toISOString().split('T')[0];
        contributions.push({
            date: dateStr,
            count: dataMap[dateStr] || 0
        });
    }

    const maxCount = Math.max(...contributions.map(d => d.count), 1);
    const weeks = Math.ceil(contributions.length / ROWS);
    const labelOffset = 28;
    const headerOffset = 16;
    const svgWidth = labelOffset + weeks * (CELL + GAP);
    const svgHeight = headerOffset + ROWS * (CELL + GAP);

    svg.setAttribute('viewBox', `0 0 ${svgWidth} ${svgHeight}`);
    svg.innerHTML = '';

    const ns = 'http://www.w3.org/2000/svg';

    // 顶部月份标签
    const monthLabels = [];
    let lastMonth = -1;
    for (let w = 0; w < weeks; w++) {
        const idx = w * ROWS;
        if (idx < contributions.length) {
            const month = new Date(contributions[idx].date).getMonth();
            if (month !== lastMonth) {
                monthLabels.push({
                    label: MONTHS[month],
                    x: labelOffset + w * (CELL + GAP)
                });
                lastMonth = month;
            }
        }
    }

    monthLabels.forEach(m => {
        const text = document.createElementNS(ns, 'text');
        text.setAttribute('x', m.x);
        text.setAttribute('y', 11);
        text.setAttribute('fill', 'rgba(255,255,255,0.5)');
        text.setAttribute('font-size', '10');
        text.setAttribute('font-family', 'sans-serif');
        text.textContent = m.label;
        svg.appendChild(text);
    });

    // 左侧星期标签
    DAYS.forEach((d, i) => {
        if (d) {
            const text = document.createElementNS(ns, 'text');
            text.setAttribute('x', 0);
            text.setAttribute('y', headerOffset + i * (CELL + GAP) + CELL - 1);
            text.setAttribute('fill', 'rgba(255,255,255,0.4)');
            text.setAttribute('font-size', '9');
            text.setAttribute('font-family', 'sans-serif');
            text.textContent = d;
            svg.appendChild(text);
        }
    });

    // 绘制格子
    contributions.forEach((day, idx) => {
        const col = Math.floor(idx / ROWS);
        const row = idx % ROWS;
        const rect = document.createElementNS(ns, 'rect');

        rect.setAttribute('x', labelOffset + col * (CELL + GAP));
        rect.setAttribute('y', headerOffset + row * (CELL + GAP));
        rect.setAttribute('width', CELL);
        rect.setAttribute('height', CELL);
        rect.setAttribute('rx', 2);
        rect.setAttribute('ry', 2);

        // 计算颜色透明度
        let opacity;
        if (day.count === 0) {
            opacity = 0.1;
        } else if (day.count === 1) {
            opacity = 0.3;
        } else if (day.count <= 3) {
            opacity = 0.6;
        } else if (day.count <= 5) {
            opacity = 0.85;
        } else {
            opacity = 1;
        }

        rect.setAttribute('fill', `rgba(135, 206, 235, ${opacity})`);
        rect.style.transition = 'all 150ms ease';
        rect.style.cursor = 'pointer';
        rect.setAttribute('data-date', day.date);
        rect.setAttribute('data-count', day.count);

        svg.appendChild(rect);
    });

    // 使用事件委托处理hover
    let currentHoverRect = null;

    svg.addEventListener('mouseover', function(e) {
        const rect = e.target;
        if (rect.tagName === 'rect' && rect.hasAttribute('data-date')) {
            if (currentHoverRect === rect) return;

            currentHoverRect = rect;
            rect.style.opacity = '1';
            rect.style.filter = 'drop-shadow(0 2px 6px rgba(135, 206, 235, 0.6))';

            const date = rect.getAttribute('data-date');
            const count = parseInt(rect.getAttribute('data-count'));
            showTooltip(date, count, rect);
        }
    });

    svg.addEventListener('mouseout', function(e) {
        const rect = e.target;
        if (rect.tagName === 'rect' && rect.hasAttribute('data-date')) {
            rect.style.opacity = '';
            rect.style.filter = '';
            currentHoverRect = null;
            hideTooltip();
        }
    });

    function showTooltip(date, count, element) {
        const d = new Date(date);
        const dateStr = `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`;
        const text = count > 0 ? `${dateStr}: ${count} 篇文章` : `${dateStr}: 无发文`;

        tooltipEl.textContent = text;
        tooltipEl.style.display = 'block';

        requestAnimationFrame(() => {
            const rect = element.getBoundingClientRect();
            const tooltipRect = tooltipEl.getBoundingClientRect();

            let x = rect.left + rect.width / 2 - tooltipRect.width / 2;
            let y = rect.top - tooltipRect.height - 8;

            const margin = 10;
            const maxX = window.innerWidth - tooltipRect.width - margin;
            x = Math.max(margin, Math.min(x, maxX));

            if (y < margin) {
                y = rect.bottom + 8;
            }

            tooltipEl.style.left = x + 'px';
            tooltipEl.style.top = y + 'px';
        });
    }

    function hideTooltip() {
        tooltipEl.style.display = 'none';
    }
}








