import { getCache, setCache } from './cache'
import httpInstance from './httpClient'
import {
  createDebouncedPromise,
  createThrottledPromise,
} from './requestControl'
import type { HttpMethod, HttpRequestConfig } from './types'

export async function request<T = unknown>(
  method: HttpMethod,
  url: string,
  config: HttpRequestConfig = {},
): Promise<T> {
  const finalConfig: HttpRequestConfig = {
    url,
    method,
    ...config,
  }

  const meta = finalConfig.meta || {}
  finalConfig.meta = meta
  const cacheKey =
    meta.cacheKey ||
    `${method}:${url}:${JSON.stringify(
      finalConfig.params || finalConfig.data || {},
    )}`

  if (meta.enableCache) {
    const cached = getCache<T>(cacheKey)
    if (cached) return cached
  }

  const executor = async () => {
    // httpInstance 的响应拦截器已经提取了 data，所以这里直接返回 T 类型
    const result = await httpInstance.request<T>(finalConfig) as unknown as T

    if (meta.enableCache) {
      setCache(cacheKey, result, meta.cacheTTL)
    }

    return result
  }

  if (meta.debounceKey && meta.debounceTime) {
    return createDebouncedPromise(meta.debounceKey, meta.debounceTime, executor)
  }

  if (meta.throttleKey && meta.throttleTime) {
    return createThrottledPromise(meta.throttleKey, meta.throttleTime, executor)
  }

  return executor()
}

export function get<T = unknown>(
  url: string,
  config?: HttpRequestConfig,
): Promise<T> {
  return request<T>('GET', url, config)
}

export function post<T = unknown>(
  url: string,
  data?: unknown,
  config?: HttpRequestConfig,
): Promise<T> {
  return request<T>('POST', url, { ...config, data })
}

export function put<T = unknown>(
  url: string,
  data?: unknown,
  config?: HttpRequestConfig,
): Promise<T> {
  return request<T>('PUT', url, { ...config, data })
}

export function del<T>(
  url: string,
  config?: HttpRequestConfig,
): Promise<T> {
  return request<T>('DELETE', url, config)
}

export function head<T = unknown>(
  url: string,
  config?: HttpRequestConfig,
): Promise<T> {
  return request<T>('HEAD', url, config)
}
