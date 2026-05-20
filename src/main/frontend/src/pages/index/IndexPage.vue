<template>
  <AppHeader current-page="index" />
  <div class="page-container">
    <el-row :gutter="16">
      <el-col :lg="5" :md="6" :xs="24">
        <WeatherWidget />
        <el-card class="sidebar-card">
          <template #header><span>标签</span></template>
          <span
            v-for="tag in tags"
            :key="tag.id"
            class="tag-badge"
            :class="{ active: false }"
            @click="goTag(tag.id)"
          >{{ tag.tagName }}</span>
        </el-card>
      </el-col>

      <el-col :lg="14" :md="12" :xs="24">
        <h4 class="section-title">最新文章</h4>

        <div class="search-bar">
          <el-input v-model="keyword" placeholder="搜索标题、作者、内容..." clearable @keyup.enter="loadBlogs(1)">
            <template #append>
              <el-button @click="loadBlogs(1)">搜索</el-button>
            </template>
          </el-input>
          <el-button class="filter-toggle" @click="showFilter = !showFilter">筛选</el-button>
        </div>

        <el-card v-if="showFilter" class="filter-panel">
          <div class="filter-group">
            <span class="filter-label">时间跨度</span>
            <el-radio-group v-model="timeSpan" @change="onTimeSpanChange">
              <el-radio-button value="all">全部</el-radio-button>
              <el-radio-button value="week">最近一周</el-radio-button>
              <el-radio-button value="month">最近一月</el-radio-button>
              <el-radio-button value="year">最近一年</el-radio-button>
            </el-radio-group>
          </div>
          <div class="filter-group">
            <span class="filter-label">自定义时间</span>
            <el-date-picker v-model="dateFrom" type="date" placeholder="起始日期" value-format="YYYY-MM-DD" />
            <el-date-picker v-model="dateTo" type="date" placeholder="结束日期" value-format="YYYY-MM-DD" class="ml-2" />
          </div>
          <div class="filter-group">
            <span class="filter-label">排序方式</span>
            <el-radio-group v-model="sortBy">
              <el-radio-button value="newest">最新发布</el-radio-button>
              <el-radio-button value="oldest">最早发布</el-radio-button>
              <el-radio-button value="likes">点赞最高</el-radio-button>
              <el-radio-button value="reads">阅读最高</el-radio-button>
              <el-radio-button value="comments">评论最多</el-radio-button>
            </el-radio-group>
          </div>
          <el-button type="primary" @click="loadBlogs(1)">应用筛选</el-button>
        </el-card>

        <el-skeleton v-if="loading" :rows="5" animated />
        <template v-else>
          <el-card
            v-for="b in blogs"
            :key="b.id"
            class="blog-card mb-3"
            shadow="hover"
            @click="goRead(b.id)"
          >
            <h5 class="blog-title">{{ b.title }}</h5>
            <p class="blog-brief">{{ b.brief || extractBrief(b.title) }}</p>
            <div class="blog-meta">
              <span><el-icon><View /></el-icon> {{ b.read_count || 0 }}</span>
              <span><el-icon><Star /></el-icon> {{ b.like_count || 0 }}</span>
              <span><el-icon><ChatDotRound /></el-icon> {{ b.comment_count || 0 }}</span>
              <a :href="'/user/profile?id=' + b.u_id" class="author-link" @click.stop>
                <img :src="b.author_avatar || '/images/default-avatar.png'" class="author-avatar-mini" alt="" />
                {{ b.author }}
              </a>
              <span class="blog-date">{{ formatTime(b.created_at) }}</span>
            </div>
            <div v-if="b.t_id?.tagList?.length" class="blog-tags">
              <span v-for="tid in b.t_id.tagList" :key="tid" class="tag-badge small">{{ tagName(tid) }}</span>
            </div>
          </el-card>

          <el-empty v-if="blogs.length === 0" description="还没有文章" />
          <el-alert v-else type="info" :closable="false" class="mb-3">
            已收录 {{ totalBlogs }} 篇文章
          </el-alert>

          <el-pagination
            v-if="totalPages > 1"
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="totalBlogs"
            layout="prev, pager, next, jumper"
            @current-change="loadBlogs"
          />
        </template>
      </el-col>

      <el-col :lg="5" :md="6" :xs="24">
        <el-card class="sidebar-card">
          <template #header><span>热门文章</span></template>
          <ul class="sidebar-list">
            <li v-for="h in hotBlogs" :key="h.id">
              <a :href="'/blogs/read/' + h.id">{{ abbreviate(h.title, 13) }}</a>
            </li>
            <li v-if="!hotBlogs.length" class="text-muted">暂无热门文章</li>
          </ul>
        </el-card>
        <el-card class="sidebar-card mt-3">
          <template #header><span>最新评论</span></template>
          <ul class="sidebar-list">
            <li v-for="c in recentComments" :key="c.id">
              <a :href="'/blogs/read/' + c.b_id">
                <div class="comment-preview">
                  <div class="comment-user">{{ c.author_name || '用户' }}</div>
                  <div class="comment-text">{{ abbreviate(stripHtml(c.content), 15) }}</div>
                </div>
              </a>
            </li>
            <li v-if="!recentComments.length" class="text-muted">暂无评论</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { View, Star, ChatDotRound } from '@element-plus/icons-vue'
import AppHeader from '@/shared/components/AppHeader.vue'
import WeatherWidget from '@/shared/components/WeatherWidget.vue'
import { useAuthStore } from '@/stores/auth'
import request from '@/utils/request'
import { extractBrief } from '@/utils/markdown'

const auth = useAuthStore()

const tags = ref([])
const blogs = ref([])
const hotBlogs = ref([])
const recentComments = ref([])
const loading = ref(true)
const keyword = ref('')
const showFilter = ref(false)
const timeSpan = ref('all')
const dateFrom = ref('')
const dateTo = ref('')
const sortBy = ref('')
const currentPage = ref(1)
const pageSize = 5
const totalBlogs = ref(0)
const totalPages = ref(0)

function formatTime(d) {
  return d ? new Date(d).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) : ''
}
function abbreviate(s, n) { return s && s.length > n ? s.substring(0, n) + '...' : (s || '') }
function stripHtml(s) { return s ? s.replace(/<[^>]*>/g, '') : '' }
function tagName(id) { return tags.value.find(t => t.id === id)?.tagName || '标签' }
function goRead(id) { window.location.href = '/blogs/read/' + id }
function goTag(id) { window.location.href = '/blogs/tag/' + id }

function onTimeSpanChange(span) {
  const now = new Date()
  if (span === 'all') { dateFrom.value = ''; dateTo.value = ''; return }
  const to = now.toISOString().slice(0, 10)
  let from = new Date(now)
  if (span === 'week') from.setDate(from.getDate() - 7)
  else if (span === 'month') from.setMonth(from.getMonth() - 1)
  else if (span === 'year') from.setFullYear(from.getFullYear() - 1)
  dateFrom.value = from.toISOString().slice(0, 10)
  dateTo.value = to
}

async function loadBlogs(page = 1) {
  loading.value = true
  currentPage.value = page
  try {
    const params = { page, pageSize }
    if (keyword.value) params.keyword = keyword.value
    if (dateFrom.value) params.dateFrom = dateFrom.value
    if (dateTo.value) params.dateTo = dateTo.value
    if (sortBy.value) params.sortBy = sortBy.value
    const { data } = await request.get('/api/blogs', { params })
    if (data.code === 200) {
      const d = data.data
      blogs.value = d.blogs || []
      totalBlogs.value = d.totalBlogs || 0
      totalPages.value = d.totalPages || 0
    }
  } finally { loading.value = false }
}

onMounted(async () => {
  await auth.init()
  try {
    const [t, h, c] = await Promise.all([
      request.get('/api/tags'),
      request.get('/api/hot-blogs'),
      request.get('/api/recent-comments')
    ])
    if (t.data.code === 200) tags.value = t.data.data || []
    if (h.data.code === 200) hotBlogs.value = h.data.data || []
    if (c.data.code === 200) recentComments.value = c.data.data || []
  } catch {}
  await loadBlogs(1)
})
</script>

<style scoped>
.section-title { margin: 0 0 16px; }
.search-bar { display: flex; gap: 8px; margin-bottom: 16px; }
.search-bar .el-input { flex: 1; }
.filter-panel { margin-bottom: 16px; }
.filter-group { margin-bottom: 12px; }
.filter-label { display: block; font-size: 0.85rem; color: #666; margin-bottom: 8px; }
.blog-title { margin: 0 0 8px; color: var(--el-color-primary); }
.blog-brief { color: #888; font-size: 0.9rem; margin: 0 0 12px; }
.blog-meta { display: flex; flex-wrap: wrap; gap: 12px; align-items: center; font-size: 0.85rem; color: #666; }
.blog-date { margin-left: auto; }
.blog-tags { margin-top: 8px; }
.tag-badge.small { font-size: 0.75rem; padding: 2px 8px; }
.sidebar-card { margin-bottom: 12px; }
.sidebar-list { list-style: none; padding: 0; margin: 0; }
.sidebar-list li { margin-bottom: 8px; }
.sidebar-list a { color: #555; font-size: 0.9rem; }
.sidebar-list a:hover { color: var(--el-color-primary); }
.comment-preview .comment-user { font-weight: 600; font-size: 0.85rem; }
.comment-preview .comment-text { font-size: 0.8rem; color: #888; }
.text-muted { color: #999; font-size: 0.85rem; }
.ml-2 { margin-left: 8px; }
.mb-3 { margin-bottom: 12px; }
.mt-3 { margin-top: 12px; }
</style>
