import { useExecuteDetail } from '@/hooks/useExecuteDetail'
import { useExecuteDetailStore } from '@/stores/executeDetail'
import { websocketRegister } from '@/utils/websocketRegister'
import { Loading } from 'bkui-vue'
import { computed, defineComponent, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import DetailHeader from './DetailHeader'
import ExecutionStatusBar from './ExecutionStatusBar'
import ExecutionTab from './ExecutionTab'
import styles from './FlowExecuteDetail.module.css'

const WS_ID = 'executeDetail'

export default defineComponent({
  name: 'ExecutionDetail',
  setup() {
    const route = useRoute()
    const executeDetailStore = useExecuteDetailStore()
    const { executeDetail, loading, executeInfo, initExecuteDetail } = useExecuteDetail()

    const showContent = computed(() => !loading.value && !!executeDetail.value)

    // ---- WebSocket real-time updates from parent (devops-nav) ----
    websocketRegister.installWsMessage(
      (data) => executeDetailStore.updateFromWebSocket(data),
      WS_ID,
    )
    websocketRegister.registerOnReconnect(() => initExecuteDetail(), WS_ID)

    onUnmounted(() => {
      websocketRegister.unInstallWsMessage(WS_ID)
    })

    watch(
      () => [route.params.buildNo, route.params.flowId, route.params.projectId],
      async ([newBuildNo, newFlowId, newProjectId], [oldBuildNo, oldFlowId, oldProjectId]) => {
        if (
          (newBuildNo !== oldBuildNo || newFlowId !== oldFlowId || newProjectId !== oldProjectId) &&
          oldBuildNo !== undefined
        ) {
          await initExecuteDetail()
        }
      },
      { immediate: false },
    )

    watch(
      () => route.query.executeCount,
      async (newCount, oldCount) => {
        if (newCount !== oldCount) {
          await initExecuteDetail()
        }
      },
      { immediate: false },
    )

    onMounted(async () => {
      await initExecuteDetail()
    })

    return () => (
      <Loading loading={loading.value} class={styles.executionDetail}>
        {showContent.value && <DetailHeader executeInfo={executeInfo.value} />}

        {showContent.value && executeDetail.value && <ExecutionStatusBar />}

        {showContent.value && (
          <div class={styles.contentWrapper}>
            <ExecutionTab />
          </div>
        )}
      </Loading>
    )
  },
})
