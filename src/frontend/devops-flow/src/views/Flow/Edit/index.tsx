import {
  FLOW_EDIT_TABS,
  FLOW_IMPORT_EDIT_TABS,
  ROUTE_NAMES,
  isValidFlowEditTab,
} from '@/constants/routes'
import { FLOW_GROUP_TYPES } from '@/constants/flowGroup'
import { useFlowModelStore } from '@/stores/flowModel'
import { useUIStore } from '@/stores/ui'
import { Tab } from 'bkui-vue'
import { storeToRefs } from 'pinia'
import { computed, defineComponent, onBeforeUnmount, onMounted, watch } from 'vue'
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
    const flowId = computed(() => route.params.flowId as string)
    const projectId = computed(() => route.params.projectId as string)
    const version = computed(() => route.params.version as string | undefined)

    const flowModelStore = useFlowModelStore()
    const isImportMode = computed(() => flowModelStore.isImportMode)


    const editTabs = computed(() =>
      flowModelStore.isImportMode ? FLOW_IMPORT_EDIT_TABS : FLOW_EDIT_TABS,
    )

    const tabConfigs = computed(() => [
      {
        name: editTabs.value.WORKFLOW_ORCHESTRATION,
        labelKey: 'flow.content.workflowOrchestration',
      },
      {
        name: editTabs.value.WORKFLOW_ENVIRONMENT,
        labelKey: 'flow.content.workflowEnvironment',
      },
      {
        name: editTabs.value.TRIGGER_EVENTS,
        labelKey: 'flow.content.triggerEvents',
      },
      {
        name: editTabs.value.NOTIFICATION_CONFIG,
        labelKey: 'flow.content.notificationConfig',
      },
      {
        name: editTabs.value.BASIC_SETTINGS,
        labelKey: 'flow.content.basicSettings',
      },
    ])

    const uiStore = useUIStore()
    const { isVariablePanelOpen } = storeToRefs(uiStore)

    const orchTabName = computed(() => editTabs.value.WORKFLOW_ORCHESTRATION)

    const activeTab = computed(() => {
      const routeName = route.name as string
      if (isValidFlowEditTab(routeName)) {
        return routeName
      }
      return orchTabName.value
    })

    const handleTabChange = (name: string) => {
      const currentTabNames = Object.values(editTabs.value) as string[]
      if (!currentTabNames.includes(name)) return
      if (name === activeTab.value) return

      if (isImportMode.value) {
        router.push({ name })
      } else {
        router.replace({ name, params: route.params })
      }
    }

    const handleVariablePanelToggle = (isOpen: boolean) => {
      uiStore.setVariablePanelOpen(isOpen)
    }

    const shouldShowVariablePanel = computed(() => {
      return activeTab.value === orchTabName.value
    })

    watch(activeTab, (newTab) => {
      if (newTab !== orchTabName.value && isVariablePanelOpen.value) {
        uiStore.setVariablePanelOpen(false)
      }
    })

    onMounted(() => {
      if (isImportMode.value) {
        if (!flowModelStore.flowModel) {
          const restored = flowModelStore.restoreImportedFlowModel()
          if (!restored) {
            router.replace({
              name: ROUTE_NAMES.FLOW_LIST,
              params: { groupId: FLOW_GROUP_TYPES.ALL_FLOWS },
            })
          }
        }
        return
      }
      flowModelStore.loadFlowModel(projectId.value, flowId.value, version.value)
    })

    onBeforeUnmount(() => {
      flowModelStore.reset()
    })

    return () => (
      <>
        <EditHeader loading={flowModelStore.loading} />
        <div class={styles.editWrapper}>
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
              {tabConfigs.value.map((tab) => (
                <Tab.TabPanel name={tab.name} label={t(tab.labelKey)} />
              ))}
            </Tab>
            <div class={styles.tabContent}>
              <RouterView />
            </div>
          </div>
          {shouldShowVariablePanel.value && (
            <VariablePanel
              v-model={isVariablePanelOpen.value}
              flowId={flowId.value}
              editable
              onToggle={handleVariablePanelToggle}
            />
          )}
        </div>
      </>
    )
  },
})
