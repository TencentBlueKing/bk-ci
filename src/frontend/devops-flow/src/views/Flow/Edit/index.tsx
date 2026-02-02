import { FLOW_EDIT_TABS, isValidFlowEditTab } from '@/constants/routes'
import { useFlowModelStore } from '@/stores/flowModel'
import { useUIStore } from '@/stores/ui'
import { Tab } from 'bkui-vue'
import { storeToRefs } from 'pinia'
import { computed, defineComponent, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { EditHeader } from '../../../components/EditHeader'
import styles from './Edit.module.css'
import VariablePanel from './VariablePanel'

export default defineComponent({
  name: 'FlowEdit',
  setup() {
    const { t } = useI18n()
    const route = useRoute()
    const router = useRouter()
    const flowId = route.params.flowId as string
    const projectId = route.params.projectId as string
    const version = route.params.version as string | undefined

    // Use flowModel store for data loading
    const flowModelStore = useFlowModelStore()

    const tabConfigs = [
      {
        name: FLOW_EDIT_TABS.WORKFLOW_ORCHESTRATION,
        labelKey: 'flow.content.workflowOrchestration',
      },
      {
        name: FLOW_EDIT_TABS.WORKFLOW_ENVIRONMENT,
        labelKey: 'flow.content.workflowEnvironment',
      },
      {
        name: FLOW_EDIT_TABS.TRIGGER_EVENTS,
        labelKey: 'flow.content.triggerEvents',
      },
      {
        name: FLOW_EDIT_TABS.NOTIFICATION_CONFIG,
        labelKey: 'flow.content.notificationConfig',
      },
      {
        name: FLOW_EDIT_TABS.BASIC_SETTINGS,
        labelKey: 'flow.content.basicSettings',
      },
    ] as const

    // 使用 Pinia store 管理变量面板状态
    const uiStore = useUIStore()
    const { isVariablePanelOpen } = storeToRefs(uiStore)

    // 从路由名称获取当前 tab
    const activeTab = computed(() => {
      const routeName = route.name as string
      if (isValidFlowEditTab(routeName)) {
        return routeName
      }
      return FLOW_EDIT_TABS.WORKFLOW_ORCHESTRATION
    })

    // 处理 tab 切换
    const handleTabChange = (name: string) => {
      if (name === activeTab.value) return
      if (!isValidFlowEditTab(name)) {
        return
      }
      router.push({
        name,
        params: { flowId },
      })
    }

    // 处理变量面板展开/收起
    const handleVariablePanelToggle = (isOpen: boolean) => {
      uiStore.setVariablePanelOpen(isOpen)
    }

    // 判断是否显示变量面板（只在创作流编排tab时显示）
    const shouldShowVariablePanel = computed(() => {
      return activeTab.value === FLOW_EDIT_TABS.WORKFLOW_ORCHESTRATION
    })

    // 当切换tab时，如果不是创作流编排tab，自动关闭变量面板
    watch(activeTab, (newTab) => {
      if (newTab !== FLOW_EDIT_TABS.WORKFLOW_ORCHESTRATION && isVariablePanelOpen.value) {
        uiStore.setVariablePanelOpen(false)
      }
    })

    // Load flow model data on mount (ensures data is available on any tab refresh)
    onMounted(() => {
      flowModelStore.loadFlowModel(projectId, flowId, version)
    })

    return () => (
      <>
        <EditHeader loading={flowModelStore.loading} />
        <div class={styles.editWrapper}>
          {/* 主内容区域 */}
          <div
            class={[
              styles.mainContent,
              isVariablePanelOpen.value &&
                shouldShowVariablePanel.value &&
                styles.mainContentWithPanel,
            ]}
          >
            <Tab
              active={activeTab.value}
              type="card-tab"
              class={styles.tabSwitcher}
              onChange={handleTabChange}
            >
              {tabConfigs.map((tab) => (
                <Tab.TabPanel key={tab.name} name={tab.name} label={t(tab.labelKey)} />
              ))}
            </Tab>
            <div class={styles.tabContent}>
              <RouterView />
            </div>
          </div>
          {/* 变量面板 - 只在创作流编排tab时显示 */}
          {shouldShowVariablePanel.value && (
            <VariablePanel
              v-model={isVariablePanelOpen.value}
              flowId={flowId}
              editable
              onToggle={handleVariablePanelToggle}
            />
          )}
        </div>
      </>
    )
  },
})
