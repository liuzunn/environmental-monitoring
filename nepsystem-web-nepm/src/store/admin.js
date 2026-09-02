import { defineStore } from 'pinia'

// 管理员会话（localStorage 持久化，key 与 NEPS/NEPV 隔离）
export const useAdminStore = defineStore('nepmAdmin', {
  state: () => ({
    token: localStorage.getItem('nep_nepm_token') || '',
    adminId: localStorage.getItem('nep_nepm_adminId') || '',
    adminCode: localStorage.getItem('nep_nepm_adminCode') || ''
  }),
  actions: {
    setLogin(data) {
      this.token = data.token
      this.adminId = String(data.adminId)
      this.adminCode = data.adminCode || ''
      localStorage.setItem('nep_nepm_token', data.token)
      localStorage.setItem('nep_nepm_adminId', String(data.adminId))
      localStorage.setItem('nep_nepm_adminCode', this.adminCode)
    },
    clear() {
      this.token = ''; this.adminId = ''; this.adminCode = ''
      localStorage.removeItem('nep_nepm_token')
      localStorage.removeItem('nep_nepm_adminId')
      localStorage.removeItem('nep_nepm_adminCode')
    }
  }
})
