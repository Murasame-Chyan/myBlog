import request from './request'

// 登录。后端 AdminAuthController 用 @RequestParam 接收，
// 所以这里用 URLSearchParams 以表单形式提交，而不是 JSON。
export function login(data) {
  const params = new URLSearchParams()
  params.append('email', data.email)
  params.append('password', data.password)
  params.append('captchaCode', data.captchaCode || '')
  params.append('captchaToken', data.captchaToken || '')
  return request.post('/admin/auth/login', params)
}

// 获取当前用户信息（含角色、权限码、菜单树）
export function getUserInfo() {
  return request.get('/admin/auth/user-info')
}
