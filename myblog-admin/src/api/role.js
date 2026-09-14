import request from './request'

// 角色列表
export function listRoles() {
  return request.get('/admin/roles')
}

// 角色详情（含已分配的 permissionIds、menuIds）
export function getRole(id) {
  return request.get(`/admin/roles/${id}`)
}

export function createRole(data) {
  return request.post('/admin/roles', data)
}

export function updateRole(id, data) {
  return request.put(`/admin/roles/${id}`, data)
}

export function deleteRole(id) {
  return request.delete(`/admin/roles/${id}`)
}

// 分配权限（全量替换）
export function assignPermissions(id, permissionIds) {
  return request.put(`/admin/roles/${id}/permissions`, { permissionIds })
}

// 分配菜单（全量替换）
export function assignMenus(id, menuIds) {
  return request.put(`/admin/roles/${id}/menus`, { menuIds })
}
