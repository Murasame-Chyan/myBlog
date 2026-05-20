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
