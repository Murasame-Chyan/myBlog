import router from './index'
import { useUserStore } from '@/stores/user'

// Vite 动态导入：收集 views 下所有 .vue 文件
// 注意：用 '/src/views/**' 绝对路径而非 '@' 别名，避免 glob 中别名解析失效
// 返回对象 key 形如 '/src/views/user/index.vue'，value 是 () => import(...) 函数
const viewModules = import.meta.glob('/src/views/**/*.vue')

// 白名单：无需登录即可访问
const whiteList = ['/login', '/404']

// 后端菜单的 component 字段是 "views/user/index.vue"，
// 需要补前缀转成 '/src/views/user/index.vue' 才能从 viewModules 取到组件
function loadView(componentPath) {
  const key = `/src/${componentPath}`
  return viewModules[key]
}

// 把后端返回的菜单树转成 Vue Router 路由
function generateRoutes(menus) {
  const routes = []
  for (const m of menus) {
    if (m.component) {
      // 有组件的菜单 → 生成路由
      const route = {
        path: m.path.replace(/^\//, ''), // 去掉开头 '/'，作为 layout 的相对子路径
        name: m.name,
        component: loadView(m.component),
        meta: { title: m.title, icon: m.icon },
      }
      if (m.children && m.children.length) {
        route.children = generateRoutes(m.children)
      }
      routes.push(route)
    } else if (m.children && m.children.length) {
      // 无组件但有子菜单（如"系统管理"这类纯目录）：
      // 不生成自己的路由，把子菜单平铺到当前层级
      routes.push(...generateRoutes(m.children))
    }
  }
  return routes
}

// 布局路由：所有业务页面都挂在它下面
const layoutRoute = {
  path: '/',
  name: 'Layout',
  component: () => import('@/layout/index.vue'),
  redirect: '/dashboard',
  children: [],
}

// 标记动态路由是否已注册，避免每次导航都重复注册
let isLoaded = false

router.beforeEach(async (to) => {
  const userStore = useUserStore()
  const token = userStore.token

  if (token) {
    if (to.path === '/login') {
      // 已登录还去登录页 → 跳首页
      return { path: '/' }
    } else if (!isLoaded) {
      // 首次进入：拉取用户信息 → 根据菜单生成动态路由
      try {
        const data = await userStore.fetchUserInfo()
        const children = generateRoutes(data.menus)
        router.addRoute(layoutRoute)
        children.forEach((r) => router.addRoute('Layout', r))
        isLoaded = true
        // 重新导航，让新注册的路由生效
        return { ...to, replace: true }
      } catch (e) {
        // 拉取失败（token 无效等）→ 清状态回登录页
        userStore.logout()
        return '/login'
      }
    } else {
      return true
    }
  } else {
    // 未登录：白名单放行，否则跳登录页
    if (whiteList.includes(to.path)) {
      return true
    } else {
      return '/login'
    }
  }
})
