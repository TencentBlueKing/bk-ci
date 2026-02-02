import { storeToRefs } from 'pinia'
import { computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import type { ExecutionRecord, ExecutionRecordQueryParams } from '../api/executionRecord'
import { useExecutionRecordStore } from '../stores/executionRecord'

/**
 * Execution record data hook
 * Fetches data from store, processes it, and provides it to components
 */
export function useExecutionRecordData(debug = false) {
  const store = useExecutionRecordStore()
  const route = useRoute()
  const projectId = computed(() => route.params.projectId as string)
  const flowId = computed(() => route.params.flowId as string)

  // Use storeToRefs to ensure reactivity
  const { records, pagination, queryParams, loading } =
    storeToRefs(store)

  // Watch for projectId and pipelineId changes
  watch(
    () => [projectId.value, flowId.value],
    ([newProjectId, newPipelineId]) => {
      if (newProjectId && newPipelineId) {
        store.setQueryParams({ 
            projectId: newProjectId, 
            pipelineId: newPipelineId,
            debug,
          })
          store.loadExecutionRecords(1)
      }
    },
    { immediate: true },
  )

  // Watch query parameter changes and auto-load data
  // 使用 JSON.stringify 来深度监听所有筛选参数的变化
  watch(
    () => JSON.stringify({
      startTime: queryParams.value.startTimeStartTime, 
      endTime: queryParams.value.endTimeEndTime, 
      status: queryParams.value.status,
      triggerMethod: queryParams.value.triggerMethod,
      triggerEvent: queryParams.value.triggerEvent,
      triggerUser: queryParams.value.triggerUser,
      triggerNode: queryParams.value.triggerNode,
      remark: queryParams.value.remark,
    }),
    () => {
      if (queryParams.value.projectId && queryParams.value.pipelineId) {
        store.loadExecutionRecords(1) // Reset to first page
      }
    },
  )

  // Load data when component mounts
  onMounted(() => {
    if (queryParams.value.projectId && queryParams.value.pipelineId && records.value.length === 0 && !loading.value) {
      store.loadExecutionRecords()
    }
  })

  /**
   * Handle page change
   * Now uses server-side pagination
   */
  const handlePageChange = (page: number) => {
    store.setPagination(page)
    store.loadExecutionRecords(page)
  }

  /**
   * Handle page size change
   * Now uses server-side pagination
   */
  const handleLimitChange = (limit: number) => {
    store.setPagination(1, limit)
    store.loadExecutionRecords(1)
  }

  /**
   * Handle select all
   */
  const handleSelectAll = (checked: boolean) => {
    store.toggleSelectAll(checked)
  }

  /**
   * Handle single selection
   */
  const handleSelect = (record: ExecutionRecord, checked: boolean) => {
    store.toggleSelect(record.id, checked)
  }

  /**
   * Update query parameters
   */
  const updateQueryParams = (
    params: Partial<Omit<ExecutionRecordQueryParams, 'page' | 'pageSize' | 'projectId' | 'pipelineId'>>,
  ) => {
    store.setQueryParams(params)
  }

  /**
   * Refresh data
   */
  const refresh = () => {
    store.loadExecutionRecords()
  }

  return {
    // Data
    records,
    pagination,
    loading,
  

    // Methods
    handlePageChange,
    handleLimitChange,
    handleSelectAll,
    handleSelect,
    updateQueryParams,
    refresh,
  }
}
