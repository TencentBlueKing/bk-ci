import { useExecuteDetail } from '@/hooks/useExecuteDetail'
import { usePolling } from '@/hooks/usePolling'
import { Loading } from 'bkui-vue'
import { computed, defineComponent, onMounted, ref, watch } from 'vue'
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

    // Track if initial data has been loaded for polling
    const hasInitialDataLoaded = ref(false)

    // Polling configuration - 5 seconds interval
    const POLLING_INTERVAL = 5000

    // Setup polling with independent module design
    const { startPolling, stopPolling, isPolling } = usePolling(
      async () => {
        // Silently refresh execute detail data (only record API, no loading)
        const result = await silentRefreshExecuteDetail()

        // Check if we should stop polling based on new status
        if (result && !shouldPollForStatus(result.status)) {
          console.log(
            '[ExecutionDetail] Execution finished, stopping polling. Status:',
            result.status,
          )
          stopPolling()
        }
      },
      {
        interval: POLLING_INTERVAL,
        immediate: false, // Don't start immediately, wait for initial data load
        maxRetries: 3,
        retryDelay: 2000,
        onError: (error) => {
          console.warn('[ExecutionDetail] Polling error:', error.message)
        },
      },
    )

    // Watch loading state to detect when initial data has been loaded
    // Start polling after first successful data fetch only if status requires it
    watch(loading, (isLoading, wasLoading) => {
      // Detect transition from loading to not loading (data fetch completed)
      if (wasLoading && !isLoading && !hasInitialDataLoaded.value && executeDetail.value) {
        hasInitialDataLoaded.value = true

        // Only start polling if current status requires it
        if (shouldPollForStatus(executeDetail.value.status)) {
          startPolling()
          console.log(
            '[ExecutionDetail] Initial data loaded, starting polling. Status:',
            executeDetail.value.status,
          )
        } else {
          console.log(
            '[ExecutionDetail] Initial data loaded, no polling needed. Status:',
            executeDetail.value.status,
          )
        }
      }
    })

    // Watch route changes to reset polling state
    watch(
      () => [route.params.buildNo, route.params.flowId, route.params.projectId],
      async ([newBuildNo, newFlowId, newProjectId], [oldBuildNo, oldFlowId, oldProjectId]) => {
        // If route parameters change, reload data and reset polling
        if (
          (newBuildNo !== oldBuildNo || newFlowId !== oldFlowId || newProjectId !== oldProjectId) &&
          oldBuildNo !== undefined
        ) {
          // Stop current polling before loading new data
          stopPolling()
          hasInitialDataLoaded.value = false
          await initExecuteDetail()
        }
      },
      { immediate: false },
    )

    // Watch executeCount changes (manual refresh or different execution)
    watch(
      () => route.query.executeCount,
      async (newCount, oldCount) => {
        // executeCount change means different execution, reload data
        if (newCount !== oldCount && oldCount !== undefined) {
          // Stop current polling before loading new data
          stopPolling()
          hasInitialDataLoaded.value = false
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
