import type { AxiosError, AxiosRequestConfig } from 'axios'
import { DEFAULT_RETRY, DEFAULT_RETRY_DELAY } from './config'
import type { HttpRequestConfig } from './types'

export async function retryRequest(
  error: AxiosError,
): Promise<AxiosRequestConfig | never> {
  const config = error.config as HttpRequestConfig | undefined
  if (!config) throw error

  const meta = config.meta || {}
  const maxRetry = meta.retry ?? DEFAULT_RETRY
  const delay = meta.retryDelay ?? DEFAULT_RETRY_DELAY

  config.meta = meta
  ;(meta as any).retryCount = (meta as any).retryCount ?? 0

  if ((meta as any).retryCount >= maxRetry) {
    throw error
  }

  ;(meta as any).retryCount++

  await new Promise((resolve) => setTimeout(resolve, delay))
  return config
}
