import { FlowHeader } from '@/components/FlowHeader'
import { FLOW_DETAIL_TABS, isValidFlowDetailTab, ROUTE_NAMES } from '@/constants/routes'
import { useFlowInfo } from '@/hooks/useFlowInfo'
import { useExecutionRecordStore } from '@/stores/executionRecord'
import { useFlowModelStore } from '@/stores/flowModel'
import layoutStyles from '@/styles/layout.module.css'
import type { FlowInfo, FlowVersion } from '@/types/flow'
import { VERSION_STATUS_ENUM } from '@/utils/flowConst'
import { computed, defineComponent, onMounted, onUnmounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterView, useRoute, useRouter } from 'vue-router'
import styles from './Detail.module.css'

const RELEASE_ONLY_TABS: Set<string> = new Set([
  FLOW_DETAIL_TABS.EXECUTION_RECORD,
  FLOW_DETAIL_TABS.TRIGGER_RECORD,
  FLOW_DETAIL_TABS.PERMISSION_SETTINGS,
  FLOW_DETAIL_TABS.PERMISSION_DELEGATION,
  FLOW_DETAIL_TABS.OPERATION_LOG,
])

const DEFAULT_NON_RELEASE_TAB = FLOW_DETAIL_TABS.WORKFLOW_ORCHESTRATION

export default defineComponent({
  name: 'FlowDetail',
  setup() {
    const { t } = useI18n()
    const route = useRoute()
    const router = useRouter()
    const flowId = computed(() => route.params.flowId as string)
    const projectId = computed(() => route.params.projectId as string)
    const version = computed(() => route.params.version as string)
    const { flowInfo, flowVersionList, loading } = useFlowInfo()
    const store = useFlowModelStore()
    const executionRecordStore = useExecutionRecordStore()

    const isReleaseVersion = computed(() => {
      return Number(version.value) === flowInfo.value?.releaseVersion
    })

    /**
     * Check if the flow only has draft version (no released version)
     * If true, redirect to edit page instead of detail page
     */
    const isOnlyDraftVersion = computed(() => {
      return flowInfo.value?.latestVersionStatus === VERSION_STATUS_ENUM.COMMITTING
    })

    // Watch for flowInfo changes and redirect if only draft version exists
    watch(
      () => flowInfo.value?.latestVersionStatus,
      (status) => {
        if (status === VERSION_STATUS_ENUM.COMMITTING) {
          router.replace({
            name: ROUTE_NAMES.FLOW_EDIT_WORKFLOW_ORCHESTRATION,
            params: {
              projectId: projectId.value,
              flowId: flowId.value,
              version: flowInfo.value?.version?.toString(),
            },
          })
        }
      },
      { immediate: true },
    )

    // 非正式版本停留在受限 tab 时，自动跳转到配置 tab
    // watch releaseVersion 而非 isReleaseVersion，确保 flowInfo 从空加载到有值时也能触发
    watch([() => flowInfo.value?.releaseVersion, version], ([releaseVersion, ver]) => {
      if (
        releaseVersion != null &&
        Number(ver) !== releaseVersion &&
        RELEASE_ONLY_TABS.has(currentTab.value)
      ) {
        router.replace({
          name: DEFAULT_NON_RELEASE_TAB,
          params: { flowId: flowId.value, version: ver },
        })
      }
    })

    // 监听路由参数变化，重新加载 FlowModel
    watch([() => route.params.flowId, () => route.params.version], ([newFlowId, newVersion]) => {
      if (newFlowId) {
        store.loadFlowModel(projectId.value, newFlowId as string, (newVersion as string) || '', true)
      }
    })

    onMounted(() => {
      store.loadFlowModel(projectId.value, flowId.value, version.value)
    })

    onUnmounted(() => {
      executionRecordStore.reset()
    })

    const handleVersionChange = (newVersion: number) => {
      const isNewVersionRelease = newVersion === flowInfo.value?.releaseVersion
      const currentOnRestrictedTab = RELEASE_ONLY_TABS.has(currentTab.value)
      const targetTab =
        !isNewVersionRelease && currentOnRestrictedTab ? DEFAULT_NON_RELEASE_TAB : currentTab.value

      router.push({
        name: targetTab,
        params: { flowId: flowId.value, version: newVersion },
      })
    }

    const handleEdit = () => {
      router.push({
        name: ROUTE_NAMES.FLOW_EDIT_WORKFLOW_ORCHESTRATION,
        params: { flowId: flowId.value, version: flowInfo.value?.version },
      })
    }

    const handleRename = () => {
      router.push({
        name: ROUTE_NAMES.FLOW_EDIT_BASIC_SETTINGS,
        params: { flowId: flowId.value, version: flowInfo.value?.version },
        query: { focusName: '1' },
      })
    }

    const handleExecute = () => {
      router.push({
        name: ROUTE_NAMES.FLOW_PREVIEW,
        params: { flowId: flowId.value, version: flowInfo.value?.releaseVersion },
      })
    }

    const currentTab = computed(() => {
      const routeName = route.name as string
      if (isValidFlowDetailTab(routeName as string)) {
        return routeName
      }
      return FLOW_DETAIL_TABS.EXECUTION_RECORD
    })

    const menuItems = [
      {
        title: t('flow.content.executionInfo'),
        tabs: [FLOW_DETAIL_TABS.EXECUTION_RECORD, FLOW_DETAIL_TABS.TRIGGER_RECORD],
      },
      {
        title: t('flow.content.workflowConfig'),
        tabs: [
          FLOW_DETAIL_TABS.WORKFLOW_ORCHESTRATION,
          FLOW_DETAIL_TABS.WORKFLOW_ENVIRONMENT,
          FLOW_DETAIL_TABS.TRIGGER_EVENTS,
          FLOW_DETAIL_TABS.NOTIFICATION_CONFIG,
          FLOW_DETAIL_TABS.BASIC_SETTINGS,
        ],
      },
      {
        title: t('flow.content.more'),
        tabs: [
          FLOW_DETAIL_TABS.PERMISSION_SETTINGS,
          FLOW_DETAIL_TABS.PERMISSION_DELEGATION,
          FLOW_DETAIL_TABS.OPERATION_LOG,
        ],
      },
    ]

    const handleMenuClick = (key: string) => {
      if (!isValidFlowDetailTab(key)) {
        router.push({
          name: FLOW_DETAIL_TABS.EXECUTION_RECORD,
          params: { flowId: flowId.value, version: flowInfo.value?.releaseVersion },
        })
        return
      }

      if (!isReleaseVersion.value && RELEASE_ONLY_TABS.has(key)) {
        return
      }

      router.push({
        name: key,
        params: { flowId: flowId.value, version: version.value },
      })
    }

    return () => {
      if (isOnlyDraftVersion.value) {
        return null
      }

      return (
        <>
          <FlowHeader
            loading={loading.value}
            flowInfo={flowInfo.value as FlowInfo}
            versionList={(flowVersionList.value ?? []) as FlowVersion[]}
            onVersionChange={handleVersionChange}
            onEdit={handleEdit}
            onRename={handleRename}
            onExecute={handleExecute}
          />
          <div class={layoutStyles.content}>
            <nav class={styles.sidebar}>
              {menuItems.map((item, index) => (
                <div key={index} class={styles.menuGroup}>
                  {item.tabs.length > 0 && (
                    <>
                      <div class={styles.menuCategory}>{item.title}</div>
                      <div class={styles.subMenu}>
                        {item.tabs.map((child) => {
                          const disabled = !isReleaseVersion.value && RELEASE_ONLY_TABS.has(child)
                          return (
                            <button
                              key={child}
                              class={[
                                styles.menuItem,
                                currentTab.value === child && styles.menuItemActive,
                                disabled && styles.menuItemDisabled,
                              ]}
                              disabled={disabled}
                              onClick={() => handleMenuClick(child)}
                            >
                              {t(`flow.content.${child}`)}
                            </button>
                          )
                        })}
                      </div>
                    </>
                  )}
                </div>
              ))}
            </nav>

            <div class={styles.content}>
              <RouterView />
            </div>
          </div>
        </>
      )
    }
  },
})
