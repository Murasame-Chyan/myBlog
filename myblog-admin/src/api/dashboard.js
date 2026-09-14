import request from './request'

// 仪表盘统计数据（总数 + 近30天趋势）
export function getDashboardStats() {
  return request.get('/admin/dashboard/stats')
}
