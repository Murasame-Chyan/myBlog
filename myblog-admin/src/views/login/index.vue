<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const loginForm = ref({
  email: '',
  password: '',
  captchaCode: '',
})
const captchaToken = ref('')
const captchaImage = ref('')
const loading = ref(false)

// 加载图形验证码（后端返回图片二进制 + X-Captcha-Token 响应头）
async function loadCaptcha() {
  try {
    // 注意：这里用原生 axios 而非 request 实例，因为验证码是图片不是 {code,msg,data}
    // 验证码接口在用户端 AuthController，路径是 /auth/captcha（不是 /api/...）
    const res = await axios.get('/auth/captcha', { responseType: 'blob' })
    captchaToken.value = res.headers['x-captcha-token']
    captchaImage.value = URL.createObjectURL(res.data)
  } catch (e) {
    ElMessage.error('验证码加载失败')
  }
}

async function handleLogin() {
  if (!loginForm.value.email || !loginForm.value.password) {
    ElMessage.warning('请输入邮箱和密码')
    return
  }
  if (!loginForm.value.captchaCode) {
    ElMessage.warning('请输入验证码')
    return
  }
  loading.value = true
  try {
    await userStore.login({
      email: loginForm.value.email,
      password: loginForm.value.password,
      captchaCode: loginForm.value.captchaCode,
      captchaToken: captchaToken.value,
    })
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e) {
    // 登录失败刷新验证码，避免旧验证码失效
    loadCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadCaptcha()
})
</script>

<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2 class="login-title">myBlog 后台管理</h2>
      <el-form @keyup.enter="handleLogin">
        <el-form-item>
          <el-input v-model="loginForm.email" placeholder="邮箱" size="large">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            size="large"
            show-password
          >
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <div class="captcha-row">
            <el-input
              v-model="loginForm.captchaCode"
              placeholder="验证码"
              size="large"
            />
            <img
              :src="captchaImage"
              class="captcha-img"
              title="点击刷新"
              @click="loadCaptcha"
            />
          </div>
        </el-form-item>
        <el-button
          type="primary"
          size="large"
          class="login-btn"
          :loading="loading"
          @click="handleLogin"
        >
          登录
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.login-container {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #304156 0%, #1f2d3d 100%);
}
.login-card {
  width: 400px;
  padding: 20px;
}
.login-title {
  text-align: center;
  margin-bottom: 24px;
  color: #304156;
}
.captcha-row {
  display: flex;
  gap: 8px;
  width: 100%;
}
.captcha-img {
  height: 40px;
  width: 120px;
  cursor: pointer;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
.login-btn {
  width: 100%;
}
</style>
