/**
 * PluginDetail Component
 * 插件详情侧边栏 - 显示单个插件的日志和配置
 * 使用 useLogFetcher hook、LogViewer 组件、LogHeader 组件和 AtomPropertyContent 组件
 * 参考 devops-pipeline 的 plugin.vue 和 pluginLog.vue 实现
 */
import { buildLogDownloadUrl } from '@/api/log'
import LogViewer from '@/components/LogViewer'
import LogHeader from '@/components/LogViewer/LogHeader'
import { AtomPropertyContent } from '@/components/WorkflowOrchestration'
import { useLogFetcher } from '@/hooks/useLogFetcher'
import { useLogSearch } from '@/hooks/useLogSearch'
import type { Element, ExecuteDetailData, StatusType } from '@/types/flow'
import { computed, defineComponent, nextTick, onBeforeUnmount, ref, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { DETAIL_TAB, type DetailTabType } from './constants'
import styles from './ExecDetail.module.css'

export default defineComponent({
  name: 'PluginDetail',
  props: {
    isShow: {
      type: Boolean,
      default: false,
    },
    execDetail: {
      type: Object as PropType<ExecuteDetailData>,
      required: true,
    },
    element: {
      type: Object as PropType<Element>,
      required: true,
    },
    containerId: {
      type: String,
      default: '',
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
    const activeTab = ref<DetailTabType>(DETAIL_TAB.LOG)
    const showDebug = ref(false)
    const showTime = ref(true)
    const currentExe = ref(props.executeCount)
    const currentSubTag = ref('')
    const showMoreMenu = ref(false)
    const moreMenuRef = ref<HTMLElement | null>(null)
    
    // 使用 useLogFetcher hook
    const {
      state: logState,
      initAndFetchLog,
      changeExecute,
      changeSubTag,
      toggleDebug,
      updateOptions,
      closeLogPolling,
    } = useLogFetcher({
      projectId: route.params.projectId as string,
      pipelineId: route.params.flowId as string,
      buildId: props.execDetail.id,
      tag: props.element?.id || undefined, // pluginLog 只传 tag，不传 jobId
      executeCount: props.executeCount,
      debug: false,
      subTag: '',
      onSubTagsUpdate: (subTags) => {
        // subTags 已经在 hook 中处理
      },
    })

    // 使用 useLogSearch hook
    const {
      searchKeyword,
      searchMatches,
      currentMatchIndex,
      prevMatch,
      nextMatch,
    } = useLogSearch({
      logs: computed(() => logState.value.logs),
      containerSelector: `.${styles.logContentArea}`,
    })

    // Computed
    const elementName = computed(() => props.element?.name || 'Plugin')
    const elementStatus = computed(() => (props.element?.status || 'QUEUE') as StatusType)

    const downloadLink = computed(() => {
      return buildLogDownloadUrl({
        projectId: route.params.projectId as string,
        pipelineId: route.params.flowId as string,
        buildId: props.execDetail.id,
        tag: props.element?.id || undefined, // pluginLog 只传 tag
        executeCount: currentExe.value,
        fileName: props.element?.name || 'plugin',
      })
    })

    // Methods
    const closePanel = () => {
      emit('close')
    }

    const handleExecuteChange = (execute: number) => {
      currentExe.value = execute
      changeExecute(execute)
    }

    const handleSubTagChange = (tag: string) => {
      currentSubTag.value = tag
      changeSubTag(tag)
    }

    const handleToggleDebug = () => {
      showDebug.value = !showDebug.value
      toggleDebug()
      showMoreMenu.value = false
    }

    const handleToggleTime = () => {
      showTime.value = !showTime.value
      showMoreMenu.value = false
    }

    const downloadLog = () => {
      location.href = downloadLink.value
      showMoreMenu.value = false
    }

    const toggleMoreMenu = () => {
      showMoreMenu.value = !showMoreMenu.value
    }

    // Handle click outside
    const handleClickOutside = (event: MouseEvent) => {
      const target = event.target as HTMLElement
      const logMain = document.querySelector(`.${styles.logMain}`)
      if (logMain && !logMain.contains(target)) {
        closePanel()
      }
      // 关闭更多菜单
      if (moreMenuRef.value && !moreMenuRef.value.contains(target)) {
        showMoreMenu.value = false
      }
    }

    // Watch for show changes
    watch(
      () => props.isShow,
      (newVal) => {
        if (newVal) {
          // 更新 tag 并重新获取日志
          updateOptions({
            tag: props.element?.id || undefined,
            executeCount: props.executeCount,
          })
          nextTick(() => {
            initAndFetchLog()
            document.addEventListener('click', handleClickOutside)
          })
        } else {
          closeLogPolling()
          document.removeEventListener('click', handleClickOutside)
        }
      },
      { immediate: true },
    )

    // Watch for element changes
    watch(
      () => props.element,
      () => {
        if (props.isShow && props.element?.id) {
          updateOptions({
            tag: props.element?.id || undefined,
          })
          initAndFetchLog()
        }
      },
    )

    onBeforeUnmount(() => {
      closeLogPolling()
      document.removeEventListener('click', handleClickOutside)
    })

    // Render setting content with AtomPropertyContent
    const renderSettingContent = () => {
      if (!props.element) {
        return (
          <div class={styles.settingPlaceholder}>
            {t('flow.log.settingPlaceholder')}
          </div>
        )
      }

      return (
        <div class={styles.settingContent}>
          <AtomPropertyContent
            element={props.element}
            editable={false}
            showAtomSelector={false}
            showStepIdField={true}
            showVersionSelector={true}
            showCustomEnvSection={true}
            showFlowControlSection={true}
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
              title={elementName.value}
              status={elementStatus.value}
              showTabs={true}
              activeTab={activeTab.value}
              showSearchBar={activeTab.value === DETAIL_TAB.LOG}
              searchKeyword={searchKeyword.value}
              searchMatches={searchMatches.value}
              currentMatchIndex={currentMatchIndex.value}
              executeCount={props.executeCount}
              currentExecute={currentExe.value}
              showMoreButton={activeTab.value === DETAIL_TAB.LOG}
              showMoreMenu={showMoreMenu.value}
              showTime={showTime.value}
              showDebug={showDebug.value}
              onTab-change={(tab: DetailTabType) => (activeTab.value = tab)}
              onSearch-change={(val: string) => (searchKeyword.value = val)}
              onSearch-prev={prevMatch}
              onSearch-next={nextMatch}
              onExecute-change={handleExecuteChange}
              onToggle-time={handleToggleTime}
              onToggle-debug={handleToggleDebug}
              onToggle-more-menu={toggleMoreMenu}
              onDownload={downloadLog}
            />

            <div class={styles.logContentArea}>
              {activeTab.value === DETAIL_TAB.LOG ? (
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
                  onTag-change={handleSubTagChange}
                />
              ) : (
                renderSettingContent()
              )}
            </div>
          </section>
        </article>
      )
    }
  },
})
