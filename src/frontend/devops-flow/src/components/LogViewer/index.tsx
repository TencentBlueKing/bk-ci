/**
 * LogViewer Component
 * Vue 3 兼容的日志渲染组件
 * 参考 @blueking/log 的 bk-log 组件实现
 */
import type { LogItem } from '@/api/log'
import {
  computed,
  defineComponent,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
  type PropType,
} from 'vue'
import { useI18n } from 'vue-i18n'
import styles from './LogViewer.module.css'

export default defineComponent({
  name: 'LogViewer',
  props: {
    logs: {
      type: Array as PropType<LogItem[]>,
      default: () => [],
    },
    loading: {
      type: Boolean,
      default: false,
    },
    finished: {
      type: Boolean,
      default: false,
    },
    error: {
      type: String,
      default: '',
    },
    showLineNumber: {
      type: Boolean,
      default: true,
    },
    showTimestamp: {
      type: Boolean,
      default: true,
    },
    searchKeyword: {
      type: String,
      default: '',
    },
    searchMatches: {
      type: Array as PropType<number[]>,
      default: () => [],
    },
    currentMatchIndex: {
      type: Number,
      default: -1,
    },
  },
  emits: ['tag-change'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const logContentRef = ref<HTMLElement | null>(null)
    const autoScroll = ref(true)
    const lastScrollTop = ref(0)

    // 格式化时间戳
    const formatTimestamp = (timestamp: number): string => {
      if (!timestamp) return ''
      const date = new Date(timestamp)
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      const seconds = String(date.getSeconds()).padStart(2, '0')
      const milliseconds = String(date.getMilliseconds()).padStart(3, '0')
      return `${hours}:${minutes}:${seconds}.${milliseconds}` || ''
    }

    // 获取日志级别样式类
    const getLevelClass = (message: string): string => {
      const msg = message.toLowerCase()
      if (msg.includes('error') || msg.includes('exception') || msg.includes('failed')) {
        return styles.levelError || ''
      }
      if (msg.includes('warning') || msg.includes('warn')) {
        return styles.levelWarning || ''
      }
      if (msg.includes('debug')) {
        return styles.levelDebug || ''
      }
      return styles.levelInfo || ''
    }

    // 高亮搜索关键字
    const highlightKeyword = (text: string): string => {
      if (!props.searchKeyword || !text) return text

      const keyword = props.searchKeyword
      const regex = new RegExp(`(${keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi')
      return text.replace(regex, `<span class="${styles.searchHighlight}">$1</span>`)
    }

    // 检查是否应该自动滚动
    const shouldAutoScroll = (): boolean => {
      if (!logContentRef.value) return false
      const container = logContentRef.value
      const scrollTop = container.scrollTop
      const scrollHeight = container.scrollHeight
      const clientHeight = container.clientHeight

      // 如果用户手动向上滚动，则禁用自动滚动
      if (scrollTop < lastScrollTop.value) {
        return false
      }

      // 如果接近底部（距离底部小于 50px），则启用自动滚动
      return scrollHeight - scrollTop - clientHeight < 50
    }

    // 滚动到底部
    const scrollToBottom = () => {
      if (!logContentRef.value) return
      nextTick(() => {
        const container = logContentRef.value
        if (container) {
          container.scrollTop = container.scrollHeight
          lastScrollTop.value = container.scrollTop
        }
      })
    }

    // 监听日志变化，自动滚动
    watch(
      () => props.logs.length,
      () => {
        if (shouldAutoScroll()) {
          scrollToBottom()
        }
      },
    )

    // 监听滚动事件，检测用户是否手动滚动
    const handleScroll = () => {
      if (!logContentRef.value) return
      const container = logContentRef.value
      lastScrollTop.value = container.scrollTop

      // 如果用户滚动到底部，重新启用自动滚动
      const scrollHeight = container.scrollHeight
      const clientHeight = container.clientHeight
      const scrollTop = container.scrollTop
      if (scrollHeight - scrollTop - clientHeight < 50) {
        autoScroll.value = true
      }
    }

    // 渲染单行日志
    const renderLogLine = (log: LogItem, index: number) => {
      const isMatch = props.searchMatches.includes(index)
      const isActiveMatch = isMatch && props.searchMatches[props.currentMatchIndex] === index

      return (
        <div
          key={`${log.lineNo}-${index}`}
          data-line-index={index}
          class={[
            styles.logLine,
            getLevelClass(log.message),
            isMatch && styles.highlighted,
            isActiveMatch && styles.highlightedActive,
          ]}
        >
          {props.showLineNumber && <span class={styles.lineNumber}>{log.lineNo}</span>}
          {props.showTimestamp && (
            <span class={styles.timestamp}>{formatTimestamp(log.timestamp)}</span>
          )}
          <span class={styles.message} innerHTML={highlightKeyword(log.message)} />
        </div>
      )
    }

    // 渲染加载状态
    const renderLoading = () => {
      if (!props.loading || props.logs.length > 0) return null

      return (
        <div class={styles.loadingPlaceholder}>
          <span class={styles.loadingSpinner}></span>
          <span>{t('flow.log.loadingLog')}</span>
        </div>
      )
    }

    // 渲染加载更多状态
    const renderLoadingMore = () => {
      if (!props.loading || props.logs.length === 0 || props.finished) return null

      return (
        <div class={styles.loadingMore}>
          <span class={styles.loadingSpinner}></span>
          <span>{t('flow.log.loadingMore')}</span>
        </div>
      )
    }

    // 渲染错误状态
    const renderError = () => {
      if (!props.error) return null

      return <div class={styles.errorMessage}>{props.error || t('flow.log.logErr')}</div>
    }

    // 渲染空状态
    const renderEmpty = () => {
      if (props.loading || props.error || props.logs.length > 0) return null

      return <div class={styles.emptyMessage}>{t('flow.log.noLog')}</div>
    }

    onMounted(() => {
      if (logContentRef.value) {
        logContentRef.value.addEventListener('scroll', handleScroll)
      }
    })

    onBeforeUnmount(() => {
      if (logContentRef.value) {
        logContentRef.value.removeEventListener('scroll', handleScroll)
      }
    })

    return () => (
      <div class={styles.logViewer}>
        <div ref={logContentRef} class={styles.logContent}>
          {renderLoading()}
          {renderError()}
          {renderEmpty()}

          {props.logs.length > 0 && (
            <div class={styles.logLines}>
              {props.logs.map((log, index) => renderLogLine(log, index))}
            </div>
          )}

          {renderLoadingMore()}
        </div>
      </div>
    )
  },
})
