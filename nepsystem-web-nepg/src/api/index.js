import request from '@/utils/request'

// ---- 文件上传（BUG-002 修复） ----
export const uploadFile = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/upload', fd)
}

// ---- 认证（网格员，复用公众登录接口） ----
export const login = (data) => request.post('/auth/login-public', data)
export const logout = () => request.post('/auth/logout-public')

// ---- 任务（网格员） ----
export const getMyTasks = (params) => request.get('/tasks/mine', { params })
export const getMyTaskStats = () => request.get('/tasks/mine/stats')
export const getMyTaskDetail = (id) => request.get('/tasks/mine/' + id)
export const acceptTask = (id) => request.put('/tasks/mine/' + id + '/accept')
export const startTask = (id) => request.put('/tasks/mine/' + id + '/start')
export const submitDetect = (id, data) => request.put('/tasks/mine/' + id + '/submit', data)

// ---- 任务详情辅助 ----
export const getDevicesPage = (params) => request.get('/devices/page', { params })
export const getQuality = (params) => request.get('/stats/quality', { params })
export const getTrend = (params) => request.get('/data/trend', { params })
export const getSensors = () => request.get('/sensors')