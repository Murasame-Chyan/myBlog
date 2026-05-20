import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import 'element-plus/dist/index.css'
import '@/shared/styles/global.scss'
import ArchivesPage from './ArchivesPage.vue'
import { useAuthStore } from '@/stores/auth'

const app = createApp(ArchivesPage)
const pinia = createPinia()
app.use(pinia)
app.use(ElementPlus, { locale: zhCn })
useAuthStore().init()
app.mount('#app')
