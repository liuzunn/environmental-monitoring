import { defineStore } from 'pinia'

// 公众会话状态：token 与用户信息（localStorage 持久化，key 与 NEPV 管理端隔离）
export const useUserStore = defineStore('nepsUser', {
  state: () => ({
    token: localStorage.getItem('nep_neps_token') || '',
    userId: localStorage.getItem('nep_neps_userId') || '',
    username: localStorage.getItem('nep_neps_username') || '',
    nickname: localStorage.getItem('nep_neps_nickname') || ''
  }),
  actions: {
    setLogin(data) {
      this.token = data.token
      this.userId = String(data.userId)
      this.username = data.username || ''
      this.nickname = data.nickname || ''
      localStorage.setItem('nep_neps_token', data.token)
      localStorage.setItem('nep_neps_userId', String(data.userId))
      localStorage.setItem('nep_neps_username', this.username)
      localStorage.setItem('nep_neps_nickname', this.nickname)
    },
    clear() {
      this.token = ''
      this.userId = ''
      this.username = ''
      this.nickname = ''
      localStorage.removeItem('nep_neps_token')
      localStorage.removeItem('nep_neps_userId')
      localStorage.removeItem('nep_neps_username')
      localStorage.removeItem('nep_neps_nickname')
    }
  }
})
