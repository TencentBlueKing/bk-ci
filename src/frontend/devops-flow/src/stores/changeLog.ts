import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { useRoute } from 'vue-router'
import {
  getChangeLogOperators,
  getChangeLogList,
  type ChangeLogRecord,
  type ChangeLogQueryParams,
} from '@/api/changeLog'

/**
 * 变更日志状态管理
 */
export const useChangeLogStore = defineStore('changeLog', () => {
  const route = useRoute()
  const flowId = computed(() => route.params.flowId as string)
  const projectId = computed(() => route.params.projectId as string)

  const operatorList = ref<string[]>([])
  const changeLogList = ref<ChangeLogRecord[]>([])
  const loading = ref(false)
  const searchValue = ref('')
  const pagination = ref({
    current: 1,
    count: 0,
    limit: 20,
  })

  /**
   * 加载操作人列表
   * @param projectId 项目ID
   * @param flowId 创作流ID
   */
  async function loadOperators() {
    try {
      const operators = await getChangeLogOperators(projectId.value, flowId.value)
      operatorList.value = operators
    } catch (error) {
      console.error('Failed to load operators:', error)
    }
  }

  /**
   * 加载变更日志列表
   * @param projectId 项目ID
   * @param flowId 创作流ID
   * @param params 查询参数
   */
  async function loadChangeLogList(page?: number, limit?: number) {
    loading.value = true
    try {
      const params: ChangeLogQueryParams = {
        projectId: projectId.value,
        flowId: flowId.value,
        creator: searchValue.value,
        page: page ?? pagination.value.current,
        pageSize: limit ?? pagination.value.limit,
      }

      const res = await getChangeLogList(params)
      changeLogList.value = res.records
      pagination.value = {
        current: res.page,
        limit: res.pageSize,
        count: res.count,
      }
    } catch (error) {
      console.error('Failed to load change log list:', error)
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
    await loadChangeLogList(page, pagination.value.limit)
  }

  /**
   * 切换每页大小
   * @param limit 每页大小
   */
  async function handleLimitChange(limit: number) {
    pagination.value.current = 1
    pagination.value.limit = limit

    await loadChangeLogList(pagination.value.current, limit)
  }

  function handleSelectChange(creator: string) {
    searchValue.value = creator
    loadChangeLogList()
  }

  return {
    // 状态
    operatorList,
    changeLogList,
    loading,
    searchValue,
    pagination,

    // 方法
    loadOperators,
    loadChangeLogList,
    handlePageChange,
    handleLimitChange,
    handleSelectChange,
  }
})
