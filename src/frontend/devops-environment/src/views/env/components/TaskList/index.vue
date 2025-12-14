<template>
    <div class="task-list-container">
        <!-- 顶部统计和搜索 -->
        <div class="task-header">
            <div class="task-stats">
                <span class="stats-text">{{ statsText }}</span>
            </div>
            <div class="task-search">
                <bk-date-picker
                    v-model="dateRange"
                    class="date-picker"
                    type="datetimerange"
                    :placeholder="$t('environment.selectExecutionTime')"
                    :clearable="true"
                    @change="handleDateChange"
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
                            <span class="task-name">{{ task.name }}</span>
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
                                width="150"
                            >
                                <template #default="{ row }">
                                    <span class="build-number">#{{ row.buildNum }}</span>
                                </template>
                            </bk-table-column>
                            <bk-table-column
                                :label="$t('environment.buildStatus')"
                                prop="status"
                                width="200"
                            >
                                <template #default="{ row }">
                                    <span
                                        class="status-badge"
                                        :class="`status-${row.status}`"
                                    >
                                        <i
                                            class="bk-icon"
                                            :class="getStatusIcon(row.status)"
                                        />
                                        {{ getStatusText(row.status) }}
                                    </span>
                                </template>
                            </bk-table-column>
                            <bk-table-column
                                :label="$t('environment.duration')"
                                prop="duration"
                                width="250"
                            >
                                <template #default="{ row }">
                                    {{ formatDuration(row.duration) }}
                                </template>
                            </bk-table-column>
                            <bk-table-column
                                :label="$t('environment.startTime')"
                                prop="startTime"
                                width="250"
                            >
                                <template #default="{ row }">
                                    {{ formatTime(row.startTime) }}
                                </template>
                            </bk-table-column>
                            <bk-table-column
                                :label="$t('environment.endTime')"
                                prop="endTime"
                                width="250"
                            >
                                <template #default="{ row }">
                                    {{ formatTime(row.endTime) }}
                                </template>
                            </bk-table-column>
                            <bk-table-column
                                :label="$t('environment.trigger')"
                                prop="triggerUser"
                            >
                                <template #default="{ row }">
                                    {{ row.triggerUser || '--' }}
                                </template>
                            </bk-table-column>
                        </bk-table>
                    </div>
                </div>
            </template>

            <!-- 空状态 -->
            <bk-exception
                v-else-if="!isLoading"
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

    export default {
        name: 'TaskList',
        components: {
            SearchSelect
        },
        props: {
            // 任务类型：'build' 或 'deploy'
            taskType: {
                type: String,
                required: true,
                validator: (value) => ['build', 'deploy'].includes(value)
            },
            // 获取任务列表的方法
            fetchTaskList: {
                type: Function,
                required: true
            },
            // 获取任务详情的方法
            fetchTaskDetail: {
                type: Function,
                required: true
            }
        },
        setup (props) {
            const { proxy } = useInstance()
            const {
                envHashId
            } = useEnvDetail()
            const {
                pagination,
                resetPagination,
                updateCount
            } = usePagination()
            
            const isLoading = ref(false)
            const isLoadingMore = ref(false)
            const taskList = ref([])
            const dateRange = ref([])
            const searchKeyword = ref('')
            const searchSelectValue = ref([])
            const taskRefs = new Map()
            const taskListRef = ref(null)
            
            // SearchSelect 配置数据
            const searchSelectData = computed(() => {
                const jobOrStep = props.taskType === 'build' ? 'Job' : 'Step'
                return [
                    {
                        name: jobOrStep,
                        id: 'task',
                        default: true
                    },
                    {
                        name: proxy.$t('environment.pipeline'),
                        id: 'pipeline'
                    },
                    {
                        name: proxy.$t('environment.operateUser'),
                        id: 'operator'
                    }
                ]
            })
            
            // 是否还有更多数据
            const hasMore = computed(() => {
                return taskList.value.length < pagination.value.count
            })
            
            // 设置 task 元素引用
            const setTaskRef = (el, task) => {
                if (el) {
                    taskRefs.set(task.id, el)
                }
            }
            
            // 计算属性
            const statsText = computed(() => {
                const total = taskList.value.length
                const countText = props.taskType === 'build'
                    ? 'Job'
                    : 'step'
                const usageText = props.taskType === 'build'
                    ? '系统共该环境用途环境'
                    : '系统此环境使用场所环境'
                return `共 ${total} 个 ${countText}，${total} ${usageText}`
            })
            
            const searchPlaceholder = computed(() => {
                return searchSelectData.value.map(item => item.name).join(' / ')
            })
            
            const executeLabel = computed(() => {
                return props.taskType === 'build'
                    ? '执行次数'
                    : '执行次数'
            })
            
            // 获取任务信息项
            const getTaskInfoItems = (task) => {
                return [
                    {
                        label: executeLabel.value,
                        value: task.executeCount || 0
                    },
                    {
                        label: proxy.$t('environment.avgDuration'),
                        value: task.avgDurationText || '--'
                    },
                    {
                        label: proxy.$t('environment.envInfo.creationTime'),
                        value: task.createTimeText || '--'
                    },
                    {
                        label: proxy.$t('environment.nodeInfo.lastRunAs'),
                        value: task.lastExecuteTimeText || '--'
                    }
                ]
            }
            
            // 格式化时长
            const formatDuration = (seconds) => {
                if (!seconds && seconds !== 0) return '--'
                
                const minutes = Math.floor(seconds / 60)
                const secs = seconds % 60
                
                if (minutes > 0) {
                    return `${minutes} 分 ${secs} 秒`
                }
                return `${secs} 秒`
            }
            
            // 格式化时间
            const formatTime = (timestamp) => {
                if (!timestamp) return '--'
                return convertTime(timestamp * 1000)
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
            
            // 获取状态文本
            const getStatusText = (status) => {
                const textMap = {
                    success: proxy.$t('environment.success'),
                    running: proxy.$t('environment.running'),
                    failed: proxy.$t('environment.failure')
                }
                return textMap[status] || status
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
                
                // 滚动到合适位置，确保 header 完全可见
                if (task.isExpanded) {
                    await nextTick()
                    const taskElement = taskRefs.get(task.id)
                    if (taskElement) {
                        // 获取滚动容器和任务元素的位置
                        const container = taskElement.closest('.task-list')
                        if (container) {
                            const containerRect = container.getBoundingClientRect()
                            const taskRect = taskElement.getBoundingClientRect()
                            
                            // 计算需要滚动的距离，让任务项显示在容器顶部并留出一些边距
                            const scrollTop = container.scrollTop + (taskRect.top - containerRect.top) - 20
                            
                            container.scrollTo({
                                top: scrollTop,
                                behavior: 'smooth'
                            })
                        }
                    }
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
                    const res = await props.fetchTaskDetail(task.id, params)
                    task.records = res.records || []
                    task.pagination.count = res.count || 0
                } catch (err) {
                    console.error('加载任务详情失败:', err)
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
                        isLoading.value = true
                        pagination.value.current = 1
                    }
                    
                    // 构建搜索参数
                    const searchParams = {}
                    searchSelectValue.value.forEach(item => {
                        if (item.id === 'task') {
                            searchParams.taskName = item.values?.[0]?.name || item.values?.[0]?.id
                        } else if (item.id === 'pipeline') {
                            searchParams.pipelineName = item.values?.[0]?.name || item.values?.[0]?.id
                        } else if (item.id === 'operator') {
                            searchParams.operator = item.values?.[0]?.name || item.values?.[0]?.id
                        }
                    })
                    
                    const params = {
                        ...searchParams,
                        startTime: dateRange.value?.[0],
                        endTime: dateRange.value?.[1],
                        page: pagination.value.current,
                        pageSize: pagination.value.limit
                    }
                    
                    const res = await props.fetchTaskList(params)
                    const newTasks = (res.records || res || []).map(item => ({
                        ...item,
                        // 预处理格式化数据
                        avgDurationText: formatDuration(item.avgDuration),
                        createTimeText: formatTime(item.createTime),
                        lastExecuteTimeText: formatTime(item.lastExecuteTime),
                        // 状态数据
                        isExpanded: false,
                        isLoadingDetail: false,
                        records: null,
                        pagination: {
                            current: 1,
                            count: 0,
                            limit: 10
                        }
                    }))
                    
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
                    updateCount(res.count || newTasks.length)
                } catch (err) {
                    console.error('加载任务列表失败:', err)
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
            
            // 搜索（保留兼容性）
            const handleSearch = () => {
                loadTaskList()
            }
            
            // 日期变化
            const handleDateChange = () => {
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
                isLoading,
                isLoadingMore,
                taskList,
                dateRange,
                searchSelectValue,
                searchSelectData,
                taskListRef,
                pagination,
                statsText,
                searchPlaceholder,
                executeLabel,
                hasMore,
                setTaskRef,
                getTaskInfoItems,
                formatDuration,
                formatTime,
                getStatusIcon,
                getStatusText,
                toggleExpand,
                handleSearchChange,
                handleSearch,
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
                
                .build-number {
                    color: #3A84FF;
                    cursor: pointer;
                    
                    &:hover {
                        text-decoration: underline;
                    }
                }
                
                .status-badge {
                    display: inline-flex;
                    align-items: center;
                    gap: 4px;
                    
                    &.status-success {
                        .icon-success-shape {
                            width: 8px;
                            height: 8px;
                            border-radius: 50%;
                            background: #E5F6EA;
                            border: 1px solid #3FC06D;
                        }
                    }
                    
                    &.status-running {
                        .bk-icon {
                            color: #3A84FF;
                            animation: rotating 1s linear infinite;
                        }
                    }
                    
                    &.status-failed {
                        .icon-failed-shape {
                            width: 8px;
                            height: 8px;
                            border-radius: 50%;
                            background: #FFE6E6;
                            border: 1px solid #EA3636;
                        }
                    }
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
