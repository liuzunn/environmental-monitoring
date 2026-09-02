import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// NEPG 网格员端 axios 实例：携带 Bearer token 与 X-User-Id（网格员身份）
const request = axios.create({ baseURL: '/api', timeout: 15000 })

request.interceptors.request.use(config => {
  const token = localStorage.getItem('nep_nepg_token')
  const userId = localStorage.getItem('nep_nepg_userId')
  if (token) config.headers.Authorization = 'Bearer ' + token
  if (userId) config.headers['X-User-Id'] = userId
  return config
})

request.interceptors.response.use(
  response => {
    if (response.config.responseType === 'blob') return response.data
    const res = response.data
    if (res.code === 200) return res.data
    if (res.code === 401) {
      localStorage.removeItem('nep_nepg_token')
      localStorage.removeItem('nep_nepg_userId')
      localStorage.removeItem('nep_nepg_username')
      localStorage.removeItem('nep_nepg_nickname')
      ElMessage.error('请先登录')
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
