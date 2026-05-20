<template>
  <el-menu mode="horizontal" :default-active="currentPage" class="navbar-custom" @select="handleSelect">
    <div class="header-container">
      <el-menu-item index="index" class="logo-item">
        <span class="logo-text">MyBlog</span>
      </el-menu-item>

      <div class="avatar-area">
        <el-dropdown v-if="auth.loggedIn" trigger="click" @command="handleAvatarCommand">
          <el-avatar :size="40" :src="auth.avatar || '/images/default-avatar.png'" class="header-avatar" />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile"><el-icon><User /></el-icon> 个人主页</el-dropdown-item>
              <el-dropdown-item command="editProfile"><el-icon><Edit /></el-icon> 编辑资料</el-dropdown-item>
              <el-dropdown-item command="archives"><el-icon><FolderOpened /></el-icon> 我的归档</el-dropdown-item>
              <el-dropdown-item command="logout" divided><el-icon><SwitchButton /></el-icon> 退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <el-avatar v-else :size="40" src="/images/default-avatar.png" class="header-avatar" style="cursor:pointer" @click="showAuth = true" />
      </div>

      <div class="header-right">
        <el-menu-item index="index">首页</el-menu-item>
        <el-button type="primary" round class="write-btn" @click="goWrite"><el-icon><Edit /></el-icon> 写作</el-button>
      </div>
    </div>
  </el-menu>

  <AuthModal v-model:visible="showAuth" @login-success="onLogin" />
  <EditProfileModal v-if="auth.loggedIn && showEdit" v-model:visible="showEdit" @saved="onSaved" />
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { User, Edit, FolderOpened, SwitchButton } from '@element-plus/icons-vue'
import AuthModal from './AuthModal.vue'
import EditProfileModal from './EditProfileModal.vue'

defineProps({ currentPage: { type: String, default: 'index' } })

const auth = useAuthStore()
const showAuth = ref(false)
const showEdit = ref(false)

function handleSelect(index) { if (index === 'index') window.location.href = '/' }
function goWrite() { window.location.href = '/blogs/write' }
function handleAvatarCommand(cmd) {
  if (cmd === 'profile') window.location.href = '/user/profile'
  else if (cmd === 'editProfile') showEdit.value = true
  else if (cmd === 'archives') window.location.href = '/archives'
  else if (cmd === 'logout') auth.logout().then(() => { ElMessage.success('已退出'); window.location.reload() })
}
function onLogin() { showAuth.value = false; window.location.reload() }
function onSaved() { showEdit.value = false; window.location.reload() }
</script>

<style scoped>
.header-container { display: flex; align-items: center; width: 100%; max-width: 1200px; margin: 0 auto; padding: 0 16px; }
.logo-item { border-bottom: none !important; }
.logo-text { font-weight: bold; font-size: 1.5rem; color: #fff; }
.avatar-area { flex: 1; display: flex; justify-content: center; }
.header-avatar { cursor: pointer; border: 2px solid rgba(255,255,255,0.5); }
.header-avatar:hover { border-color: #fff; }
.header-right { display: flex; align-items: center; gap: 8px; }
.write-btn { margin-left: 8px; }
.navbar-custom { background-color: rgba(135,206,235,0.8) !important; backdrop-filter: blur(10px); }
</style>
