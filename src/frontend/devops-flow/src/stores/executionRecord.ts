import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  getExecutionRecords,
  type ExecutionRecord,
  type ExecutionRecordQueryParams,
} from '../api/executionRecord'

export const useExecutionRecordStore = defineStore('executionRecord', () => {
  // Execution record list
  const records = ref<ExecutionRecord[]>([])

  // Pagination info
  const pagination = ref({
    current: 1,
    count: 0,
    limit: 20,
    totalPages: 0,
  })

  const initialQueryParams: Omit<ExecutionRecordQueryParams, 'page' | 'pageSize'> = {
    projectId: '',
    pipelineId: '',
    startTimeStartTime: undefined,
    endTimeEndTime: undefined,
    status: undefined,
    triggerMethod: undefined,
    triggerEvent: undefined,
    triggerUser: undefined,
    triggerNode: undefined,
    remark: undefined,
    debug: false,
  }

  // Query parameters
  const queryParams = ref<Omit<ExecutionRecordQueryParams, 'page' | 'pageSize'>>({
    ...initialQueryParams,
  })

  // Loading state
  const loading = ref(false)

  // Select all state
  const isAllChecked = computed(() => {
    return records.value.length > 0 && records.value.every((item) => item.checked)
  })

  const isIndeterminate = computed(() => {
    const checkedCount = records.value.filter((item) => item.checked).length
    return checkedCount > 0 && checkedCount < records.value.length
  })

  /**
   * Load execution records
   */
  async function loadExecutionRecords(page?: number) {
    if (!queryParams.value.projectId || !queryParams.value.pipelineId) {
      console.warn('projectId or pipelineId is not set')
      return
    }

    loading.value = true
    try {
      const params: ExecutionRecordQueryParams = {
        ...queryParams.value,
        page: page || pagination.value.current,
        pageSize: pagination.value.limit,
      }
      console.log('params', params)
      const response = await getExecutionRecords(params)
      records.value = response.list
      pagination.value.count = response.count
      pagination.value.current = response.page
      pagination.value.limit = response.limit
      pagination.value.totalPages = response.totalPages
    } catch (error) {
      console.error('Failed to load execution records:', error)
      records.value = []
    } finally {
      loading.value = false
    }
  }

  /**
   * Set query parameters
   */
  function setQueryParams(params: Partial<typeof queryParams.value>) {
    queryParams.value = {
      ...queryParams.value,
      ...params,
    }
  }

  /**
   * Set pagination
   */
  function setPagination(page: number, limit?: number) {
    pagination.value.current = page
    if (limit !== undefined) {
      pagination.value.limit = limit
    }
  }

  /**
   * Toggle select all
   */
  function toggleSelectAll(checked: boolean) {
    records.value.forEach((item) => {
      item.checked = checked
    })
  }

  /**
   * Toggle single selection
   */
  function toggleSelect(recordId: string, checked: boolean) {
    const record = records.value.find((item) => item.id === recordId)
    if (record) {
      record.checked = checked
    }
  }

  /**
   * Reset state
   */
  function reset() {
    records.value = []
    pagination.value = {
      current: 1,
      count: 0,
      limit: 20,
      totalPages: 0,
    }
    queryParams.value = { ...initialQueryParams }
  }

  return {
    // State
    records,
    pagination,
    queryParams,
    loading,
    isAllChecked,
    isIndeterminate,

    // Methods
    loadExecutionRecords,
    setQueryParams,
    setPagination,
    toggleSelectAll,
    toggleSelect,
    reset,
  }
})
