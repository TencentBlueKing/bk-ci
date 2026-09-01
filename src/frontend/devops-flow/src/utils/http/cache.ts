import { DEFAULT_CACHE_TTL } from './config'

interface CacheEntry<T = any> {
  data: T
  expiresAt: number
}

const cacheMap = new Map<string, CacheEntry>()

export function getCache<T = any>(key: string): T | null {
  const entry = cacheMap.get(key)
  if (!entry) return null
  if (entry.expiresAt < Date.now()) {
    cacheMap.delete(key)
    return null
  }
  return entry.data as T
}

export function setCache<T = any>(key: string, data: T, ttl?: number) {
  const expiresAt = Date.now() + (ttl ?? DEFAULT_CACHE_TTL)
  cacheMap.set(key, { data, expiresAt })
}

export function clearCache(key?: string) {
  if (key) {
    cacheMap.delete(key)
  } else {
    cacheMap.clear()
  }
}
