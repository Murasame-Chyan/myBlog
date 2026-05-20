<template>
  <AppHeader current-page="archives" />
  <div class="page-container" v-loading="loading">
    <template v-if="archive">
      <h2>{{ archive.title }}</h2>
      <div class="meta-bar">
        <a :href="'/user/profile?id=' + archive.u_id" class="author-link">
          <img :src="authorAvatar || '/images/default-avatar.png'" class="author-avatar-mini" alt="" />
          {{ authorName }}
        </a>
        <span>发布：{{ formatTime(archive.created_at) }}</span>
        <span v-if="archive.deleted_at">删除：{{ formatTime(archive.deleted_at) }}</span>
      </div>
      <el-divider />
      <div class="blog-content" v-html="contentHtml"></div>
      <el-alert type="info" :closable="false" class="mt-4">此文章已归档，仅支持阅读，不支持评论和编辑。</el-alert>
      <el-button class="mt-3" @click="goArchives">← 返回归档</el-button>
    </template>
    <el-result v-else-if="!loading" icon="warning" title="归档文章不存在" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import AppHeader from '@/shared/components/AppHeader.vue'
import { useAuthStore } from '@/stores/auth'
import request from '@/utils/request'
import { renderMarkdown } from '@/utils/markdown'

const auth = useAuthStore()
const loading = ref(true)
const archive = ref(null)
const authorName = ref('')
const authorAvatar = ref('')

const archiveId = () => {
  const m = window.location.pathname.match(/\/archives\/read\/(\d+)/)
  return m ? Number(m[1]) : 0
}

const contentHtml = computed(() => archive.value ? renderMarkdown(archive.value.content) : '')

function formatTime(d) {
  return d ? new Date(d).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) : ''
}
function goArchives() { window.location.href = '/archives' }

async function load() {
  const id = archiveId()
  if (!id) { loading.value = false; return }
  try {
    let found = null
    try {
      const { data } = await request.get('/api/archives/' + id, { _silent: true })
      if (data.code === 200) {
        found = data.data.archive || data.data
        authorName.value = data.data.authorName || ''
        authorAvatar.value = data.data.authorAvatar || ''
      }
    } catch {}
    if (!found) {
      const { data } = await request.get('/api/archives')
      if (data.code === 200) {
        const list = data.data || []
        found = list.find(a => a.id === id)
      }
    }
    if (found) {
      archive.value = found
      if (!authorName.value) authorName.value = '作者'
    }
  } finally { loading.value = false }
}

onMounted(async () => {
  await auth.init()
  await load()
})
</script>

<style scoped>
.meta-bar { display: flex; flex-wrap: wrap; gap: 16px; align-items: center; color: #666; font-size: 0.9rem; margin-bottom: 12px; }
.mt-3 { margin-top: 12px; }
.mt-4 { margin-top: 16px; }
</style>
