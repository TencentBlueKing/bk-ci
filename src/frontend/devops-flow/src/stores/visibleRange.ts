import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { useRoute } from 'vue-router'
import {
  getVisibleRangeList,
  addVisibleRange,
  removeVisibleRange,
  type VisibleRangeRecord,
  type VisibleRangeQueryParams,
  type AddVisibleRangeItem,
} from '@/api/visibleRange'

/**
 * 可用范围状态管理
 */
export const useVisibleRangeStore = defineStore('visibleRange', () => {
  const route = useRoute()
  const pipelineId = computed(() => route.params.flowId as string)
  const projectId = computed(() => route.params.projectId as string)

  const visibleRangeList = ref<VisibleRangeRecord[]>([])
  const loading = ref(false)
  const searchValue = ref('')
  const selectedType = ref('')
  const pagination = ref({
    current: 1,
    count: 0,
    limit: 10,
  })

  /**
   * 加载可用范围列表
   * @param page 页码
   * @param limit 每页大小
   */
  async function loadVisibleRangeList(page?: number, limit?: number) {
    loading.value = true
    try {
      const params: VisibleRangeQueryParams = {
        projectId: projectId.value,
        pipelineId: pipelineId.value,
        page: page ?? pagination.value.current,
        pageSize: limit ?? pagination.value.limit,
      }

      const res = await getVisibleRangeList(params)
      visibleRangeList.value = res.records
      pagination.value = {
        current: page ?? pagination.value.current,
        limit: limit ?? pagination.value.limit,
        count: res.count,
      }
    } catch (error) {
      console.error('Failed to load visible range list:', error)
      visibleRangeList.value = []
    } finally {
      loading.value = false
    }
  }

  /**
   * 切换页码
   * @param page 页码
   */
  async function handlePageChange(page: number) {
    pagination.value.current = page
    await loadVisibleRangeList(page, pagination.value.limit)
  }

  /**
   * 切换每页大小
   * @param limit 每页大小
   */
  async function handleLimitChange(limit: number) {
    pagination.value.current = 1
    pagination.value.limit = limit

    await loadVisibleRangeList(pagination.value.current, limit)
  }

  /**
   * 处理搜索
   * @param value 搜索值
   */
  function handleSearch(value: string) {
    searchValue.value = value
    pagination.value.current = 1
    loadVisibleRangeList()
  }

  /**
   * 处理类型筛选
   * @param type 类型
   */
  function handleTypeChange(type: string) {
    selectedType.value = type
    pagination.value.current = 1
    loadVisibleRangeList()
  }

  /**
   * 清空筛选
   */
  function clearFilters() {
    searchValue.value = ''
    selectedType.value = ''
    pagination.value.current = 1
    loadVisibleRangeList()
  }

  /**
   * 添加可用范围
   * @param items 添加数据列表
   */
  async function handleAdd(items: AddVisibleRangeItem[]) {
    try {
      await addVisibleRange({
        projectId: projectId.value,
        pipelineId: pipelineId.value,
        items,
      })
      // TODO: 添加成功后的处理逻辑
      // 例如：刷新列表、显示成功提示等
      await loadVisibleRangeList()
      console.log('添加成功')
    } catch (error) {
      console.error('Failed to add visible range:', error)
      throw error
    }
  }

  /**
   * 移除可用范围
   * @param ids 可见范围ID列表
   */
  async function handleRemove(ids: string[]) {
    try {
      await removeVisibleRange({
        projectId: projectId.value,
        pipelineId: pipelineId.value,
        ids,
      })
      // TODO: 移除成功后的处理逻辑
      // 例如：刷新列表、显示成功提示等
      await loadVisibleRangeList()
      console.log('移除成功')
    } catch (error) {
      console.error('Failed to remove visible range:', error)
      throw error
    }
  }

  return {
    // 状态
    visibleRangeList,
    loading,
    searchValue,
    selectedType,
    pagination,

    // 方法
    loadVisibleRangeList,
    handlePageChange,
    handleLimitChange,
    handleSearch,
    handleTypeChange,
    clearFilters,
    handleAdd,
    handleRemove,
  }
})
