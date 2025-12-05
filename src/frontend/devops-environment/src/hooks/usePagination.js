
import { reactive, toRefs } from 'vue'

/**
 * bk-table 分页逻辑 Hook
 * @param {Object} options - 配置选项
 * @param {Number} options.limit - 每页显示数量，默认 20
 * @param {Number} options.current - 当前页码，默认 1
 * @param {Array} options.limitList - 每页数量选项列表，默认 [10, 20, 50, 100]
 * @returns {Object} 返回分页状态和方法
 */
export default function usePagination (options = {}) {
    const {
        limit = 20,
        current = 1,
        limitList = [10, 20, 50, 100]
    } = options

    // 分页状态
    const state = reactive({
        pagination: {
            current,
            count: 20,
            limit,
            limitList
        }
    })

    /**
     * 处理页码变化
     * @param {Number} page - 新的页码
     */
    const pageChange = (page) => {
        state.pagination.current = page
    }

    /**
     * 处理每页数量变化
     * @param {Number} limit - 新的每页数量
     */
    const pageLimitChange = (limit) => {
        state.pagination.limit = limit
        state.pagination.current = 1 // 重置到第一页
    }

    /**
     * 重置分页到第一页
     */
    const resetPage = () => {
        state.pagination.current = 1
    }

    /**
     * 重置所有状态
     */
    const resetPagination = () => {
        state.pagination.current = current
        state.pagination.count = 0
        state.pagination.limit = limit
    }

    /**
     * 更新总数
     * @param {Number} count - 新的总数
     */
    const updateCount = (count) => {
        state.pagination.count = count
    }

    /**
     * 更新分页信息
     * @param {Object} paginationData - 分页数据
     * @param {Number} paginationData.count - 总数
     * @param {Number} paginationData.page - 当前页
     * @param {Number} paginationData.pageSize - 每页数量
     */
    const updatePagination = (paginationData) => {
        if (paginationData.count !== undefined) {
            state.pagination.count = paginationData.count
        }
        if (paginationData.page !== undefined) {
            state.pagination.current = paginationData.page
        }
        if (paginationData.pageSize !== undefined) {
            state.pagination.limit = paginationData.pageSize
        }
    }

    return {
        ...toRefs(state),
        resetPage,
        resetPagination,
        pageChange,
        pageLimitChange,
        updateCount,
        updatePagination
    }
}
