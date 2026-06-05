// 全局图表实例
let trendChart, hotBlogsChart, tagChart, interactionChart, heatmapChart;

// 页面加载时获取数据
document.addEventListener('DOMContentLoaded', function() {
    fetchAnalyticsData();
});

// 获取数据
function fetchAnalyticsData() {
    fetch('/creator/analytics/data')
        .then(res => res.json())
        .then(data => {
            if (data.code === 200) {
                renderData(data.data);
            } else {
                console.error('数据加载失败:', data.msg);
                alert(data.msg || '数据加载失败');
            }
        })
        .catch(err => {
            console.error('数据加载失败', err);
            alert('网络错误');
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

    // 4. 标签分析
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

// 发文热力图（GitHub风格）
function renderHeatmapChart(publishHeatmap) {
    const chartDom = document.getElementById('heatmapChart');
    if (!heatmapChart) {
        heatmapChart = echarts.init(chartDom);
    }

    const data = publishHeatmap.map(d => [d.date, d.publishCount]);
    const maxCount = Math.max(...publishHeatmap.map(d => d.publishCount), 1);

    const option = {
        backgroundColor: 'rgba(255, 255, 255, 0.05)',
        tooltip: {
            formatter: function(params) {
                const count = params.value[1];
                return `<div style="text-align:left;">
                    <strong>${params.value[0]}</strong><br/>
                    ${count > 0 ? `<span style="color:#87CEEB;">${count}篇</span> 文章` : '无发文'}
                </div>`;
            },
            backgroundColor: 'rgba(30, 35, 50, 0.95)',
            borderColor: 'rgba(135, 206, 235, 0.3)',
            textStyle: { color: '#fff' },
            padding: 10
        },
        visualMap: {
            show: false,
            min: 0,
            max: maxCount,
            type: 'continuous',
            inRange: {
                color: [
                    'rgba(135, 206, 235, 0.05)',  // 0篇 - 几乎透明
                    'rgba(135, 206, 235, 0.25)',  // 少量
                    'rgba(135, 206, 235, 0.5)',   // 中等
                    'rgba(135, 206, 235, 0.75)',  // 较多
                    'rgba(135, 206, 235, 1)'      // 很多
                ]
            }
        },
        calendar: {
            top: 40,
            left: 40,
            right: 20,
            bottom: 10,
            cellSize: ['auto', 14],
            range: new Date().getFullYear(),
            itemStyle: {
                borderWidth: 2,
                borderColor: 'rgba(20, 24, 38, 1)',
                borderRadius: 2
            },
            yearLabel: {
                show: true,
                position: 'top',
                formatter: '{start}',
                color: 'rgba(255,255,255,0.6)',
                fontSize: 14
            },
            dayLabel: {
                firstDay: 1,
                nameMap: ['日', '一', '二', '三', '四', '五', '六'],
                color: 'rgba(255,255,255,0.4)',
                fontSize: 11
            },
            monthLabel: {
                color: 'rgba(255,255,255,0.5)',
                fontSize: 11
            },
            splitLine: {
                show: false
            }
        },
        series: [{
            type: 'heatmap',
            coordinateSystem: 'calendar',
            data: data,
            emphasis: {
                itemStyle: {
                    borderColor: '#87CEEB',
                    borderWidth: 2,
                    shadowBlur: 10,
                    shadowColor: 'rgba(135, 206, 235, 0.5)'
                }
            }
        }]
    };

    heatmapChart.setOption(option);
    window.addEventListener('resize', () => heatmapChart.resize());
}





