import request from './request'

// 全站评论分页
export function listComments(params) {
  return request.get('/admin/comments', { params })
}

// 删除评论
export function deleteComment(id) {
  return request.delete(`/admin/comments/${id}`)
}
