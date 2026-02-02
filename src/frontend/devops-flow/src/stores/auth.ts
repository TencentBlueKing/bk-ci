import { defineStore } from 'pinia'
import router from '@/router'

interface AuthState {
  token: string | null
  refreshToken: string | null
  userInfo: any | null
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: null,
    refreshToken: null,
    userInfo: null,
  }),
  actions: {
    setToken(token: string | null) {
      this.token = token
    },
    setRefreshToken(refreshToken: string | null) {
      this.refreshToken = refreshToken
    },
    setUserInfo(userInfo: any | null) {
      this.userInfo = userInfo
    },
    logout() {
      this.token = null
      this.refreshToken = null
      this.userInfo = null
      router.push({ name: 'login' })
    },
    async handleUnauthorized() {
      // TODO: implement token refresh logic if available, fallback to logout
      this.logout()
    },
  },
})
