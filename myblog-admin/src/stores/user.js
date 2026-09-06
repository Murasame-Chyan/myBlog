import { defineStore } from 'pinia'
import { login, getUserInfo } from '@/api/auth'

// 用户状态管理：token、用户信息、角色、权限码、菜单
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('admin_token') || '',
    userInfo: {},
    roles: [],
    permissions: [],
    menus: [],
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
  },

  actions: {
    // 登录：拿到 token 存起来
    async login(loginForm) {
      const res = await login(loginForm)
      this.token = res.data.ac_token
      localStorage.setItem('admin_token', this.token)
      return res
    },

    // 登录后拉取用户信息 + 权限 + 菜单（驱动动态路由和按钮权限）
    async fetchUserInfo() {
      const res = await getUserInfo()
      this.userInfo = res.data.user
      this.roles = res.data.roles
      this.permissions = res.data.permissions
      this.menus = res.data.menus
      return res.data
    },

    // 退出登录：清空所有状态
    logout() {
      this.token = ''
      this.userInfo = {}
      this.roles = []
      this.permissions = []
      this.menus = []
      localStorage.removeItem('admin_token')
    },
  },
})
