/**
 * JobDetail Component
 * Job/Container 详情侧边栏 - 显示 Job 内所有插件的日志（可折叠）和 Job 配置
 * 使用 LogHeader 组件、LogViewer 组件、useMultiLogFetcher hook 和 JobPropertyContent 组件
 */
import { buildLogDownloadUrl } from '@/api/log'
import LogViewer from '@/components/LogViewer'
import LogHeader from '@/components/LogViewer/LogHeader'
import StatusIcon from '@/components/StatusIcon'
import { JobPropertyContent } from '@/components/WorkflowOrchestration'
import { useMultiLogFetcher, type PluginInfo } from '@/hooks/useMultiLogFetcher'
import type { Container, ExecuteDetailData, Stage, StatusType } from '@/types/flow'
import { computed, defineComponent, nextTick, onBeforeUnmount, ref, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { SvgIcon } from '../SvgIcon'
import { DETAIL_TAB, type DetailTabType } from './constants'
import styles from './ExecDetail.module.css'

export default defineComponent({
  name: 'JobDetail',
  props: {
    isShow: {
      type: Boolean,
      default: false,
    },
    execDetail: {
      type: Object as PropType<ExecuteDetailData>,
      required: true,
    },
    container: {
      type: Object as PropType<Container>,
      required: true,
    },
    /** Parent Stage of the container */
    stage: {
      type: Object as PropType<Stage | null>,
      default: null,
    },
    /** Index of the container in the stage */
    containerIndex: {
      type: Number,
      default: -1,
    },
    executeCount: {
      type: Number,
      default: 1,
    },
  },
  emits: ['close'],
  setup(props, { emit, slots }) {
    const { t } = useI18n()
    const route = useRoute()

    // State
    const activeTab = ref<DetailTabType>(DETAIL_TAB.LOG)
    const showDebug = ref(false)
    const showTime = ref(true)
    const currentExe = ref(props.executeCount)
    const showMoreMenu = ref(false)

    // Get plugins list from container
    const getPlugins = (): PluginInfo[] => {
      const elements = props.container?.elements || []
      return [
        {
          id: `startVM-${props.container.id}`,
          name: 'Set up job',
          status: (props.container?.startVMStatus as string) || 'QUEUE',
        },
        ...elements.map(el => ({
          id: el.id,
          name: el.name,
          status: el.status || 'QUEUE',
        })),
      ]
    }

    // Use useMultiLogFetcher hook
    const {
      pluginStates,
      initPluginStates,
      updatePluginStatuses,
      togglePlugin,
      expandAll: hookExpandAll,
      collapseAll: hookCollapseAll,
      clearAllLogs,
      changeExecute: hookChangeExecute,
      toggleDebug: hookToggleDebug,
    } = useMultiLogFetcher({
      projectId: route.params.projectId as string,
      pipelineId: route.params.flowId as string,
      buildId: props.execDetail.id,
      executeCount: props.executeCount,
      debug: false,
      onError: (pluginId, error) => {
        console.error(`Plugin ${pluginId} log error:`, error)
      },
    })

    // Computed
    const containerName = computed(() => props.container?.name || 'Job')
    const containerStatus = computed(() => (props.container?.status || 'QUEUE') as StatusType)

    // Expand all plugins
    const expandAll = () => {
      hookExpandAll()
      showMoreMenu.value = false
    }

    // Collapse all plugins
    const collapseAll = () => {
      hookCollapseAll()
      showMoreMenu.value = false
    }

    // Download plugin log
    const downloadPluginLog = (pluginId: string, pluginName: string) => {
      const url = buildLogDownloadUrl({
        projectId: route.params.projectId as string,
        pipelineId: route.params.flowId as string,
        buildId: props.execDetail.id,
        tag: pluginId,
        executeCount: currentExe.value,
        fileName: pluginName,
      })
      location.href = url
    }

    // Download all job logs
    const downloadAllLog = () => {
      const url = buildLogDownloadUrl({
        projectId: route.params.projectId as string,
        pipelineId: route.params.flowId as string,
        buildId: props.execDetail.id,
        executeCount: currentExe.value,
        fileName: props.container?.name || 'job',
      })
      location.href = url
      showMoreMenu.value = false
    }

    // Change execute count
    const changeExecute = (execute: number) => {
      currentExe.value = execute
      hookChangeExecute(execute)
      // Re-initialize with new execute count
      initPluginStates(getPlugins())
    }

    // Toggle debug mode
    const toggleShowDebug = () => {
      showDebug.value = !showDebug.value
      hookToggleDebug()
      showMoreMenu.value = false
    }

    // Toggle time display
    const handleToggleTime = () => {
      showTime.value = !showTime.value
      showMoreMenu.value = false
    }

    // Close panel
    const closePanel = () => {
      emit('close')
    }

    // Handle click outside
    const handleClickOutside = (event: MouseEvent) => {
      const target = event.target as HTMLElement
      const logMain = document.querySelector(`.${styles.logMain}`)
      if (logMain && !logMain.contains(target)) {
        closePanel()
      }
    }

    // Render more menu items (custom items for JobDetail)
    const renderMoreMenuItems = () => (
      <>
        <li class={styles.logMoreButton} onClick={expandAll}>
          {t('flow.log.expandAll')}
        </li>
        <li class={styles.logMoreButton} onClick={collapseAll}>
          {t('flow.log.collapseAll')}
        </li>
      </>
    )

    // Watch for show changes
    watch(
      () => props.isShow,
      (newVal) => {
        if (newVal) {
          initPluginStates(getPlugins())
          nextTick(() => {
            document.addEventListener('click', handleClickOutside)
          })
        } else {
          clearAllLogs()
          document.removeEventListener('click', handleClickOutside)
        }
      },
      { immediate: true },
    )

    // Watch for container changes
    watch(
      () => props.container,
      () => {
        if (props.isShow) {
          // Update plugin statuses without clearing logs
          updatePluginStatuses(getPlugins())
        }
      },
    )

    onBeforeUnmount(() => {
      clearAllLogs()
      document.removeEventListener('click', handleClickOutside)
    })

    // Render log content
    const renderLogContent = () => {
      return (
        <div class={styles.multipleLog}>
          {pluginStates.value.map((state) => (
            <div key={state.id} class={styles.multipleLogItem}>
              <div
                class={[styles.multipleLogItemHeader, state.expanded && styles.expanded]}
                onClick={() => togglePlugin(state.id)}
              >
                <span class={[styles.multipleLogArrow, state.expanded && styles.expanded]}>
                  <SvgIcon name="angle-right" />
                </span>
                <StatusIcon status={state.status as StatusType} size="small" />
                <span class={styles.multipleLogItemName}>{state.name}</span>
                <div class={styles.multipleLogItemActions}>
                  <button
                    class={styles.downloadBtn}
                    onClick={(e) => {
                      e.stopPropagation()
                      downloadPluginLog(state.id, state.name)
                    }}
                  >
                    <SvgIcon name="download" size={14} />
                  </button>
                </div>
              </div>
              {state.expanded && (
                <div class={styles.multipleLogContent}>
                  <LogViewer
                    logs={state.logs}
                    loading={state.loading}
                    finished={state.finished}
                    error={state.hasError ? state.errorMessage : ''}
                    showLineNumber={true}
                    showTimestamp={showTime.value}
                    searchKeyword=""
                    searchMatches={[]}
                    currentMatchIndex={-1}
                  />
                </div>
              )}
            </div>
          ))}
        </div>
      )
    }

    // Render setting content with JobPropertyContent
    const renderSettingContent = () => {
      return (
        <div class={styles.settingContent}>
          <JobPropertyContent
            container={props.container}
            stage={props.stage}
            containerIndex={props.containerIndex}
            editable={false}
            isNew={false}
            isFinally={props.stage?.finally || false}
            showNameField={false}
            showJobIdField={true}
          />
        </div>
      )
    }

    return () => {
      if (!props.isShow) return null

      return (
        <article class={styles.logHome}>
          <section class={[styles.logMain, activeTab.value === DETAIL_TAB.LOG ? styles.blackTheme : styles.whiteTheme, styles.overHidden]}>
            <LogHeader
              title={containerName.value}
              status={containerStatus.value}
              showTabs={true}
              activeTab={activeTab.value}
              showSearchBar={activeTab.value === DETAIL_TAB.LOG}
              showExecuteSelector={activeTab.value === DETAIL_TAB.LOG}
              executeCount={props.executeCount}
              currentExecute={currentExe.value}
              showMoreButton={activeTab.value === DETAIL_TAB.LOG}
              showMoreMenu={showMoreMenu.value}
              showTime={showTime.value}
              showDebug={showDebug.value}
              onTab-change={(tab: DetailTabType) => (activeTab.value = tab)}
              onExecute-change={changeExecute}
              onToggle-time={handleToggleTime}
              onToggle-debug={toggleShowDebug}
              onToggle-more-menu={() => (showMoreMenu.value = !showMoreMenu.value)}
              onDownload={downloadAllLog}
            >
              {{
                'more-menu': renderMoreMenuItems,
              }}
            </LogHeader>

            <div class={styles.logContentArea}>
              {activeTab.value === DETAIL_TAB.LOG ? renderLogContent() : renderSettingContent()}
            </div>
          </section>
        </article>
      )
    }
  },
})
