<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { getDashboardStats } from '@/api/dashboard'
import { useUserStore } from '@/stores/user'
import * as echarts from 'echarts'

const userStore = useUserStore()

const stats = ref({})
const blogChartRef = ref()
const commentChartRef = ref()
let blogChart = null
let commentChart = null

// 统计卡片配置
const cards = [
  { key: 'userCount', label: '用户总数', color: '#409eff' },
  { key: 'blogCount', label: '博客总数', color: '#67c23a' },
  { key: 'commentCount', label: '评论总数', color: '#e6a23c' },
  { key: 'announcementCount', label: '公告总数', color: '#f56c6c' },
]

async function fetchStats() {
  const res = await getDashboardStats()
  stats.value = res.data
  renderCharts(res.data)
}

// 把 "2026-09-06" 截成 "09-06"，x 轴更紧凑
function shortDate(date) {
  return date ? date.slice(5) : date
}

function renderCharts(data) {
  // 博客发布趋势
  blogChart = echarts.init(blogChartRef.value)
  blogChart.setOption({
    title: { text: '近30天博客发布趋势', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: data.blogTrend.map((d) => shortDate(d.date)) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '发布数',
        type: 'line',
        smooth: true,
        data: data.blogTrend.map((d) => d.count),
        areaStyle: { opacity: 0.2 },
        itemStyle: { color: '#409eff' },
      },
    ],
  })

  // 评论趋势
  commentChart = echarts.init(commentChartRef.value)
  commentChart.setOption({
    title: { text: '近30天评论趋势', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: data.commentTrend.map((d) => shortDate(d.date)) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '评论数',
        type: 'line',
        smooth: true,
        data: data.commentTrend.map((d) => d.count),
        areaStyle: { opacity: 0.2 },
        itemStyle: { color: '#67c23a' },
      },
    ],
  })
}

// 窗口大小变化时重绘
function handleResize() {
  blogChart?.resize()
  commentChart?.resize()
}

onMounted(() => {
  fetchStats()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  blogChart?.dispose()
  commentChart?.dispose()
})
</script>

<template>
  <div>
    <el-card style="margin-bottom: 16px">
      <h2>欢迎回来，{{ userStore.userInfo.nickname || '管理员' }}</h2>
    </el-card>

    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col v-for="card in cards" :key="card.key" :span="6">
        <el-card>
          <div class="stat-card">
            <div class="stat-value" :style="{ color: card.color }">
              {{ stats[card.key] ?? 0 }}
            </div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 趋势图 -->
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card>
          <div ref="blogChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <div ref="commentChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.stat-card {
  text-align: center;
  padding: 8px 0;
}
.stat-value {
  font-size: 32px;
  font-weight: bold;
}
.stat-label {
  margin-top: 8px;
  color: #909399;
  font-size: 13px;
}
</style>
