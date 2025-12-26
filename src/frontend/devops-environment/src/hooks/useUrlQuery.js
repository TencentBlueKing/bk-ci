import { ref, watch, computed } from 'vue'
import useInstance from './useInstance'

/**
 * URL 查询参数管理 Hook
 * 用于统一管理节点列表/详情页的查询参数，支持刷新页面时恢复状态
 *
 * @param {Object} options - 配置选项
 * @param {Boolean} options.syncToUrl - 是否同步到 URL，默认 true
 * @param {Boolean} options.parseOnMount - 是否在挂载时解析 URL 参数，默认 true
 * @returns {Object} 返回查询参数状态和方法
 */
export default function useUrlQuery (options = {}) {
    const {
        syncToUrl = true,
        parseOnMount = true
    } = options

    const { proxy } = useInstance()

    // 查询参数状态
    const queryParams = ref({
        // 搜索条件
        searchValue: [],
        // 标签搜索
        tagSearchValue: [],
        // 时间范围
        dateTimeRange: [],
        // 节点 HashId（用于详情页展开）
        nodeHashId: '',
        // 分页参数
        page: 1,
        pageSize: 10,
        // 排序参数
        sortType: '',
        collation: '',
        // 其他搜索参数（动态添加）
        ...{}
    })

    /**
     * 将查询参数转换为 URL query 格式
     */
    const convertToUrlQuery = () => {
        const query = {}
        // 处理搜索条件
        // 从 [{ id: 'displayName', values: [{ id: '123' }] }]
        // 转为 { displayName: '123' }
        if (queryParams.value.searchValue?.length) {
            queryParams.value.searchValue.forEach(item => {
                if (item.values && item.values.length > 0) {
                    query[item.id] = item.values[0].id
                } else if (item.id) {
                    query[item.id] = item.id
                }
            })
        }

        // 处理标签搜索
        // 从 [{ id: 'tagKeyId', values: [{ id: 'tagValueId' }] }]
        // 转为 { tagKey_tagKeyId: 'tagValueId1,tagValueId2' }
        if (queryParams.value.tagSearchValue?.length) {
            const tagsData = {}
            queryParams.value.tagSearchValue.forEach(item => {
                if (item.values && item.values.length > 0) {
                    const valueIds = item.values.map(v => v.id).join(',')
                    tagsData[`tagKey_${item.id}`] = valueIds
                }
            })
            Object.assign(query, tagsData)
        }

        // 处理时间范围
        if (queryParams.value.dateTimeRange?.length === 2) {
            query.startTime = queryParams.value.dateTimeRange[0]
            query.endTime = queryParams.value.dateTimeRange[1]
        }

        // 处理节点 HashId
        if (queryParams.value.nodeHashId) {
            query.nodeHashId = queryParams.value.nodeHashId
        }

        // 处理分页
        query.page = queryParams.value.page || 1
        query.pageSize = queryParams.value.pageSize || 10

        // 处理排序
        if (queryParams.value.sortType) {
            query.sortType = queryParams.value.sortType
        }
        if (queryParams.value.collation) {
            query.collation = queryParams.value.collation
        }

        return query
    }

    /**
     * 从 URL query 解析查询参数
     */
    const parseFromUrlQuery = () => {
        const query = proxy.$route.query
        
        // 重置参数
        queryParams.value.searchValue = []
        queryParams.value.tagSearchValue = []
        queryParams.value.dateTimeRange = []
        queryParams.value.nodeHashId = ''
        queryParams.value.page = 1
        queryParams.value.pageSize = 10
        queryParams.value.sortType = ''
        queryParams.value.collation = ''

        // 解析搜索条件
        // 从 { displayName: '123' } 转为 [{ id: 'displayName', values: [{ id: '123', name: '123' }] }]
        const searchKeys = ['keywords', 'nodeIp', 'displayName', 'osName', 'nodeType', 'createdUser', 'nodeStatus', 'agentVersion', 'lastModifiedUser', 'latestBuildPipelineId']
        searchKeys.forEach(key => {
            if (query[key]) {
                queryParams.value.searchValue.push({
                    id: key,
                    name: key, // 这里可以根据需要映射为中文名称
                    values: [{
                        id: query[key],
                        name: query[key]
                    }]
                })
            }
        })

        // 解析标签搜索
        // 从 { tagKey_tag1: 'val1,val2' } 转为 [{ id: 'tag1', values: [{ id: 'val1' }, { id: 'val2' }] }]
        Object.keys(query).forEach(key => {
            if (key.startsWith('tagKey_')) {
                const tagKeyId = key.replace('tagKey_', '')
                const valueIds = query[key].split(',')
                queryParams.value.tagSearchValue.push({
                    id: tagKeyId,
                    name: tagKeyId, // 这里可以根据需要从 nodeTagList 中查找对应的名称
                    values: valueIds.map(id => ({
                        id: id,
                        name: id // 这里可以根据需要从 nodeTagList 中查找对应的名称
                    }))
                })
            }
        })

        // 解析时间范围
        if (query.startTime && query.endTime) {
            queryParams.value.dateTimeRange = [
                Number(query.startTime),
                Number(query.endTime)
            ]
            queryParams.value.startTime = Number(query.startTime)
            queryParams.value.endTime = Number(query.endTime)
        }

        // 解析节点 HashId
        if (query.nodeHashId) {
            queryParams.value.nodeHashId = query.nodeHashId
        }

        // 解析分页
        if (query.page) {
            queryParams.value.page = Number(query.page)
        }
        if (query.pageSize) {
            queryParams.value.pageSize = Number(query.pageSize)
        }

        // 解析排序
        if (query.sortType) {
            queryParams.value.sortType = query.sortType
        }
        if (query.collation) {
            queryParams.value.collation = query.collation
        }

        return queryParams.value
    }

    /**
     * 同步查询参数到 URL
     * @param {Boolean} replace - 是否使用 replace 模式，默认 true
     */
    const syncParamsToUrl = (replace = true) => {
        if (!syncToUrl) return

        const query = convertToUrlQuery()
        const currentRoute = proxy.$route

        // 比较 query 是否有变化
        const isSame = JSON.stringify(currentRoute.query) === JSON.stringify(query)
        if (isSame) return

        const method = replace ? 'replace' : 'push'
        proxy.$router[method]({
            ...currentRoute,
            query
        }).catch(err => {
            console.error('URL sync error:', err)
        })
    }

    /**
     * 更新查询参数
     * @param {Object} params - 要更新的参数
     * @param {Boolean} sync - 是否立即同步到 URL，默认 true
     */
    const updateQueryParams = (params, sync = true) => {
        Object.keys(params).forEach(key => {
            queryParams.value[key] = params[key]
        })

        if (sync) {
            syncParamsToUrl()
        }
    }

    /**
     * 重置所有查询参数
     * @param {Boolean} sync - 是否同步到 URL，默认 true
     */
    const resetQueryParams = (sync = true) => {
        queryParams.value = {
            searchValue: [],
            tagSearchValue: [],
            dateTimeRange: [],
            nodeHashId: '',
            page: 1,
            pageSize: 10,
            sortType: '',
            collation: ''
        }

        if (sync) {
            syncParamsToUrl()
        }
    }

    /**
     * 更新搜索条件
     */
    const updateSearchValue = (value) => {
        updateQueryParams({ searchValue: value })
    }

    /**
     * 更新标签搜索
     */
    const updateTagSearchValue = (value) => {
        updateQueryParams({ tagSearchValue: value })
    }

    /**
     * 更新时间范围
     */
    const updateDateTimeRange = (startTime, endTime) => {
        if (startTime && endTime) {
            updateQueryParams({
                dateTimeRange: [startTime, endTime],
                startTime,
                endTime
            })
        } else {
            updateQueryParams({
                dateTimeRange: [],
                startTime: null,
                endTime: null
            })
        }
    }

    /**
     * 更新节点 HashId
     */
    const updateNodeHashId = (value) => {
        updateQueryParams({ nodeHashId: value })
    }

    /**
     * 更新分页
     */
    const updatePagination = (page, pageSize) => {
        const params = {}
        if (page !== undefined) params.page = page
        if (pageSize !== undefined) params.pageSize = pageSize
        updateQueryParams(params)
    }

    /**
     * 更新排序
     */
    const updateSort = (sortType, collation) => {
        updateQueryParams({ sortType, collation })
    }

    /**
     * 清除特定参数
     */
    const clearParam = (key) => {
        if (Array.isArray(queryParams.value[key])) {
            queryParams.value[key] = []
        } else {
            queryParams.value[key] = ''
        }
        syncParamsToUrl()
    }

    /**
     * 获取请求参数（用于 API 调用）
     * 将查询参数转换为后端 API 需要的格式
     */
    const getRequestParams = () => {
        const params = {}

        // 处理搜索条件
        if (queryParams.value.searchValue?.length) {
            queryParams.value.searchValue.forEach(item => {
                if (item.values && item.values.length > 0) {
                    params[item.id] = item.values[0].id.trim()
                } else if (item.id) {
                    params[item.id] = item.id
                }
            })
        }

        // 处理时间范围
        if (queryParams.value.startTime && queryParams.value.endTime) {
            params.latestBuildTimeStart = queryParams.value.startTime
            params.latestBuildTimeEnd = queryParams.value.endTime
        }

        // 处理分页
        params.page = queryParams.value.page
        params.pageSize = queryParams.value.pageSize

        // 处理排序
        if (queryParams.value.sortType) {
            params.sortType = queryParams.value.sortType
        }
        if (queryParams.value.collation) {
            params.collation = queryParams.value.collation
        }

        return params
    }

    /**
     * 获取标签请求参数
     */
    const getTagRequestParams = () => {
        if (!queryParams.value.tagSearchValue?.length) {
            return []
        }

        return queryParams.value.tagSearchValue.map(item => ({
            tagKeyId: item.id,
            tagValues: item.values.map(value => value.id)
        }))
    }

    // 监听路由变化，解析查询参数
    if (parseOnMount) {
        watch(
            () => proxy.$route.query,
            () => {
                parseFromUrlQuery()
            },
            { immediate: true, deep: true }
        )
    }

    // 计算属性：是否有筛选条件
    const hasFilters = computed(() => {
        return !!(
            queryParams.value.searchValue?.length
            || queryParams.value.tagSearchValue?.length
            || queryParams.value.dateTimeRange?.length
        )
    })

    return {
        // 状态
        queryParams,
        hasFilters,

        // 方法
        updateQueryParams,
        resetQueryParams,
        updateSearchValue,
        updateTagSearchValue,
        updateDateTimeRange,
        updateNodeHashId,
        updatePagination,
        updateSort,
        clearParam,
        syncParamsToUrl,
        parseFromUrlQuery,
        getRequestParams,
        getTagRequestParams
    }
}
