<template>
  <AppHeader current-page="index" />
  <div class="page-container">
    <el-result icon="error" title="出错了！" :sub-title="errorMsg">
      <template #extra>
        <el-button type="primary" @click="goHome">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AppHeader from '@/shared/components/AppHeader.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const errorMsg = ref('页面发生错误')

function goHome() { window.location.href = '/' }

onMounted(async () => {
  await auth.init()
  const params = new URLSearchParams(window.location.search)
  if (params.get('msg')) errorMsg.value = params.get('msg')
})
</script>
