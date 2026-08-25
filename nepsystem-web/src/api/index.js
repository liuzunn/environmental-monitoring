import request from '@/utils/request'

// ---- 认证 ----
export const login = (data) => request.post('/auth/login', data)
export const logout = () => request.post('/auth/logout')

// ---- 设备 ----
export const getDevicesPage = (params) => request.get('/devices/page', { params })
export const addDevice = (data) => request.post('/devices', data)
export const updateDevice = (data) => request.put('/devices', data)
export const deleteDevice = (id) => request.delete('/devices/' + id)
export const getOnlineCount = () => request.get('/devices/online/count')
export const getDeviceLatest = (id) => request.get('/devices/' + id + '/latest')

// ---- 监测数据 ----
export const reportData = (data) => request.post('/data/report', data)
export const getLatestData = (params) => request.get('/data/latest', { params })
export const getHistory = (params) => request.get('/data/history', { params })
export const getTrend = (params) => request.get('/data/trend', { params })
export const exportCsv = (params) => request.get('/data/export', { params, responseType: 'blob' })

// ---- 告警 ----
export const getAlertsPage = (params) => request.get('/alerts/page', { params })
export const handleAlert = (id, handleUser) => request.put('/alerts/' + id + '/handle', null, { params: { handleUser } })
export const getUnhandled = () => request.get('/alerts/unhandled')
export const getAlertsStat = () => request.get('/alerts/stat')

// ---- 阈值 ----
export const getThresholds = (params) => request.get('/thresholds', { params })
export const addThreshold = (data) => request.post('/thresholds', data)
export const updateThreshold = (data) => request.put('/thresholds', data)
export const deleteThreshold = (id) => request.delete('/thresholds/' + id)

// ---- 统计 ----
export const getOverview = () => request.get('/stats/overview')
export const getQuality = (params) => request.get('/stats/quality', { params })
export const getDeviceRanking = (params) => request.get('/stats/device-ranking', { params })

// ---- 用户 ----
export const getUsersPage = (params) => request.get('/users/page', { params })
export const addUser = (data) => request.post('/users', data)
export const updateUser = (data) => request.put('/users', data)
export const deleteUser = (id) => request.delete('/users/' + id)
export const changeUserStatus = (id, status) => request.put('/users/' + id + '/status', null, { params: { status } })

// ---- 指标字典 ----
export const getSensors = () => request.get('/sensors')
