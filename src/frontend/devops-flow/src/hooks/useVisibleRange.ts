import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useVisibleRangeStore } from '@/stores/visibleRange'
import type { VisibleRangeRecord } from '@/api/visibleRange'

/**
 * 可用范围数据 Hook
 */
export function useVisibleRange() {
  const store = useVisibleRangeStore()
  const { visibleRangeList, loading, pagination, searchValue, selectedType } = storeToRefs(store)

  /**
   * 格式化列表数据 - 将 API 数据转换为表格展示数据
   */
  const formattedList = computed(() => {
    return visibleRangeList.value.map((item: VisibleRangeRecord) => ({
      id: item.scopeId,
      userName: item.scopeName,
      userType: formatUserType(item.type),
      groupName: '-', // API 未返回，使用默认值
      updatedBy: '-', // API 未返回，使用默认值
      updatedAt: '-', // API 未返回，使用默认值
      rawType: item.type, // 保留原始类型用于筛选
    }))
  })

  /**
   * 类型筛选后的数据
   */
  const filteredList = computed(() => {
    let result = formattedList.value

    // 按类型筛选
    if (selectedType.value) {
      result = result.filter((item) => item.rawType === selectedType.value)
    }

    // 按搜索词筛选
    if (searchValue.value) {
      result = result.filter((item) =>
        item.userName.toLowerCase().includes(searchValue.value.toLowerCase()),
      )
    }

    return result
  })

  /**
   * 格式化用户类型
   */
  function formatUserType(type: string): string {
    const typeMap: Record<string, string> = {
      USER: '内部用户',
      ORG: '组织',
      GROUP: '用户组',
    }
    return typeMap[type] || type
  }

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
    selectedType,

    // 加工后的数据
    formattedList,
    filteredList,

    // 操作方法
    init,
    handleSearch: store.handleSearch,
    handleTypeChange: store.handleTypeChange,
    handlePageChange: store.handlePageChange,
    handleLimitChange: store.handleLimitChange,
    clearFilters: store.clearFilters,
    handleAdd: store.handleAdd,
    handleRemove: store.handleRemove,
  }
}
