import request from '@/utils/request'

// ---- 认证（管理员） ----
export const login = (data) => request.post('/auth/login', data)
export const logout = () => request.post('/auth/logout')

// ---- 工作台 ----
export const getWorkbenchStats = () => request.get('/supervision/admin/stats')
export const getPendingEvents = (params) => request.get('/supervision/admin/pending', { params })
export const getAlertsPage = (params) => request.get('/alerts/page', { params })
export const getUnhandled = () => request.get('/alerts/unhandled')

// ---- 监督事件 ----
export const getEventsPage = (params) => request.get('/supervision/admin/list', { params })
export const getEventDetail = (id) => request.get('/supervision/' + id)
export const approveEvent = (id, remark) => request.put('/supervision/' + id + '/approve', { remark })
export const rejectEvent = (id, remark) => request.put('/supervision/' + id + '/reject', { remark })
export const assignEvent = (id, data) => request.put('/supervision/' + id + '/assign', data)

// ---- 网格 ----
export const getGrids = (params) => request.get('/grids/list', { params })
export const addGrid = (data) => request.post('/grids', data)
export const updateGrid = (data) => request.put('/grids', data)
export const deleteGrid = (id) => request.delete('/grids/' + id)
export const changeGridStatus = (id, status) => request.put('/grids/' + id + '/status', null, { params: { status } })

// ---- 网格员 ----
export const getGridMembers = (params) => request.get('/grid-members/list', { params })
export const assignGridMember = (data) => request.post('/grid-members/assign', data)
export const removeGridMember = (id) => request.delete('/grid-members/' + id)

// ---- 任务 ----
export const getTasksPage = (params) => request.get('/tasks/page', { params })
export const createTask = (data) => request.post('/tasks', data)
export const getAssigneeStats = () => request.get('/tasks/assignee-stats')
export const getTaskRecords = (id) => request.get('/tasks/' + id + '/records')
export const verifyTask = (id, remark) => request.put('/tasks/' + id + '/verify', null, { params: { remark } })
export const closeTask = (id, remark) => request.put('/tasks/' + id + '/close', null, { params: { remark } })

// ---- 用户（网格员分配候选） ----
export const getUsersPage = (params) => request.get('/users/page', { params })

// ---- 设备/指标/趋势（事件详情辅助） ----
export const getDevicesPage = (params) => request.get('/devices/page', { params })
export const getQuality = (params) => request.get('/stats/quality', { params })
export const getTrend = (params) => request.get('/data/trend', { params })
export const getSensors = () => request.get('/sensors')
