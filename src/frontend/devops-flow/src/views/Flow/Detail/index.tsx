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
          // Only draft version exists, redirect to edit page
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
      { immediate: true }
    )

    // 监听路由参数变化，重新加载 FlowModel
    watch(
      [route.params.flowId, route.params.version],
      ([newFlowId, newVersion]) => {
        if (newFlowId) {
          store.loadFlowModel(projectId.value, newFlowId as string, (newVersion as string) || '')
        }
      }
    )

    onMounted(() => {
      store.loadFlowModel(projectId.value, flowId.value, version.value)
    })

    // Clear execution record store when leaving the FlowDetail page
    // This ensures that when switching to a different flow, the old records are cleared
    onUnmounted(() => {
      executionRecordStore.reset()
    })

    const handleVersionChange = (version: number) => {
      router.push({
        name: ROUTE_NAMES.FLOW_DETAIL_EXECUTION_RECORD,
        params: { flowId: flowId.value, version },
      })
    }

    const handleEdit = () => {
      router.push({
        name: ROUTE_NAMES.FLOW_EDIT_WORKFLOW_ORCHESTRATION,
        params: { flowId: flowId.value, version: flowInfo.value?.version },
      })
    }

    const handleExecute = () => {
      router.push({
        name: ROUTE_NAMES.FLOW_PREVIEW,
        params: { flowId: flowId.value, version: flowInfo.value?.releaseVersion },
      })
    }

    // 从路由名称获取当前 tab（直接使用路由名称）
    const currentTab = computed(() => {
      const routeName = route.name as string
      // 如果路由名称在 FLOW_DETAIL_TABS 中，直接返回
      if (isValidFlowDetailTab(routeName as string)) {
        return routeName
      }
      return FLOW_DETAIL_TABS.EXECUTION_RECORD
    })

    // 菜单分组配置 - 根据 FLOW_DETAIL_TABS 自动生成

    // 根据 FLOW_DETAIL_TABS 生成菜单数据
    // key 和 label 都使用 routeName，label 通过国际化转换
    const menuItems = [
      {
        title: t('flow.content.executionInfo'),
        tabs: [FLOW_DETAIL_TABS.EXECUTION_RECORD, FLOW_DETAIL_TABS.TRIGGER_RECORD,],
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

    // 处理菜单点击 - 跳转到对应的子路由
    // 由于 FLOW_DETAIL_TABS 的值就是路由名称，所以可以直接使用
    const handleMenuClick = (key: string) => {
      if (!isValidFlowDetailTab(key)) {
        // 如果 tab 不合法，重定向到默认 tab
        router.push({
          name: FLOW_DETAIL_TABS.EXECUTION_RECORD,
          params: { flowId: flowId.value, version: flowInfo.value?.releaseVersion },
        })
        return
      }

      // key 就是路由名称，直接使用
      router.push({
        name: key,
        params: { flowId: flowId.value, version: flowInfo.value?.releaseVersion },
      })
    }

    return () => {
      // Don't render if only draft version (will redirect)
      if (isOnlyDraftVersion.value) {
        return null
      }

      return (
        <>
          <FlowHeader
            loading={loading.value}
            flowInfo={flowInfo.value as FlowInfo}
            versionList={(flowVersionList.value ?? []) as FlowVersion[] }
            onVersionChange={handleVersionChange}
            onEdit={handleEdit}
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
                        {item.tabs.map((child) => (
                          <button
                            key={child}
                            class={[
                              styles.menuItem,
                              currentTab.value === child && styles.menuItemActive,
                            ]}
                            onClick={() => handleMenuClick(child)}
                          >
                            {t(`flow.content.${child}`)}
                          </button>
                        ))}
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
