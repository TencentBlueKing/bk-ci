const debounceTimers = new Map<string, number>()
const throttleTimestamps = new Map<string, number>()

export function createDebouncedPromise<T>(
  key: string,
  delay: number,
  executor: () => Promise<T>,
): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const timer = debounceTimers.get(key)
    if (timer) {
      clearTimeout(timer)
    }

    const newTimer = window.setTimeout(async () => {
      debounceTimers.delete(key)
      try {
        const result = await executor()
        resolve(result)
      } catch (e) {
        reject(e)
      }
    }, delay)

    debounceTimers.set(key, newTimer)
  })
}

export function createThrottledPromise<T>(
  key: string,
  interval: number,
  executor: () => Promise<T>,
): Promise<T> {
  const now = Date.now()
  const last = throttleTimestamps.get(key) || 0
  if (now - last < interval) {
    return Promise.reject(new Error('Request is throttled, please try again later.'))
  }
  throttleTimestamps.set(key, now)
  return executor()
}
