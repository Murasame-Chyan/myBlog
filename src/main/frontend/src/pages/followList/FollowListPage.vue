<template>
  <AppHeader current-page="index" />
  <div class="page-container" v-loading="loading">
    <el-button text @click="goProfile">← 返回用户主页</el-button>
    <h4 class="list-title">{{ pageTitle }}</h4>
    <el-card v-for="item in list" :key="item.user.id" class="follow-item mb-2">
      <div class="follow-row">
        <a :href="'/user/profile?id=' + item.user.id" @click.stop>
          <el-avatar :size="40" :src="item.user.avatar || '/images/default-avatar.png'" />
        </a>
        <div class="follow-info">
          <a :href="'/user/profile?id=' + item.user.id" class="follow-name">{{ item.user.nickname }}</a>
          <el-tag size="small">LV.{{ item.user.level || 1 }}</el-tag>
          <p v-if="item.user.intro" class="follow-intro">{{ item.user.intro }}</p>
        </div>
        <div v-if="auth.loggedIn" class="follow-actions">
          <el-button
            v-if="listType === 'followers'"
            :type="item.isFollowing ? 'success' : 'primary'"
            size="small"
            @click="toggleFollow(item.user.id, item.isFollowing)"
          >{{ item.isFollowing ? '✓ 已关注' : '+ 回关' }}</el-button>
          <el-button v-else type="success" size="small" @click="toggleFollow(item.user.id, true)">✓ 已关注</el-button>
          <el-button v-if="listType === 'followers'" type="danger" size="small" plain @click="removeFollower(item.user.id)">移除</el-button>
        </div>
      </div>
    </el-card>
    <el-empty v-if="!loading && !list.length" description="暂无数据" />
    <el-pagination
      v-if="totalPages > 1"
      v-model:current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      layout="prev, pager, next"
      class="mt-4"
      @current-change="loadList"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AppHeader from '@/shared/components/AppHeader.vue'
import { useAuthStore } from '@/stores/auth'
import request from '@/utils/request'

const auth = useAuthStore()
const loading = ref(true)
const list = ref([])
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)
const totalPages = ref(0)

const routeInfo = () => {
  const m = window.location.pathname.match(/\/user\/(\d+)\/(followers|following)/)
  return {
    userId: m ? Number(m[1]) : 0,
    listType: m ? m[2] : 'followers'
  }
}

const listType = computed(() => routeInfo().listType)
const userId = computed(() => routeInfo().userId)
const pageTitle = computed(() => listType.value === 'following' ? '关注列表' : '粉丝列表')

function goProfile() { window.location.href = '/user/profile?id=' + userId.value }

async function loadList(page = 1) {
  loading.value = true
  currentPage.value = page
  try {
    const { data } = await request.get(`/user/follow/${listType.value}`, {
      params: { userId: userId.value, page, pageSize }
    })
    if (data.code === 200) {
      list.value = data.data.list || []
      total.value = data.data.total || 0
      totalPages.value = Math.ceil(total.value / pageSize) || 1
    }
  } finally { loading.value = false }
}

async function toggleFollow(targetId, isFollowing) {
  const url = isFollowing ? `/user/unfollow/${targetId}` : `/user/follow/${targetId}`
  try {
    const { data } = await request.post(url)
    if (data.code === 200) await loadList(currentPage.value)
    else if (data.code === 401) ElMessage.warning('请先登录')
  } catch {}
}

async function removeFollower(targetId) {
  try {
    const { data } = await request.post(`/user/follow/remove-follower/${targetId}`)
    if (data.code === 200) await loadList(currentPage.value)
  } catch {}
}

onMounted(async () => {
  await auth.init()
  if (!userId.value) { loading.value = false; return }
  await loadList(1)
})
</script>

<style scoped>
.list-title { margin: 16px 0; }
.follow-row { display: flex; align-items: center; gap: 12px; }
.follow-info { flex: 1; }
.follow-name { font-weight: 600; margin-right: 8px; color: var(--el-color-primary); }
.follow-intro { font-size: 0.85rem; color: #888; margin: 4px 0 0; }
.follow-actions { display: flex; gap: 8px; }
.mb-2 { margin-bottom: 8px; }
.mt-4 { margin-top: 16px; }
</style>
