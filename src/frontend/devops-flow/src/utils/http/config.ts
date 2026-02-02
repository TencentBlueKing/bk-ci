export const API_BASE_URL =
  import.meta.env.VITE_DEVOPS_API_BASE_URL || '/ms/'

export const HTTP_TIMEOUT = 30_000

export const DEFAULT_RETRY = 1
export const DEFAULT_RETRY_DELAY = 1_000

export const DEFAULT_CACHE_TTL = 30_000

export const IS_DEV = import.meta.env.DEV
