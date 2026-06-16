<template>
    <div class="task-execution-content">
        <!-- 状态一：确认执行任务 -->
        <div
            v-if="localExecutionStatus === 'pending'"
            class="task-confirm-section"
        >
            <div>
                <p class="header-title">
                    {{ $t('confirmTaskExecution') }}
                </p>
                <p class="header-desc">
                    {{ $t('importantReminder') }}
                </p>
            </div>

            <hr style="border: 0; border-top: 1px solid #DCDEE5; margin: 16px 0 24px;" />

            <div class="content">
                <div class="detail-item">
                    <p>
                        <span>{{ $t('sourceProject') }}</span>
                        <span class="value">{{ taskData?.projectId || '--' }}</span>
                    </p>
                    <p>
                        <span>{{ $t('targetProject') }}</span>
                        <span class="value">{{ taskData?.targetProjectId || '--' }}</span>
                    </p>
                </div>
                <div class="detail-item">
                    <p>
                        <span>{{ $t('pipelineCopyCount') }}</span>
                        <span class="value">{{ taskData?.pipelineCount || 0 }} {{ $t('strip') }}<template v-if="taskData?.subPipelineCount">({{ $t('includingAutoAdded', [taskData?.subPipelineCount]) }})</template></span>
                    </p>
                    <p>
                        <span
                            class="auto-execute"
                            v-bk-tooltips="$t('autoExecuteAfterCopyTooltip')"
                        >{{ $t('autoExecuteAfterCopy') }}</span>
                        <span class="value">{{ taskData?.autoFinishCount || 0 }} {{ $t('execItem') }}</span>
                    </p>
                </div>
                <div class="detail-item">
                    <p>
                        <span>{{ $t('unhandledResources') }}</span>
                        <span class="value">{{ taskData?.unprocessedCount ? `${taskData?.unprocessedCount} ${$t('execItem')}` : $t('none') }}</span>
                    </p>
                    <p>
                        <span>{{ $t('highRiskOperations') }}</span>
                        <span
                            class="value"
                            :class="{ 'high-risk': taskData?.highRiskCount > 0 }"
                        >
                            {{ taskData?.highRiskCount || 0 }} {{ $t('execItem') }}
                            <span @click="handleViewHighRisk">
                                <Logo
                                    v-if="taskData?.highRiskCount > 0"
                                    name="orientation"
                                    size="12"
                                    v-bk-tooltips="$t('newlist.view')"
                                />
                            </span>
                        </span>
                    </p>
                </div>
                <div class="detail-item">
                    <p>
                        <span>{{ $t('pipelineIdStrategy') }}</span>
                        <span class="value">{{ pipelineIdStrategyText }}</span>
                    </p>
                    <p></p>
                </div>
            </div>

            <div class="confirm-checkbox">
                <bk-checkbox
                    v-model="taskConfirmed"
                >
                    {{ $t('iConfirmedConfigAndRisk') }}
                </bk-checkbox>
            </div>
        </div>
        <!-- 操作按钮 -->
        <div
            v-if="localExecutionStatus === 'pending'"
            class="action-buttons"
        >
            <bk-button
                theme="default"
                @click="handlePrevStep"
            >
                {{ $t('previousStep') }}
            </bk-button>
            <bk-button
                theme="primary"
                :disabled="!taskConfirmed"
                @click="handleStartCopy"
            >
                {{ $t('startCopy') }}
            </bk-button>
            <bk-button
                theme="default"
                @click="handleCancel"
            >
                {{ $t('cancel') }}
            </bk-button>
        </div>

        <!-- 状态二:执行中 -->
        <div
            v-else-if="localExecutionStatus === 'executing'"
            class="task-executing-section"
        >
            <!-- 执行头部信息 -->
            <div class="executing-header">
                <Logo
                    name="task-copy"
                    size="76"
                />
                <div class="header-right">
                    <div class="header-info">
                        <span class="header-title">
                            {{ $t('crossProjectCopying') }}
                        </span>
                        <div class="header-stats">
                            <span class="progress-text">{{ progressInfo?.executedCount }}/{{ progressInfo?.totalCount }}</span>
                            <!-- <span class="divider">|</span>
                            <span class="estimate-time">{{ $t('estimatedRemaining') }} {{ progressInfo?.estimatedTime }}</span> -->
                        </div>
                    </div>

                    <!-- 进度条 -->
                    <div class="progress-bar-container">
                        <div class="progress-bar-wrapper">
                            <div
                                class="progress-bar-fill"
                                :style="{ width: progressPercent + '%' }"
                            ></div>
                        </div>
                    </div>
                </div>
            </div>

            

            <!-- 流水线列表 -->
            <bk-table
                :data="pipelineList"
                :pagination="pagination"
                :max-height="400"
                :outer-border="false"
                v-bkloading="{ isLoading: tableLoading }"
                class="pipeline-list-table"
                @page-change="handlePageChange"
                @page-limit-change="handlePageLimitChange"
            >
                <bk-table-column
                    :label="$t('pipelineName')"
                    width="600"
                >
                    <template slot-scope="{ row }">
                        <div class="pipeline-name-cell">
                            <span class="pipeline-name-text">{{ row.pipelineName }}</span>
                            <span
                                v-if="row.subPipeline"
                                class="sub-tag"
                            >
                                {{ $t('subPipeline') }}
                                <Logo
                                    name="link"
                                    size="10"
                                />
                            </span>
                            <span
                                v-if="row.pac"
                                class="pac-tag"
                            >
                                <i class="devops-icon icon-code" />
                                PAC
                            </span>
                            <bk-tag
                                v-if="row.versionStatus === 'COMMITTING'"
                                theme="success"
                                class="draft-tag"
                            >
                                {{ $t('draft') }}
                            </bk-tag>
                            <span
                                v-if="row.constraint"
                                class="template-tag"
                            >
                                {{ $t('constraint') }}
                            </span>
                        </div>
                    </template>
                </bk-table-column>
                <bk-table-column
                    :label="$t('creator')"
                    prop="pipelineCreator"
                    width="120"
                ></bk-table-column>
                <bk-table-column
                    width="300"
                    :label="$t('execStatus')"
                >
                    <template slot-scope="{ row }">
                        <span :class="['status-text', `status-text--${row.status}`]">
                            {{ getStatusText(row.status) }}
                        </span>
                        <bk-popover
                            placement="top"
                            :tippy-options="{
                                theme: 'light'
                            }"
                            always
                            ext-cls="pipeline-error-popover"
                            v-if="row.errorTypeName"
                        >
                            <span class="fail-reason">{{ row.errorTypeName }}</span>
                            <div slot="content">
                                <template v-if="row.errorType !== PipelineBatchTaskDetailErrorType.DEPENDENCY_CREATE_FAILED">
                                    <span>{{ row.errorMessageText }}</span>
                                </template>
                                <template v-else>
                                    <div
                                        v-for="(item, index) in row.errorMessageText"
                                        :key="index"
                                        class="error-item"
                                    >
                                        <span>{{ item.resourceTypeName }}</span>
                                        <p
                                            v-for="err in item.resources"
                                            :key="err.resourceType"
                                        >
                                            <span class="error-item-title">{{ err.resourceName }}</span>
                                            <a
                                                class="jump-icon"
                                                @click="handleJump(item.resourceType, err)"
                                            >
                                                <Logo
                                                    name="tiaozhuan"
                                                    size="12"
                                                />
                                            </a>
                                            <span>{{ err.errorMessageText }}</span>
                                        </p>
                                    </div>
                                </template>
                            </div>
                        </bk-popover>
                    </template>
                </bk-table-column>
            </bk-table>
        </div>

        <!-- 状态三:执行完成 -->
        <div
            v-else-if="localExecutionStatus === 'completed'"
            class="task-completed-section"
        >
            <!-- 第一部分:状态提示 -->
            <div
                v-if="taskResult === 'partial'"
                class="task-result-alert is-failed"
            >
                <span class="info-icon">
                    <i class="devops-icon icon-exclamation" />
                </span>
                <div class="task-result-title partial">
                    <div>
                        <p class="title-text">{{ $t('taskCompletedPartial') }}</p>
                        <p class="title-desc">{{ $t('taskCompletedPartialDesc') }}</p>
                    </div>
                    <bk-button
                        theme="primary"
                        text
                        class="title-button"
                        @click="handleRetryFailed"
                    >
                        {{ $t('retryFailedItems') }} ({{ pipelineStatusSummary.failedCount }})
                    </bk-button>
                </div>
            </div>
            <div
                v-if="taskResult === 'success'"
                class="task-result-alert is-success"
            >
                <i class="devops-icon icon-check-1 success-icon" />
                <div
                    class="task-result-title"
                >
                    <p class="title-text">{{ $t('taskCompletedSuccess') }}</p>
                    <p class="title-desc">{{ $t('pipelinesCopiedTo', [pipelineStatusSummary.totalCount, taskData?.targetProjectId || '--']) }}</p>
                </div>
            </div>
            <!-- 第二部分:流水线列表 -->
            <div class="pipeline-list-section">
                <!-- 功能切换卡片部分 -->
                <div class="tab-cards">
                    <div
                        v-for="tab in completedTabs"
                        :key="tab.key"
                        v-bk-tooltips="{ content: $t('noData'), disabled: tab.totalCount > 0 }"
                        :class="['tab-card', `tab-${tab.key}`, { 'active': activeTab === tab.key, 'disabled': tab.totalCount === 0 }]"
                        @click="tab.totalCount > 0 && handleTabChange(tab.key)"
                    >
                        <div class="tab-content">
                            <p>
                                <Logo
                                    :name="tab.icon"
                                    :style="{ color: tab.color }"
                                    size="14"
                                    class="tab-icon"
                                />
                                <span
                                    class="tab-label"
                                    :style="{ color: activeTab === tab.key ? tab.color : '#4D4F56' }"
                                >{{ tab.label }}</span>
                            </p>
                        </div>
                        <div
                            class="tab-counts"
                            v-if="tab.subCount"
                        >
                            <span class="sub-count">{{ tab.subCount }}</span>
                            <span class="tab-count">{{ tab.totalCount }}</span>
                        </div>
                        <div
                            class="total-count"
                            v-else
                        >
                            {{ tab.totalCount }}
                        </div>
                    </div>
                </div>

                <!-- 切换卡片的数据部分(预留) -->
                <div class="tab-content-area">
                    <!-- 动态组件 -->
                    <component
                        :is="currentTabComponent"
                        ref="tabComponent"
                        :resource-data="activeTab !== 'PIPELINE' ? tabData[activeTab] : undefined"
                        :status-summary="pipelineStatusSummary"
                        @update-resource="handleUpdateResource"
                        @retry="handleRetry"
                        @jump="handleJump"
                    />
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import Logo from '@/components/Logo'
    import { mapActions } from 'vuex'
    import { PipelineIdStrategy, PipelineBatchTaskStatus, PipelineCopyResourceType, PipelineCopyStrategy, PipelineBatchTaskStep, PipelineBatchTaskDetailStatus } from '@/store/modules/crossProjectCopy/constants'
    import CustomTabs from './components/CustomTabs.vue'
    import PipelineListContent from './components/PipelineListContent.vue'
    import PendingResourcesContent from './components/PendingResourcesContent.vue'
    import TransferIssuesContent from './components/TransferIssuesContent.vue'
    import AutoCompleteContent from './components/AutoCompleteContent.vue'
    
    export default {
        name: 'TaskExecution',
        components: {
            Logo,
            CustomTabs,
            PipelineListContent,
            PendingResourcesContent,
            TransferIssuesContent,
            AutoCompleteContent
        },
        props: {
            formData: {
                type: Object,
                required: true
            },
            taskData: {
                type: Object,
                default: () => ({})
            }
        },
        data () {
            return {
                // 执行状态: pending-待确认, executing-执行中, completed-执行完成
                localExecutionStatus: 'pending',
                // 任务确认状态
                taskConfirmed: false,
                // 轮询定时器
                pollingTimer: null,
                // 轮询间隔(毫秒)
                pollingInterval: 2000,
                // 重试轮询相关
                retryPollingTimer: null,
                isRetryPolling: false,
                progressInfo: {
                    totalCount: 0,
                    executedCount: 0
                },
                // 流水线列表
                pipelineList: [],
                // 表格 loading 状态
                tableLoading: false,
                pagination: {
                    current: 1,
                    count: 0,
                    limit: 10
                },
                // 任务执行结果: success-全部成功, partial-部分失败
                taskResult: '',
                // 当前激活的tab
                activeTab: 'PIPELINE',
                // 资源数据
                tabData: {
                    NEED_COMPLETION: null,
                    NEED_TRANSFER: null,
                    AUTO_FINISH: null
                },
                // 流水线状态汇总数据
                pipelineStatusSummary: {
                    successCount: 0,
                    failedCount: 0,
                    totalCount: 0
                },
                // 第一个高风险资源的类型
                firstHighRiskResourceType: null,
                tabsNeedResource: ['NEED_COMPLETION', 'NEED_TRANSFER', 'AUTO_FINISH'],
                // 完成页tabs配置
                completedTabs: [
                    {
                        key: 'PIPELINE',
                        label: this.$t('pipeline'),
                        icon: 'pipeline-result',
                        color: '#3764DC',
                        totalCount: 0
                    },
                    {
                        key: 'NEED_COMPLETION',
                        label: this.$t('pendingCompletion'),
                        icon: 'exclamation-triangle-shape',
                        color: '#EBB401',
                        subCount: 0,
                        totalCount: 0
                    },
                    {
                        key: 'NEED_TRANSFER',
                        label: this.$t('resourceTransferIssues'),
                        icon: 'transfer',
                        color: '#D75573',
                        subCount: 0,
                        totalCount: 0
                    },
                    {
                        key: 'AUTO_FINISH',
                        label: this.$t('autoComplete'),
                        icon: 'auto-complete',
                        color: '#559BD2',
                        totalCount: 0
                    }
                ]
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            taskId () {
                return this.$route.params.taskId
            },
            // 流水线ID策略显示文本
            pipelineIdStrategyText () {
                const strategy = this.taskData?.pipelineCopyStrategy
                if (strategy === PipelineIdStrategy.PIPELINE_CREATE_NEW_ID) return this.$t('autoGenerateNewId')
                if (strategy === PipelineIdStrategy.PIPELINE_REUSE_SOURCE_ID) return this.$t('reuseSourcePipelineId')
                return '--'
            },
            // 进度百分比
            progressPercent () {
                const total = this.progressInfo?.totalCount ?? 0
                const executed = this.progressInfo?.executedCount ?? 0
                if (total === 0) return 0
                return Math.floor((executed / total) * 100)
            },
            // 当前tab对应的组件
            currentTabComponent () {
                const componentMap = {
                    PIPELINE: 'PipelineListContent',
                    NEED_COMPLETION: 'PendingResourcesContent',
                    NEED_TRANSFER: 'TransferIssuesContent',
                    AUTO_FINISH: 'AutoCompleteContent'
                }
                return componentMap[this.activeTab]
            }
        },
        watch: {
            localExecutionStatus: {
                immediate: true,
                handler (newVal, oldVal) {
                    if (newVal === 'executing' && oldVal !== 'executing') {
                        this.startPolling()
                    } else if (newVal !== 'executing' && oldVal === 'executing') {
                        this.stopPolling()
                    }

                    if (newVal === 'completed' && !this.$route.query.execTab) {
                        this.$router.replace({
                            query: {
                                ...this.$route.query,
                                execTab: this.activeTab
                            }
                        })
                    }
                }
            },
            // 监听 taskData.status 来动态更新 localExecutionStatus
            'taskData.status': {
                immediate: true,
                handler (newStatus) {
                    this.updateTaskResult(newStatus)

                    if (newStatus === PipelineBatchTaskStatus.DRAFT) {
                        this.localExecutionStatus = 'pending'
                    } else if (newStatus === PipelineBatchTaskStatus.EXECUTING) {
                        this.localExecutionStatus = 'executing'
                    } else if (this.isTaskFinished(newStatus)) {
                        this.localExecutionStatus = 'completed'
                        this.fetchExecuteSummary()
                    }
                }
            }
        },
        created () {
            this.PipelineBatchTaskDetailStatus = PipelineBatchTaskStatus
        },
        mounted () {
            const tabFromParams = this.$route.query.execTab
            if (tabFromParams && this.completedTabs.some(t => t.key === tabFromParams)) {
                this.activeTab = tabFromParams
            }
            
            // 如果初始就是执行中状态,立即开始轮询
            if (this.localExecutionStatus === 'executing') {
                this.startPolling()
            }

            // 有高风险时获取第一个高风险资源
            if (this.taskData?.highRiskCount > 0) {
                this.fetchFirstHighRiskResource()
            }
            
            if (this.tabsNeedResource.includes(this.activeTab) && !this.tabData[this.activeTab]) {
                this.fetchResourceData()
            }
        },
        beforeDestroy () {
            // 组件销毁前清理定时器
            this.stopPolling()
        },
        methods: {
            ...mapActions('crossProjectCopy', [
                'executeCopy',
                'getExecuteProgress',
                'getTaskDetails',
                'getTaskStatusSummary',
                'listResourceDetails',
                'getExecuteSummary',
                'getCopyTaskDetail',
                'confirmResource',
                'retryFailedTask',
                'retryFailedTaskDetail'
            ]),
            /**
             * 判断任务是否已完成
             */
            isTaskFinished (status) {
                return (
                    status === PipelineBatchTaskStatus.SUCCESS
                    || status === PipelineBatchTaskStatus.FAILED
                    || status === PipelineBatchTaskStatus.PARTIAL_FAILED
                )
            },
            isTaskFailed (status) {
                return (
                    status === PipelineBatchTaskStatus.FAILED
                    || status === PipelineBatchTaskStatus.PARTIAL_FAILED
                )
            },
            isTaskSuccess (status) {
                return status === PipelineBatchTaskStatus.SUCCESS
            },

            /**
             * 更新任务结果展示状态
             */
            updateTaskResult (status) {
                if (this.isTaskSuccess(status)) {
                    this.taskResult = 'success'
                } else if (this.isTaskFailed(status)) {
                    this.taskResult = 'partial'
                } else {
                    this.taskResult = ''
                }
            },
            handlePrevStep () {
                this.$emit('prev-step')
            },
            /**
             * 获取第一个高风险资源
             */
            async fetchFirstHighRiskResource () {
                try {
                    const data = await this.listResourceDetails({
                        projectId: this.projectId,
                        taskId: this.taskId,
                        params: {}
                    })
                    
                    // 遍历所有资源类型，找到第一个高风险资源
                    const highRiskConfig = [
                        { type: PipelineCopyResourceType.BUILD_ENV, strategy: PipelineCopyStrategy.BUILD_ENV_CREATE_AND_MOVE_NODE },
                        { type: PipelineCopyResourceType.DEPLOY_ENV, strategy: PipelineCopyStrategy.DEPLOY_ENV_CREATE_AND_MOVE_NODE },
                        { type: PipelineCopyResourceType.BUILD_NODE, strategy: PipelineCopyStrategy.BUILD_NODE_MOVE_TO_TARGET_PROJECT },
                        { type: PipelineCopyResourceType.DEPLOY_NODE, strategy: PipelineCopyStrategy.DEPLOY_NODE_MOVE_TO_TARGET_PROJECT },
                        { type: PipelineCopyResourceType.CREDENTIAL, strategy: PipelineCopyStrategy.CREDENTIAL_CREATE_NEW }
                    ]
                    
                    for (const config of highRiskConfig) {
                        const resourceData = data.find(item => item.resourceType === config.type)
                        
                        if (resourceData && resourceData.resources) {
                            const highRiskItem = resourceData.resources.find(
                                item => item.copyStrategy === config.strategy
                            )
                            
                            if (highRiskItem) {
                                // 找到第一个高风险资源，存储其资源类型
                                this.firstHighRiskResourceType = config.type
                                break
                            }
                        }
                    }
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                }
            },
            handleViewHighRisk () {
                this.$router.push({
                    name: 'crossProjectCopy',
                    params: {
                        projectId: this.projectId,
                        taskId: this.taskId,
                        tab: PipelineBatchTaskStep.RESOURCE_DEPEND
                    },
                    query: {
                        ...this.$route.query,
                        sourceType: this.firstHighRiskResourceType
                    }
                })
            },
            async handleStartCopy () {
                if (!this.taskConfirmed) {
                    return
                }
                
                try {
                    await this.executeCopy({
                        projectId: this.projectId,
                        taskId: this.taskId
                    })
                    this.localExecutionStatus = 'executing'
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                }
            },
            handleCancel () {
                this.$emit('cancel')
            },
            /**
             * 处理单个流水线的重试
             */
            handleRetry (row) {
                this.$bkInfo({
                    width: 400,
                    title: this.$t('confirmRetryFailedPipeline'),
                    subTitle: `${this.$t('pipeline')}： ${row.pipelineName}`,
                    okText: this.$t('retry'),
                    cancelText: this.$t('cancel'),
                    confirmFn: () => {
                        // 调接口后不阻塞，立即开始轮询刷新数据
                        this.retryFailedTaskDetail({
                            projectId: this.projectId,
                            taskId: this.taskId,
                            pipelineId: row.pipelineId
                        }).then(() => {
                            this.$bkMessage({ theme: 'success', message: this.$t('subpage.retrySuc') })
                        }).catch((error) => {
                            this.$bkMessage({ theme: 'error', message: error.message || error })
                        })

                        // 立即开始轮询
                        this.startRetryPolling()
                    }
                })
            },
            handleRetryFailed () {
                this.$bkInfo({
                    width: 400,
                    title: this.$t('confirmRetryFailedPipeline'),
                    subHeader: this.$createElement('div', {
                        style: {
                            fontSize: '14px',
                            color: '#4D4F56',
                            padding: '12px 16px',
                            backgroundColor: '#F5F7FA'
                        }
                    }, this.$t('currentFailedPipelineCount', [this.pipelineStatusSummary.failedCount])),
                    okText: this.$t('retry'),
                    cancelText: this.$t('cancel'),
                    confirmFn: () => {
                        // 调接口后不阻塞，立即开始轮询刷新数据
                        this.retryFailedTask({
                            projectId: this.projectId,
                            taskId: this.taskId
                        }).then(() => {
                            this.$bkMessage({ theme: 'success', message: this.$t('retryTaskSubmitted') })
                        }).catch((error) => {
                            this.$bkMessage({ theme: 'error', message: error.message || error })
                        })

                        // 立即开始轮询
                        this.startRetryPolling()
                    }
                })
            },
            /**
             * 更新资源数据
             */
            async handleUpdateResource (updatedData) {
                const tabKey = this.activeTab
                if (!this.tabData[tabKey] || !Array.isArray(this.tabData[tabKey])) {
                    return
                }
                
                try {
                    const result = await this.confirmResource({
                        projectId: this.projectId,
                        taskId: this.taskId,
                        resourceType: updatedData.resourceType,
                        resourceId: updatedData.resourceId
                    })

                    if (result === true) {
                        // 找到对应的资源项并更新
                        const index = this.tabData[tabKey].findIndex(item => {
                            const itemId = item.resourceId
                            const updatedId = updatedData.resourceId
                            return itemId === updatedId
                        })
                        
                        if (index !== -1) {
                            this.$set(this.tabData[tabKey], index, updatedData)
                        }
                    }
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                }
            },
            startPolling () {
                // 先立即执行一次
                this.fetchExecutionStatus()
                // 然后启动定时轮询
                this.pollingTimer = setInterval(() => {
                    this.fetchExecutionStatus()
                }, this.pollingInterval)
            },
            stopPolling () {
                if (this.pollingTimer) {
                    clearInterval(this.pollingTimer)
                    this.pollingTimer = null
                }
            },
            /**
             * 开始重试轮询（立即执行一次，之后每5s一次）
             */
            startRetryPolling () {
                this.stopRetryPolling()
                this.isRetryPolling = true
                this.executeRetryPolling()
            },
            /**
             * 执行重试轮询：刷新全部数据，根据 taskData.status 决定是否停止
             * 轮询期间不通知父组件更新 taskData，避免页面跳转；轮询结束后再通知
             */
            async executeRetryPolling () {
                if (!this.isRetryPolling) return

                try {
                    // 1. 更新 completedTabs 和 pipelineStatusSummary
                    await this.fetchExecuteSummary()

                    // 2. 获取 taskData
                    const taskData = await this.getCopyTaskDetail({
                        projectId: this.projectId,
                        taskId: this.taskId
                    })
                    const status = taskData?.status
                    this.updateTaskResult(status)

                    // 3. 更新流水线表格数据
                    if (this.$refs.tabComponent && typeof this.$refs.tabComponent.fetchPipelineList === 'function') {
                        await this.$refs.tabComponent.fetchPipelineList()
                    }

                    // 4. 根据状态判断是否停止轮询
                    if (this.isTaskFinished(status)) {
                        this.stopRetryPolling()
                        // 轮询结束，通知父组件更新 taskData
                        this.$emit('update-task-data', taskData)
                        return
                    }

                    // 5. 继续轮询
                    this.retryPollingTimer = setTimeout(() => {
                        this.executeRetryPolling()
                    }, 5000)
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                    // 即使出错也继续轮询
                    this.retryPollingTimer = setTimeout(() => {
                        this.executeRetryPolling()
                    }, 5000)
                }
            },
            /**
             * 停止重试轮询
             */
            stopRetryPolling () {
                if (this.retryPollingTimer) {
                    clearTimeout(this.retryPollingTimer)
                    this.retryPollingTimer = null
                }
                this.isRetryPolling = false
            },
            /**
             * 获取执行状态
             */
            async fetchExecutionStatus () {
                this.tableLoading = true
                try {
                    const baseParams = {
                        projectId: this.projectId,
                        taskId: this.taskId
                    }
                    const [progressData, detailsData] = await Promise.all([
                        this.getExecuteProgress(baseParams),
                        this.getTaskDetails({
                            ...baseParams,
                            params: {
                                page: this.pagination.current,
                                pageSize: this.pagination.limit
                            }
                        })
                    ])
                    this.progressInfo = progressData
                    this.pipelineList = detailsData.records || []
                    this.pagination.count = detailsData.count || 0
                    
                    const status = progressData.status
                    if (this.isTaskFinished(status)) {
                        this.stopPolling()
                        this.localExecutionStatus = 'completed'
                        this.updateTaskResult(status)
                        // 任务完成后获取执行汇总数据更新 completedTabs
                        this.fetchExecuteSummary()
                    }
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                } finally {
                    this.tableLoading = false
                }
            },
            /**
             * 获取执行汇总数据
             */
            async fetchExecuteSummary () {
                try {
                    const data = await this.getExecuteSummary({
                        projectId: this.projectId,
                        taskId: this.taskId
                    })
                    
                    const countMap = {
                        'PIPELINE': data?.pipelineCount,
                        'NEED_COMPLETION': data?.needCompletionCount,
                        'NEED_TRANSFER': data?.needTransferCount,
                        'AUTO_FINISH': data?.autoFinishCount
                    }
                    this.completedTabs = this.completedTabs.map(tab => ({
                        ...tab,
                        totalCount: countMap[tab.key] ?? tab.totalCount
                    }))

                    // 将汇总数据传递给父组件，用于右侧任务指引面板展示
                    this.$emit('update-execution-summary', {
                        pipelineCount: data?.pipelineCount ?? 0,
                        needCompletionCount: data?.needCompletionCount ?? 0,
                        needTransferCount: data?.needTransferCount ?? 0,
                        autoFinishCount: data?.autoFinishCount ?? 0
                    })

                    await this.fetchTaskStatusSummary()
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                }
            },
            /**
             * 获取流水线状态汇总
             */
            async fetchTaskStatusSummary () {
                try {
                    const data = await this.getTaskStatusSummary({
                        projectId: this.projectId,
                        taskId: this.taskId
                    })
                    const statusList = data || []
                    const successItem = statusList.find(item => item.status === 'SUCCESS')
                    const failedItem = statusList.find(item => item.status === 'FAILED')
                    const successCount = successItem?.count || 0
                    const failedCount = failedItem?.count || 0
                    
                    this.pipelineStatusSummary = {
                        successCount,
                        failedCount,
                        totalCount: successCount + failedCount
                    }
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                }
            },
            /**
             * 获取资源数据
             */
            async fetchResourceData () {
                try {
                    const data = await this.listResourceDetails({
                        projectId: this.projectId,
                        taskId: this.taskId,
                        params: {
                            copyAction: this.activeTab
                        }
                    })
                    
                    let resourceList
                    
                    // AUTO_FINISH 不需要扁平化,直接使用返回数据
                    if (this.activeTab === 'AUTO_FINISH') {
                        resourceList = Array.isArray(data) ? data : []
                    } else {
                        // 其他 tab 需要将每个对象的 resources 扁平化拼在一起
                        const list = Array.isArray(data) ? data : []
                        resourceList = list.reduce((acc, item) => {
                            if (Array.isArray(item.resources)) {
                                acc.push(...item.resources)
                            }
                            return acc
                        }, [])
                    }
                    
                    // 根据当前 activeTab 存储到对应的 tabData 中
                    this.$set(this.tabData, this.activeTab, resourceList)
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                }
            },
            getStatusText (status) {
                const statusMap = {
                    [PipelineBatchTaskDetailStatus.EXCLUDED]: this.$t('excluded'),
                    [PipelineBatchTaskDetailStatus.SUCCESS]: this.$t('success'),
                    [PipelineBatchTaskDetailStatus.FAILED]: this.$t('failed'),
                    [PipelineBatchTaskDetailStatus.WAIT_COPY]: this.$t('pendingCopy')
                }
                return statusMap[status] || '-'
            },
            handleTabChange (tabKey) {
                this.activeTab = tabKey
                this.$router.replace({
                    query: {
                        ...this.$route.query,
                        execTab: tabKey
                    }
                })
                
                const tabsNeedResource = ['NEED_COMPLETION', 'NEED_TRANSFER', 'AUTO_FINISH']
                if (tabsNeedResource.includes(tabKey) && !this.tabData[tabKey]) {
                    this.fetchResourceData()
                }
            },
            /**
             * 分页页码变化
             */
            handlePageChange (page) {
                this.pagination.current = page
            },
            /**
             * 分页每页条数变化
             */
            handlePageLimitChange (limit) {
                this.pagination.current = 1
                this.pagination.limit = limit
            },
            handleJump (item) {
                const { resourceType, resourceId, resourceName } = item
                const projectId = this.projectId
                let url = ''
                switch (resourceType) {
                    case 'PIPELINE_TEMPLATE':
                        url = `/console/pipeline/${projectId}/template/${resourceId}`
                        break
                    case 'REPOSITORY':
                        url = `/console/codelib/${projectId}/?id=${resourceId}&searchName=${resourceName}`
                        break
                    case 'BUILD_ENV':
                    case 'DEPLOY_ENV':
                        url = `/console/environment/${projectId}/pipeline/env/ALL/${resourceId}/node`
                        break
                    case 'BUILD_NODE':
                    case 'DEPLOY_NODE':
                        url = `/console/environment/${projectId}/pipeline/node/allNode?nodeHashId=${resourceId}`
                        break
                    case 'CREDENTIAL':
                        url = `/console/ticket/${projectId}`
                        break
                }
                if (url) {
                    window.open(url, '_blank')
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
.task-execution-content {
    
    // 状态一：确认执行任务
    .task-confirm-section {
        box-shadow: 0 2px 4px 0 #1919290d;
        background: white;
        padding: 16px 24px 24px;
        .header-title {
            display: flex;
            align-items: center;
            margin-bottom: 8px;
            font-size: 14px;
            font-weight: 700;
            color: #313238;
        }

        .header-desc {
            font-size: 12px;
            color: #979BA5;
            line-height: 20px;
        }

        .content {
            display: flex;
            padding: 12px 0;
            border-radius: 2px;
            background: #F5F7FA;

            .detail-item {
                display: flex;
                padding: 12px 32px;
                flex-direction: column;
                flex: 1 0 0;
                border-right: 1px solid #DCDEE5;
                font-size: 12px;
                color: #979BA5;
                gap: 8px;

                &:last-child {
                    border-right: none;
                }

                p {
                    display: flex;
                    justify-content: space-between;
                    padding: 8px 0;
                }

                .value {
                    color: #313238;
                }

                .high-risk {
                    color: #E38B02;
                    font-weight: 700;

                    svg {
                        color: #3A84FF;
                        margin-left: 8px;
                    }
                }

                .auto-execute {
                    border-bottom: 1px dashed #313238;
                }
            }
        }

        .confirm-checkbox {
            margin-top: 16px;
            font-size: 12px;
        }
    }
    
    // 执行按钮
    .action-buttons {
        display: flex;
        justify-content: flex-start;
        gap: 8px;
        margin-top: 24px;
    }

    // 状态二:执行中
    .task-executing-section {
        background: white;
        padding: 16px 24px 24px;
        // 执行头部
        .executing-header {
            width: 60%;
            display: flex;
            align-items: center;
            margin: 0 auto 24px;
            gap: 24px;
            
            .header-right {
                flex: 1;

                .header-info {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    margin-bottom: 12px;

                    .header-title {
                        font-size: 20px;
                        color: #313238;
                        margin-bottom: 8px;
                    }
                    
                    .header-stats {
                        display: flex;
                        align-items: center;
                        gap: 16px;
                        font-size: 14px;
                        color: #63656E;
                        
                        .progress-text {
                            font-size: 14px;
                            color: #4D4F56;
                        }
                        
                        .divider {
                            color: #DCDEE5;
                        }
                        
                        .estimate-time {
                            color: #979BA5;
                        }
                    }
                }

                // 进度条
                .progress-bar-container {
                    margin-bottom: 15px;
                    
                    .progress-bar-wrapper {
                        width: 100%;
                        height: 8px;
                        background: #F0F1F5;
                        border-radius: 4px;
                        overflow: hidden;
                        
                        .progress-bar-fill {
                            height: 100%;
                            background: linear-gradient(270deg, #69D7DC 11.81%, #699DF4 80.35%);
                            border-radius: 4px;
                            transition: width 0.3s ease;
                        }
                    }
                }
                
            }
        }
        
        // 流水线列表表格
        .pipeline-list-table {
            .pac-tag {
                background: #E1ECFF;
                border-radius: 12px;
                width: 60px;
                height: 22px;
                line-height: 1;
                display: grid;
                align-items: center;
                grid-auto-flow: column;
                font-size: 12px;
                color: #699DF4;
                cursor: pointer;
                
                &:hover {
                    color: #3A84FF;
                    .devops-icon {
                        background: #3A84FF;
                    }
                }
                
                .devops-icon {
                    width: 20px;
                    height: 20px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    background: #699DF4;
                    color: white;
                    border-radius: 50%;
                    flex-shrink: 0;
                }
            }

            .draft-tag {
                flex-shrink: 0;
            }

            .sub-tag {
                padding: 2px 6px;
                border-radius: 2px;
                font-size: 10px;
                color: #1768EF;
                background: #E1ECFF;
            }
            
            .template-tag {
                padding: 2px 6px;
                border-radius: 2px;
                font-size: 10px;
                color: #4D4F56;
                border: 1px solid #DCDEE5;
                background-color: #F0F1F5;
            }
            
            .pipeline-name-cell {
                display: flex;
                align-items: center;
                gap: 8px;

                .pipeline-name-text {
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                }

                .bk-tag {
                    flex-shrink: 0;
                }
            }

            .status-text {
                display: inline-flex;
                align-items: center;
                gap: 8px;
                font-size: 12px;
                color: #4D4F56;

                &::before {
                    content: '';
                    display: inline-block;
                    width: 8px;
                    height: 8px;
                    border-radius: 50%;
                    border: 1px solid transparent;
                }

                &--WAIT_COPY::before {
                    background-color: #FDF4E8;
                    border-color: #F59500;
                }

                &--EXCLUDED::before {
                    background-color: #F5F7FA;
                    border-color: #C4C6CC;
                }

                &--SUCCESS::before {
                    background-color: #DAF6E5;
                    border-color: #2CAF5E;
                }

                &--FAILED::before {
                    background-color: #FFEBEB;
                    border-color: #EA3636;
                }
            }
            
            .fail-reason {
                cursor: pointer;
                font-size: 12px;
                color: #E71818;
                background-color: #FFEBEB;
                padding: 2px 8px;
                border-radius: 2px;
                margin-left: 24px;
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

    // 状态三：执行完成
    .task-completed-section {
        background: white;
        padding: 16px 24px 24px;
        
        // 状态提示
        .task-result-alert {
            display: flex;
            align-items: center;
            margin-bottom: 16px;
            gap: 16px;
            padding: 12px 16px;
            border-radius: 2px;

            &.is-failed {
                border: 1px solid #F9D090;
                background: #FDF4E8;
            }

            &.is-success {
                background-color: #EBFAF0;
                border: 1px solid #A1E3BA;

                .success-icon {
                    color: #2CAF5E;
                    background-color: #CBF0DA;
                    border-radius: 50%;
                    font-size: 8px;
                    padding: 8px;
                    flex-shrink: 0;
                    font-weight: 700;
                }
            }

            .info-icon {
                display: inline-flex;
                align-items: center;
                justify-content: center;
                background-color: #FCE5C0;
                color: #F59500;
                width: 28px;
                height: 28px;
                font-size: 12px;
                border-radius: 50%;
                flex-shrink: 0;
            }

            .partial {
                display: flex;
            }

            .task-result-title {
                flex: 1;
                align-items: center;
                justify-content: space-between;

                .title-text {
                    color: #313238;
                    font-size: 16px;
                }

                .title-desc {
                    color: #979BA5;
                    font-size: 12px;
                    margin-top: 6px;
                }

                .title-button {
                    color: #3a84ff;
                    font-size: 14px;
                    cursor: pointer;
                }
            }
        }
        
        // 流水线列表区域
        .pipeline-list-section {
            // 功能切换卡片
            .tab-cards {
                display: flex;
                gap: 16px;
                align-items: center;
                margin-bottom: 16px;
                
                .tab-card {
                    display: flex;
                    flex: 1;
                    align-items: center;
                    padding: 16px;
                    border: none;
                    cursor: pointer;
                    border-radius: 4px;
                    transition: all 0.3s ease;
                    justify-content: space-between;
                    border-bottom: 3px solid transparent;
                    
                    .tab-content {
                        display: flex;
                        flex: 1;
                        align-items: center;
                        gap: 8px;

                        p {
                            display: flex;
                            align-items: center;
                        }

                        .tab-label {
                            flex: 1;
                            font-size: 14px;
                            color: #4D4F56;
                            margin-left: 8px;
                            font-size: 12px;
                            transition: color 0.3s ease;
                        }
                        
                    }

                    .total-count {
                        font-size: 10px;
                        color: #4D4F56;
                        padding: 2px 6px;
                        border-radius: 8px;
                        background-color: #FFFFFF;
                    }

                    .tab-counts {
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        height: 16px;
                        line-height: 16px;
                        border-radius: 20px;
                        background-color: #FFFFFF;
                        font-size: 10px;


                        .tab-count {
                            color: #4D4F56;
                            padding: 0 6px;
                            border-radius: 20px;
                            background-color: #F0F1F5;
                        }

                        .sub-count {
                            color: #E38B02;
                            padding: 0 6px;
                            border-radius: 20px;
                            background-color: #FFFFFF;
                        }
                    }

                    
                    .tab-sub-counts {
                        display: flex;
                        gap: 8px;
                        margin-top: 8px;
                        padding-left: 28px;
                        
                        .sub-count {
                            display: inline-flex;
                            align-items: center;
                            min-width: 24px;
                            height: 20px;
                            padding: 0 6px;
                            background: #FFFFFF;
                            border: 1px solid #DCDEE5;
                            border-radius: 2px;
                            font-size: 12px;
                            color: #63656E;
                        }
                    }
                    
                    // 流水线 tab
                    &.tab-PIPELINE {
                        background: #F0F2F9;
                        
                        &:hover .tab-label {
                            color: #3764DC;
                        }
                        
                        &.active {
                            background: #F0F2F9;
                            border-bottom-color: #3764DC;
                            
                            .tab-content {
                                .tab-icon {
                                    color: #3764DC;
                                }
                            }
                        }
                    }
                    
                    // 待补齐资源 tab
                    &.tab-NEED_COMPLETION {
                        background: #F9F6EB;
                        
                        &:hover .tab-label {
                            color: #D7A500;
                        }
                        
                        &.active {
                            background: #F9F6EB;
                            border-bottom-color: #EBB401;
                            
                            .tab-content {
                                .tab-icon {
                                    color: #EBB401;
                                }
                            }
                        }
                    }
                    
                    // 资源转移处理事项 tab
                    &.tab-NEED_TRANSFER {
                        background: #FDF2F4;
                        
                        &:hover .tab-label {
                            color: #D75573;
                        }
                        
                        &.active {
                            background: #FDF2F4;
                            border-bottom-color: #D75573;
                            
                            .tab-content {
                                .tab-icon {
                                    color: #D75573;
                                }
                            }
                        }
                    }
                    
                    // 自动完成 tab
                    &.tab-AUTO_FINISH {
                        background: #F0F5F9;
                        
                        &:hover .tab-label {
                            color: #559BD2;
                        }
                        
                        &.active {
                            background: #F0F5F9;
                            border-bottom-color: #559BD2;
                            
                            .tab-content {
                                .tab-icon {
                                    color: #559BD2;
                                }
                            }
                        }
                    }
                    
                    // 禁用状态：count 为 0
                    &.disabled {
                        background: #F5F7FA;
                        cursor: not-allowed;
                        
                        .tab-content {
                            color: #979BA5;
                            
                            .tab-label {
                                color: #979BA5;
                            }
                            
                            .tab-icon {
                                color: #979BA5;
                            }
                        }
                        
                        .total-count {
                            background-color: #F0F1F5;
                            color: #979BA5;
                        }
                        
                        .tab-counts {
                            .tab-count {
                                color: #979BA5;
                                background-color: #F0F1F5;
                            }
                            
                            .sub-count {
                                color: #979BA5;
                                background-color: #F0F1F5;
                            }
                        }
                        
                        &:hover {
                            background: #F5F7FA;
                            
                            .tab-label {
                                color: #979BA5;
                            }
                        }
                    }
                }
            }
        }
    }
}
</style>

<style lang="scss">
.pipeline-error-popover {

    .error-type {
        cursor: pointer;
    }

    .error-item {
        margin-bottom: 8px;
    }

    .error-item-title {
        color: #313238;
    }

    .jump-icon {
        margin: 0 4px;
    }

}
</style>