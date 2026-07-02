import { isValidFlowExecutionDetailTab, ROUTE_NAMES } from '@/constants/routes'
import { Tab } from 'bkui-vue'
import { computed, defineComponent, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterView, useRoute, useRouter } from 'vue-router'
import styles from './ExecutionTab.module.css'

export default defineComponent({
  name: 'ExecutionTab',
  setup() {
    const { t } = useI18n()
    const route = useRoute()
    const router = useRouter()

    const currentTab = computed(() => {
      return route.name as string
    })

    // Tab 配置（子组件直接从 store 获取数据，不需要传递 props）
    const tabConfig = [
      {
        name: ROUTE_NAMES.FLOW_DETAIL_EXECUTION_DETAIL_TAB,
        label: t('flow.execute.executeDetail'),
      },
      {
        name: ROUTE_NAMES.FLOW_DETAIL_ARTIFACTS,
        label: t('flow.execute.outputArtifact'),
      },
      {
        name: ROUTE_NAMES.FLOW_DETAIL_OUTPUTS,
        label: t('flow.execute.outputReport'),
        routeName: ROUTE_NAMES.FLOW_DETAIL_OUTPUTS,
      },
      {
        name: ROUTE_NAMES.FLOW_DETAIL_START_PARAMS,
        label: t('flow.execute.startupParameters'),
      },
    ]

    // 处理 Tab 切换
    function handleTabChange(tabName: string) {
      if (!isValidFlowExecutionDetailTab(tabName)) {
        // 如果 tab 不合法，重定向到默认 tab
        router.replace({
          name: ROUTE_NAMES.FLOW_DETAIL_EXECUTION_DETAIL_TAB,
          params: route.params,
          query: route.query,
        })
        return
      }

      // 使用 replace 而不是 push，避免历史记录堆积
      router.replace({
        name: tabName,
        params: route.params,
        query: route.query,
      })
    }

    return () => (
      <div class={styles.executionTab}>
        <Tab
          v-model:active={currentTab.value}
          type="card-tab"
          class={styles.tabSwitcher}
          onChange={handleTabChange}
        >
          {tabConfig.map((tab) => (
            <Tab.TabPanel key={tab.name} name={tab.name} label={tab.label}>
              {/* Tab 内容通过 RouterView 渲染 */}
            </Tab.TabPanel>
          ))}
        </Tab>
        <div class={styles.tabContent}>
          <RouterView />
        </div>
      </div>
    )
  },
})
