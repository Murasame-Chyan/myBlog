import request from './request'

// 平铺列表
export function listPermissions() {
  return request.get('/admin/permissions')
}

// 树形结构（前端树形展示用）
export function getPermissionTree() {
  return request.get('/admin/permissions/tree')
}

export function createPermission(data) {
  return request.post('/admin/permissions', data)
}

export function updatePermission(id, data) {
  return request.put(`/admin/permissions/${id}`, data)
}

export function deletePermission(id) {
  return request.delete(`/admin/permissions/${id}`)
}
