# Vue3 + Element Plus 前端迁移 — Cursor 执行计划

> 后端 JWT API 已就绪。此文档供 Cursor 逐任务执行前端部分。

## 环境

- 工作目录：`src/main/frontend/`（项目根目录下的子目录）
- 构建输出：`src/main/resources/static/dist/`
- 后端开发服务器：`http://localhost:8080`
- Maven: `export JAVA_HOME="C:/Program Files/Microsoft/jdk-17.0.18.8-hotspot" && "C:/Program Files/JetBrains/IntelliJ IDEA 2026.1.1/plugins/maven/lib/maven3/bin/mvn" spring-boot:run -f pom.xml`

## 已就绪的后端 API

所有接口返回 JSON 格式 `{ code: 200, msg: "...", data: ... }` 或 `{ code: 401, msg: "..." }`。

| 端点 | 方法 | 说明 |
|------|------|------|
| `/auth/login` | POST | 参数: email, password, captchaCode, captchaToken。返回 accessToken, refreshToken, expiresIn, nickname, avatar |
| `/auth/register` | POST | 参数: email, nickname, password, emailCode。返回 accessToken, refreshToken |
| `/auth/captcha` | GET | 返回图片，响应头 X-Captcha-Token 是本次验证码令牌 |
| `/auth/send-code` | POST | 参数: email。发送注册验证码 |
| `/auth/send-reset-code` | POST | 参数: email。发送重置密码验证码 |
| `/auth/reset-password` | POST | 参数: email, newPassword, emailCode |
| `/auth/refresh` | POST | 参数: refreshToken。返回新 accessToken + refreshToken |
| `/auth/logout` | POST | 需 Authorization 头 |
| `/auth/status` | GET | 需 Authorization 头，返回 loggedIn, nickname |
| `/api/blogs` | GET | 参数: keyword?, dateFrom?, dateTo?, sortBy?, page, pageSize。返回 blogs[], totalBlogs, totalPages |
| `/api/tags` | GET | 返回标签数组 [{id, tagName}] |
| `/api/hot-blogs` | GET | 返回热门文章数组 |
| `/api/recent-comments` | GET | 返回最新评论数组 |
| `/api/blogs/{id}` | GET | 返回 blog, comments, commentCount, authorName, authorAvatar, liked |
| `/api/user/profile/{id}` | GET | 返回用户资料 |
| `/api/user/profile-data` | GET | 需登录，返回当前用户完整资料（用于编辑） |
| `/blogs/publish` | POST | 需登录，参数: title, content, tagIds?, newTagNames? |
| `/blogs/update` | POST | 需登录，参数: id, title, content, tagIds?, newTagNames? |
| `/blogs/delete/{id}` | POST | 需登录 |
| `/blogs/recover/{id}` | POST | 需登录 |
| `/blogs/like/{id}` | POST | 需登录 |
| `/blogs/unlike/{id}` | POST | 需登录 |
| `/blogs/isLiked/{id}` | GET | 需登录 |
| `/blogs/incrementRead/{id}` | POST | 增加阅读量 |
| `/user/comment/add` | POST | 需登录，参数: bId, content, parentId? |
| `/user/avatar/upload` | POST | 需登录，multipart file |
| `/user/profile/update` | POST | 需登录，参数: nickname, intro, email, gender, githubUsername, githubToken? |

**JWT 使用方式：**
- 登录/注册后获得 `accessToken` 和 `refreshToken`，存 localStorage
- 所有请求带 `Authorization: Bearer <accessToken>` 头
- 收到 401 时，用 refreshToken 调 `/auth/refresh` 换取新 token

---

## 前端技术栈

- Vue 3.5 + Composition API（JavaScript，不用 TypeScript）
- Vite 6（多入口构建，每个页面独立入口）
- Element Plus 2.9（组件库，替代 Bootstrap）
- Axios（HTTP 客户端，统一拦截器）
- Pinia（登录态管理）
- marked + DOMPurify（Markdown 渲染 + XSS 清洗）
- @element-plus/icons-vue（图标）

---

## 项目文件结构

```
src/main/frontend/
├── package.json
├── vite.config.js
├── src/
│   ├── utils/
│   │   ├── token.js          # localStorage token 读写
│   │   ├── request.js         # Axios 实例 + JWT 拦截器
│   │   └── markdown.js        # marked + DOMPurify
│   ├── stores/
│   │   └── auth.js            # Pinia 登录态
│   ├── shared/
│   │   ├── components/
│   │   │   ├── AppHeader.vue         # 导航栏 + 头像下拉
│   │   │   ├── AuthModal.vue         # 登录/注册/重置密码弹窗
│   │   │   ├── EditProfileModal.vue  # 编辑资料弹窗
│   │   │   ├── CommentSection.vue    # 评论树组件
│   │   │   └── WeatherWidget.vue     # 天气小组件
│   │   └── styles/
│   │       └── global.scss           # 全局样式 + Element Plus 变量覆盖
│   └── pages/
│       ├── index/
│       │   ├── index.html        # Vite 入口 HTML
│       │   ├── main.js           # Vue 挂载入口
│       │   └── IndexPage.vue     # 博客列表页
│       ├── readBlog/
│       │   ├── index.html
│       │   ├── main.js
│       │   └── ReadBlogPage.vue  # 博客详情页
│       ├── writeBlog/
│       │   ├── index.html
│       │   ├── main.js
│       │   └── WriteBlogPage.vue # 写作/编辑页
│       ├── profile/
│       │   ├── index.html
│       │   ├── main.js
│       │   └── ProfilePage.vue   # 用户主页
│       ├── archives/
│       │   ├── index.html
│       │   ├── main.js
│       │   └── ArchivesPage.vue  # 归档页
│       ├── readArchive/
│       │   ├── index.html
│       │   ├── main.js
│       │   └── ReadArchivePage.vue
│       ├── followList/
│       │   ├── index.html
│       │   ├── main.js
│       │   └── FollowListPage.vue
│       └── error/
│           ├── index.html
│           ├── main.js
│           └── ErrorPage.vue     # 错误页
```

---

## Task 1: 项目搭建

### 1.1 创建 package.json

路径：`src/main/frontend/package.json`

```json
{
  "name": "myblog-frontend",
  "private": true,
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "vue": "^3.5.13",
    "element-plus": "^2.9.1",
    "@element-plus/icons-vue": "^2.3.1",
    "axios": "^1.7.9",
    "pinia": "^2.3.0",
    "marked": "^15.0.4",
    "dompurify": "^3.2.4"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.2.1",
    "vite": "^6.0.7",
    "sass-embedded": "^1.83.4"
  }
}
```

### 1.2 创建 vite.config.js

路径：`src/main/frontend/vite.config.js`

```javascript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import { fileURLToPath } from 'url'

const __dirname = fileURLToPath(new URL('.', import.meta.url))

const pages = ['index', 'readBlog', 'writeBlog', 'profile', 'archives', 'readArchive', 'followList', 'error']

function buildInput() {
  const input = {}
  for (const page of pages) {
    input[page] = resolve(__dirname, `src/pages/${page}/index.html`)
  }
  return input
}

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': resolve(__dirname, 'src') }
  },
  build: {
    outDir: resolve(__dirname, '../resources/static/dist'),
    emptyOutDir: true,
    rollupOptions: {
      input: buildInput()
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/auth': 'http://localhost:8080',
      '/api': 'http://localhost:8080',
      '/blogs': 'http://localhost:8080',
      '/user': 'http://localhost:8080',
      '/archives': 'http://localhost:8080',
      '/images': 'http://localhost:8080',
      '/pics': 'http://localhost:8080',
      '/css': 'http://localhost:8080',
      '/js': 'http://localhost:8080'
    }
  }
})
```

### 1.3 创建目录结构并安装依赖

```bash
mkdir -p src/main/frontend/src/{utils,stores,shared/{components,styles},pages/{index,readBlog,writeBlog,profile,archives,readArchive,followList,error},assets}
cd src/main/frontend && npm install
```

---

## Task 2: 共享工具层

### 2.1 token.js

路径：`src/main/frontend/src/utils/token.js`

```javascript
const ACCESS_KEY = 'myblog_access_token'
const REFRESH_KEY = 'myblog_refresh_token'

export function getAccessToken() { return localStorage.getItem(ACCESS_KEY) }
export function getRefreshToken() { return localStorage.getItem(REFRESH_KEY) }
export function setTokens(access, refresh) {
  localStorage.setItem(ACCESS_KEY, access)
  localStorage.setItem(REFRESH_KEY, refresh)
}
export function clearTokens() {
  localStorage.removeItem(ACCESS_KEY)
  localStorage.removeItem(REFRESH_KEY)
}
```

### 2.2 request.js

路径：`src/main/frontend/src/utils/request.js`

```javascript
import axios from 'axios'
import { getAccessToken, getRefreshToken, setTokens, clearTokens } from './token'
import { ElMessage } from 'element-plus'

const request = axios.create({ baseURL: '/', timeout: 15000 })

// 请求拦截器：附加 Authorization 头
request.interceptors.request.use(config => {
  const token = getAccessToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截器：401 自动刷新令牌
let isRefreshing = false
let refreshSubscribers = []

function onRefreshed(token) {
  refreshSubscribers.forEach(cb => cb(token))
  refreshSubscribers = []
}

request.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config
    if (error.response?.status === 401 && !originalRequest._retry) {
      const refreshToken = getRefreshToken()
      if (!refreshToken) { clearTokens(); return Promise.reject(error) }

      if (isRefreshing) {
        return new Promise(resolve => {
          refreshSubscribers.push(token => {
            originalRequest.headers.Authorization = `Bearer ${token}`
            originalRequest._retry = true
            resolve(request(originalRequest))
          })
        })
      }

      isRefreshing = true
      originalRequest._retry = true
      try {
        const { data } = await axios.post('/auth/refresh',
          new URLSearchParams({ refreshToken }),
          { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } })
        if (data.code === 200) {
          setTokens(data.data.accessToken, data.data.refreshToken)
          onRefreshed(data.data.accessToken)
          originalRequest.headers.Authorization = `Bearer ${data.data.accessToken}`
          return request(originalRequest)
        }
      } catch {}
      clearTokens()
      ElMessage.error('登录已过期，请重新登录')
      return Promise.reject(error)
    }
    const msg = error.response?.data?.msg
    if (msg && !originalRequest._silent) ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default request
```

### 2.3 markdown.js

路径：`src/main/frontend/src/utils/markdown.js`

```javascript
import { marked } from 'marked'
import DOMPurify from 'dompurify'

marked.setOptions({ breaks: true, gfm: true })

export function renderMarkdown(md) {
  if (!md) return ''
  return DOMPurify.sanitize(marked.parse(md), {
    ALLOWED_TAGS: ['h1','h2','h3','h4','h5','h6','p','br','hr','ul','ol','li',
      'blockquote','pre','code','strong','em','del','ins','sub','sup',
      'a','img','table','thead','tbody','tr','th','td','div','span'],
    ALLOWED_ATTR: ['href','src','alt','title','class','id','target','rel']
  })
}

export function extractBrief(md, max = 200) {
  if (!md) return ''
  const text = md.replace(/[#*`>\[\]()!_~|]/g, '').replace(/<[^>]*>/g, '').replace(/\s+/g, ' ').trim()
  return text.length > max ? text.substring(0, max) + '...' : text
}
```

### 2.4 auth.js store

路径：`src/main/frontend/src/stores/auth.js`

```javascript
import { defineStore } from 'pinia'
import { getAccessToken, setTokens, clearTokens } from '@/utils/token'
import request from '@/utils/request'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    loggedIn: !!getAccessToken(),
    nickname: '',
    avatar: ''
  }),
  actions: {
    async init() {
      const token = getAccessToken()
      if (!token) { this.loggedIn = false; return }
      try {
        const { data } = await request.get('/auth/status', { _silent: true })
        if (data.code === 200 && data.data?.loggedIn) {
          this.loggedIn = true
          this.nickname = data.data.nickname || ''
          this.avatar = data.data.avatar || ''
        } else { this.logout() }
      } catch { this.loggedIn = false }
    },
    loginSuccess(accessToken, refreshToken, nickname, avatar) {
      setTokens(accessToken, refreshToken)
      this.loggedIn = true
      this.nickname = nickname || ''
      this.avatar = avatar || ''
    },
    async logout() {
      try { await request.post('/auth/logout', {}, { _silent: true }) } catch {}
      clearTokens()
      this.loggedIn = false
      this.nickname = ''
      this.avatar = ''
    }
  }
})
```

### 2.5 global.scss

路径：`src/main/frontend/src/shared/styles/global.scss`

```scss
:root {
  --el-color-primary: #5BA4CF;
  --el-color-primary-light-3: #87CEEB;
  --el-border-radius-base: 8px;
}

* { box-sizing: border-box; }

body {
  margin: 0;
  background-color: #f5f7fa;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}

a { text-decoration: none; color: var(--el-color-primary); }

.page-container { max-width: 1200px; margin: 0 auto; padding: 20px 16px; }

.blog-card { cursor: pointer; transition: transform 0.2s, box-shadow 0.2s; }
.blog-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.1); }

.tag-badge {
  display: inline-block; padding: 4px 12px; margin: 3px;
  border-radius: 12px; background: #e8f4fd; color: #5BA4CF;
  font-size: 0.85rem; transition: all 0.2s; cursor: pointer;
  &:hover, &.active { background: #5BA4CF; color: #fff; }
}

.author-avatar-mini { width: 22px; height: 22px; object-fit: cover; border-radius: 50%; margin-right: 4px; vertical-align: middle; }

// 博客正文渲染样式
.blog-content {
  line-height: 1.8; color: #333;
  h1,h2,h3,h4,h5,h6 { margin: 1.2em 0 0.6em; }
  p { margin: 0.8em 0; }
  img { max-width: 100%; border-radius: 4px; }
  pre { background: #f4f4f4; padding: 12px 16px; border-radius: 6px; overflow-x: auto; }
  code { background: #f4f4f4; padding: 2px 6px; border-radius: 3px; font-size: 0.9em; }
  blockquote { border-left: 4px solid #5BA4CF; padding-left: 16px; margin: 12px 0; color: #666; }
  table { border-collapse: collapse; width: 100%; margin: 12px 0; }
  th,td { border: 1px solid #e0e0e0; padding: 8px 12px; text-align: left; }
  th { background: #f8f9fa; }
}
```

---

## Task 3: 共享组件

### 3.1 AppHeader.vue

路径：`src/main/frontend/src/shared/components/AppHeader.vue`

```vue
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
```

### 3.2 AuthModal.vue

路径：`src/main/frontend/src/shared/components/AuthModal.vue`

```vue
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
```

### 3.3 CommentSection.vue

路径：`src/main/frontend/src/shared/components/CommentSection.vue`

```vue
<template>
  <div class="comment-section">
    <el-divider />
    <h4>评论 ({{ comments.length }})</h4>

    <div v-if="auth.loggedIn" class="comment-input-box">
      <el-input v-model="newComment" type="textarea" :rows="3" placeholder="写下你的评论..." maxlength="500" show-word-limit />
      <el-button type="primary" size="small" class="comment-submit-btn" @click="submitComment" :loading="submitting">发表评论</el-button>
    </div>
    <el-alert v-else title="请先登录后发表评论" type="info" :closable="false" show-icon />

    <el-empty v-if="comments.length === 0" description="暂无评论" />

    <div v-for="c in comments" :key="c.id" class="comment-item">
      <div class="comment-header">
        <img :src="c.author_avatar || '/images/default-avatar.png'" class="comment-avatar" />
        <span class="comment-author">{{ c.author_name }}</span>
        <span class="comment-time">{{ formatTime(c.created_at) }}</span>
      </div>
      <div class="comment-body" v-html="c.content"></div>
      <el-button text size="small" @click="toggleReply(c.id)"><el-icon><ChatLineSquare /></el-icon> 回复</el-button>

      <div v-if="replyTarget === c.id" class="reply-box">
        <el-input v-model="replyContent" type="textarea" :rows="2" placeholder="写下回复..." maxlength="500" show-word-limit />
        <el-button type="primary" size="small" @click="submitReply(c.id)" :loading="replySub">回复</el-button>
        <el-button size="small" @click="replyTarget = null">取消</el-button>
      </div>

      <div v-if="c.children?.length" class="child-comments">
        <div v-for="ch in c.children" :key="ch.id" class="comment-item child">
          <div class="comment-header">
            <img :src="ch.author_avatar || '/images/default-avatar.png'" class="comment-avatar" />
            <span class="comment-author">{{ ch.author_name }}</span>
            <span class="comment-time">{{ formatTime(ch.created_at) }}</span>
          </div>
          <div class="comment-body" v-html="ch.content"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatLineSquare } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import request from '@/utils/request'

const props = defineProps({ blogId: { type: Number, required: true }, comments: { type: Array, default: () => [] } })
const emit = defineEmits(['comment-added'])
const auth = useAuthStore()

const newComment = ref(''), submitting = ref(false)
const replyTarget = ref(null), replyContent = ref(''), replySub = ref(false)

function formatTime(d) { return d ? new Date(d).toLocaleString('zh-CN', { year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit' }) : '' }

async function submitComment() {
  if (!newComment.value.trim()) return
  submitting.value = true
  try {
    const { data } = await request.post('/user/comment/add', new URLSearchParams({ bId: props.blogId, content: newComment.value.trim() }))
    if (data.code === 200) { ElMessage.success('评论成功'); newComment.value = ''; emit('comment-added') }
  } finally { submitting.value = false }
}

function toggleReply(id) { replyTarget.value = replyTarget.value === id ? null : id; replyContent.value = '' }

async function submitReply(parentId) {
  if (!replyContent.value.trim()) return
  replySub.value = true
  try {
    const { data } = await request.post('/user/comment/add', new URLSearchParams({ bId: props.blogId, parentId, content: replyContent.value.trim() }))
    if (data.code === 200) { ElMessage.success('回复成功'); replyTarget.value = null; emit('comment-added') }
  } finally { replySub.value = false }
}
</script>

<style scoped>
.comment-section { margin-top: 24px; } .comment-input-box { margin-bottom: 16px; } .comment-submit-btn { margin-top: 8px; }
.comment-item { padding: 16px 0; border-bottom: 1px solid #f0f0f0; } .comment-item.child { margin-left: 40px; }
.comment-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.comment-avatar { width: 32px; height: 32px; border-radius: 50%; object-fit: cover; }
.comment-author { font-weight: 600; } .comment-time { font-size: 0.8rem; color: #999; margin-left: auto; }
.comment-body { color: #555; line-height: 1.6; }
.reply-box { margin: 8px 0; display: flex; gap: 8px; flex-wrap: wrap; }
.child-comments { margin-top: 12px; border-left: 2px solid #e8f4fd; padding-left: 12px; }
</style>
```

### 3.4 EditProfileModal.vue

路径：`src/main/frontend/src/shared/components/EditProfileModal.vue`

```vue
<template>
  <el-dialog :model-value="visible" @update:model-value="$emit('update:visible',$event)" title="编辑资料" width="480px" :close-on-click-modal="false">
    <el-form label-position="top">
      <el-form-item label="头像">
        <div class="edit-avatar-area">
          <el-avatar :size="80" :src="avatarPreview" />
          <div><input ref="fi" type="file" accept="image/*" style="display:none" @change="onFile"><el-button size="small" @click="$refs.fi.click()">上传新头像</el-button><div v-if="avatarMsg" class="avatar-msg">{{ avatarMsg }}</div></div>
        </div>
      </el-form-item>
      <el-form-item label="昵称"><el-input v-model="form.nickname" maxlength="32" /></el-form-item>
      <el-form-item label="简介"><el-input v-model="form.intro" maxlength="255" /></el-form-item>
      <el-form-item label="邮箱"><el-input v-model="form.email" maxlength="255" /></el-form-item>
      <el-form-item label="性别">
        <el-select v-model="form.gender">
          <el-option label="未设置" :value="0" /><el-option label="男" :value="1" /><el-option label="女" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="GitHub 用户名"><el-input v-model="form.githubUsername" maxlength="255" /></el-form-item>
      <el-form-item>
        <template #label>GitHub Token <span class="subtle-hint">（<a href="https://github.com/settings/tokens" target="_blank">获取方式</a>，需勾选 read:user）</span></template>
        <el-input v-model="form.githubToken" type="password" maxlength="255" placeholder="ghp_xxx" autocomplete="off" />
      </el-form-item>
    </el-form>
    <template #footer><el-button @click="$emit('update:visible',false)">取消</el-button><el-button type="primary" @click="save" :loading="saving">保存</el-button></template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

defineProps({ visible: Boolean })
const emit = defineEmits(['update:visible', 'saved'])
const avatarPreview = ref('/images/default-avatar.png'), avatarMsg = ref(''), saving = ref(false)
const form = reactive({ nickname:'',intro:'',email:'',gender:0,githubUsername:'',githubToken:'' })

onMounted(async () => {
  try {
    const { data } = await request.get('/api/user/profile-data'); if (data.code === 200) { const u = data.data; Object.assign(form, { nickname:u.nickname||'',intro:u.intro||'',email:u.email||'',gender:u.gender||0,githubUsername:u.githubUsername||'' }); avatarPreview.value = u.avatar || '/images/default-avatar.png' }
  } catch {}
})

async function onFile(e) {
  const file = e.target.files?.[0]; if (!file) return
  if (file.size > 10*1024*1024) { avatarMsg.value = '图片不能超过10MB'; return }
  avatarMsg.value = '上传中...'; const fd = new FormData(); fd.append('file', file)
  try { const { data } = await request.post('/user/avatar/upload', fd); if (data.code === 200) { avatarPreview.value = data.data+'?t='+Date.now(); avatarMsg.value = '头像已更新' } } catch { avatarMsg.value = '上传失败' }
  e.target.value = ''
}

async function save() {
  saving.value = true
  try {
    const p = new URLSearchParams(); p.append('nickname',form.nickname); p.append('intro',form.intro); p.append('email',form.email); p.append('gender',form.gender); p.append('githubUsername',form.githubUsername); if (form.githubToken) p.append('githubToken',form.githubToken)
    const { data } = await request.post('/user/profile/update', p)
    if (data.code === 200) { ElMessage.success('保存成功'); emit('saved') }
  } finally { saving.value = false }
}
</script>

<style scoped>
.edit-avatar-area { display: flex; gap: 16px; align-items: center; } .avatar-msg { font-size: 0.72rem; color: #999; margin-top: 4px; }
.subtle-hint { font-size: 0.68rem; color: #aaa; font-weight: 400; } .subtle-hint a { color: var(--el-color-primary); }
</style>
```

### 3.5 WeatherWidget.vue

路径：`src/main/frontend/src/shared/components/WeatherWidget.vue`

```vue
<template>
  <el-card class="weather-card">
    <div v-if="loading" class="weather-loading"><img src="/pics/weather/loading.svg" width="40" alt="loading" /></div>
    <div v-else-if="error" class="weather-error">天气不可用</div>
    <div v-else class="weather-content">
      <img :src="iconSrc" :alt="weather.text" class="weather-icon" />
      <span class="weather-temp">{{ weather.temperature }}°C</span>
      <span class="weather-text">{{ weather.text }}</span>
    </div>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
const weather = ref({ temperature: '--', text: '加载中' })
const loading = ref(true), error = ref(false)
const iconSrc = ref('/pics/weather/loading.svg')

const iconMap = { '晴': 'sunny', '多云': 'cloudy', '阴': 'cloudy', '雨': 'rainy', '雪': 'snowy', '雾': 'foggy' }

onMounted(async () => {
  try {
    const resp = await fetch('/api/weather/now?location=beijing')
    const data = await resp.json()
    if (data.code === 200 && data.data) {
      weather.value = data.data
      const w = data.data.text || ''
      let icon = 'unknown'
      for (const [k, v] of Object.entries(iconMap)) { if (w.includes(k)) { icon = v; break } }
      iconSrc.value = `/pics/weather/${icon}.svg`
    } else error.value = true
  } catch { error.value = true }
  loading.value = false
})
</script>

<style scoped>
.weather-card { text-align: center; } .weather-content { display: flex; align-items: center; gap: 8px; justify-content: center; }
.weather-icon { width: 40px; height: 40px; } .weather-temp { font-size: 1.5rem; font-weight: bold; } .weather-text { color: #666; }
</style>
```

---

## Task 4: 逐页实现

每个页面遵循相同模式：
1. 创建 `src/main/frontend/src/pages/<name>/index.html`（极简 HTML）
2. 创建 `src/main/frontend/src/pages/<name>/main.js`（Vue 挂载入口）
3. 创建 `src/main/frontend/src/pages/<name>/<PageName>.vue`（页面组件）

### 4.1 index.html 模板（每个页面复制此模板）

```html
<!DOCTYPE html>
<html lang="zh">
<head><meta charset="UTF-8"><title><!-- 页面标题 --></title></head>
<body><div id="app"></div><script type="module" src="./main.js"></script></body>
</html>
```

### 4.2 main.js 模板（每个页面复制此模板，改 import 组件名）

```javascript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import 'element-plus/dist/index.css'
import '@/shared/styles/global.scss'

// 改这里：导入对应页面的 Vue 组件
import IndexPage from './IndexPage.vue'

const app = createApp(IndexPage)
app.use(createPinia())
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
```

### 4.3 IndexPage.vue — 博客列表页

这是最复杂的页面。要求：
- 三栏布局：左（天气 + 标签）、中（博客卡片列表 + 分页）、右（热门文章 + 最新评论）
- 使用 `el-row`/`el-col` 布局，`el-card` 做博客卡片，`el-pagination` 分页
- 搜索栏：`el-input` + `el-button`，可选展开筛选面板（时间范围 `el-radio-group`、排序 `el-radio-group`）
- 调用 API：`/api/blogs`、`/api/tags`、`/api/hot-blogs`、`/api/recent-comments`
- 加载态：`el-skeleton`
- 点击卡片跳转 `/blogs/read/{id}`，点击标签跳转 `/blogs/tag/{id}`

index.html: `<title>博客主页</title>`

### 4.4 ReadBlogPage.vue — 博客详情页

要求：
- 博客正文用 `v-html` 渲染（经过 `renderMarkdown()`），加 class `blog-content`
- 元信息栏：作者头像+昵称、日期、阅读量、点赞数
- 点赞按钮：根据 `liked` 状态切换，调用 `/blogs/like/{id}` 或 `/blogs/unlike/{id}`
- 页面加载后延迟调用 `/blogs/incrementRead/{id}` 增加阅读量
- 评论区：`<CommentSection :blog-id="blog.id" :comments="comments" @comment-added="reloadComments" />`
- 调用 API：`/api/blogs/{id}`

index.html: `<title>阅读博客</title>`

### 4.5 WriteBlogPage.vue — 写作/编辑页

要求：
- 两种模式：新建（URL `/blogs/write`）和编辑（URL `/blogs/edit/{id}`，从 API 获取已有内容）
- 表单：`el-form` + `el-input`（标题）+ `el-input type=textarea`（内容，Markdown 原文）
- 标签选择：`el-select multiple` 显示已有标签 + `el-input` 输入新标签名
- 发布调用 `/blogs/publish`，更新调用 `/blogs/update`
- 需登录（未登录跳转首页）

index.html: `<title>写博客</title>`

### 4.6 ProfilePage.vue — 用户主页

要求：
- 用户信息卡片：头像、昵称、简介、等级
- `el-tabs` 切换：文章列表 / 点赞列表
- 调用 API：`/api/user/profile/{id}`（id 从 URL 解析 `/user/profile?id=xxx`）

index.html: `<title>个人主页</title>`

### 4.7 剩余页面

- **ArchivesPage.vue** — 归档列表，调用 `/api/archives`，显示回收站中的博客，支持恢复 `/blogs/recover/{id}`
- **ReadArchivePage.vue** — 归档详情，类似 ReadBlogPage 但内容从回收站读取
- **FollowListPage.vue** — 关注/粉丝列表
- **ErrorPage.vue** — `el-result` 显示错误信息

---

## Task 5: 构建与集成

### 5.1 构建前端

```bash
cd src/main/frontend && npm run build
```

输出到 `src/main/resources/static/dist/`。

### 5.2 更新 Thymeleaf 模板为 Vue 壳

构建完成后，`npm run build` 为每个页面生成带哈希的 JS/CSS 文件和一个入口 HTML。将每个页面的构建输出 HTML 复制到对应 Thymeleaf 模板位置：

例如 index 页：Vite 输出 `dist/index.html`（包含正确的 `<script>` 和 `<link>` 标签）→ 复制到 `templates/index.html`，调整资源路径为绝对路径（`/dist/...`）。

每个模板最终形态：
```html
<!DOCTYPE html>
<html lang="zh">
<head>
  <meta charset="UTF-8">
  <title>博客主页</title>
  <link rel="stylesheet" href="/dist/assets/index-<hash>.css">
</head>
<body>
  <div id="app"></div>
  <script type="module" src="/dist/assets/index-<hash>.js"></script>
</body>
</html>
```

### 5.3 验证

```bash
# 启动后端
mvn spring-boot:run
# 访问 http://localhost:8080/ 查看效果
```

---

## 关键注意事项

1. **JWT Token 存储键名**：`myblog_access_token` 和 `myblog_refresh_token`
2. **请求 Content-Type**：POST/PUT 用 `application/x-www-form-urlencoded`（`new URLSearchParams(...)`）
3. **验证码流程**：`GET /auth/captcha` → 从响应头 `X-Captcha-Token` 获取 token → 登录时传 `captchaToken` 参数
4. **XSS 防护**：博客正文必须经过 `renderMarkdown()`（marked + DOMPurify）再 `v-html`
5. **MPA 无 Vue Router**：页面间跳转用 `window.location.href`，不用 `<router-link>`
6. **环境变量**：Vite 开发服务器通过 proxy 转发 API 请求到 `localhost:8080`
