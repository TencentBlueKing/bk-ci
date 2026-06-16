/**
 * useLogFetcher Hook
 * Encapsulates single plugin log fetching logic
 * Uses shared core from logFetcherCore
 */
import type { LogItem } from '@/api/log'
import { nextTick, onBeforeUnmount, ref } from 'vue'
import {
  createLogPostData,
  fetchLogCore,
  generateHashId,
  getNextPollDelay,
  shouldContinuePolling,
  type LogPostData,
} from './logFetcherCore'

export interface LogFetcherOptions {
  projectId: string
  pipelineId: string
  buildId: string
  tag?: string
  jobId?: string
  executeCount: number
  debug?: boolean
  subTag?: string
  onLogsUpdate?: (logs: LogItem[]) => void
  onSubTagsUpdate?: (subTags: string[]) => void
  onFinished?: () => void
  onError?: (error: string) => void
}

export interface LogFetcherState {
  logs: LogItem[]
  isLoading: boolean
  hasError: boolean
  errorMessage: string
  isFinished: boolean
  subTags: string[]
}

export function useLogFetcher(options: LogFetcherOptions) {
  const state = ref<LogFetcherState>({
    logs: [],
    isLoading: true,
    hasError: false,
    errorMessage: '',
    isFinished: false,
    subTags: [],
  })

  const postData = ref<LogPostData>(createLogPostData(options))
  const timeId = ref<ReturnType<typeof setTimeout>>()
  const clearIds = ref<string[]>([])

  // Fetch log with polling
  const getLog = async () => {
    const currentHashId = generateHashId()
    postData.value.hashId = currentHashId

    const result = await fetchLogCore(postData.value)

    // Check if this request was cancelled
    if (clearIds.value.includes(currentHashId)) return

    if (!result.success) {
      state.value.isLoading = false
      state.value.hasError = true
      state.value.errorMessage = result.errorMessage ?? 'Failed to fetch log'
      options.onError?.(result.errorMessage ?? 'Failed to fetch log')
      return
    }

    // Update line number for next fetch
    postData.value.lineNo = result.lastLineNo

    // Handle subTags
    if (result.subTags && result.subTags.length > 0 && state.value.subTags.length === 0) {
      state.value.subTags = result.subTags
      options.onSubTagsUpdate?.(result.subTags)
    }

    // Append new logs
    state.value.logs = [...state.value.logs, ...result.logs]
    options.onLogsUpdate?.(state.value.logs)

    // Turn off loading after first batch
    if (state.value.isLoading && state.value.logs.length > 0) {
      state.value.isLoading = false
    }

    // Also turn off loading if finished with no logs
    if (state.value.isLoading && result.finished && result.logs.length === 0) {
      state.value.isLoading = false
    }

    if (result.finished) {
      state.value.isFinished = true
      options.onFinished?.()
    }

    // Continue polling if needed
    if (shouldContinuePolling(result)) {
      timeId.value = setTimeout(getLog, getNextPollDelay(result))
    }
  }

  // Initialize and start fetching
  const initAndFetchLog = () => {
    // Reset state
    state.value.logs = []
    state.value.isLoading = true
    state.value.hasError = false
    state.value.errorMessage = ''
    state.value.isFinished = false
    state.value.subTags = []

    // Reset post data
    postData.value = createLogPostData(options)

    // Clear previous polling
    closeLogPolling()

    // Start fetching
    nextTick(() => {
      getLog()
    })
  }

  // Stop polling
  const closeLogPolling = () => {
    clearTimeout(timeId.value)
    if (postData.value.hashId) {
      clearIds.value.push(postData.value.hashId)
    }
  }

  // Change execute count
  const changeExecute = (execute: number) => {
    postData.value.currentExe = execute
    postData.value.lineNo = 0
    closeLogPolling()
    getLog()
  }

  // Change subTag
  const changeSubTag = (tag: string) => {
    postData.value.subTag = tag
    postData.value.lineNo = 0
    closeLogPolling()
    getLog()
  }

  // Toggle debug mode
  const toggleDebug = () => {
    postData.value.debug = !postData.value.debug
    postData.value.lineNo = 0
    closeLogPolling()
    getLog()
  }

  // Update options
  const updateOptions = (newOptions: Partial<LogFetcherOptions>) => {
    Object.assign(options, newOptions)
    if (newOptions.tag !== undefined) {
      postData.value.tag = newOptions.tag
      options.tag = newOptions.tag
    }
    if (newOptions.jobId !== undefined) {
      postData.value.jobId = newOptions.jobId
      options.jobId = newOptions.jobId
    }
    if (newOptions.executeCount !== undefined) {
      postData.value.currentExe = newOptions.executeCount
      options.executeCount = newOptions.executeCount
    }
    if (newOptions.debug !== undefined) {
      postData.value.debug = newOptions.debug
      options.debug = newOptions.debug
    }
    if (newOptions.subTag !== undefined) {
      postData.value.subTag = newOptions.subTag || ''
      options.subTag = newOptions.subTag
    }
  }

  onBeforeUnmount(() => {
    closeLogPolling()
  })

  return {
    state,
    initAndFetchLog,
    changeExecute,
    changeSubTag,
    toggleDebug,
    updateOptions,
    closeLogPolling,
  }
}
