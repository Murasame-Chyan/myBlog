const ACCESS_KEY = 'myblog_access_token'
const REFRESH_KEY = 'myblog_refresh_token'

export function getAccessToken() { return localStorage.getItem(ACCESS_KEY) }
export function getRefreshToken() { return localStorage.getItem(REFRESH_KEY) }
export function setTokens(access, refresh) {
  localStorage.setItem(ACCESS_KEY, access)
  localStorage.setItem(REFRESH_KEY, refresh)
}
export function clearTokens() {
  localStorage.removeItem(ACCESS_KEY)
  localStorage.removeItem(REFRESH_KEY)
}
