import request from './request'


// 分页列表（keyword 可选）
export function listUsers(params) {
  return request.get('/admin/users', { params })
}

// 编辑昵称/简介
export function updateUser(id, data) {
  return request.put(`/admin/users/${id}`, data)
}

// 封号/解封
export function updateUserStatus(id, status) {
  return request.put(`/admin/users/${id}/status`, { status })
}

// 清空头像
export function clearAvatar(id) {
  return request.put(`/admin/users/${id}/avatar/clear`)
}

// 授予经验
export function grantExp(id, delta) {
  return request.put(`/admin/users/${id}/exp`, { delta })
}