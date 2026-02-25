import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useChangeLogStore } from '@/stores/changeLog'

/**
 * 变更日志数据 Hook
 */
export function useChangeLog() {
  const store = useChangeLogStore()
  const { operatorList, changeLogList, loading, pagination, searchValue } = storeToRefs(store)

  const operatorOptions = computed(() =>
    operatorList.value.map((item) => ({
      name: item,
      value: item,
    })),
  )

  async function init() {
    try {
      await Promise.all([store.loadOperators(), store.loadChangeLogList()])
    } catch (error) {
      console.error('Failed to load init:', error)
    }
  }

  return {
    // 原始数据
    changeLogList,
    loading,
    pagination,
    searchValue,

    // 二次加工的数据
    operatorOptions,

    // 操作方法
    init,
    handleSelectChange: store.handleSelectChange,
    handlePageChange: store.handlePageChange,
    handleLimitChange: store.handleLimitChange,
  }
}
