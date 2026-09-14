import request from './request'

export function listAchievements() {
  return request.get('/admin/achievements')
}

export function createAchievement(data) {
  return request.post('/admin/achievements', data)
}

export function updateAchievement(id, data) {
  return request.put(`/admin/achievements/${id}`, data)
}

export function deleteAchievement(id) {
  return request.delete(`/admin/achievements/${id}`)
}

// 授予荣誉
export function grantAchievement(data) {
  return request.post('/admin/achievements/grant', data)
}

// 撤销荣誉
export function revokeAchievement(data) {
  return request.post('/admin/achievements/revoke', data)
}
