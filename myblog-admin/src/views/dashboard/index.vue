<script setup>
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
</script>

<template>
  <div>
    <el-card>
      <h2>欢迎回来，{{ userStore.userInfo.nickname || '管理员' }}</h2>
      <p style="margin-top: 12px; color: #909399">
        这是 myBlog 后台管理系统。通过左侧菜单管理用户、博客、评论、角色权限等。
      </p>
    </el-card>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="8">
        <el-card>
          <div class="stat-label">我的角色</div>
          <div class="stat-value">{{ userStore.roles.length }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <div class="stat-label">拥有权限</div>
          <div class="stat-value">{{ userStore.permissions.length }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <div class="stat-label">可见菜单</div>
          <div class="stat-value">{{ userStore.menus.length }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.stat-label {
  color: #909399;
  font-size: 13px;
}
.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  margin-top: 8px;
}
</style>
