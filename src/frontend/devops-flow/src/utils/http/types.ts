import { type AxiosRequestConfig, type AxiosResponse } from 'axios'
export interface HttpResponseEnvelope<T = unknown> {
  status: number
  message: string
  data: T
  code?: number // 兼容旧格式
}

export interface RequestMeta {
  retry?: number
  retryDelay?: number
  enableCache?: boolean
  cacheKey?: string
  cacheTTL?: number
  debounceKey?: string
  debounceTime?: number
  throttleKey?: string
  throttleTime?: number
  silent?: boolean
  showBusinessError?: boolean
  skipAuth?: boolean
}

export interface HttpRequestConfig<TData = unknown> extends AxiosRequestConfig<TData> {
  meta?: RequestMeta
}

export type HttpResponse<T = unknown> = AxiosResponse<T>

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH' | 'HEAD'

export interface BusinessErrorPayload {
  code: number | string
  message: string
  traceId?: string
}
