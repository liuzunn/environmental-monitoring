import { defineStore } from 'pinia'

// 网格员会话（localStorage 持久化，key 与各端隔离）
export const useWorkerStore = defineStore('nepgWorker', {
  state: () => ({
    token: localStorage.getItem('nep_nepg_token') || '',
    userId: localStorage.getItem('nep_nepg_userId') || '',
    username: localStorage.getItem('nep_nepg_username') || '',
    nickname: localStorage.getItem('nep_nepg_nickname') || ''
  }),
  actions: {
    setLogin(data) {
      this.token = data.token
      this.userId = String(data.userId)
      this.username = data.username || ''
      this.nickname = data.nickname || ''
      localStorage.setItem('nep_nepg_token', data.token)
      localStorage.setItem('nep_nepg_userId', String(data.userId))
      localStorage.setItem('nep_nepg_username', this.username)
      localStorage.setItem('nep_nepg_nickname', this.nickname)
    },
    clear() {
      this.token = ''; this.userId = ''; this.username = ''; this.nickname = ''
      localStorage.removeItem('nep_nepg_token')
      localStorage.removeItem('nep_nepg_userId')
      localStorage.removeItem('nep_nepg_username')
      localStorage.removeItem('nep_nepg_nickname')
    }
  }
})
