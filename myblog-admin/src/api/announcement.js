import request from './request'

// 公告分页列表
export function listAnnouncements(params) {
  return request.get('/admin/announcements', { params })
}

export function getAnnouncement(id) {
  return request.get(`/admin/announcements/${id}`)
}

export function createAnnouncement(data) {
  return request.post('/admin/announcements', data)
}

export function updateAnnouncement(id, data) {
  return request.put(`/admin/announcements/${id}`, data)
}

// 发布公告
export function publishAnnouncement(id) {
  return request.put(`/admin/announcements/${id}/publish`)
}

// 撤回公告
export function withdrawAnnouncement(id) {
  return request.put(`/admin/announcements/${id}/withdraw`)
}

export function deleteAnnouncement(id) {
  return request.delete(`/admin/announcements/${id}`)
}
