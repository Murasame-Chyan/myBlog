<template>
  <el-dialog :model-value="visible" @update:model-value="$emit('update:visible',$event)" width="440px" :close-on-click-modal="false" center>
    <template #header><div class="auth-dialog-header"><span class="auth-logo">My<span class="auth-logo-hl">Blog</span></span></div></template>

    <el-tabs v-model="activeTab">
      <!-- 登录 -->
      <el-tab-pane label="登录" name="login">
        <el-form ref="loginFormRef" :model="lf" :rules="loginRules" label-position="top" @submit.prevent>
          <el-form-item label="邮箱" prop="email"><el-input v-model="lf.email" placeholder="请输入邮箱" maxlength="255" /></el-form-item>
          <el-form-item label="密码" prop="password"><el-input v-model="lf.password" type="password" placeholder="请输入密码" maxlength="255" show-password /></el-form-item>
          <el-form-item label="图形验证码" prop="captchaCode">
            <div class="captcha-row">
              <el-input v-model="lf.captchaCode" placeholder="不区分大小写" maxlength="4" />
              <img :src="captchaUrl" class="captcha-img" @click="refreshCaptcha" title="点击刷新" />
              <a class="captcha-refresh" @click="refreshCaptcha">换一张</a>
            </div>
          </el-form-item>
          <el-button type="primary" class="auth-submit-btn" @click="handleLogin" :loading="loginLoading">登 录</el-button>
        </el-form>
        <div class="auth-switch"><a @click="activeTab='reset'">忘记密码？</a> 还没有账号？<a @click="activeTab='register'">立即注册</a></div>
      </el-tab-pane>

      <!-- 注册 -->
      <el-tab-pane label="注册" name="register">
        <el-form ref="regFormRef" :model="rf" :rules="regRules" label-position="top" @submit.prevent>
          <el-form-item label="昵称" prop="nickname"><el-input v-model="rf.nickname" placeholder="如何称呼你？" maxlength="32" /></el-form-item>
          <el-form-item label="邮箱" prop="email"><el-input v-model="rf.email" placeholder="请输入邮箱" maxlength="255" /></el-form-item>
          <el-form-item label="密码" prop="password"><el-input v-model="rf.password" type="password" placeholder="至少 6 位密码" maxlength="255" show-password /></el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword"><el-input v-model="rf.confirmPassword" type="password" placeholder="再次输入密码" maxlength="255" show-password /></el-form-item>
          <el-form-item label="邮箱验证码" prop="emailCode">
            <div class="captcha-row">
              <el-input v-model="rf.emailCode" placeholder="6位数字验证码" maxlength="6" />
              <el-button :disabled="sendCd > 0" @click="sendRegCode" size="small">{{ sendCd > 0 ? sendCd+'s' : '发送验证码' }}</el-button>
            </div>
          </el-form-item>
          <el-button type="primary" class="auth-submit-btn" @click="handleRegister" :loading="regLoading">注 册</el-button>
        </el-form>
        <div class="auth-switch">已有账号？<a @click="activeTab='login'">去登录</a></div>
      </el-tab-pane>

      <!-- 重置密码 -->
      <el-tab-pane label="忘记密码" name="reset">
        <el-form ref="resetFormRef" :model="zf" :rules="resetRules" label-position="top" @submit.prevent>
          <el-form-item label="邮箱" prop="email"><el-input v-model="zf.email" placeholder="请输入注册邮箱" maxlength="255" /></el-form-item>
          <el-form-item label="邮箱验证码" prop="emailCode">
            <div class="captcha-row">
              <el-input v-model="zf.emailCode" placeholder="6位数字验证码" maxlength="6" />
              <el-button :disabled="resetCd > 0" @click="sendResetCode" size="small">{{ resetCd > 0 ? resetCd+'s' : '发送验证码' }}</el-button>
            </div>
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword"><el-input v-model="zf.newPassword" type="password" placeholder="至少 6 位新密码" maxlength="255" show-password /></el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword"><el-input v-model="zf.confirmPassword" type="password" placeholder="再次输入新密码" maxlength="255" show-password /></el-form-item>
          <el-button type="primary" class="auth-submit-btn" @click="handleReset" :loading="resetLoading">重置密码</el-button>
        </el-form>
        <div class="auth-switch"><a @click="activeTab='login'">返回登录</a></div>
      </el-tab-pane>
    </el-tabs>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useAuthStore } from '@/stores/auth'

defineProps({ visible: Boolean })
const emit = defineEmits(['update:visible', 'login-success'])
const auth = useAuthStore()
const activeTab = ref('login')

// ===== captcha =====
const captchaToken = ref('')
const captchaUrl = ref('')

async function refreshCaptcha() {
  try {
    const resp = await fetch('/auth/captcha')
    captchaToken.value = resp.headers.get('X-Captcha-Token') || ''
    const blob = await resp.blob()
    captchaUrl.value = URL.createObjectURL(blob)
  } catch {}
}
onMounted(() => refreshCaptcha())

// ===== login =====
const loginFormRef = ref(null), loginLoading = ref(false)
const lf = reactive({ email: '', password: '', captchaCode: '' })
const loginRules = {
  email: [{ required: true, message: '请输入邮箱' }, { type: 'email', message: '格式不正确' }],
  password: [{ required: true, message: '请输入密码' }],
  captchaCode: [{ required: true, message: '请输入验证码' }]
}

async function handleLogin() {
  const valid = await loginFormRef.value?.validate().catch(() => false); if (!valid) return
  loginLoading.value = true
  try {
    const p = new URLSearchParams()
    p.append('email', lf.email); p.append('password', lf.password)
    p.append('captchaCode', lf.captchaCode); p.append('captchaToken', captchaToken.value)
    const { data } = await request.post('/auth/login', p)
    if (data.code === 200) {
      auth.loginSuccess(data.data.accessToken, data.data.refreshToken, data.data.nickname, data.data.avatar)
      ElMessage.success('登录成功'); emit('login-success')
    } else refreshCaptcha()
  } finally { loginLoading.value = false }
}

// ===== register =====
const regFormRef = ref(null), regLoading = ref(false), sendCd = ref(0)
const rf = reactive({ nickname: '', email: '', password: '', confirmPassword: '', emailCode: '' })
const regRules = {
  nickname: [{ required: true, message: '请输入昵称' }],
  email: [{ required: true, message: '请输入邮箱' }, { type: 'email', message: '格式不正确' }],
  password: [{ required: true, min: 6, message: '密码至少6位' }],
  confirmPassword: [{ required: true, message: '请确认密码' }, { validator: (r, v, cb) => cb(v !== rf.password ? new Error('两次输入不一致') : undefined), trigger: 'blur' }],
  emailCode: [{ required: true, message: '请输入验证码' }]
}

async function sendRegCode() {
  try {
    await request.post('/auth/send-code', new URLSearchParams({ email: rf.email }))
    ElMessage.success('验证码已发送')
    sendCd.value = 60; const t = setInterval(() => { sendCd.value--; if (sendCd.value <= 0) clearInterval(t) }, 1000)
  } catch {}
}

async function handleRegister() {
  const valid = await regFormRef.value?.validate().catch(() => false); if (!valid) return
  regLoading.value = true
  try {
    const p = new URLSearchParams(); p.append('nickname', rf.nickname); p.append('email', rf.email)
    p.append('password', rf.password); p.append('emailCode', rf.emailCode)
    const { data } = await request.post('/auth/register', p)
    if (data.code === 200) {
      auth.loginSuccess(data.data.accessToken, data.data.refreshToken, data.data.nickname, data.data.avatar)
      ElMessage.success('注册成功'); emit('login-success')
    }
  } finally { regLoading.value = false }
}

// ===== reset password =====
const resetFormRef = ref(null), resetLoading = ref(false), resetCd = ref(0)
const zf = reactive({ email: '', emailCode: '', newPassword: '', confirmPassword: '' })
const resetRules = {
  email: [{ required: true, message: '请输入邮箱' }, { type: 'email', message: '格式不正确' }],
  emailCode: [{ required: true, message: '请输入验证码' }],
  newPassword: [{ required: true, min: 6, message: '密码至少6位' }],
  confirmPassword: [{ required: true, message: '请确认密码' }, { validator: (r, v, cb) => cb(v !== zf.newPassword ? new Error('两次输入不一致') : undefined), trigger: 'blur' }]
}

async function sendResetCode() {
  try {
    await request.post('/auth/send-reset-code', new URLSearchParams({ email: zf.email }))
    ElMessage.success('验证码已发送')
    resetCd.value = 60; const t = setInterval(() => { resetCd.value--; if (resetCd.value <= 0) clearInterval(t) }, 1000)
  } catch {}
}

async function handleReset() {
  const valid = await resetFormRef.value?.validate().catch(() => false); if (!valid) return
  resetLoading.value = true
  try {
    const p = new URLSearchParams(); p.append('email', zf.email)
    p.append('newPassword', zf.newPassword); p.append('emailCode', zf.emailCode)
    const { data } = await request.post('/auth/reset-password', p)
    if (data.code === 200) { ElMessage.success('密码重置成功'); activeTab.value = 'login' }
  } finally { resetLoading.value = false }
}
</script>

<style scoped>
.auth-dialog-header { text-align: center; } .auth-logo { font-size: 1.5rem; font-weight: bold; }
.auth-logo-hl { color: var(--el-color-primary); } .auth-submit-btn { width: 100%; margin-top: 12px; height: 44px; font-size: 1rem; }
.auth-switch { text-align: center; margin-top: 16px; font-size: 0.85rem; color: #999; }
.auth-switch a { color: var(--el-color-primary); cursor: pointer; }
.captcha-row { display: flex; gap: 10px; align-items: center; width: 100%; }
.captcha-img { height: 40px; cursor: pointer; border-radius: 6px; border: 1px solid #e0e0e0; }
.captcha-refresh { font-size: 0.78rem; color: var(--el-color-primary); cursor: pointer; white-space: nowrap; }
</style>
