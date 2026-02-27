import { defineStore } from 'pinia'
import router from '@/router'
import { get } from '@/utils/http'

const PROJECT_API_URL_PREFIX = '/project/api'

interface UserInfo {
  username: string
  chineseName?: string
  [key: string]: any
}

interface AuthState {
  token: string | null
  refreshToken: string | null
  userInfo: UserInfo | null
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: null,
    refreshToken: null,
    userInfo: null,
  }),
  getters: {
    username: (state): string => state.userInfo?.username ?? '',
  },
  actions: {
    setToken(token: string | null) {
      this.token = token
    },
    setRefreshToken(refreshToken: string | null) {
      this.refreshToken = refreshToken
    },
    setUserInfo(userInfo: UserInfo | null) {
      this.userInfo = userInfo
    },
    async fetchUserInfo() {
      if (this.userInfo) return this.userInfo
      try {
        const data = await get<UserInfo>(`${PROJECT_API_URL_PREFIX}/user/users`, {
          meta: { silent: true },
        })
        this.userInfo = data
        return data
      } catch (error) {
        console.error('Failed to fetch user info:', error)
        return null
      }
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
