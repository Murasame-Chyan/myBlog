import request from './request'

// 全站评论分页
export function listComments(params) {
  return request.get('/admin/comments', { params })
}

// 删除评论
export function deleteComment(id) {
  return request.delete(`/admin/comments/${id}`)
}

// 置顶评论
export function pinComment(id) {
  return request.post(`/admin/comments/${id}/pin`)
}

// 取消置顶
export function unpinComment(id) {
  return request.post(`/admin/comments/${id}/unpin`)
}
