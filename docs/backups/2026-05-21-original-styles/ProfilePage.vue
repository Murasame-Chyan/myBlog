<template>
  <AppHeader current-page="index" />
  <div class="page-container" v-loading="loading">
    <template v-if="user">
      <el-card class="profile-card">
        <div class="profile-header">
          <el-avatar :size="96" :src="user.avatar || '/images/default-avatar.png'" />
          <div class="profile-info">
            <h3>{{ user.nickname }} <el-tag size="small">LV.{{ level }}</el-tag></h3>
            <p class="profile-intro" v-if="user.intro">「{{ user.intro }}」</p>
            <p class="profile-meta">
              <span v-if="user.gender === 1">♂ 男</span>
              <span v-else-if="user.gender === 2">♀ 女</span>
              <span v-else>⚥ 未设置</span>
              <span v-if="user.exp != null"> · 经验 {{ user.exp }}</span>
            </p>
            <p class="uid">UID：{{ user.id }}</p>
          </div>
        </div>
      </el-card>

      <el-tabs v-model="activeTab" class="mt-4">
        <el-tab-pane label="文章列表" name="articles">
          <el-card v-for="a in articles" :key="a.id" class="blog-card mb-2" shadow="hover" @click="goRead(a.id)">
            <h5>{{ a.title }}</h5>
            <p class="brief">{{ a.brief }}</p>
            <span class="meta">{{ formatTime(a.created_at) }}</span>
          </el-card>
          <el-empty v-if="!articles.length" description="暂无文章" />
        </el-tab-pane>
        <el-tab-pane label="点赞列表" name="liked">
          <el-card v-for="a in likedBlogs" :key="a.id" class="blog-card mb-2" shadow="hover" @click="goRead(a.id)">
            <h5>{{ a.title }}</h5>
            <p class="brief">{{ a.brief }}</p>
          </el-card>
          <el-empty v-if="!likedBlogs.length" description="暂无点赞文章" />
        </el-tab-pane>
      </el-tabs>
    </template>
    <el-result v-else-if="!loading" icon="warning" title="用户不存在" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AppHeader from '@/shared/components/AppHeader.vue'
import { useAuthStore } from '@/stores/auth'
import request from '@/utils/request'

const auth = useAuthStore()
const loading = ref(true)
const user = ref(null)
const level = ref(1)
const articles = ref([])
const likedBlogs = ref([])
const activeTab = ref('articles')

function profileId() {
  const params = new URLSearchParams(window.location.search)
  return params.get('id') ? Number(params.get('id')) : null
}

function formatTime(d) {
  return d ? new Date(d).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }) : ''
}
function goRead(id) { window.location.href = '/blogs/read/' + id }

onMounted(async () => {
  await auth.init()
  const id = profileId()
  if (!id) {
    loading.value = false
    return
  }
  try {
    const { data } = await request.get('/api/user/profile/' + id)
    if (data.code === 200) {
      user.value = data.data.user
      level.value = data.data.level || 1
      articles.value = data.data.articles || []
      likedBlogs.value = data.data.likedBlogs || []
    }
  } finally { loading.value = false }
})
</script>

<style scoped>
.profile-header { display: flex; gap: 24px; align-items: flex-start; }
.profile-info h3 { margin: 0 0 8px; }
.profile-intro { color: #666; font-style: italic; }
.profile-meta, .uid { color: #999; font-size: 0.9rem; }
.brief { color: #888; font-size: 0.9rem; margin: 4px 0; }
.meta { font-size: 0.8rem; color: #aaa; }
.mt-4 { margin-top: 16px; }
.mb-2 { margin-bottom: 8px; }
</style>
