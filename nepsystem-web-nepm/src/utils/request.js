import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// NEPM 管理端 axios 实例：携带 Bearer token 与 X-Admin-Id（管理员身份）
const request = axios.create({ baseURL: '/api', timeout: 15000 })

request.interceptors.request.use(config => {
  const token = localStorage.getItem('nep_nepm_token')
  const adminId = localStorage.getItem('nep_nepm_adminId')
  if (token) config.headers.Authorization = 'Bearer ' + token
  if (adminId) config.headers['X-Admin-Id'] = adminId
  return config
})

request.interceptors.response.use(
  response => {
    if (response.config.responseType === 'blob') return response.data
    const res = response.data
    if (res.code === 200) return res.data
    if (res.code === 401) {
      localStorage.removeItem('nep_nepm_token')
      localStorage.removeItem('nep_nepm_adminId')
      localStorage.removeItem('nep_nepm_adminCode')
      ElMessage.error('登录已过期，请重新登录')
      router.push('/login')
      return Promise.reject(new Error(res.message))
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message))
  },
  error => {
    ElMessage.error(error.response?.data?.message || '网络异常，请稍后重试')
    return Promise.reject(error)
  }
)
export default request
