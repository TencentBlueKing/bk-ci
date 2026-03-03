import { useExecuteDetail } from '@/hooks/useExecuteDetail'
import { usePolling } from '@/hooks/usePolling'
import { Loading } from 'bkui-vue'
import { computed, defineComponent, onMounted, watch } from 'vue'
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

  // Terminal states - no need to poll
  const terminalStates = [
    'SUCCEED', // Success (final)
    'FAILED', // Failed (final)
    'CANCELED', // Canceled (final)
    'TERMINATE', // Terminated (final)
    'REVIEW_ABORT', // Review rejected (final)
    'HEARTBEAT_TIMEOUT', // Heartbeat timeout (final)
    'UNEXEC', // Never executed (final)
    'SKIP', // Skipped (final)
    'QUALITY_CHECK_FAIL', // Quality check failed (final)
    'QUEUE_TIMEOUT', // Queue timeout (final)
    'EXEC_TIMEOUT', // Execution timeout (final)
    'STAGE_SUCCESS', // Stage success (final)
    'QUOTA_FAILED', // Quota failed (final)
  ]

  return !terminalStates.includes(status)
}

export default defineComponent({
  name: 'ExecutionDetail',
  setup() {
    const route = useRoute()
    // Get execution history detail data (from store, globally unique)
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

    // Auto-restart polling when status changes to a running state
    // Handles cases like: manual review approval, stage gate passed, etc.
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

        {/* Execution status bar - only render after data is loaded */}
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
