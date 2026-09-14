import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建 axios 实例，baseURL 指向 /api（vite 代理到后端 8080）
const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// 请求拦截器：自动附带 token
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('admin_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

// 响应拦截器：统一处理后端返回的 { code, msg, data }
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res
    }
    // 业务错误（如权限不足、参数错误）
    ElMessage.error(res.msg || '操作失败')
    return Promise.reject(new Error(res.msg || '操作失败'))
  },
  (error) => {
    // HTTP 层错误
    if (error.response?.status === 401) {
      // 未登录/token 过期 → 清 token 跳登录页
      localStorage.removeItem('admin_token')
      ElMessage.error('登录已过期，请重新登录')
      window.location.href = '/login'
    } else if (error.response?.status === 403) {
      ElMessage.error('权限不足，您的请求非法')
    } else {
      ElMessage.error(error.response?.data?.msg || '网络错误，请稍后重试')
    }
    return Promise.reject(error)
  },
)

export default request
