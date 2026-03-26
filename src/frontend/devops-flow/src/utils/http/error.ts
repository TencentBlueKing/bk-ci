import type { AxiosError } from 'axios'
import type { BusinessErrorPayload } from './types'
import { IS_DEV } from './config'
import { useHttpLogStore } from '@/stores/httpLog'
import { useAuthStore } from '@/stores/auth'

export type HttpErrorType = 'network' | 'timeout' | 'http' | 'business' | 'unknown'

export class HttpError extends Error {
  type: HttpErrorType
  status?: number
  business?: BusinessErrorPayload
  raw?: AxiosError

  constructor(options: {
    type: HttpErrorType
    message: string
    status?: number
    business?: BusinessErrorPayload
    raw?: AxiosError
  }) {
    super(options.message)
    this.name = 'HttpError'
    this.type = options.type
    this.status = options.status
    this.business = options.business
    this.raw = options.raw
  }
}

export function handleHttpError(err: HttpError) {
  const httpLogStore = useHttpLogStore()

  httpLogStore.addError({
    type: err.type,
    status: err.status,
    message: err.message,
    business: err.business,
  })

  // Replace with your UI message component
  const message = (window as any).$bkMessage

  if (err.type === 'network' || err.type === 'timeout') {
    message?.error?.('Network error, please check your connection.')
    return
  }

  if (err.type === 'http') {
    switch (err.status) {
      case 401: {
        const authStore = useAuthStore()
        authStore.handleUnauthorized()
        break
      }
      case 403:
        message?.error?.('Permission denied.')
        break
      case 500:
        message?.error?.('Internal server error.')
        break
      default:
        message?.error?.(`Request failed: ${err.message}`)
        break
    }
    return
  }

  if (err.type === 'business' && err.business) {
    message?.error?.(err.business.message || 'Business error.')
    return
  }

  if (IS_DEV) {
     
    console.error('[HttpError]', err)
  }
  message?.error?.('Unknown error occurred.')
}
