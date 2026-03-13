/**
 * CompleteLog Component
 * 完整日志组件 - 全屏显示流水线执行的完整日志
 * 使用 useLogFetcher hook、LogViewer 组件和 LogHeader 组件
 */
import { downloadLogFile } from '@/api/log'
import LogViewer from '@/components/LogViewer'
import LogHeader from '@/components/LogViewer/LogHeader'
import { useLogFetcher } from '@/hooks/useLogFetcher'
import { useLogSearch } from '@/hooks/useLogSearch'
import type { ExecuteDetailData, StatusType } from '@/types/flow'
import { computed, defineComponent, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import styles from './CompleteLog.module.css'

export default defineComponent({
  name: 'CompleteLog',
  props: {
    execDetail: {
      type: Object as () => ExecuteDetailData,
      required: true,
    },
    executeCount: {
      type: Number,
      default: 1,
    },
  },
  emits: ['close'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const route = useRoute()

    // State
    const currentExe = ref(props.executeCount)
    const showTime = ref(true)
    const showMoreMenu = ref(false)

    // 使用 useLogFetcher hook
    const {
      state: logState,
      initAndFetchLog,
      changeExecute: changeExecuteCount,
      closeLogPolling,
    } = useLogFetcher({
      projectId: route.params.projectId as string,
      pipelineId: route.params.flowId as string,
      buildId: props.execDetail.id,
      executeCount: props.executeCount,
      debug: false,
    })

    // 使用 useLogSearch hook
    const {
      searchKeyword,
      searchMatches,
      currentMatchIndex,
      searchResultText,
      prevMatch,
      nextMatch,
    } = useLogSearch({
      logs: computed(() => logState.value.logs),
      containerSelector: `.${styles.logContent}`,
    })

    // Computed
    const pipelineName = computed(() => props.execDetail.pipelineName || '')
    const status = computed(() => props.execDetail.status as StatusType)

    // Methods
    const closeLog = () => {
      emit('close')
    }

    const handleExecuteChange = (execute: number) => {
      currentExe.value = execute
      changeExecuteCount(execute)
    }

    const handleToggleTime = () => {
      showTime.value = !showTime.value
      showMoreMenu.value = false
    }

    const handleToggleMoreMenu = () => {
      showMoreMenu.value = !showMoreMenu.value
    }

    const downloadLog = () => {
      const fileName = props.execDetail.pipelineName || 'pipeline'
      downloadLogFile({
        projectId: route.params.projectId as string,
        pipelineId: route.params.flowId as string,
        buildId: props.execDetail.id,
        executeCount: currentExe.value,
        fileName,
      })
      showMoreMenu.value = false
    }

    // Handle click outside to close
    const handleClickOutside = (event: MouseEvent) => {
      const target = event.target as HTMLElement
      const logMain = document.querySelector(`.${styles.logMain}`)
      if (logMain && !logMain.contains(target)) {
        closeLog()
      }
    }

    // Lifecycle
    onMounted(() => {
      initAndFetchLog()
      // Add click outside listener with delay to prevent immediate close
      setTimeout(() => {
        document.addEventListener('click', handleClickOutside)
      }, 100)
    })

    onBeforeUnmount(() => {
      closeLogPolling()
      document.removeEventListener('click', handleClickOutside)
    })

    // Watch for executeCount changes
    watch(
      () => props.executeCount,
      (newVal) => {
        currentExe.value = newVal
      },
    )

    return () => (
      <article class={styles.logHome}>
        <section class={[styles.logMain, styles.blackTheme, styles.overHidden]}>
          <LogHeader
            title={pipelineName.value}
            status={status.value}
            showTabs={false}
            searchKeyword={searchKeyword.value}
            searchMatches={searchMatches.value}
            currentMatchIndex={currentMatchIndex.value}
            showMoreButton={true}
            showMoreMenu={showMoreMenu.value}
            showTime={showTime.value}
            onSearch-change={(val: string) => (searchKeyword.value = val)}
            onSearch-prev={prevMatch}
            onSearch-next={nextMatch}
            onToggle-time={handleToggleTime}
            onToggle-more-menu={handleToggleMoreMenu}
            onDownload={downloadLog}
          />

          <div class={styles.logContent}>
            <LogViewer
              logs={logState.value.logs}
              loading={logState.value.isLoading}
              finished={logState.value.isFinished}
              error={logState.value.hasError ? logState.value.errorMessage : ''}
              showLineNumber={true}
              showTimestamp={showTime.value}
              searchKeyword={searchKeyword.value}
              searchMatches={searchMatches.value}
              currentMatchIndex={currentMatchIndex.value}
            />
          </div>
        </section>
      </article>
    )
  },
})
