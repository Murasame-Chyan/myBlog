<template>
  <AppHeader current-page="archives" />
  <div class="page-container" v-loading="loading">
    <el-alert type="info" :closable="false" class="mb-3">
      此处展示已删除的文章，仅支持阅读。登录后可恢复。
    </el-alert>
    <h4>已归档文章</h4>
    <el-card v-for="a in archives" :key="a.id" class="blog-card mb-3" shadow="hover">
      <div class="archive-row" @click="goRead(a.id)">
        <div class="archive-main">
          <h5>{{ a.title }}</h5>
          <p class="brief">{{ extractBrief(a.content) }}</p>
          <span class="meta">删除于 {{ formatTime(a.deleted_at) }}</span>
        </div>
        <el-button v-if="auth.loggedIn" type="primary" size="small" @click.stop="recover(a.id)" :loading="recoveringId === a.id">恢复</el-button>
      </div>
    </el-card>
    <el-empty v-if="!loading && !archives.length" description="归档箱为空" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AppHeader from '@/shared/components/AppHeader.vue'
import { useAuthStore } from '@/stores/auth'
import request from '@/utils/request'
import { extractBrief } from '@/utils/markdown'

const auth = useAuthStore()
const loading = ref(true)
const archives = ref([])
const recoveringId = ref(null)

function formatTime(d) {
  return d ? new Date(d).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) : ''
}
function goRead(id) { window.location.href = '/archives/read/' + id }

async function load() {
  loading.value = true
  try {
    const { data } = await request.get('/api/archives')
    if (data.code === 200) archives.value = data.data || []
    else if (data.code === 401) {
      ElMessage.warning('请先登录')
      window.location.href = '/'
    }
  } finally { loading.value = false }
}

async function recover(id) {
  recoveringId.value = id
  try {
    const { data } = await request.post('/blogs/recover/' + id)
    if (data.code === 200) {
      ElMessage.success('恢复成功')
      archives.value = archives.value.filter(a => a.id !== id)
    }
  } finally { recoveringId.value = null }
}

onMounted(async () => {
  await auth.init()
  await load()
})
</script>

<style scoped>
.archive-row { display: flex; justify-content: space-between; align-items: center; cursor: pointer; }
.archive-main { flex: 1; }
.brief { color: #888; font-size: 0.9rem; }
.meta { font-size: 0.8rem; color: #aaa; }
.mb-3 { margin-bottom: 12px; }
</style>
