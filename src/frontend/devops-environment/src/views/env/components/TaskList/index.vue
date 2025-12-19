<template>
    <div class="task-list-container">
        <!-- 顶部统计和搜索 -->
        <div class="task-header">
            <div class="task-stats">
                <span
                    class="stats-text"
                    v-if="pipelineCount"
                >
                    <i18n path="environment.totalJobTasks">
                        <span class="count-number">{{ jobCount }}</span>
                        <span class="count-number">{{ pipelineCount }}</span>
                    </i18n>
                </span>
            </div>
            <div class="task-search">
                <bk-date-picker
                    v-model="dateRange"
                    class="date-picker"
                    type="datetimerange"
                    :placeholder="$t('environment.selectExecutionTime')"
                    :clearable="true"
                    @clear="handleDateClear"
                    @pick-success="handleDateChange"
                />
                <SearchSelect
                    v-model="searchSelectValue"
                    class="search-input"
                    :data="searchSelectData"
                    :placeholder="searchPlaceholder"
                    @change="handleSearchChange"
                />
            </div>
        </div>

        <!-- 任务列表 -->
        <div
            ref="taskListRef"
            class="task-list"
            v-bkloading="{ isLoading }"
            @scroll="handleScroll"
        >
            <template v-if="taskList.length > 0">
                <div
                    v-for="(task, index) in taskList"
                    :key="task.id || index"
                    :ref="el => setTaskRef(el, task)"
                    class="task-item"
                    :class="{ expanded: task.isExpanded }"
                >
                    <!-- 任务头部 -->
                    <div
                        class="task-item-header"
                        @click="toggleExpand(task)"
                    >
                        <i
                            class="bk-icon expand-icon"
                            :class="task.isExpanded ? 'icon-angle-down' : 'icon-angle-right'"
                        />
                        <div class="task-title">
                            <span class="task-name">{{ task.jobName }}</span>
                            <span class="task-pipeline-name">
                                <i class="bk-icon icon-pipeline" />
                                <span
                                    class="pipeline-text"
                                    v-bk-overflow-tips
                                >
                                    {{ task.pipelineName }}
                                </span>
                            </span>
                        </div>
                        <div class="task-info">
                            <div
                                v-for="item in getTaskInfoItems(task)"
                                :key="item.label"
                                class="info-item"
                            >
                                <span class="info-label">{{ item.label }}</span>
                                <span class="info-value">{{ item.value }}</span>
                            </div>
                        </div>
                    </div>

                    <!-- 任务详情表格 -->
                    <div
                        v-if="task.isExpanded"
                        class="task-detail"
                    >
                        <bk-table
                            v-bkloading="{ isLoading: task.isLoadingDetail }"
                            :data="task.records || []"
                            :outer-border="false"
                            :header-border="false"
                            :pagination="task.pagination"
                            @page-change="handlePageChange(task, $event)"
                            @page-limit-change="handlePageSizeChange(task, $event)"
                        >
                            <bk-table-column
                                :label="$t('environment.buildNumber')"
                                prop="buildNum"
                                width="100"
                            >
                                <template #default="{ row }">
                                    <span>#{{ row.buildNum }}</span>
                                </template>
                            </bk-table-column>
                            <bk-table-column
                                :label="$t('environment.buildStatus')"
                                prop="status"
                            >
                                <template #default="{ row }">
                                    <span class="status-text-container">
                                        <pipeline-status-icon
                                            :status="row.status"
                                        />
                                        {{ row.statusText }}
                                    </span>
                                </template>
                            </bk-table-column>
                            <bk-table-column
                                :label="$t('environment.duration')"
                                prop="duration"
                            />
                            <bk-table-column
                                :label="$t('environment.startTime')"
                                prop="startTime"
                            />
                            <bk-table-column
                                :label="$t('environment.endTime')"
                                prop="endTime"
                            />
                            <bk-table-column
                                :label="$t('environment.trigger')"
                                prop="creator"
                            />
                        </bk-table>
                    </div>
                </div>
            </template>

            <!-- 空状态 -->
            <bk-exception
                v-else-if="!isLoading"
                class="task-empty"
                type="empty"
                scene="part"
            >
                {{ $t('environment.noData') }}
            </bk-exception>
            
            <!-- 加载更多提示 -->
            <div
                v-if="isLoadingMore"
                class="loading-more"
            >
                <i class="bk-icon icon-circle-2-1 rotating-icon" />
                <span>{{ $t('environment.loading') }}</span>
            </div>
            
            <!-- 没有更多数据提示 -->
            <div
                v-if="!isLoading && !isLoadingMore && taskList.length > 0 && !hasMore"
                class="no-more"
            >
                {{ $t('environment.noMore') }}
            </div>
        </div>
    </div>
</template>

<script>
    import { ref, computed, watch, nextTick } from 'vue'
    import { convertTime } from '@/utils/util'
    import useInstance from '@/hooks/useInstance'
    import useEnvDetail from '@/hooks/useEnvDetail'
    import usePagination from '@/hooks/usePagination'
    import SearchSelect from '@blueking/search-select'
    import '@blueking/search-select/dist/styles/index.css'
    import PipelineStatusIcon from './PipelineStatusIcon'

    export default {
        name: 'TaskList',
        components: {
            SearchSelect,
            PipelineStatusIcon
        },
        setup (props) {
            const { proxy } = useInstance()
            const {
                envHashId,
                fetchJobTaskList,
                fetchPipelineBuildHistory,
                searchJobByName,
                searchPipelineByName,
                searchByCreator
            } = useEnvDetail()
            const {
                pagination,
                resetPagination,
                updateCount
            } = usePagination()
            const pipelineCount = ref(0)
            const jobCount = ref(0)
            const isLoading = ref(false)
            const isLoadingMore = ref(false)
            const taskList = ref([])
            const dateRange = ref([])
            const searchSelectValue = ref([])
            const taskRefs = new Map()
            const taskListRef = ref(null)
            
            const searchSelectData = computed(() => {
                return [
                    {
                        name: 'Job',
                        id: 'jobId',
                        default: true,
                        remoteMethod: async (keyword) => {
                            try {
                                const res = await searchJobByName(keyword)
                                return res.map(item => ({
                                    id: item.jobId,
                                    name: item.jobName
                                }))
                            } catch (e) {
                                return []
                            }
                        }
                    },
                    {
                        name: proxy.$t('environment.pipeline'),
                        id: 'pipelineId',
                        remoteMethod: async (keyword) => {
                            try {
                                const res = await searchPipelineByName(keyword)
                                return res.map(item => ({
                                    id: item.pipelineId,
                                    name: item.pipelineName
                                }))
                            } catch (e) {
                                return []
                            }
                        }
                    },
                    {
                        name: proxy.$t('environment.operateUser'),
                        id: 'creator',
                        remoteMethod: async (keyword) => {
                            try {
                                const res = await searchByCreator(keyword)
                                return res.map(item => ({
                                    id: item,
                                    name: item
                                }))
                            } catch (e) {
                                return []
                            }
                        }
                    }
                ]
            })
            // 是否还有更多数据
            const hasMore = computed(() => {
                return taskList.value.length < pagination.value.count
            })

            const filterQuery = computed(() => {
                return searchSelectValue.value.reduce((query, item) => {
                    query[item.id] = item.values.map(value => value.id).join(',')
                    return query
                }, {})
            })

            // 时间范围参数（秒级时间戳）
            const timeRangeParams = computed(() => {
                const [startDate, endDate] = dateRange.value || []
                return {
                    ...(startDate ? { startTime: Math.floor(new Date(startDate).getTime() / 1000) } : {}),
                    ...(endDate ? { endTime: Math.floor(new Date(endDate).getTime() / 1000) } : {})
                }
            })
            
            // 设置 task 元素引用
            const setTaskRef = (el, task) => {
                if (el) {
                    taskRefs.set(task.pipelineId, el)
                }
            }
            
            const searchPlaceholder = computed(() => {
                return searchSelectData.value.map(item => item.name).join(' / ')
            })
            
            // 获取任务信息项
            const getTaskInfoItems = (task) => {
                return [
                    {
                        label:  proxy.$t('environment.executionCount'),
                        value: task.buildCount || 0
                    },
                    {
                        label: proxy.$t('environment.avgDuration'),
                        value: task.avgTimeInterval || '--'
                    },
                    {
                        label: proxy.$t('environment.nodeInfo.lastRunAs'),
                        value: task.lastBuildTime || '--'
                    }
                ]
            }
            
            // 将秒数转换为中文时间格式
            const formatSeconds = (seconds) => {
                if (seconds === null || seconds === undefined || seconds < 0) return '--'
                const days = Math.floor(seconds / 86400)
                const hours = Math.floor((seconds % 86400) / 3600)
                const minutes = Math.floor((seconds % 3600) / 60)
                const secs = seconds % 60
                
                const parts = []
                if (days > 0) {
                    parts.push(`${days}${proxy.$t('environment.day')}`)
                }
                if (hours > 0) {
                    parts.push(`${hours}${proxy.$t('environment.hour')}`)
                }
                if (minutes > 0) {
                    parts.push(`${minutes}${proxy.$t('environment.minute')}`)
                }
                if (secs > 0 || parts.length === 0) {
                    parts.push(`${secs}${proxy.$t('environment.second')}`)
                }
                
                return parts.join('')
            }
            
            // 计算执行耗时
            const calculateDuration = (startTime, endTime) => {
                if (!startTime || !endTime) return '--'
                
                const startTimestamp = new Date(startTime).getTime()
                const endTimestamp = new Date(endTime).getTime()
                const seconds = Math.floor((endTimestamp - startTimestamp) / 1000)
                
                return formatSeconds(seconds)
            }
            
            // 格式化时间
            const formatTime = (time) => {
                if (!time) return '--'
                if (typeof time === 'string') {
                    return convertTime(new Date(time).getTime())
                }
                // 如果是秒级时间戳
                return convertTime(time * 1000)
            }
            
            // 获取状态图标
            const getStatusIcon = (status) => {
                const iconMap = {
                    success: 'icon-success-shape',
                    running: 'icon-circle-2-1',
                    failed: 'icon-failed-shape'
                }
                return iconMap[status] || 'icon-circle'
            }
            
            // 展开/收起任务
            const toggleExpand = async (task) => {
                const wasExpanded = task.isExpanded
                
                // 收起所有其他任务
                taskList.value.forEach(item => {
                    item.isExpanded = false
                })
                
                // 切换当前任务状态
                task.isExpanded = !wasExpanded
                
                // 如果展开且没有加载过详情，则加载
                if (task.isExpanded && !task.records) {
                    await loadTaskDetail(task)
                }
            }
            
            // 加载任务详情
            const loadTaskDetail = async (task) => {
                try {
                    task.isLoadingDetail = true
                    const params = {
                        page: task.pagination.current,
                        pageSize: task.pagination.limit
                    }
                    const res = await fetchPipelineBuildHistory({
                        pipelineId: task.pipelineId,
                        containerId: task.lastContainerId,
                        params
                    })
                    task.records = res.records.map(i => {
                        return {
                            ...i,
                            statusText: proxy.$t(`environment.statusMap.${i.status}`) || '',
                            startTime: formatTime(i.startTime),
                            endTime: formatTime(i.endTime),
                            duration: calculateDuration(i.startTime, i.endTime)
                        }
                    }) || []
                    task.pagination.count = res.count || 0
                } catch (err) {
                    proxy.$bkMessage({
                        theme: 'error',
                        message: err.message || err
                    })
                    throw err
                } finally {
                    task.isLoadingDetail = false
                }
            }
            
            // 加载任务列表
            const loadTaskList = async (isLoadMore = false) => {
                try {
                    if (isLoadMore) {
                        isLoadingMore.value = true
                    } else {
                        pagination.value.current = 1
                    }
                    const params = {
                        ...filterQuery.value,
                        ...timeRangeParams.value,
                        envId: envHashId.value,
                        page: pagination.value.current,
                        pageSize: pagination.value.limit
                    }
                    
                    const res = await fetchJobTaskList(params)
                    pipelineCount.value = res.pipelineCount
                    jobCount.value = res.jobCount
                    const newTasks = (res.result.records || []).map(task => {
                        return {
                            ...task,
                            lastBuildTime: formatTime(task.lastBuildTime),
                            avgTimeInterval: formatSeconds(task.avgTimeInterval),
                      
                            // 流水线列表状态数据
                            isExpanded: false,
                            isLoadingDetail: false,
                            records: null,
                            pagination: {
                                current: 1,
                                count: 0,
                                limit: 10
                            }
                        }
                    })
                    
                    if (isLoadMore) {
                        // 加载更多，追加数据
                        taskList.value = [...taskList.value, ...newTasks]
                    } else {
                        // 首次加载或刷新，替换数据
                        taskList.value = newTasks
                        
                        // 首次加载时，自动展开第一个任务
                        if (newTasks.length > 0) {
                            await nextTick()
                            const firstTask = taskList.value[0]
                            firstTask.isExpanded = true
                            await loadTaskDetail(firstTask)
                        }
                    }
                    
                    // 更新总数
                    updateCount(res.result.count || newTasks.length)
                } catch (err) {
                    proxy.$bkMessage({
                        theme: 'error',
                        message: err.message || err
                    })
                } finally {
                    isLoading.value = false
                    isLoadingMore.value = false
                }
            }
            
            // 加载更多
            const loadMore = async () => {
                if (isLoadingMore.value || !hasMore.value) {
                    return
                }
                
                pagination.value.current += 1
                await loadTaskList(true)
            }
            
            // 滚动事件处理
            const handleScroll = (event) => {
                const target = event.target
                const scrollTop = target.scrollTop
                const scrollHeight = target.scrollHeight
                const clientHeight = target.clientHeight
                
                // 距离底部 100px 时触发加载更多
                if (scrollHeight - scrollTop - clientHeight < 100) {
                    loadMore()
                }
            }
            
            // 搜索
            const handleSearchChange = () => {
                loadTaskList()
            }
            
            // 日期变化
            const handleDateChange = () => {
                loadTaskList()
            }

            const handleDateClear = () => {
                dateRange.value = []
                loadTaskList()
            }
            
            // 分页变化
            const handlePageChange = (task, page) => {
                task.pagination.current = page
                loadTaskDetail(task)
            }
            
            // 每页数量变化
            const handlePageSizeChange = (task, limit) => {
                task.pagination.limit = limit
                task.pagination.current = 1
                loadTaskDetail(task)
            }
            
            // 初始化加载
            loadTaskList()
            
            // 监听 envHashId 变化
            watch(() => envHashId.value, (newVal, oldVal) => {
                if (newVal && newVal !== oldVal) {
                    // 重置分页
                    resetPagination()
                    // 清空任务列表
                    taskList.value = []
                    // 清空搜索条件
                    dateRange.value = []
                    searchSelectValue.value = []
                    // 重新加载数据
                    loadTaskList()
                }
            })
            
            return {
                // data
                isLoading,
                isLoadingMore,
                dateRange,
                searchSelectValue,
                searchSelectData,
                taskListRef,
                pagination,
                searchPlaceholder,
                hasMore,
                taskList,
                jobCount,
                pipelineCount,

                // function
                setTaskRef,
                getTaskInfoItems,
                formatTime,
                getStatusIcon,
                toggleExpand,
                handleSearchChange,
                handleDateClear,
                handleDateChange,
                handlePageChange,
                handlePageSizeChange,
                handleScroll
            }
        }
    }
</script>

<style lang="scss" scoped>
.task-list-container {
    height: 100%;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    .date-picker {
        width: 300px;
        margin-right: 8px;
    }
    .search-input {
        width: 480px;
        z-index: 1000;
        background: white;
        ::placeholder {
            color: #c4c6cc;
        }
    }
    
    .task-header {
        flex-shrink: 0;
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 0 24px;
        
        .task-stats {
            .stats-text {
                font-size: 12px;
                color: #63656E;
            }
            .count-number {
                font-weight: 700;
                color: #3c88ff;
            }
        }
        
        .task-search {
            display: flex;
            align-items: center;
        }
    }
    
    .task-list {
        flex: 1;
        overflow-y: auto;
        padding: 16px 24px;
        min-height: 0;
        
        .task-item {
            background: #FFFFFF;
            border-radius: 2px;
            margin-bottom: 12px;
            border: 1px solid #DCDEE5;
            transition: background-color 0.3s;
            
            &.expanded {
                background: #F0F5FF;
            }
            
            &:last-child {
                margin-bottom: 0;
            }
            
            .task-item-header {
                display: flex;
                align-items: center;
                padding: 16px 90px 16px 16px;
                height: 64px;
                cursor: pointer;
                transition: background-color 0.2s;
                position: sticky;
                top: -16px;
                z-index: 10;
                background: inherit;
                
                &:hover {
                    background: #F5F7FA;
                }
                
                .expand-icon {
                    font-size: 26px;
                    color: #979BA5;
                    margin-right: 8px;
                    transition: transform 0.2s;
                }
                
                .task-title {
                    flex: 1;
                    min-width: 0;
                    
                    .task-name {
                        font-size: 14px;
                        color: #313238;
                        font-weight: 500;
                        display: block;
                        margin-bottom: 8px;
                    }
                    
                    .task-pipeline-name {
                        font-size: 12px;
                        color: #979BA5;
                        display: flex;
                        align-items: center;
                        min-width: 0;
                        
                        .bk-icon {
                            margin-right: 4px;
                            flex-shrink: 0;
                        }
                        
                        .pipeline-text {
                            overflow: hidden;
                            white-space: nowrap;
                            text-overflow: ellipsis;
                        }
                    }
                }
                
                .task-info {
                    display: flex;
                    align-items: center;
                    gap: 80px;
                    flex-shrink: 0;
                    
                    .info-item {
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        font-size: 12px;
                        width: 120px;
                        flex-shrink: 0;
                        
                        .info-label {
                            color: #979BA5;
                            margin-bottom: 8px;
                            white-space: nowrap;
                        }
                        
                        .info-value {
                            color: #313238;
                            white-space: nowrap;
                            overflow: hidden;
                            text-overflow: ellipsis;
                            width: 100%;
                            text-align: center;
                        }
                    }
                }
            }
            
            .task-detail {
                padding: 16px 16px 0;
                background: #fff;
                .status-text-container {
                    display: flex;
                    align-items: center;
                    gap: 2px;
                }
            }
        }
        
        .loading-more,
        .no-more {
            text-align: center;
            padding: 16px;
            font-size: 12px;
            color: #979BA5;
        }
        
        .loading-more {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
            
            .rotating-icon {
                animation: rotating 1s linear infinite;
            }
        }
    }
    .task-empty {
        margin-top: 4%;
    }
}

@keyframes rotating {
    from {
        transform: rotate(0deg);
    }
    to {
        transform: rotate(360deg);
    }
}
</style>
