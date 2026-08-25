<template>
    <div class="task-list-container">
        <!-- 顶部视图切换、统计和搜索 -->
        <div class="task-header">
            <div class="task-header-left">
                <bk-radio-group
                    v-model="currentView"
                    class="view-switcher"
                    @change="handleViewChange"
                >
                    <template v-if="isCreateResType">
                        <bk-radio-button value="PIPELINE">{{ $t('environment.viewCreationPipeline') }}</bk-radio-button>
                        <bk-radio-button value="BUILD">{{ $t('environment.viewCreationTask') }}</bk-radio-button>
                    </template>
                    <template v-else>
                        <bk-radio-button value="PIPELINE">{{ $t('environment.viewPipeline') }}</bk-radio-button>
                        <bk-radio-button value="JOB">{{ $t('environment.viewJob') }}</bk-radio-button>
                        <bk-radio-button value="BUILD">{{ $t('environment.viewBuild') }}</bk-radio-button>
                    </template>
                </bk-radio-group>
                <span class="stats-text">
                    <i18n :path="statsConfig.path">
                        <span
                            v-for="(value, idx) in statsConfig.values"
                            :key="idx"
                            class="count-number"
                        >{{ value }}</span>
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
                    :key="task.taskKey || index"
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

                        <!-- 流水线视图头部 -->
                        <div
                            v-if="currentView === 'PIPELINE'"
                            class="task-title pipeline-view-title"
                        >
                            <Logo
                                v-if="isCreateResType"
                                name="color-logo-creative"
                                :size="30"
                                class="title-icon"
                            />
                            <i
                                v-else
                                class="bk-icon icon-pipeline title-icon"
                            />
                            <div class="task-title-content">
                                <span class="task-name">
                                    <a
                                        class="pipeline-title text-link"
                                        v-bk-overflow-tips
                                        :href="getPipelineHistoryUrl(task)"
                                        target="_blank"
                                        @click.stop
                                    >{{ task.pipelineName }}</a>
                                </span>
                                <span class="task-sub">
                                    <i18n :path="isCreateResType ? 'environment.creationTaskUsedCount' : 'environment.pipelineBuildUsedCount'">
                                        <span>{{ task.buildCount || 0 }}</span>
                                    </i18n>
                                </span>
                            </div>
                        </div>

                        <!-- Job 视图头部 -->
                        <div
                            v-else-if="currentView === 'JOB'"
                            class="task-title pipeline-view-title"
                        >
                            <Logo
                                name="job"
                                :size="30"
                                class="title-icon"
                            />
                            <div class="task-title-content">
                                <span class="task-name">
                                    <bk-tag v-if="getJobStageLabel(task)">{{ getJobStageLabel(task) }}</bk-tag>
                                    {{ task.jobName }}
                                </span>
                                <span class="task-pipeline-name">
                                    <span class="pipeline-prefix">{{ $t('environment.pipeline') }}：</span>
                                    <a
                                        class="pipeline-text text-link"
                                        v-bk-overflow-tips
                                        :href="getPipelineHistoryUrl(task)"
                                        target="_blank"
                                        @click.stop
                                    >
                                        {{ task.pipelineName }}
                                        <Logo
                                            name="tiaozhuan"
                                            :size="12"
                                            class="jump-icon"
                                        />
                                    </a>
                                </span>
                            </div>
                        </div>

                        <!-- 构建视图头部 -->
                        <div
                            v-else
                            class="task-title build-view-title"
                        >
                            <div class="build-main-info">
                                <pipeline-status-icon
                                    v-if="task.status"
                                    class="build-status-icon"
                                    :status="task.status"
                                    :size="26"
                                />
                                <div class="build-text-info">
                                    <span class="task-name build-name">
                                        <a
                                            v-if="task.buildId && task.buildNum"
                                            class="text-link build-num"
                                            :href="getBuildDetailUrl(task)"
                                            target="_blank"
                                            @click.stop
                                        >#{{ task.buildNum }}</a>
                                        <span
                                            v-else
                                            class="build-num"
                                        >{{ task.buildNum ? '#' + task.buildNum : task.pipelineName }}</span>
                                        <span
                                            v-if="task.statusText"
                                            class="build-status-text"
                                        >{{ task.statusText }}</span>
                                    </span>
                                    <span class="task-pipeline-name">
                                        <span class="pipeline-prefix">{{ isCreateResType ? $t('environment.creationFlow') : $t('environment.pipeline') }}：</span>
                                        <a
                                            class="pipeline-text text-link"
                                            v-bk-overflow-tips
                                            :href="getPipelineHistoryUrl(task)"
                                            target="_blank"
                                            @click.stop
                                        >
                                            {{ task.pipelineName }}
                                            <Logo
                                                name="tiaozhuan"
                                                :size="12"
                                                class="jump-icon"
                                            />
                                        </a>
                                    </span>
                                </div>
                            </div>
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
                        <!-- 流水线视图明细 -->
                        <bk-table
                            v-if="currentView === 'PIPELINE' && !isCreateResType"
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
                                width="80"
                            >
                                <template #default="{ row }">
                                    <a
                                        class="text-link build-num-link"
                                        :href="getBuildDetailUrl(row)"
                                        target="_blank"
                                    >#{{ row.buildNum }}</a>
                                </template>
                            </bk-table-column>
                            <bk-table-column
                                :label="$t('environment.buildStatus')"
                                prop="status"
                            >
                                <template #default="{ row }">
                                    <span class="status-text-container">
                                        <pipeline-status-icon :status="row.status" />
                                        {{ row.statusText }}
                                    </span>
                                </template>
                            </bk-table-column>
                            <bk-table-column
                                :label="$t('environment.retryCount')"
                                min-width="100"
                            >
                                <template #default="{ row }">
                                    <span v-if="Number(row.executeCount) - 1 > 0">
                                        {{ $t('environment.retryCountLabel', [Number(row.executeCount) - 1]) }}
                                    </span>
                                    <span v-else>--</span>
                                </template>
                            </bk-table-column>
                            <bk-table-column
                                :label="$t('environment.associatedJob')"
                                min-width="200"
                            >
                                <template #default="{ row }">
                                    <associated-jobs :jobs="row.tasks" />
                                </template>
                            </bk-table-column>
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

                        <!-- 创作流视图明细 -->
                        <bk-table
                            v-else-if="currentView === 'PIPELINE' && isCreateResType"
                            v-bkloading="{ isLoading: task.isLoadingDetail }"
                            :data="task.records || []"
                            :outer-border="false"
                            :header-border="false"
                            :pagination="task.pagination"
                            @page-change="handlePageChange(task, $event)"
                            @page-limit-change="handlePageSizeChange(task, $event)"
                        >
                            <bk-table-column
                                :label="$t('environment.taskNumber')"
                                prop="buildNum"
                                width="80"
                            >
                                <template #default="{ row }">
                                    <a
                                        class="text-link build-num-link"
                                        :href="getBuildDetailUrl(row)"
                                        target="_blank"
                                    >#{{ row.buildNum }}</a>
                                </template>
                            </bk-table-column>
                            <bk-table-column
                                :label="$t('environment.taskStatusLabel')"
                                prop="status"
                            >
                                <template #default="{ row }">
                                    <span class="status-text-container">
                                        <pipeline-status-icon :status="row.status" />
                                        {{ row.statusText }}
                                    </span>
                                </template>
                            </bk-table-column>
                            <bk-table-column
                                :label="$t('environment.retryCount')"
                                min-width="100"
                            >
                                <template #default="{ row }">
                                    <span v-if="Number(row.executeCount) - 1 > 0">
                                        {{ $t('environment.retryCountLabel', [Number(row.executeCount) - 1]) }}
                                    </span>
                                    <span v-else>--</span>
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

                        <!-- Job 视图明细 -->
                        <bk-table
                            v-else-if="currentView === 'JOB'"
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
                                width="80"
                            >
                                <template #default="{ row }">
                                    <a
                                        class="text-link build-num-link"
                                        :href="getBuildDetailUrl(row)"
                                        target="_blank"
                                    >#{{ row.buildNum }}</a>
                                </template>
                            </bk-table-column>
                            <bk-table-column
                                :label="$t('environment.buildStatus')"
                                prop="status"
                            >
                                <template #default="{ row }">
                                    <span class="status-text-container">
                                        <pipeline-status-icon :status="row.status" />
                                        {{ row.statusText }}
                                    </span>
                                </template>
                            </bk-table-column>
                            <bk-table-column
                                v-if="isEnvDetail"
                                :label="$t('environment.workerNode')"
                                min-width="120"
                            >
                                <template #default="{ row }">
                                    <a
                                        v-if="row.nodeInfo?.displayName"
                                        class="text-link node-link"
                                        :href="getNodeDetailUrl(row)"
                                        target="_blank"
                                    >{{ row.nodeInfo.displayName }}</a>
                                    <span v-else>--</span>
                                </template>
                            </bk-table-column>
                            <bk-table-column
                                :label="$t('environment.retryCount')"
                                min-width="100"
                            >
                                <template #default="{ row }">
                                    <span v-if="Number(row.executeCount) - 1 > 0">
                                        {{ Number(row.executeCount) - 1 }}
                                    </span>
                                    <span v-else>--</span>
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

                        <!-- 构建视图明细 -->
                        <bk-table
                            v-else
                            v-bkloading="{ isLoading: task.isLoadingDetail }"
                            :data="task.records || []"
                            :outer-border="false"
                            :header-border="false"
                            :pagination="task.pagination"
                            @page-change="handlePageChange(task, $event)"
                            @page-limit-change="handlePageSizeChange(task, $event)"
                        >
                            <bk-table-column
                                :label="$t('environment.job')"
                                min-width="180"
                            >
                                <template #default="{ row }">
                                    <span class="job-cell">
                                        <span
                                            v-if="row.seq"
                                            class="job-seq"
                                        >{{ row.seq }}</span>
                                        <span
                                            class="job-name"
                                            v-bk-overflow-tips
                                        >{{ row.jobName }}</span>
                                    </span>
                                </template>
                            </bk-table-column>
                            <bk-table-column
                                :label="$t('environment.statusLabel')"
                                prop="status"
                            >
                                <template #default="{ row }">
                                    <span class="status-text-container">
                                        <pipeline-status-icon :status="row.status" />
                                        {{ row.statusText }}
                                    </span>
                                </template>
                            </bk-table-column>
                            <bk-table-column
                                v-if="isEnvDetail"
                                :label="$t('environment.workerNode')"
                                min-width="120"
                            >
                                <template #default="{ row }">
                                    <a
                                        v-if="row.nodeInfo?.nodeHashId"
                                        class="text-link node-link"
                                        :href="getNodeDetailUrl(row)"
                                        target="_blank"
                                    >{{ row.nodeInfo.displayName }}</a>
                                    <span v-else>--</span>
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
    import { ref, computed, watch } from 'vue'
    import { convertTime } from '@/utils/util'
    import { SERVICE_RESOURCE_TYPE } from '@/store/constants'
    import useInstance from '@/hooks/useInstance'
    import useEnvAside from '@/hooks/useEnvAside'
    import useEnvDetail from '@/hooks/useEnvDetail'
    import useNodeDetail from '@/hooks/useNodeDetail'
    import useTaskDetail from '@/hooks/useTaskDetail'
    import usePagination from '@/hooks/usePagination'
    import SearchSelect from '@blueking/search-select'
    import '@blueking/search-select/dist/styles/index.css'
    import PipelineStatusIcon from './PipelineStatusIcon'
    import AssociatedJobs from './AssociatedJobs'
    import Logo from '@/components/Logo'

    export default {
        name: 'TaskList',
        components: {
            SearchSelect,
            PipelineStatusIcon,
            AssociatedJobs,
            Logo
        },
        setup (props) {
            const { proxy } = useInstance()
            const routeName = proxy.$route.name
            const isEnvDetail = routeName === 'envDetail'
            const { isCreateResType, resType } = useEnvAside()
            const { envHashId } = useEnvDetail()
            const { nodeHashId } = useNodeDetail()
            const {
                fetchTaskList,
                fetchBuildDetail,
                searchJobByName,
                searchPipelineByName,
                searchByCreator
            } = useTaskDetail()
            const {
                pagination,
                resetPagination,
                updateCount
            } = usePagination()
            const currentView = ref('BUILD')
            const pipelineCount = ref(0)
            const jobCount = ref(0)
            const buildCount = ref(0)
            const isLoading = ref(false)
            const isLoadingMore = ref(false)
            const taskList = ref([])
            const dateRange = ref([])
            const searchSelectValue = ref([])
            const taskRefs = new Map()
            const taskListRef = ref(null)

            // 各视图的搜索字段
            const jobField = computed(() => ({
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
            }))
            const pipelineField = computed(() => ({
                name: isCreateResType.value ? proxy.$t('environment.creationFlow') : proxy.$t('environment.pipeline'),
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
            }))
            const statusField = computed(() => ({
                name: proxy.$t('environment.taskStatus'),
                id: 'taskStatusList',
                children: [
                    { id: 'QUEUE', name: proxy.$t('environment.pipelineTaskStatusMap.QUEUE') },
                    { id: 'RUNNING', name: proxy.$t('environment.pipelineTaskStatusMap.RUNNING') },
                    // 运行完成 = DONE + FAILURE，传参时在 filterQuery 中展开为枚举数组
                    { id: 'FINISHED', name: proxy.$t('environment.pipelineTaskStatusMap.FINISHED') }
                ]
            }))
            const creatorField = computed(() => ({
                name: proxy.$t('environment.trigger'),
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
            }))

            const searchSelectData = computed(() => {
                if (currentView.value === 'JOB') {
                    return [jobField.value, pipelineField.value, statusField.value, creatorField.value]
                }
                return [pipelineField.value, statusField.value, creatorField.value]
            })

            // 顶部统计文案（按资源类型与视图区分）
            const statsConfig = computed(() => {
                if (isCreateResType.value) {
                    if (currentView.value === 'BUILD') {
                        return { path: 'environment.creationBuildViewCount', values: [buildCount.value, pipelineCount.value] }
                    }
                    return { path: 'environment.creationPipelineViewCount', values: [pipelineCount.value, buildCount.value] }
                }
                if (currentView.value === 'JOB') {
                    return { path: 'environment.totalJobTasks', values: [jobCount.value, pipelineCount.value] }
                }
                if (currentView.value === 'BUILD') {
                    return { path: 'environment.buildViewCount', values: [buildCount.value, pipelineCount.value] }
                }
                return { path: 'environment.pipelineViewCount', values: [pipelineCount.value, buildCount.value] }
            })
            // 是否还有更多数据
            const hasMore = computed(() => {
                return taskList.value.length < pagination.value.count
            })

            const filterQuery = computed(() => {
                return searchSelectValue.value.reduce((query, item) => {
                    // 状态筛选为 List<Enum>：FINISHED（运行完成）展开为 ['DONE', 'FAILURE']，其余状态原样传
                    if (item.id === 'taskStatusList') {
                        const statusList = item.values.flatMap(value =>
                            value.id === 'FINISHED' ? ['DONE', 'FAILURE'] : [value.id]
                        )
                        query.taskStatusList = [...new Set(statusList)]
                        return query
                    }
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
                    taskRefs.set(task.taskKey, el)
                }
            }
            
            const searchPlaceholder = computed(() => {
                return searchSelectData.value.map(item => item.name).join(' / ')
            })
            
            const projectId = computed(() => proxy.$route.params?.projectId)

            // 流水线编排页
            const getPipelineHistoryUrl = (task) => {
                return `/console/pipeline/${projectId.value}/${task.pipelineId}/history/pipeline`
            }

            // 构建执行详情页
            const getBuildDetailUrl = (row) => {
                return `/console/pipeline/${row.projectId || projectId.value}/${row.pipelineId}/detail/${row.buildId}/executeDetail`
            }

            // 工作节点详情页（resType 区分流水线资源/创作流资源）
            const getNodeDetailUrl = (row) => {
                const nodeHashId = row.nodeInfo?.nodeHashId || ''
                const displayName = row.nodeInfo?.displayName || ''
                return `/console/environment/${projectId.value}/${resType.value || SERVICE_RESOURCE_TYPE.PIPELINE}/node/allNode?nodeHashId=${nodeHashId}?displayName=${displayName}`
            }
            
            // 获取任务信息项（右侧统计），按视图区分
            const getTaskInfoItems = (task) => {
                if (currentView.value === 'PIPELINE') {
                    return [
                        {
                            label: isCreateResType.value
                                ? proxy.$t('environment.creationTaskCountLabel')
                                : proxy.$t('environment.buildCountLabel'),
                            value: task.buildCount || 0
                        },
                        { label: proxy.$t('environment.nodeInfo.lastRunAs'), value: task.lastBuildTime || '--' }
                    ]
                }
                if (currentView.value === 'BUILD') {
                    const items = []
                    // 重试次数：后端 executeCount 表示总执行次数（含首次），展示值 = executeCount - 1
                    // 为 0 时不展示该信息项
                    const retryCount = formatRetryCount(task.executeCount)
                    if (retryCount !== '') {
                        items.push({ label: proxy.$t('environment.retryCount'), value: retryCount })
                    }
                    items.push(
                        { label: proxy.$t('environment.duration'), value: task.duration || '--' },
                        { label: proxy.$t('environment.trigger'), value: task.creator || '--' },
                        { label: proxy.$t('environment.startTime'), value: task.startTime || '--' }
                    )
                    return items
                }
                // JOB
                return [
                    { label: proxy.$t('environment.executionCount'), value: task.buildCount || 0 },
                    { label: proxy.$t('environment.avgDuration'), value: task.avgTimeInterval || '--' },
                    { label: proxy.$t('environment.nodeInfo.lastRunAs'), value: task.lastBuildTime || '--' }
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

            // 时长格式化：兼容毫秒/秒
            const formatDuration = (duration) => {
                if (duration === null || duration === undefined || duration === '') return '--'
                const durationNum = Number(duration)
                if (Number.isNaN(durationNum) || durationNum < 0) return '--'
                // 10 位以上通常是毫秒级时长，转换为秒
                const seconds = durationNum >= 100000 ? Math.floor(durationNum / 1000) : Math.floor(durationNum)
                return formatSeconds(seconds)
            }
            
            // 计算执行耗时
            const calculateDuration = (startTime, endTime) => {
                if (!startTime || !endTime) return '--'
                
                const startTimestamp = new Date(startTime).getTime()
                const endTimestamp = new Date(endTime).getTime()
                const seconds = Math.floor((endTimestamp - startTimestamp) / 1000)
                
                return formatSeconds(seconds)
            }
            
            // 重试次数展示：后端 executeCount 表示总执行次数（含首次），展示值 = executeCount - 1
            // 0、1 时为空；其他情况直接展示数字 N
            const formatRetryCount = (executeCount) => {
                const num = Number(executeCount)
                if (Number.isNaN(num) || num <= 1) return ''
                return `${num - 1}`
            }

            // 格式化时间
            const formatTime = (time) => {
                if (!time) return '--'
                if (typeof time === 'string') {
                    return convertTime(new Date(time).getTime())
                }
                // 兼容秒级/毫秒级时间戳
                const timestamp = Number(time)
                if (Number.isNaN(timestamp)) return '--'
                return convertTime(timestamp >= 1000000000000 ? timestamp : timestamp * 1000)
            }
            
            // 流水线编排不展示第一个Stage，转换 stageId 显示：后端从 stage-2 开始，前端显示从 stage-1 开始
            const convertStageId = (stageId) => {
                if (!stageId || typeof stageId !== 'string') return stageId
                
                const match = stageId.match(/^stage-(\d+)$/)
                if (match) {
                    const stageNum = parseInt(match[1], 10)
                    return `stage-${stageNum - 1}`
                }
                return stageId
            }

            const getJobStageLabel = (task) => {
                const stageNum = task.stageNumb ?? ''
                const lastContainerId = task.lastContainerId ?? ''
                if (stageNum && lastContainerId) return `${stageNum}-${lastContainerId}`
                return stageNum || lastContainerId || ''
            }


            // 列表记录映射（按视图区分展示字段）
            const mapListRecord = (view, item, index) => {
                const base = {
                    ...item,
                    isExpanded: false,
                    isLoadingDetail: false,
                    records: null,
                    pagination: {
                        current: 1,
                        count: 0,
                        limit: 10
                    }
                }
                if (view === 'PIPELINE') {
                    return {
                        ...base,
                        taskKey: item.pipelineId,
                        buildCount: item.buildCount,
                        lastBuildTime: formatTime(item.lastBuildTime)
                    }
                }
                if (view === 'BUILD') {
                    // 构建视图
                    const latestBuild = item.buildHistory || {}
                    const startTime = latestBuild.startTime || item.startTime || item.lastBuildTime
                    const endTime = latestBuild.endTime || item.endTime
                    return {
                        ...base,
                        taskKey: item.buildId || latestBuild.buildId || `${item.pipelineId}-${startTime || index}`,
                        buildId: item.buildId || latestBuild.buildId,
                        buildNum: latestBuild.buildNum || item.buildNum,
                        status: latestBuild.status || item.status,
                        statusText: (latestBuild.status || item.status)
                            ? (proxy.$t(`environment.statusMap.${latestBuild.status || item.status}`) || (latestBuild.status || item.status))
                            : '',
                        creator: latestBuild.userId || item.creator || '--',
                        startTime: formatTime(startTime),
                        // 来源：listAgentPipeline 接口 buildHistory.executeCount，回退为 0
                        executeCount: latestBuild.executeCount ?? item.executeCount ?? 0,
                        duration: (startTime && endTime)
                            ? calculateDuration(startTime, endTime)
                            : formatDuration(latestBuild.totalTime || latestBuild.executeTime || item.avgTimeInterval)
                    }
                }
                // JOB
                return {
                    ...base,
                    taskKey: item.jobId || item.pipelineId,
                    stageId: convertStageId(item.stageId),
                    buildCount: item.buildCount,
                    avgTimeInterval: formatSeconds(item.avgTimeInterval),
                    lastBuildTime: formatTime(item.lastBuildTime)
                }
            }

            // 明细记录映射（按视图区分展示字段）
            const mapDetailRecord = (view, i) => {
                if (view === 'BUILD') {
                    // 构建视图展开：每行是该构建下的一个 Job
                    const firstTask = (i.tasks && i.tasks[0]) || {}
                    return {
                        ...i,
                        seq: (() => {
                            const vmSeqId = firstTask.vmSeqId || i.containerId || ''
                            const stageNumb = firstTask.stageNumb
                            if (!stageNumb) return vmSeqId
                            return vmSeqId ? `${stageNumb}-${vmSeqId}` : stageNumb
                        })(),
                        jobName: firstTask.taskName || '--',
                        statusText: proxy.$t(`environment.statusMap.${i.status}`) || '',
                        startTime: formatTime(i.startTime),
                        endTime: formatTime(i.endTime),
                        duration: calculateDuration(i.startTime, i.endTime)
                    }
                }
                // PIPELINE / JOB 展开：每行是一次构建
                return {
                    ...i,
                    statusText: proxy.$t(`environment.statusMap.${i.status}`) || '',
                    startTime: formatTime(i.startTime),
                    endTime: formatTime(i.endTime),
                    duration: calculateDuration(i.startTime, i.endTime),
                    tasks: i.tasks || []
                }
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
                // 构建视图展开需要 buildId，后端列表未返回时跳过请求，避免 400
                if (currentView.value === 'BUILD' && !task.buildId) {
                    task.records = []
                    task.pagination.count = 0
                    return
                }
                try {
                    task.isLoadingDetail = true
                    const params = {
                        page: task.pagination.current,
                        pageSize: task.pagination.limit
                    }
                    const res = await fetchBuildDetail(currentView.value, {
                        pipelineId: task.pipelineId,
                        jobId: task.jobId,
                        buildId: task.buildId,
                        // 数据源：listAgentPipeline 接口里的 executeCount；即使 0/null 也强制传入
                        executeCount: task.executeCount ?? '',
                        params
                    })
                    task.records = (res.records || []).map(i => mapDetailRecord(currentView.value, i))
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
                        isLoading.value = true
                        pagination.value.current = 1
                    }
                    const params = {
                        ...filterQuery.value,
                        ...timeRangeParams.value,
                        page: pagination.value.current,
                        pageSize: pagination.value.limit
                    }
                    
                    const res = await fetchTaskList(currentView.value, params)
                    pipelineCount.value = res.pipelineCount
                    jobCount.value = res.jobCount
                    buildCount.value = res.buildCount
                    const newTasks = (res.result.records || []).map((task, idx) => mapListRecord(currentView.value, task, idx))
                    
                    if (isLoadMore) {
                        // 加载更多，追加数据
                        taskList.value = [...taskList.value, ...newTasks]
                    } else {
                        // 首次加载或刷新，替换数据
                        taskList.value = newTasks
                        
                        // 首次加载时，自动展开第一个任务
                        if (newTasks.length > 0) {
                            isLoading.value = false
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

            // 切换视图
            const handleViewChange = () => {
                resetPagination()
                taskList.value = []
                searchSelectValue.value = []
                loadTaskList()
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
            
            // 监听 envHashId 或 nodeHashId 变化
            const watchHashId = computed(() =>
                routeName === 'envDetail' ? envHashId.value : nodeHashId.value
            )
            
            watch(watchHashId, (newVal, oldVal) => {
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
                isCreateResType,
                isEnvDetail,
                statsConfig,
                currentView,
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
                buildCount,

                // function
                setTaskRef,
                getTaskInfoItems,
                getPipelineHistoryUrl,
                getBuildDetailUrl,
                getNodeDetailUrl,
                formatTime,
                getJobStageLabel,
                toggleExpand,
                handleViewChange,
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
        width: 240px;
        margin-right: 8px;
    }
    .search-input {
        width: 350px;
        z-index: 1000;
        background: white;
        ::placeholder {
            color: #c4c6cc;
        }
    }
    
    .task-header {
        flex-shrink: 0;
        display: flex;
        flex-wrap: wrap;
        justify-content: space-between;
        align-items: center;
        gap: 8px 16px;
        
        .task-header-left {
            display: flex;
            align-items: center;
            gap: 16px;
            flex-shrink: 0;
            min-width: 0;
        }
        .stats-text {
            font-size: 12px;
            color: #63656E;
            white-space: nowrap;
        }
        .count-number {
            font-weight: 700;
            color: #3c88ff;
        }
        
        .task-search {
            display: flex;
            align-items: center;
            flex: 1 1 auto;
            justify-content: flex-end;
            min-width: 0;
        }
    }
    
    .task-list {
        flex: 1;
        overflow-y: auto;
        padding: 16px 0;
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

                    &.pipeline-view-title {
                        display: flex;
                        align-items: center;
                        min-width: 0;

                        .title-icon {
                            flex-shrink: 0;
                            margin-right: 12px;

                            &.bk-icon {
                                font-size: 30px;
                                width: 30px;
                                height: 30px;
                                line-height: 30px;
                                color: #3a84ff;
                            }
                        }

                        .task-title-content {
                            min-width: 0;
                            flex: 1;
                        }
                    }
                    
                    .task-name {
                        font-size: 14px;
                        color: #313238;
                        font-weight: 500;
                        display: flex;
                        align-items: center;
                        margin-bottom: 8px;

                        &.build-name {
                            gap: 6px;
                            margin-bottom: 6px;
                            align-items: baseline;
                        }

                        .build-num {
                            font-weight: 500;
                        }

                        a.build-num {
                            color: #3A84FF;
                        }

                        .build-status-text {
                            color: #313238;
                        }
                    }

                    &.build-view-title {
                        .build-main-info {
                            display: flex;
                            align-items: center;
                            min-width: 0;
                        }

                        .build-status-icon {
                            margin-right: 8px;
                            margin-top: 0;
                            flex-shrink: 0;
                            width: 26px;
                            height: 26px;
                            line-height: 26px;
                        }

                        .build-text-info {
                            min-width: 0;
                            flex: 1;
                        }
                    }

                    .pipeline-title {
                        overflow: hidden;
                        white-space: nowrap;
                        text-overflow: ellipsis;
                        color: #3A84FF;
                    }

                    .task-sub {
                        font-size: 12px;
                        color: #979BA5;
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

                        .pipeline-prefix {
                            flex-shrink: 0;
                        }
                        
                        .pipeline-text {
                            display: flex;
                            align-items: center;
                            overflow: hidden;
                            white-space: nowrap;
                            text-overflow: ellipsis;
                            color: inherit;
                            cursor: pointer;
                            text-decoration: none;
                            &:hover {
                                color: #3a84ff;
                            }
                        }

                        .jump-icon {
                            margin-left: 8px;
                            color: #3a84ff;
                            flex-shrink: 0;
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
                .job-cell {
                    display: flex;
                    align-items: center;
                    min-width: 0;
                    .job-seq {
                        display: inline-flex;
                        align-items: center;
                        justify-content: center;
                        height: 18px;
                        padding: 0 4px;
                        margin-right: 6px;
                        font-size: 12px;
                        line-height: 16px;
                        color: #63656e;
                        background: #fafbfd;
                        border: 1px solid #dcdee5;
                        border-radius: 2px;
                        flex-shrink: 0;
                        box-sizing: border-box;
                    }
                    .job-name {
                        overflow: hidden;
                        white-space: nowrap;
                        text-overflow: ellipsis;
                    }
                }
            }
        }

        .text-link {
            color: inherit;
            cursor: pointer;
            text-decoration: none;
            &:hover {
                color: #3a84ff;
            }
        }

        a.build-num-link {
            color: #3A84FF;
        }

        a.node-link {
            color: #3A84FF;
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
