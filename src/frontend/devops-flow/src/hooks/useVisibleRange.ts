import { storeToRefs } from 'pinia'
import { useVisibleRangeStore } from '@/stores/visibleRange'

/**
 * 可用范围数据 Hook
 */
export function useVisibleRange() {
  const store = useVisibleRangeStore()
  const { visibleRangeList, loading, pagination, searchValue } = storeToRefs(store)

  /**
   * 初始化数据
   */
  async function init() {
    try {
      await store.loadVisibleRangeList()
    } catch (error) {
      console.error('Failed to load init:', error)
    }
  }

  return {
    // 原始数据
    visibleRangeList,
    loading,
    pagination,
    searchValue,

    // 操作方法
    init,
    handleSearch: store.handleSearch,
    handlePageChange: store.handlePageChange,
    handleLimitChange: store.handleLimitChange,
    addVisibleRange: store.addVisibleRange,
    handleRemove: store.handleRemove,
  }
}
