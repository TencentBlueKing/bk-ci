import { onUnmounted, ref } from 'vue'

export interface PollingOptions {
  /** Polling interval in milliseconds, default 5000ms */
  interval?: number
  /** Whether to start polling immediately, default false */
  immediate?: boolean
  /** Maximum number of retries on error, default 3 */
  maxRetries?: number
  /** Delay before retry in milliseconds, default 1000ms */
  retryDelay?: number
  /** Callback when polling encounters an error */
  onError?: (error: Error) => void
  /** Callback when polling succeeds */
  onSuccess?: () => void
}

/**
 * Generic polling hook for periodic data fetching
 * Designed as an independent module to avoid coupling with existing features
 *
 * @param pollFn - The async function to execute on each poll
 * @param options - Polling configuration options
 * @returns Polling control methods and state
 */
export function usePolling(pollFn: () => Promise<void>, options: PollingOptions = {}) {
  const {
    interval = 5000,
    immediate = false,
    maxRetries = 3,
    retryDelay = 1000,
    onError,
    onSuccess,
  } = options

  // Polling state
  const isPolling = ref(false)
  const isPaused = ref(false)
  const hasError = ref(false)
  const errorMessage = ref<string | null>(null)
  const retryCount = ref(0)

  // Internal timer reference
  let timerId: ReturnType<typeof setTimeout> | null = null

  /**
   * Execute the polling function with error handling
   */
  const executePoll = async (): Promise<boolean> => {
    try {
      await pollFn()
      // Reset error state on success
      hasError.value = false
      errorMessage.value = null
      retryCount.value = 0
      onSuccess?.()
      return true
    } catch (error) {
      const err = error instanceof Error ? error : new Error(String(error))
      hasError.value = true
      errorMessage.value = err.message
      retryCount.value++
      onError?.(err)

      // Check if we should stop polling due to max retries
      if (retryCount.value >= maxRetries) {
        console.warn(`[usePolling] Max retries (${maxRetries}) reached, stopping polling`)
        stopPolling()
        return false
      }

      return false
    }
  }

  /**
   * Schedule the next polling execution
   */
  const scheduleNextPoll = (delay: number = interval) => {
    if (!isPolling.value || isPaused.value) {
      return
    }

    timerId = setTimeout(async () => {
      if (!isPolling.value || isPaused.value) {
        return
      }

      const success = await executePoll()

      // If failed but not max retries, use retry delay
      const nextDelay = success ? interval : retryDelay
      scheduleNextPoll(nextDelay)
    }, delay)
  }

  /**
   * Start the polling process
   */
  const startPolling = () => {
    if (isPolling.value) {
      return // Already polling
    }

    isPolling.value = true
    isPaused.value = false
    hasError.value = false
    errorMessage.value = null
    retryCount.value = 0

    // Schedule first poll after interval (don't execute immediately on start)
    scheduleNextPoll()
  }

  /**
   * Stop the polling process completely
   */
  const stopPolling = () => {
    isPolling.value = false
    isPaused.value = false

    if (timerId) {
      clearTimeout(timerId)
      timerId = null
    }
  }

  /**
   * Pause polling temporarily (can be resumed)
   */
  const pausePolling = () => {
    isPaused.value = true
    if (timerId) {
      clearTimeout(timerId)
      timerId = null
    }
  }

  /**
   * Resume paused polling
   */
  const resumePolling = () => {
    if (!isPolling.value) {
      return // Not in polling mode
    }

    isPaused.value = false
    scheduleNextPoll()
  }

  /**
   * Toggle polling on/off
   */
  const togglePolling = () => {
    if (isPolling.value) {
      stopPolling()
    } else {
      startPolling()
    }
  }

  /**
   * Manually trigger a poll immediately (outside of schedule)
   */
  const pollNow = async () => {
    return await executePoll()
  }

  // Start immediately if configured
  if (immediate) {
    startPolling()
  }

  // Cleanup on unmount to prevent memory leaks
  onUnmounted(() => {
    stopPolling()
  })

  return {
    // State
    isPolling,
    isPaused,
    hasError,
    errorMessage,
    retryCount,

    // Methods
    startPolling,
    stopPolling,
    pausePolling,
    resumePolling,
    togglePolling,
    pollNow,
  }
}
