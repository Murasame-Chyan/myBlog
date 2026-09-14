import request from './request'

// 在线博客分页
export function listBlogs(params) {
  return request.get('/admin/blogs', { params })
}

// 回收站分页
export function listBinBlogs(params) {
  return request.get('/admin/blogs/bin', { params })
}

// 下架博客（移入回收站）
export function offlineBlog(id) {
  return request.post(`/admin/blogs/${id}/offline`)
}

// 恢复博客（从回收站移回）
export function recoverBlog(id) {
  return request.post(`/admin/blogs/${id}/recover`)
}

// 永久删除
export function deleteBlog(id) {
  return request.delete(`/admin/blogs/${id}`)
}
