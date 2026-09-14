import request from './request'

// 列出所有标签
export function listTags() {
  return request.get('/admin/tags')
}

// 创建标签（后端用 @RequestBody 接收 JSON）
export function createTag(data) {
  return request.post('/admin/tags', data)
}

// 删除标签
export function deleteTag(id) {
  return request.delete(`/admin/tags/${id}`)
}
