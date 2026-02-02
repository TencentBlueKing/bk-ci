import { defineStore } from 'pinia'

interface HttpLogItem {
  url: string
  method: string
  status?: number
  duration: number
  timestamp: number
  error?: {
    type: string
    status?: number
    message: string
    business?: any
  }
}

interface HttpLogState {
  logs: HttpLogItem[]
}

export const useHttpLogStore = defineStore('httpLog', {
  state: (): HttpLogState => ({
    logs: [],
  }),
  actions: {
    addLog(log: HttpLogItem) {
      this.logs.unshift(log)
      if (this.logs.length > 200) {
        this.logs.pop()
      }
    },
    addError(error: HttpLogItem['error']) {
      // For now we just log to console; you can extend this to store or report errors.
      if (error) {
        // eslint-disable-next-line no-console
        console.warn('[HttpErrorLog]', error)
      }
    },
  },
})
