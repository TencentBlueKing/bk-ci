import axios from 'axios'
import type {
  AxiosError,
  AxiosInstance,
  AxiosRequestHeaders,
  InternalAxiosRequestConfig,
} from 'axios'
import { API_BASE_URL, HTTP_TIMEOUT } from './config'
import type { HttpRequestConfig, HttpResponse, HttpResponseEnvelope, RequestMeta } from './types'
import { retryRequest } from './retry'
import { handleHttpError, HttpError } from './error'
import { useAuthStore } from '@/stores/auth'
import { useHttpLogStore } from '@/stores/httpLog'

const httpInstance: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: HTTP_TIMEOUT,
  withCredentials: true,
})

interface ExtendedAxiosRequestConfig extends InternalAxiosRequestConfig {
  __startTime?: number
  meta?: RequestMeta
}

httpInstance.interceptors.request.use(
  (config: InternalAxiosRequestConfig): InternalAxiosRequestConfig => {
    const authStore = useAuthStore()
    const token = authStore.token
    const extendConfig = config as ExtendedAxiosRequestConfig

    extendConfig.headers.set('Accept', 'application/json')
    extendConfig.headers.set('X-DEVOPS-CHANNEL', 'CREATIVE_STREAM')
    if (!extendConfig.headers.get('Content-Type')) {
      extendConfig.headers.set('Content-Type', 'application/json;charset=UTF-8')
    }

    extendConfig.__startTime = Date.now()

    return config
  },
  (error) => Promise.reject(error),
)
httpInstance.interceptors.response.use(
  (response: HttpResponse<HttpResponseEnvelope<unknown>>): any => {
    const httpLogStore = useHttpLogStore()
    const config = response.config as ExtendedAxiosRequestConfig
    const duration = Date.now() - (config.__startTime || Date.now())

    httpLogStore.addLog({
      url: config.url || '',
      method: (config.method || 'GET').toUpperCase(),
      status: response.status,
      duration,
      timestamp: Date.now(),
    })

    const envelope = response.data

    // 统一处理 {status: number, data: any, message: string} 格式的响应
    if (typeof envelope === 'object' && envelope && 'status' in envelope && 'data' in envelope) {
      // status === 0 表示成功，直接返回 data
      if (envelope.status === 0) {
        return envelope.data
      }

      // status !== 0 表示业务错误
      const businessError = new HttpError({
        type: 'business',
        message: envelope.message || 'Business error',
        business: {
          code: envelope.status,
          message: envelope.message,
        },
      })

      if (!config.meta?.silent && config.meta?.showBusinessError !== false) {
        handleHttpError(businessError)
      }

      return Promise.reject(businessError)
    }

    // 兼容旧的 code 字段格式
    if (typeof envelope === 'object' && envelope && 'code' in envelope && 'data' in envelope) {
      const legacyEnvelope = envelope as { code: number; data: unknown; message?: string }

      if (legacyEnvelope.code === 0) {
        return legacyEnvelope.data
      }

      const businessError = new HttpError({
        type: 'business',
        message: legacyEnvelope.message || 'Business error',
        business: {
          code: legacyEnvelope.code,
          message: legacyEnvelope.message || '',
        },
      })

      if (!config.meta?.silent && config.meta?.showBusinessError !== false) {
        handleHttpError(businessError)
      }

      return Promise.reject(businessError)
    }

    return response.data
  },
  async (error: AxiosError) => {
    const config = error.config as ExtendedAxiosRequestConfig

    if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
      if (config?.meta?.retry || config?.meta?.retry === 0) {
        try {
          const newConfig = await retryRequest(error)
          return httpInstance.request(newConfig)
        } catch (err) {
          const timeoutError = new HttpError({
            type: 'timeout',
            message: 'Request timeout',
            raw: error,
          })
          if (!config?.meta?.silent) {
            handleHttpError(timeoutError)
          }
          return Promise.reject(timeoutError)
        }
      }
    }

    if (!error.response) {
      const networkError = new HttpError({
        type: 'network',
        message: 'Network error',
        raw: error,
      })
      if (!config?.meta?.silent) {
        handleHttpError(networkError)
      }
      return Promise.reject(networkError)
    }

    // 检查 response.data 是否包含业务错误信息
    const responseData = error.response.data as any
    if (
      responseData &&
      typeof responseData === 'object' &&
      ('message' in responseData || 'status' in responseData || 'code' in responseData)
    ) {
      // 如果 response.data 包含业务错误信息，直接 reject 这个数据
      if (!config?.meta?.silent && config?.meta?.showBusinessError !== false) {
        const businessError = new HttpError({
          type: 'business',
          message: responseData.message || 'Business error',
          business: {
            code: responseData.status || responseData.code,
            message: responseData.message,
          },
        })
        handleHttpError(businessError)
      }
      return Promise.reject(responseData)
    }

    const status = error.response.status
    const httpError = new HttpError({
      type: 'http',
      message: error.response.statusText || `HTTP Error ${status}`,
      status,
      raw: error,
    })

    if (!config?.meta?.silent) {
      handleHttpError(httpError)
    }

    return Promise.reject(httpError)
  },
)

export default httpInstance
