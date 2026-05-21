<template>
  <AppHeader current-page="index" />
  <div class="page-container" v-loading="loading">
    <template v-if="blog">
      <h2>{{ blog.title }}</h2>
      <div class="meta-bar">
        <span><el-icon><View /></el-icon> {{ blog.read_count || 0 }}</span>
        <span><el-icon><ChatDotRound /></el-icon> {{ commentCount }}</span>
        <a :href="'/user/profile?id=' + blog.u_id" class="author-link">
          <img :src="authorAvatar || '/images/default-avatar.png'" class="author-avatar-mini" alt="" />
          {{ authorName }}
        </a>
        <span>发布：{{ formatTime(blog.created_at) }}</span>
        <span v-if="blog.updated_at">更新：{{ formatTime(blog.updated_at) }}</span>
      </div>
      <div v-if="blog.t_id?.tagList?.length" class="blog-tags mb-3">
        <span v-for="tid in blog.t_id.tagList" :key="tid" class="tag-badge">{{ tagName(tid) }}</span>
      </div>
      <el-divider />
      <div class="blog-content" v-html="contentHtml"></div>
      <div class="interaction-section">
        <el-button :type="liked ? 'primary' : 'default'" @click="toggleLike" :loading="likeLoading">
          <el-icon><Star /></el-icon> {{ blog.like_count || 0 }}
        </el-button>
      </div>
      <CommentSection :blog-id="blog.id" :comments="comments" @comment-added="reload" />
    </template>
    <el-result v-else-if="!loading" icon="warning" title="博客不存在" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { View, Star, ChatDotRound } from '@element-plus/icons-vue'
import AppHeader from '@/shared/components/AppHeader.vue'
import CommentSection from '@/shared/components/CommentSection.vue'
import { useAuthStore } from '@/stores/auth'
import request from '@/utils/request'
import { renderMarkdown } from '@/utils/markdown'

const auth = useAuthStore()
const loading = ref(true)
const blog = ref(null)
const comments = ref([])
const commentCount = ref(0)
const authorName = ref('')
const authorAvatar = ref('')
const liked = ref(false)
const likeLoading = ref(false)
const allTags = ref([])

const blogId = () => {
  const m = window.location.pathname.match(/\/blogs\/read\/(\d+)/)
  return m ? Number(m[1]) : 0
}

const contentHtml = computed(() => blog.value ? renderMarkdown(blog.value.content) : '')

function formatTime(d) {
  return d ? new Date(d).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) : ''
}
function tagName(id) { return allTags.value.find(t => t.id === id)?.tagName || '标签' }

async function load() {
  const id = blogId()
  if (!id) { loading.value = false; return }
  try {
    const { data } = await request.get('/api/blogs/' + id)
    if (data.code === 200) {
      const d = data.data
      blog.value = d.blog
      comments.value = d.comments || []
      commentCount.value = d.commentCount || 0
      authorName.value = d.authorName || ''
      authorAvatar.value = d.authorAvatar || ''
      liked.value = !!d.liked
    }
  } finally { loading.value = false }
}

async function reload() { await load() }

async function toggleLike() {
  if (!auth.loggedIn) { ElMessage.warning('请先登录'); return }
  const id = blog.value?.id
  if (!id) return
  likeLoading.value = true
  try {
    const url = liked.value ? `/blogs/unlike/${id}` : `/blogs/like/${id}`
    const { data } = await request.post(url)
    if (data.code === 200) {
      liked.value = !liked.value
      blog.value.like_count = (blog.value.like_count || 0) + (liked.value ? 1 : -1)
    }
  } finally { likeLoading.value = false }
}

onMounted(async () => {
  await auth.init()
  try {
    const { data } = await request.get('/api/tags')
    if (data.code === 200) allTags.value = data.data || []
  } catch {}
  await load()
  const id = blogId()
  if (id) {
    setTimeout(() => { request.post(`/blogs/incrementRead/${id}`, {}, { _silent: true }).catch(() => {}) }, 500)
  }
})
</script>

<style scoped>
.meta-bar { display: flex; flex-wrap: wrap; gap: 16px; align-items: center; color: #666; font-size: 0.9rem; margin-bottom: 12px; }
.interaction-section { margin: 24px 0; }
.blog-tags { display: flex; flex-wrap: wrap; gap: 6px; }
</style>
