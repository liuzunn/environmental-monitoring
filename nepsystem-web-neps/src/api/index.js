import request from '@/utils/request'

// ---- 文件上传（BUG-002 修复） ----
export const uploadFile = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/upload', fd)
}

// ---- 认证（公众） ----
export const loginPublic = (data) => request.post('/auth/login-public', data)
export const logoutPublic = () => request.post('/auth/logout-public')

// ---- 首页数据 ----
export const getQuality = (params) => request.get('/stats/quality', { params })
export const getDevicesPage = (params) => request.get('/devices/page', { params })
export const getAlertsPage = (params) => request.get('/alerts/page', { params })
export const getOverview = () => request.get('/stats/overview')

// ---- 监督事件 ----
export const createSupervision = (data) => request.post('/supervision', data)
export const getSupervisionMine = (params) => request.get('/supervision/mine', { params })
export const getSupervisionDetail = (id) => request.get('/supervision/' + id)