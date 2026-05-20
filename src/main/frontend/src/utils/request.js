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
