import { useExecuteDetail } from '@/hooks/useExecuteDetail'
import { usePolling } from '@/hooks/usePolling'
import { useExecuteDetailStore } from '@/stores/executeDetail'
import { websocketRegister } from '@/utils/websocketRegister'
import { Loading } from 'bkui-vue'
import { computed, defineComponent, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import DetailHeader from './DetailHeader'
import ExecutionStatusBar from './ExecutionStatusBar'
import ExecutionTab from './ExecutionTab'
import styles from './FlowExecuteDetail.module.css'

/**
 * Check if the execution status requires polling
 * Returns true for running/waiting states, false for terminal states (finished/error/canceled)
 */
function shouldPollForStatus(status: string | undefined): boolean {
  if (!status) return false

  const terminalStates = [
    'SUCCEED',
    'FAILED',
    'CANCELED',
    'TERMINATE',
    'REVIEW_ABORT',
    'HEARTBEAT_TIMEOUT',
    'UNEXEC',
    'SKIP',
    'QUALITY_CHECK_FAIL',
    'QUEUE_TIMEOUT',
    'EXEC_TIMEOUT',
    'STAGE_SUCCESS',
    'QUOTA_FAILED',
  ]

  return !terminalStates.includes(status)
}

const WS_ID = 'executeDetail'

export default defineComponent({
  name: 'ExecutionDetail',
  setup() {
    const route = useRoute()
    const executeDetailStore = useExecuteDetailStore()
    const { executeDetail, loading, executeInfo, initExecuteDetail, silentRefreshExecuteDetail } =
      useExecuteDetail()

    const showContent = computed(() => !loading.value && !!executeDetail.value)

    const POLLING_INTERVAL = 5000

    const { startPolling, stopPolling, isPolling } = usePolling(
      async () => {
        const result = await silentRefreshExecuteDetail()
        if (result && !shouldPollForStatus(result.status)) {
          stopPolling()
        }
      },
      {
        interval: POLLING_INTERVAL,
        immediate: false,
        maxRetries: 3,
        retryDelay: 2000,
        onError: (error) => {
          console.warn('[ExecutionDetail] Polling error:', error.message)
        },
      },
    )

    // ---- WebSocket real-time updates ----
    websocketRegister.installWsMessage(
      (data) => {
        executeDetailStore.updateFromWebSocket(data)
        if (data && !shouldPollForStatus(data.status)) {
          stopPolling()
        }
      },
      'IFRAMEprocess',
      WS_ID,
    )

    onUnmounted(() => {
      websocketRegister.unInstallWsMessage(WS_ID)
    })
    // ---- End WebSocket ----

    watch(
      () => executeDetail.value?.status,
      (newStatus) => {
        if (shouldPollForStatus(newStatus) && !isPolling.value) {
          startPolling()
        }
      },
    )

    async function loadAndStartPolling() {
      stopPolling()
      await initExecuteDetail()
      if (executeDetail.value && shouldPollForStatus(executeDetail.value.status)) {
        startPolling()
      }
    }

    watch(
      () => [route.params.buildNo, route.params.flowId, route.params.projectId],
      async ([newBuildNo, newFlowId, newProjectId], [oldBuildNo, oldFlowId, oldProjectId]) => {
        if (
          (newBuildNo !== oldBuildNo || newFlowId !== oldFlowId || newProjectId !== oldProjectId) &&
          oldBuildNo !== undefined
        ) {
          await loadAndStartPolling()
        }
      },
      { immediate: false },
    )

    watch(
      () => route.query.executeCount,
      async (newCount, oldCount) => {
        if (newCount !== oldCount) {
          await loadAndStartPolling()
        }
      },
      { immediate: false },
    )

    onMounted(async () => {
      await loadAndStartPolling()
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
