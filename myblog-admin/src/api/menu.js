import request from './request'

// 平铺列表
export function listMenus() {
  return request.get('/admin/menus')
}

// 树形结构
export function getMenuTree() {
  return request.get('/admin/menus/tree')
}

export function createMenu(data) {
  return request.post('/admin/menus', data)
}

export function updateMenu(id, data) {
  return request.put(`/admin/menus/${id}`, data)
}

export function deleteMenu(id) {
  return request.delete(`/admin/menus/${id}`)
}
