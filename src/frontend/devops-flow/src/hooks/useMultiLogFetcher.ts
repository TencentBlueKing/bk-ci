/**
 * useMultiLogFetcher Hook
 * Manage multiple plugin log fetchers for JobDetail component
 * Uses shared core from logFetcherCore
 */
import type { LogItem } from '@/api/log'
import { onBeforeUnmount, ref } from 'vue'
import {
  fetchLogCore,
  generateHashId,
  getNextPollDelay,
  shouldContinuePolling,
  type LogPostData,
} from './logFetcherCore'

export interface PluginInfo {
  id: string
  name: string
  status: string
}

export interface PluginLogState {
  id: string
  name: string
  status: string
  expanded: boolean
  loading: boolean
  logs: LogItem[]
  lineNo: number
  finished: boolean
  hasError: boolean
  errorMessage: string
  hashId?: string
}

export interface MultiLogFetcherOptions {
  projectId: string
  pipelineId: string
  buildId: string
  executeCount: number
  debug?: boolean
  onError?: (pluginId: string, error: string) => void
}

export function useMultiLogFetcher(options: MultiLogFetcherOptions) {
  const pluginStates = ref<PluginLogState[]>([])
  const logPostData = ref<Record<string, LogPostData>>({})
  const timeIds = ref<Record<string, ReturnType<typeof setTimeout>>>({})
  const clearIds = ref<string[]>([])

  // Create log post data for a plugin
  const createPluginPostData = (pluginId: string): LogPostData => ({
    projectId: options.projectId,
    pipelineId: options.pipelineId,
    buildId: options.buildId,
    tag: pluginId,
    currentExe: options.executeCount,
    lineNo: 0,
    debug: options.debug || false,
  })

  // Initialize plugin states from plugin list
  const initPluginStates = (plugins: PluginInfo[]) => {
    // Clear existing data first
    clearAllLogs()
    
    pluginStates.value = plugins.map(plugin => ({
      id: plugin.id,
      name: plugin.name,
      status: plugin.status,
      expanded: false,
      loading: false,
      logs: [],
      lineNo: 0,
      finished: false,
      hasError: false,
      errorMessage: '',
    }))
  }

  // Update plugin statuses (when container data changes)
  const updatePluginStatuses = (plugins: PluginInfo[]) => {
    plugins.forEach(plugin => {
      const state = pluginStates.value.find(s => s.id === plugin.id)
      if (state) {
        state.status = plugin.status
        state.name = plugin.name
      }
    })
  }

  // Fetch log for a specific plugin using core function
  const fetchLog = async (pluginId: string, postData: LogPostData) => {
    const state = pluginStates.value.find(s => s.id === pluginId)
    if (!state) return

    const currentHashId = generateHashId()
    postData.hashId = currentHashId
    state.hashId = currentHashId

    const result = await fetchLogCore(postData)

    // Check if this request was cancelled
    if (clearIds.value.includes(currentHashId)) return

    state.loading = false

    if (!result.success) {
      state.hasError = true
      state.errorMessage = result.errorMessage ?? 'Failed to fetch log'
      options.onError?.(pluginId, result.errorMessage ?? 'Failed to fetch log')
      return
    }

    // Update line number
    postData.lineNo = result.lastLineNo
    state.lineNo = result.lastLineNo

    // Append new logs
    state.logs = [...state.logs, ...result.logs]

    if (result.finished) {
      state.finished = true
    }

    // Continue polling if needed
    if (shouldContinuePolling(result)) {
      timeIds.value[pluginId] = setTimeout(
        () => fetchLog(pluginId, postData),
        getNextPollDelay(result)
      )
    }
  }

  // Start loading log for a specific plugin
  const loadPluginLog = (pluginId: string) => {
    const state = pluginStates.value.find(s => s.id === pluginId)
    if (!state) return

    state.loading = true
    state.logs = []
    state.lineNo = 0
    state.finished = false
    state.hasError = false
    state.errorMessage = ''

    let postData = logPostData.value[pluginId]
    if (!postData) {
      postData = logPostData.value[pluginId] = createPluginPostData(pluginId)
    } else {
      // Reset for re-fetch
      postData.lineNo = 0
      postData.debug = options.debug || false
      postData.currentExe = options.executeCount
    }

    fetchLog(pluginId, postData)
  }

  // Toggle plugin expand/collapse
  const togglePlugin = (pluginId: string) => {
    const state = pluginStates.value.find(s => s.id === pluginId)
    if (!state) return

    state.expanded = !state.expanded

    if (state.expanded && state.logs.length === 0 && !state.loading) {
      loadPluginLog(pluginId)
    }
  }

  // Expand all plugins
  const expandAll = () => {
    pluginStates.value.forEach(state => {
      if (!state.expanded) {
        state.expanded = true
        if (state.logs.length === 0 && !state.loading) {
          loadPluginLog(state.id)
        }
      }
    })
  }

  // Collapse all plugins
  const collapseAll = () => {
    pluginStates.value.forEach(state => {
      state.expanded = false
    })
  }

  // Stop polling for a specific plugin
  const stopPluginPolling = (pluginId: string) => {
    if (timeIds.value[pluginId]) {
      clearTimeout(timeIds.value[pluginId])
      delete timeIds.value[pluginId]
    }
    
    const postData = logPostData.value[pluginId]
    if (postData?.hashId) {
      clearIds.value.push(postData.hashId)
    }
  }

  // Clear all logs and stop all polling
  const clearAllLogs = () => {
    // Stop all timers
    Object.values(timeIds.value).forEach(id => clearTimeout(id))
    timeIds.value = {}
    
    // Mark all current requests as cancelled
    Object.values(logPostData.value).forEach(postData => {
      if (postData.hashId) {
        clearIds.value.push(postData.hashId)
      }
    })
    logPostData.value = {}
    
    // Reset all plugin states
    pluginStates.value.forEach(state => {
      state.logs = []
      state.lineNo = 0
      state.expanded = false
      state.finished = false
      state.hasError = false
      state.errorMessage = ''
      state.loading = false
    })
  }

  // Update options (e.g., when executeCount or debug changes)
  const updateOptions = (newOptions: Partial<MultiLogFetcherOptions>) => {
    if (newOptions.executeCount !== undefined) {
      options.executeCount = newOptions.executeCount
    }
    if (newOptions.debug !== undefined) {
      options.debug = newOptions.debug
    }
    if (newOptions.buildId !== undefined) {
      options.buildId = newOptions.buildId
    }
  }

  // Reload all expanded plugins' logs
  const reloadExpandedLogs = () => {
    pluginStates.value.forEach(state => {
      if (state.expanded) {
        stopPluginPolling(state.id)
        loadPluginLog(state.id)
      }
    })
  }

  // Change execute count and reload
  const changeExecute = (execute: number) => {
    options.executeCount = execute
    clearAllLogs()
  }

  // Toggle debug mode and reload
  const toggleDebug = () => {
    options.debug = !options.debug
    // Reload all expanded plugins
    pluginStates.value.forEach(state => {
      if (state.expanded) {
        stopPluginPolling(state.id)
        state.logs = []
        state.lineNo = 0
        state.finished = false
        state.hasError = false
        state.errorMessage = ''
        loadPluginLog(state.id)
      }
    })
  }

  onBeforeUnmount(() => {
    clearAllLogs()
  })

  return {
    pluginStates,
    initPluginStates,
    updatePluginStatuses,
    togglePlugin,
    expandAll,
    collapseAll,
    loadPluginLog,
    clearAllLogs,
    updateOptions,
    changeExecute,
    toggleDebug,
    reloadExpandedLogs,
  }
}
