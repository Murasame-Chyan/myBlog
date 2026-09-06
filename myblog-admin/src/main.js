import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
// 引入路由守卫（内部执行 router.beforeEach 注册动态路由逻辑）
import './router/permission'
// 注册自定义权限指令 v-permission
import permission from './directives/permission'
import './style.css'

const app = createApp(App)

// 注册所有 Element Plus 图标为全局组件（侧边栏菜单图标用）
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.use(permission)

app.mount('#app')
