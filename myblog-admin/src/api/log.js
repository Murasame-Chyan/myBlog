import request from './request'

// 操作日志分页（支持 module/userId/时间范围筛选）
export function listOperationLogs(params) {
  return request.get('/admin/logs/operation', { params })
}

// 签到日志分页
export function listSignInLogs(params) {
  return request.get('/admin/logs/sign-in', { params })
}

// 登录日志分页
export function listLoginLogs(params) {
  return request.get('/admin/logs/login', { params })
}
