/**
 * MultipleLog Component
 * 多任务日志组件 - 显示 Job 内所有插件的日志（可折叠）
 * 参考 @blueking/log 的 bkMultipleLog 组件
 */
import {
  buildLogDownloadUrl,
  getAfterLog,
  getInitLog,
  type LogItem,
} from '@/api/log'
import StatusIcon from '@/components/StatusIcon'
import { LogIcon } from '@/components/LogViewer/LogIcon'
import type { Container, StatusType } from '@/types/flow'
import { hashID } from '@/utils/util'
import { defineComponent, onBeforeUnmount, ref, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import styles from './CompleteLog.module.css'

interface PluginLogState {
  id: string
  name: string
  status: StatusType
  expanded: boolean
  loading: boolean
  logs: LogItem[]
  lineNo: number
  finished: boolean
  hasError: boolean
  errorMessage: string
  hashId?: string
}

interface LogPostData {
  projectId: string
  pipelineId: string
  buildId: string
  tag: string
  jobId: string
  currentExe: number
  lineNo: number
  debug: boolean
  subTag?: string
  hashId?: string
}

export default defineComponent({
  name: 'MultipleLog',
  props: {
    container: {
      type: Object as PropType<Container>,
      required: true,
    },
    buildId: {
      type: String,
      required: true,
    },
    executeCount: {
      type: Number,
      default: 1,
    },
    showDebug: {
      type: Boolean,
      default: false,
    },
  },
  setup(props) {
    const { t } = useI18n()
    const route = useRoute()

    // State
    const pluginStates = ref<PluginLogState[]>([])
    const logPostData = ref<Record<string, LogPostData>>({})
    const timeIds = ref<Record<string, ReturnType<typeof setTimeout>>>({})
    const clearIds = ref<string[]>([])

    // Initialize plugin states from container elements
    const initPluginStates = () => {
      const elements = props.container?.elements || []
      
      // Add "Set up job" as the first item
      const plugins = [
        {
          id: props.container?.id || '',
          name: 'Set up job',
          status: (props.container?.startVMStatus || 'QUEUE') as StatusType,
        },
        ...elements.map(el => ({
          id: el.id,
          name: el.name,
          status: (el.status || 'QUEUE') as StatusType,
        })),
      ]

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

    // Toggle plugin expand/collapse
    const togglePlugin = (pluginId: string) => {
      const state = pluginStates.value.find(s => s.id === pluginId)
      if (!state) return

      state.expanded = !state.expanded

      // Load log when first expanded
      if (state.expanded && state.logs.length === 0 && !state.loading) {
        loadPluginLog(pluginId)
      }
    }

    // Load plugin log
    const loadPluginLog = (pluginId: string) => {
      const state = pluginStates.value.find(s => s.id === pluginId)
      if (!state) return

      state.loading = true

      let postData = logPostData.value[pluginId]
      if (!postData) {
        postData = logPostData.value[pluginId] = {
          projectId: route.params.projectId as string,
          pipelineId: route.params.flowId as string,
          buildId: props.buildId,
          tag: pluginId,
          jobId: props.container?.id || '',
          currentExe: props.executeCount,
          lineNo: 0,
          debug: props.showDebug,
        }
      }

      fetchLog(pluginId, postData)
    }

    // Fetch log data
    const fetchLog = async (pluginId: string, postData: LogPostData) => {
      const state = pluginStates.value.find(s => s.id === pluginId)
      if (!state) return

      const id = hashID()
      postData.hashId = id

      try {
        const logMethod = postData.lineNo <= 0 ? getInitLog : getAfterLog
        const params = {
          projectId: postData.projectId,
          pipelineId: postData.pipelineId,
          buildId: postData.buildId,
          tag: postData.tag,
          jobId: postData.jobId,
          executeCount: postData.currentExe,
          subTag: postData.subTag,
          debug: postData.debug,
          lineNo: postData.lineNo,
        }

        const res = await logMethod(params)

        if (clearIds.value.includes(id)) return

        state.loading = false
        // 直接使用 res，因为 httpClient 已经提取了 data
        const data = res || {}

        if (data.status !== 0) {
          state.hasError = true
          state.errorMessage = data.message ?? t('flow.log.logErr')
          return
        }

        const logs = data.logs || []
        const lastLog = logs[logs.length - 1] as LogItem | undefined
        const lastLogNo = lastLog?.lineNo ?? (postData.lineNo > 0 ? postData.lineNo - 1 : -1)
        postData.lineNo = +lastLogNo + 1

        state.logs = [...state.logs, ...logs]

        if (data.finished) {
          state.finished = true
          if (data.hasMore) {
            timeIds.value[pluginId] = setTimeout(() => fetchLog(pluginId, postData), 100)
          }
        } else {
          timeIds.value[pluginId] = setTimeout(() => fetchLog(pluginId, postData), 1000)
        }
      } catch (err: unknown) {
        state.loading = false
        state.hasError = true
        state.errorMessage = err instanceof Error ? err.message : String(err)
      }
    }

    // Download plugin log
    const downloadPluginLog = (pluginId: string, pluginName: string) => {
      const url = buildLogDownloadUrl({
        projectId: route.params.projectId as string,
        pipelineId: route.params.flowId as string,
        buildId: props.buildId,
        tag: pluginId,
        jobId: props.container?.id || '',
        executeCount: props.executeCount,
        fileName: pluginName,
      })
      location.href = url
    }

    // Clear all logs and polling
    const clearAllLogs = () => {
      Object.values(timeIds.value).forEach(id => clearTimeout(id))
      timeIds.value = {}

      Object.values(logPostData.value).forEach(postData => {
        if (postData.hashId) {
          clearIds.value.push(postData.hashId)
        }
      })
      logPostData.value = {}

      pluginStates.value.forEach(state => {
        state.logs = []
        state.lineNo = 0
        state.expanded = false
        state.finished = false
        state.hasError = false
        state.errorMessage = ''
      })
    }

    // Format timestamp
    const formatTimestamp = (timestamp: number) => {
      const date = new Date(timestamp)
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      const seconds = String(date.getSeconds()).padStart(2, '0')
      const ms = String(date.getMilliseconds()).padStart(3, '0')
      return `${hours}:${minutes}:${seconds}.${ms}`
    }

    // Watch for container changes
    watch(
      () => props.container,
      () => {
        clearAllLogs()
        initPluginStates()
      },
      { immediate: true },
    )

    // Watch for executeCount or showDebug changes
    watch(
      [() => props.executeCount, () => props.showDebug],
      () => {
        clearAllLogs()
        initPluginStates()
      },
    )

    onBeforeUnmount(() => {
      clearAllLogs()
    })

    return () => (
      <div class={styles.multipleLog}>
        {pluginStates.value.map((state) => (
          <div key={state.id} class={styles.multipleLogItem}>
            <div
              class={[styles.multipleLogItemHeader, state.expanded && styles.expanded]}
              onClick={() => togglePlugin(state.id)}
            >
              <span class={[styles.multipleLogArrow, state.expanded && styles.expanded]}>
                <LogIcon name="angle-right" size={12} />
              </span>
              <StatusIcon status={state.status} size="small" />
              <span class={styles.multipleLogItemName}>{state.name}</span>
              <div class={styles.multipleLogItemActions}>
                <button
                  class={styles.downloadBtn}
                  onClick={(e) => {
                    e.stopPropagation()
                    downloadPluginLog(state.id, state.name)
                  }}
                >
                  <LogIcon name="download" size={14} />
                </button>
              </div>
            </div>
            {state.expanded && (
              <div class={styles.multipleLogContent}>
                {state.loading && state.logs.length === 0 ? (
                  <div class={styles.logPlaceholder}>
                    <span class={styles.loadingSpinner}></span>
                    {t('flow.log.loadingLog')}
                  </div>
                ) : state.hasError && state.logs.length === 0 ? (
                  <div class={styles.logError}>{state.errorMessage}</div>
                ) : (
                  <div class={styles.logLines}>
                    {state.logs.map((log: LogItem, index: number) => (
                      <div key={`${log.lineNo}-${index}`} class={styles.logLine}>
                        <span class={styles.logLineNumber}>{log.lineNo}</span>
                        <span class={styles.logTimestamp}>{formatTimestamp(log.timestamp)}</span>
                        <span class={styles.logMessage}>{log.message}</span>
                      </div>
                    ))}
                    {!state.finished && !state.hasError && (
                      <div class={styles.logLoading}>
                        <span class={styles.loadingSpinner}></span>
                        {t('flow.log.loadingLog')}
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}
          </div>
        ))}
      </div>
    )
  },
})

