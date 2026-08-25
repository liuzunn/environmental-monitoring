import { defineStore } from 'pinia'

// 用户会话状态：token 与管理员信息（localStorage 持久化）
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('nep_token') || '',
    adminCode: localStorage.getItem('nep_adminCode') || '',
    adminId: localStorage.getItem('nep_adminId') || ''
  }),
  actions: {
    setLogin(data) {
      this.token = data.token
      this.adminCode = data.adminCode
      this.adminId = String(data.adminId)
      localStorage.setItem('nep_token', data.token)
      localStorage.setItem('nep_adminCode', data.adminCode)
      localStorage.setItem('nep_adminId', String(data.adminId))
    },
    clear() {
      this.token = ''
      this.adminCode = ''
      this.adminId = ''
      localStorage.removeItem('nep_token')
      localStorage.removeItem('nep_adminCode')
      localStorage.removeItem('nep_adminId')
    }
  }
})
