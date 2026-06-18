<template>
    <div
        class="cross-project-copy"
        ref="pageWrapper"
    >
        <!-- 头部区域 -->
        <div
            class="sticky-header"
            :class="{ 'is-sticky': isSticky }"
        >
            <div class="header-full">
                <div class="header-full__top">
                    <div class="task-name">
                        <span @click="handleCancel">
                            <Logo
                                name="arrows-left"
                                class="task-name__logo"
                                size="18"
                            />
                        </span>
                        <span class="task-name__text">{{ taskName }}</span>
                        <span
                            v-if="taskStatusText"
                            class="task-name__status"
                        >{{ taskStatusText }}</span>
                    </div>
                    <!-- <span class="auto-save-time">{{ $t('autoSave') }} {{ autoSaveTime }}</span> -->
                </div>
                <div class="header-full__project">
                    <p class="project-info">
                        <span class="source-project">{{ $t('source') }}</span>
                        <span class="project-name">{{ taskData?.projectId || '--' }}</span>
                        <Logo
                            name="arrows-right"
                            size="12"
                        />
                        <span class="target-project">{{ $t('target') }}</span>
                        <span
                            class="project-name"
                            :class="{ 'is-empty': !targetProjectName }"
                        >
                            {{ targetProjectName || $t('notSelected') }}
                        </span>
                    </p>
                    <span class="pipeline-count">{{ $t('totalPipelines', [(taskData?.pipelineCount) || 0]) }}</span>
                </div>
            </div>
            <div
                class="header-full__meta"
                v-show="!isSticky"
            >
                <span class="meta-item">
                    <span class="meta-label">ID：</span>
                    <span>{{ (taskData?.taskId) || '--' }}</span>
                </span>
                <span class="meta-separator">|</span>
                <span class="meta-item">
                    <span class="meta-label">{{ $t('taskType') }}：</span>
                    <span>{{ $t('crossProjectCopy') }}</span>
                </span>
                <span class="meta-separator">|</span>
                <span class="meta-item">
                    <span class="meta-label">{{ $t('submitter') }}：</span>
                    <span>{{ (taskData?.creator) || '--' }}</span>
                </span>
            </div>
        </div>

        <!-- 步骤条 -->
        <div
            class="step-bar"
            :class="{ 'is-sticky': isSticky }"
        >
            <div
                v-for="(step, index) in steps"
                :key="step.name"
                class="step-item"
                :class="{
                    'is-completed': step.completed,
                    'is-current': index === currentStepIndex
                }"
                @click="handleStepClick(step, index)"
            >
                <div class="step-circle">
                    {{ index + 1 }}
                </div>
                <div class="step-info">
                    <div class="step-title">{{ step.title }}</div>
                    <div class="step-desc">{{ step.desc }}</div>
                </div>
            </div>
        </div>
        <!-- 主内容区 -->
        <div
            class="main-content"
            :style="{ 'padding-bottom': footerHeight }"
        >
            <!-- 左侧内容区 -->
            <div
                class="left-content"
                :style="{ background: currentStepIndex < 2 ? 'white' : 'none' }"
                v-bkloading="{ isLoading: savingDraft }"
                ref="leftContent"
            >
                <component
                    :is="currentTabComponent"
                    :key="currentStepIndex"
                    :config-scope-data="configScopeData"
                    :task-data="taskData"
                    :analyzing-pipeline="analyzingPipeline"
                    :is-read-only="isReadOnly"
                    @update-form-data="handleUpdateFormData"
                    @update-loading-state="handleUpdateLoadingState"
                    @update-validation-data="handleUpdateValidationData"
                    @update-execution-summary="handleUpdateExecutionSummary"
                    @update-task-data="handleUpdateTaskData"
                    @prev-step="handlePrev"
                    @cancel="handleCancel"
                    ref="currentComponent"
                />
            </div>

            <!-- 右侧提交检查/任务摘要面板 -->
            <div
                class="right-sidebar"
                :class="{ 'is-sticky': isSticky }"
            >
                <template v-if="currentStepIndex < 2">
                    <!-- 提交检查区域 -->
                    <div
                        v-if="!loadingStatus && !isReadOnly"
                        class="sidebar-section submit-check-section"
                    >
                        <div class="section-header">
                            <h3 class="section-title">{{ $t('submitCheck') }}</h3>
                            <span
                                v-if="errorValidationCount > 0"
                                class="error-count-badge"
                            >
                                {{ $t('waitingForHandle', [errorValidationCount]) }}
                            </span>
                        </div>
                        
                        <!-- 检查项列表 -->
                        <div
                            class="check-list"
                        >
                            <div
                                v-for="(item, index) in validationItems"
                                :key="index"
                                :class="['check-item', `check-item-${item.type}`]"
                                @click="handleErrorClick(item)"
                            >
                                <i
                                    v-if="item.type === 'success'"
                                    class="devops-icon icon-check-1 success-icon"
                                />
                                <i
                                    v-else
                                    class="devops-icon icon-close-small error-icon"
                                />
                                <span
                                    :class="item.type === 'success' ? 'success-text' : 'error-text'"
                                >{{ item.message }}</span>
                            </div>
                        </div>
                    </div>

                    <!-- 任务摘要区域 -->
                    <div class="sidebar-section task-summary-section">
                        <h3 class="section-title">{{ $t('taskSummary') }}</h3>
                        
                        <div class="summary-list">
                            <div class="summary-item">
                                <span class="summary-label">{{ $t('sourceProject') }}</span>
                                <span class="summary-value">{{ taskData?.projectId || '--' }}</span>
                            </div>
                            <div class="summary-item">
                                <span class="summary-label">{{ $t('targetProject') }}</span>
                                <span
                                    class="summary-value"
                                    :class="{ 'is-error': !targetProjectName }"
                                >
                                    {{ targetProjectName || $t('notSelected') }}
                                </span>
                            </div>
                            <hr style="border: 0; border-top: 1px solid #DCDEE5;" />
                            <div class="summary-item">
                                <span class="summary-label">{{ $t('pipelineCopyCount') }}</span>
                                <span class="summary-value"><span class="number-bold">{{ (taskData?.pipelineCount) || 0 }}</span> {{ $t('strip') }}</span>
                            </div>
                            <div class="summary-item">
                                <span class="summary-label">{{ $t('autoAddSubPipeline') }}</span>
                                <span class="summary-value"><span class="number-bold">{{ (taskData?.subPipelineCount) || 0 }}</span> {{ $t('strip') }}</span>
                            </div>
                            <div class="summary-item">
                                <span class="summary-label">{{ $t('autoRemovePacPipeline') }}</span>
                                <span class="summary-value"><span class="number-bold">{{ (taskData?.pacCount) || 0 }}</span> {{ $t('strip') }}</span>
                            </div>
                            <div class="summary-item">
                                <span class="summary-label">{{ $t('pipelineIdStrategy') }}</span>
                                <span class="summary-value">{{ pipelineCopyStrategyText }}</span>
                            </div>
                            <template v-if="!isReadOnly">
                                <div class="summary-item">
                                    <span class="summary-label">{{ $t('unhandledResources') }}</span>
                                    <span
                                        class="summary-value"
                                        :class="{ 'is-warning': currentUnprocessedCount > 0 }"
                                    >
                                        <span class="number-bold">{{ currentUnprocessedCount }}</span>
                                    </span>
                                </div>
                                <div class="summary-item">
                                    <span class="summary-label">{{ $t('highRiskOperations') }}</span>
                                    <span
                                        class="summary-value"
                                        :class="{ 'is-success': currentHighRiskCount === 0 }"
                                    >
                                        <span
                                            class="number-bold high-risk-number"
                                            v-if="currentHighRiskCount !== 0"
                                        >{{ currentHighRiskCount }}</span>
                                        <span
                                            class="none-high-risk"
                                            v-else
                                        >{{ $t('none') }}</span>
                                    </span>
                                </div>
                                <div class="summary-item">
                                    <span class="summary-label">{{ $t('autoExecuteAfterCopy') }}</span>
                                    <span
                                        class="summary-value"
                                        v-if="taskData?.autoFinishCount"
                                    >
                                        <span class="number-bold">{{ taskData?.autoFinishCount }}</span>
                                        {{ $t('strip') }}
                                    </span>
                                    <span v-else>--</span>
                                </div>
                            </template>
                        </div>
                    </div>
                </template>
                <template v-else>
                    <!-- 任务执行中：展示通用指引 -->
                    <div
                        v-if="!isTaskCompleted"
                        class="sidebar-section task-execution"
                    >
                        <div class="section-title">{{ $t('taskExecutionGuidance') }}</div>
                        <div
                            v-for="(item, index) in taskExecutionGuidance"
                            :key="index"
                            class="task-executionlist"
                        >
                            <Logo
                                name="guidance-arrow"
                                size="16"
                            />
                            <p>
                                <span class="title">{{ item.title }}</span>
                                <span>{{ item.desc }}</span>
                            </p>
                        </div>
                    </div>
                    <!-- 任务已完成：展示结果指引 -->
                    <div
                        v-else
                        class="sidebar-section task-execution-result"
                    >
                        <div class="section-title">{{ $t('currentTaskGuidance') }}</div>
                        <!-- 补齐资源配置 -->
                        <div class="guidance-item">
                            <div class="guidance-header">
                                <Logo
                                    name="guidance-arrow"
                                    size="16"
                                />
                                <span class="guidance-title">{{ $t('completeResourceConfig') }}</span>
                            </div>
                            <p class="guidance-desc">
                                <template v-if="executionSummary.needCompletionCount > 0">
                                    {{ $t('needCompletionGuidance', [executionSummary.needCompletionCount]) }}
                                    <p
                                        class="view-resource-link"
                                        @click="handleViewResources('NEED_COMPLETION')"
                                    >
                                        {{ $t('viewResource') }}
                                    </p>
                                </template>
                                <template v-else>
                                    {{ $t('noNeedCompletionGuidance') }}
                                </template>
                            </p>
                        </div>
                        <!-- 资源转移处理事项 -->
                        <div class="guidance-item">
                            <div class="guidance-header">
                                <Logo
                                    name="guidance-arrow"
                                    size="16"
                                />
                                <span class="guidance-title">{{ $t('resourceTransferIssues') }}</span>
                            </div>
                            <p class="guidance-desc">
                                <template v-if="executionSummary.needTransferCount > 0">
                                    {{ $t('needTransferGuidance', [executionSummary.needTransferCount]) }}
                                    <p
                                        class="view-resource-link"
                                        @click="handleViewResources('NEED_TRANSFER')"
                                    >
                                        {{ $t('viewResource') }}
                                    </p>
                                </template>
                                <template v-else>
                                    {{ $t('noNeedTransferGuidance') }}
                                </template>
                            </p>
                        </div>
                        <!-- 到目标项目验证流水线 -->
                        <div class="guidance-item">
                            <div class="guidance-header">
                                <Logo
                                    name="guidance-arrow"
                                    size="16"
                                />
                                <span class="guidance-title">{{ $t('verifyPipelineInTarget') }}</span>
                            </div>
                            <p class="guidance-desc">{{ $t('verifyPipelineDesc') }}</p>
                        </div>
                    </div>
                </template>
            </div>
        </div>


        <!-- 底部操作栏（第三步时不显示，且只读状态下不显示） -->
        <div
            v-if="currentStepIndex < 2 && !isReadOnly"
            class="footer-actions"
        >
            <!-- 高风险操作确认（仅在资源依赖步骤且有高风险操作时显示） -->
            <div
                v-if="currentStepIndex === 1 && resourceValidationData.highRiskOperationCount > 0"
                class="high-risk-confirm"
            >
                <bk-checkbox v-model="highRiskConfirmed" />
                <i18n
                    path="highRiskConfirmText"
                    tag="span"
                    class="high-risk-confirm__text"
                >
                    <span class="high-risk-count">{{ resourceValidationData.highRiskOperationCount }}</span>
                    <span
                        class="high-risk-link"
                        @click="handleShowHighRiskDialog"
                    >{{ $t('highRiskOperations') }}</span>
                </i18n>
            </div>
            
            <div class="footer-buttons">
                <bk-popover
                    v-for="btn in footerButtons"
                    :key="btn.action"
                    :disabled="!btn.tooltip"
                    placement="top"
                >
                    <bk-button
                        :theme="btn.theme"
                        :disabled="btn.disabled"
                        @click="btn.handler"
                    >
                        {{ btn.text }}
                    </bk-button>
                    <div slot="content">{{ btn.tooltip }}</div>
                </bk-popover>
            </div>
        </div>
        
        <!-- 高风险操作提示弹窗 -->
        <bk-dialog
            v-model="highRiskDialog.visible"
            theme="primary"
            :mask-close="false"
            :esc-close="false"
            :width="640"
            footer-position="right"
            header-position="left"
            :title="$t('riskWarning')"
            class="high-risk-dialog"
        >
            <bk-tab
                v-if="highRiskDialog.visible"
                :key="highRiskTabs.map(t => t.type).join(',')"
                :active.sync="highRiskDialog.activeTab"
                type="unborder-card"
            >
                <bk-tab-panel
                    v-for="tab in highRiskTabs"
                    :key="tab.type"
                    :name="tab.type"
                    :label="tab.label"
                >
                    <RiskWarningContent
                        :resource-type="tab.type"
                        :resource-names="tab.resourceNames"
                    />
                </bk-tab-panel>
            </bk-tab>
            <template #footer>
                <bk-button
                    theme="primary"
                    @click="handleHighRiskDialogConfirm"
                >
                    {{ $t('IKnow') }}
                </bk-button>
            </template>
        </bk-dialog>
    </div>
</template>

<script>
    import ConfigScope from './ConfigScope.vue'
    import ResourceDependency from './ResourceDependency.vue'
    import TaskExecution from './TaskExecution.vue'
    import RiskWarningContent from './components/RiskWarningContent.vue'
    import Logo from '@/components/Logo'
    import { PipelineBatchTaskStep, PipelineBatchTaskStatus, PipelineIdStrategy, PipelineCopyResourceType, PipelineCopyStrategy } from '@/store/modules/crossProjectCopy/constants'
    import { mapActions } from 'vuex'

    export default {
        name: 'CrossProjectCopy',
        components: {
            ConfigScope,
            ResourceDependency,
            TaskExecution,
            RiskWarningContent,
            Logo,
        },
        data () {
            return {
                fromSource: '',
                // 吸顶相关
                isSticky: false,
                shouldEnableSticky: true,
                resizeObserver: null,
                // autoSaveTime: '--',
                // 轮询相关
                pollingTimer: null,
                isPolling: false,
                taskStatus: '',

                loadingStatus: false,
                // 分析中的 loading 状态（null: 未开始, true: 分析中, false: 分析完成）
                analyzingPipeline: null,
                savingDraft: false,
                taskData: null,
                resourceValidationData: {
                    pendingDependencyResourceCount: 0,
                    highRiskOperationCount: 0,
                    pendingConflictCount: 0
                },
                highRiskConfirmed: false,
                highRiskDialog: {
                    visible: false,
                    activeTab: ''
                },
                configScopeData: {
                    targetProjectId: '',
                    taskName: '',
                    pipelineCopyStrategy: ''
                },
                resourceData: [],
                executionSummary: {
                    pipelineCount: 0,
                    needCompletionCount: 0,
                    needTransferCount: 0,
                    autoFinishCount: 0
                },
                steps: [
                    {
                        name: PipelineBatchTaskStep.CONFIG,
                        title: this.$t('configScope'),
                        desc: this.$t('configScopeDesc'),
                        component: 'ConfigScope',
                        completed: false
                    },
                    {
                        name: PipelineBatchTaskStep.RESOURCE_DEPEND,
                        title: this.$t('resourceDependency'),
                        desc: this.$t('resourceDependencyDesc'),
                        component: 'ResourceDependency',
                        completed: false
                    },
                    {
                        name: PipelineBatchTaskStep.EXECUTE,
                        title: this.$t('taskExecution'),
                        desc: this.$t('taskExecutionDesc'),
                        component: 'TaskExecution',
                        completed: false
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
            taskName () {
                return this.configScopeData.taskName
                    || (this.taskData && this.taskData.taskName)
                    || this.$t('newCrossProjectCopyTask')
            },
            targetProjectName () {
                const targetProjectId = this.configScopeData.targetProjectId
                if (!targetProjectId) return ''
                
                const currentComponent = this.$refs.currentComponent
                if (currentComponent && currentComponent.projectList) {
                    const targetProject = currentComponent.projectList.find(p => p.id === targetProjectId)
                    return targetProject ? targetProject.name : targetProjectId
                }
                return targetProjectId
            },
            pipelineCopyStrategyText () {
                const strategy = this.configScopeData.pipelineCopyStrategy || (this.taskData && this.taskData.pipelineCopyStrategy)
                if (strategy === PipelineIdStrategy.PIPELINE_CREATE_NEW_ID) {
                    return this.$t('autoGenerateNewId')
                } else if (strategy === PipelineIdStrategy.PIPELINE_REUSE_SOURCE_ID) {
                    return this.$t('keepSourceId')
                }
                return '--'
            },
            taskStatusText () {
                const statusTextMap = {
                    [PipelineBatchTaskStatus.DRAFT]: this.$t('draft'),
                    [PipelineBatchTaskStatus.PIPELINE_ANALYZING]: this.$t('pipelineAnalyzing'),
                    [PipelineBatchTaskStatus.PIPELINE_RESOURCE_ANALYZING]: this.$t('pipelineResourceAnalyzing'),
                    [PipelineBatchTaskStatus.EXECUTING]: this.$t('executing'),
                    [PipelineBatchTaskStatus.SUCCESS]: this.$t('success'),
                    [PipelineBatchTaskStatus.FAILED]: this.$t('failed'),
                    [PipelineBatchTaskStatus.PARTIAL_FAILED]: this.$t('partialFailed')
                }
                return statusTextMap[this.taskStatus] || ''
            },
            isReadOnly () {
                const readOnlyStatuses = [
                    PipelineBatchTaskStatus.EXECUTING,
                    PipelineBatchTaskStatus.SUCCESS,
                    PipelineBatchTaskStatus.FAILED,
                    PipelineBatchTaskStatus.PARTIAL_FAILED
                ]
                return readOnlyStatuses.includes(this.taskStatus)
            },
            isTaskCompleted () {
                const completedStatuses = [
                    PipelineBatchTaskStatus.SUCCESS,
                    PipelineBatchTaskStatus.FAILED,
                    PipelineBatchTaskStatus.PARTIAL_FAILED
                ]
                return completedStatuses.includes(this.taskStatus)
            },
            currentStepIndex () {
                const tab = this.$route.params.tab
                const index = this.steps.findIndex(step => step.name === tab)
                return index !== -1 ? index : 0
            },
            activeTab () {
                return this.steps[this.currentStepIndex].name
            },
            // 未处理资源总数
            currentUnprocessedCount () {
                return this.resourceValidationData.pendingDependencyResourceCount ?? 0
            },
            // 高风险操作数量
            currentHighRiskCount () {
                return this.resourceValidationData.highRiskOperationCount ?? 0
            },
            currentTabComponent () {
                return this.steps[this.currentStepIndex].component
            },
            validationItems () {
                // 创建校验项的辅助函数
                const createValidationItem = (field, isValid, successKey, errorKey, stepIndex, errorParams = []) => ({
                    field,
                    message: isValid ? this.$t(successKey) : this.$t(errorKey, errorParams),
                    stepIndex,
                    type: isValid ? 'success' : 'error'
                })
                
                // 第一步：配置复制范围
                if (this.currentStepIndex === 0) {
                    const { targetProjectId, taskName, pipelineCopyStrategy } = this.configScopeData
                    
                    return [
                        createValidationItem('targetProjectId', !!targetProjectId, 'selectedTargetProject', 'notSelectedTargetProject', 0),
                        createValidationItem('taskName', !!(taskName && taskName.trim()), 'filledTaskName', 'notFilledTaskName', 0),
                        createValidationItem('pipelineCopyStrategy', !!pipelineCopyStrategy, 'selectedIdStrategy', 'notSelectedIdStrategy', 0)
                    ]
                }
                
                // 第二步：资源依赖
                if (this.currentStepIndex === 1) {
                    const { pendingDependencyResourceCount, highRiskOperationCount, pendingConflictCount } = this.resourceValidationData
                    const hasHighRiskOperation = highRiskOperationCount > 0
                    
                    return [
                        createValidationItem('dependencyResources', pendingDependencyResourceCount === 0, 'allResourcesHandled', 'unhandledResourceCount', 1, [pendingDependencyResourceCount]),
                        hasHighRiskOperation
                            ? createValidationItem('highRiskOperation', this.highRiskConfirmed, 'highRiskOperationConfirmed', 'highRiskOperationNotConfirmed', 1)
                            : createValidationItem('highRiskOperation', true, 'noHighRiskOperation', 'highRiskOperationNotConfirmed', 1),
                        createValidationItem('pipelineConflict', pendingConflictCount === 0, 'allConflictsHandled', 'unhandledConflictCount', 1, [pendingConflictCount])
                    ]
                }
                
                return []
            },
            // 待处理（error 类型）的校验项数量
            errorValidationCount () {
                return this.validationItems.filter(item => item.type === 'error').length
            },
            footerHeight () {
                if (this.isReadOnly) return '0'
                if (this.currentStepIndex === 2) return '24px'
                const hasHighRiskConfirm = this.currentStepIndex === 1 && this.resourceValidationData.highRiskOperationCount > 0
                return hasHighRiskConfirm ? '86px' : '48px'
            },
            // 任务执行指引列表
            taskExecutionGuidance () {
                return [
                    {
                        title: this.$t('canClosePage'),
                        desc: this.$t('canClosePageDesc')
                    },
                    {
                        title: this.$t('canRetryAfterFailure'),
                        desc: this.$t('canRetryAfterFailureDesc')
                    },
                    {
                        title: this.$t('verifyAfterCompletion'),
                        desc: this.$t('verifyAfterCompletionDesc')
                    }
                ]
            },
            // 高风险操作tabs配置
            highRiskTabs () {
                const resourceData = this.resourceData
                if (!resourceData || !Array.isArray(resourceData)) return []
                
                // 高风险策略配置，与 ResourceDependency 中 HIGH_RISK_STRATEGIES 保持一致
                const highRiskConfig = [
                    { type: PipelineCopyResourceType.BUILD_ENV, strategy: PipelineCopyStrategy.BUILD_ENV_CREATE_AND_MOVE_NODE, label: this.$t('buildEnvironment') },
                    { type: PipelineCopyResourceType.BUILD_NODE, strategy: PipelineCopyStrategy.BUILD_NODE_MOVE_TO_TARGET_PROJECT, label: this.$t('buildNode') },
                    { type: PipelineCopyResourceType.DEPLOY_ENV, strategy: PipelineCopyStrategy.DEPLOY_ENV_CREATE_AND_MOVE_NODE, label: this.$t('deployEnvironment') },
                    { type: PipelineCopyResourceType.DEPLOY_NODE, strategy: PipelineCopyStrategy.DEPLOY_NODE_MOVE_TO_TARGET_PROJECT, label: this.$t('deployNode') },
                    { type: PipelineCopyResourceType.CREDENTIAL, strategy: PipelineCopyStrategy.CREDENTIAL_CREATE_NEW, label: this.$t('credential') }
                ]
                
                // 将 resourceData 数组转为以 resourceType 为 key 的映射
                const resourceDataMap = {}
                resourceData.forEach(item => {
                    resourceDataMap[item.resourceType] = item
                })
                
                const tabs = []
                // 只显示有高风险操作的资源类型
                highRiskConfig.forEach(config => {
                    const resources = resourceDataMap[config.type]?.resources || []
                    const highRiskList = resources.filter(item => item.copyStrategy === config.strategy)
                    
                    if (highRiskList.length > 0) {
                        tabs.push({
                            type: config.type,
                            label: `${config.label} ${highRiskList.length}`,
                            resourceNames: highRiskList.map(item => item.resourceName)
                        })
                    }
                })
                return tabs
            },
            footerButtons () {
                const buttons = []
                
                // 上一步按钮
                if (this.currentStepIndex > 0) {
                    buttons.push({
                        action: 'prev',
                        theme: 'default',
                        disabled: this.loadingStatus,
                        text: this.$t('previousStep'),
                        handler: this.handlePrev
                    })
                }
                
                // 下一步按钮（只在前两步显示）
                let isDisabled = false
                let tooltip = ''
                
                // 第一步：检查必填项
                if (this.currentStepIndex === 0) {
                    const hasTargetProject = !!this.configScopeData.targetProjectId
                    const hasTaskName = !!(this.configScopeData.taskName && this.configScopeData.taskName.trim())
                    const hasPipelineIdStrategy = !!this.configScopeData.pipelineCopyStrategy
                    
                    isDisabled = !hasTargetProject || !hasTaskName || !hasPipelineIdStrategy
                    
                    if (!hasTargetProject) {
                        tooltip = this.$t('pleaseSelectTargetProjectFirst')
                    } else if (!hasTaskName) {
                        tooltip = this.$t('pleaseEnterTaskName')
                    } else if (!hasPipelineIdStrategy) {
                        tooltip = this.$t('pleaseSelectPipelineIdStrategy')
                    }
                } else if (this.currentStepIndex === 1) {
                    // 第二步：检查资源依赖是否全部处理完成
                    const hasPendingDependency = this.resourceValidationData.pendingDependencyResourceCount > 0
                    const hasPendingConflict = this.resourceValidationData.pendingConflictCount > 0
                    const hasHighRiskOperation = this.resourceValidationData.highRiskOperationCount > 0
                    const needHighRiskConfirm = hasHighRiskOperation && !this.highRiskConfirmed
                    isDisabled = hasPendingDependency || hasPendingConflict || needHighRiskConfirm
                    
                    if (hasPendingDependency) {
                        tooltip = this.$t('pendingDependencyResourceTooltip', [this.resourceValidationData.pendingDependencyResourceCount])
                    } else if (hasPendingConflict) {
                        tooltip = this.$t('pendingConflictTooltip', [this.resourceValidationData.pendingConflictCount])
                    } else if (needHighRiskConfirm) {
                        tooltip = this.$t('highRiskOperationTooltip', [this.resourceValidationData.highRiskOperationCount])
                    }
                }
                
                buttons.push({
                    action: 'next',
                    theme: 'primary',
                    text: this.currentStepIndex === 0
                        ? this.$t('nextStepToResourceDependency')
                        : this.$t('nextStepToTaskExecution'),
                    disabled: isDisabled,
                    tooltip: tooltip,
                    handler: this.handleNext
                })
                
                buttons.push({
                    action: 'saveDraft',
                    theme: 'default',
                    text: this.$t('saveDraft'),
                    handler: this.handleSaveDraft
                })
                
                buttons.push({
                    action: 'cancel',
                    theme: 'default',
                    text: this.$t('cancel'),
                    handler: this.handleCancel
                })
                
                return buttons
            }
        },
        watch: {
            '$route': {
                immediate: true,
                handler (to, from) {
                    const newTab = to.params.tab
                    // 仅当 params.tab 发生变化时执行
                    if (from && newTab === from.params.tab) return
                    const index = this.steps.findIndex(t => t.name === newTab)
                    
                    if (index !== -1) {
                        // 仅在页面初始进入时（!from），初始化 completed 状态
                        if (!from) {
                            this.initStepsCompleted(index)
                        }
                        
                        this.stopPolling()
                        
                        this.$nextTick(() => {
                            this.setupResizeObserver()
                            this.checkContentHeight()
                        })
                        
                        // 所有步骤统一使用轮询获取数据
                        this.startPolling()
                    }
                }
            },
            // 监听高风险操作数量变化，数量变化时重置确认状态
            'resourceValidationData.highRiskOperationCount' (newVal, oldVal) {
                if (oldVal !== undefined && newVal !== oldVal) {
                    this.highRiskConfirmed = false
                }
            }
        },
        mounted () {
            this.fromSource = sessionStorage.getItem('crossProjectCopyFrom')
            
            this.$nextTick(() => {
                this.checkContentHeight()
                this.setupResizeObserver()
                const el = this.$refs.leftContent
                if (el) {
                    el.addEventListener('scroll', this.handleScroll)
                }
            })
        },
        beforeDestroy () {
            const el = this.$refs.leftContent
            if (el) {
                el.removeEventListener('scroll', this.handleScroll)
            }
            if (this.resizeObserver) {
                this.resizeObserver.disconnect()
                this.resizeObserver = null
            }

            this.stopPolling()
            
            sessionStorage.removeItem('crossProjectCopyFrom')
        },
        methods: {
            ...mapActions('crossProjectCopy', [
                'getCopyTaskDetail',
                'analyzeResourceDepend',
                'saveConfigDraft',
                'saveResourceDraft',
                'prepareExecute'
            ]),
            async startPolling () {
                this.stopPolling()
                this.isPolling = true
                await this.pollTaskStatus()
            },
            stopPolling () {
                if (this.pollingTimer) {
                    clearTimeout(this.pollingTimer)
                    this.pollingTimer = null
                }
                this.isPolling = false
            },
            /**
             * 初始化步骤条状态
             * completed 表示是否已至该步骤（包括当前步骤及之前）
             * 仅在页面初始进入时调用
             * @param {number} currentIndex - 当前步骤索引
             */
            initStepsCompleted (currentIndex) {
                this.steps.forEach((step, index) => {
                    // 只读状态下，所有步骤都标记为已完成（因为流程已执行中或已完成）
                    if (this.isReadOnly) {
                        step.completed = true
                    } else {
                        step.completed = index <= currentIndex
                    }
                })
            },
            /**
             * 轮询任务状态（获取任务详情及状态）
             */
            async pollTaskStatus () {
                if (!this.isPolling) return
                
                try {
                    const taskData = await this.getCopyTaskDetail({ projectId: this.projectId, taskId: this.taskId})

                    this.taskData = taskData
                    this.taskStatus = taskData.status
                    
                    if (taskData.status === PipelineBatchTaskStatus.PIPELINE_ANALYZING || taskData.status === PipelineBatchTaskStatus.PIPELINE_RESOURCE_ANALYZING) {
                        this.analyzingPipeline = true
                        this.pollingTimer = setTimeout(() => {
                            this.pollTaskStatus()
                        }, 2000)
                    } else {
                        this.configScopeData = {
                            targetProjectId: taskData.targetProjectId,
                            taskName: taskData.taskName,
                            pipelineCopyStrategy: taskData.pipelineCopyStrategy
                        }
                        this.stopPolling()
                        this.analyzingPipeline = false
                        // 数据获取后，重新初始化步骤状态（确保 isReadOnly 正确）
                        if (this.isReadOnly) {
                            this.initStepsCompleted(this.currentStepIndex)
                        }
                    }
                } catch (error) {
                    this.stopPolling()
                    this.analyzingPipeline = false
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                }
            },
            handleUpdateFormData (field, value) {
                if (this.currentStepIndex === 0) {
                    this.configScopeData[field] = value
                } else if (field === 'resourceData') {
                    this.resourceData = value
                }
            },
            handleUpdateLoadingState (value) {
                this.loadingStatus = value
            },
            /**
             * 接收资源依赖步骤的校验数据
             */
            handleUpdateValidationData (data) {
                this.resourceValidationData = {
                    ...this.resourceValidationData,
                    ...data
                }
            },
            /**
             * 接收任务执行步骤的汇总数据
             */
            handleUpdateExecutionSummary (data) {
                this.executionSummary = {
                    ...this.executionSummary,
                    ...data
                }
            },
            /**
             * 处理任务数据更新
             */
            handleUpdateTaskData (taskData) {
                this.taskData = taskData
                this.taskStatus = taskData.status
            },
            /**
             * 点击"查看资源"链接，通知子组件切换对应 tab
             */
            handleViewResources (tabKey) {
                const currentComponent = this.$refs.currentComponent
                if (currentComponent && typeof currentComponent.handleTabChange === 'function') {
                    currentComponent.handleTabChange(tabKey)
                }
            },
            /**
             * 显示高风险操作提示弹窗
             */
            handleShowHighRiskDialog () {
                const firstTab = this.highRiskTabs[0]
                if (firstTab) {
                    this.highRiskDialog.activeTab = firstTab.type
                }
                this.highRiskDialog.visible = true
            },
            handleHighRiskDialogConfirm () {
                this.highRiskDialog.visible = false
            },
            /**
             * 设置ResizeObserver监听子组件高度变化
             */
            setupResizeObserver () {
                // 先断开旧的监听
                if (this.resizeObserver) {
                    this.resizeObserver.disconnect()
                }
                
                const currentComponent = this.$refs.currentComponent
                if (!currentComponent) return
                
                const componentEl = currentComponent.$el || currentComponent
                if (!componentEl) return
                
                // 创建ResizeObserver监听子组件高度变化
                this.resizeObserver = new ResizeObserver(() => {
                    this.checkContentHeight()
                })
                
                // 开始观察
                this.resizeObserver.observe(componentEl)
            },
            /**
             * 检测内容高度,判断是否需要启用吸顶效果
             * 逻辑:如果内容高度不足以产生滚动,则不启用吸顶
             */
            checkContentHeight () {
                const leftContent = this.$refs.leftContent
                const currentComponent = this.$refs.currentComponent
                if (!leftContent || !currentComponent) return
                
                // 获取容器当前可视高度
                const containerHeight = leftContent.clientHeight  // 容器当前可视高度
                
                // 获取子组件的实际高度
                const componentEl = currentComponent.$el || currentComponent
                const contentHeight = componentEl ? componentEl.offsetHeight : 0 // 子组件的实际高度
                
                const metaHeight = 48 // header-full__meta 的高度
                
                // 如果吸顶后(容器高度会增加 metaHeight),内容仍然小于容器
                // 则说明根本不会产生滚动,不需要启用吸顶效果
                this.shouldEnableSticky = contentHeight > (containerHeight + metaHeight)
                
                // 如果不应该启用吸顶,则重置状态
                if (!this.shouldEnableSticky) {
                    this.isSticky = false
                }
            },
            handleScroll () {
                // 如果不应该启用吸顶效果,直接返回
                if (!this.shouldEnableSticky) return
                
                const el = this.$refs.leftContent
                const scrollTop = el ? el.scrollTop : 0
                this.isSticky = scrollTop > 0
            },
            async handleNext () {
                // 校验当前步骤的表单（如果组件有 validate 方法）
                const currentComponent = this.$refs.currentComponent
                if (currentComponent && typeof currentComponent.validate === 'function') {
                    const isValid = await currentComponent.validate()
                    if (isValid === false) {
                        return
                    }
                }
                
                // 第一步：校验通过后，调用资源依赖分析接口
                if (this.currentStepIndex === 0) {
                    const success = await this.analyzeResource()
                    if (!success) return
                } else {
                    // 第二步：调用执行准备接口
                    const success = await this.prepareResourceExecute()
                    if (!success) return
                }
                
                // 执行下一步（通过路由跳转）
                if (this.currentStepIndex < this.steps.length - 1) {
                    this.initStepsCompleted(this.currentStepIndex + 1)
                    const nextStep = this.steps[this.currentStepIndex + 1]
                    this.$router.push({
                        name: 'crossProjectCopy',
                        params: {
                            projectId: this.projectId,
                            taskId: this.taskId,
                            tab: nextStep.name
                        }
                    })
                }
            },
            /**
             * 分析资源依赖
             */
            async analyzeResource () {
                try {
                    const result = await this.analyzeResourceDepend({
                        projectId: this.projectId,
                        taskId: this.taskId,
                        params: { ...this.configScopeData }
                    })
                    return result === true
                } catch (error) {
                    this.$bkMessage({ theme: 'error', message: error.message || error })
                    return false
                }
            },
            /**
             * 转换资源数据格式
             * 过滤出 resourceType 不是 PIPELINE_GROUP 和 PIPELINE_LABEL 的项
             * 把其他的每一项的 resources 组合在一起，放在 resources 数组中
             * PIPELINE_GROUP 和 PIPELINE_LABEL 这两项，只需要把每一项的 copyStrategy 找到传给 pipelineLabelCopyStrategy 和 pipelineGroupCopyStrategy
             */
            transformResourceData () {
                const result = {
                    pipelineLabelCopyStrategy: '',
                    pipelineGroupCopyStrategy: '',
                    resources: []
                }

                this.resourceData.forEach(item => {
                    if (item.resourceType === PipelineCopyResourceType.PIPELINE_LABEL) {
                        result.pipelineLabelCopyStrategy = item.copyStrategy || ''
                    } else if (item.resourceType === PipelineCopyResourceType.PIPELINE_GROUP) {
                        result.pipelineGroupCopyStrategy = item.copyStrategy || ''
                    } else {
                        if (item.resources && Array.isArray(item.resources)) {
                            result.resources.push(...item.resources)
                        }
                    }
                })

                return result
            },
            /**
             * 执行准备
             */
            async prepareResourceExecute () {
                try {
                    const params = this.transformResourceData()
                    await this.prepareExecute({
                        projectId: this.projectId,
                        taskId: this.taskId,
                        params
                    })
                    return true
                } catch (error) {
                    this.$bkMessage({ theme: 'error', message: error.message || error })
                    return false
                }
            },
            async handlePrev () {
                if (this.currentStepIndex > 0) {
                    const canNavigate = await this.saveDraftBeforeNavigate()
                    if (!canNavigate) {
                        return
                    }
                    
                    const prevStep = this.steps[this.currentStepIndex - 1]
                    this.$router.push({
                        name: 'crossProjectCopy',
                        params: {
                            projectId: this.projectId,
                            taskId: this.taskId,
                            tab: prevStep.name
                        }
                    })
                    this.analyzingPipeline = null
                }
            },
            /**
             * 点击步骤条，跳转到已完成的步骤查看
             * 只有 completed 为 true 且不是当前步骤时，才允许跳转
             */
            async handleStepClick (step, index) {
                if (step.completed && index !== this.currentStepIndex) {
                    // 只读状态下直接跳转，无需保存草稿
                    if (this.isReadOnly) {
                        this.$router.push({
                            name: 'crossProjectCopy',
                            params: {
                                projectId: this.projectId,
                                taskId: this.taskId,
                                tab: step.name
                            }
                        })
                        return
                    }
                    
                    const canNavigate = await this.saveDraftBeforeNavigate(index)
                    if (!canNavigate) {
                        return
                    }
                    
                    this.$router.push({
                        name: 'crossProjectCopy',
                        params: {
                            projectId: this.projectId,
                            taskId: this.taskId,
                            tab: step.name
                        }
                    })
                }
            },
            /**
             * 导航前保存草稿
             * @param {Number} targetIndex - 目标步骤索引，默认是上一步
             * @returns {Boolean} 是否允许导航
             */
            async saveDraftBeforeNavigate (targetIndex = this.currentStepIndex - 1) {
                if (this.currentStepIndex === 1 && targetIndex < this.currentStepIndex) {
                    this.savingDraft = true
                    const saveSuccess = await this.saveResourceDependencyDraft()
                    this.savingDraft = false
                    
                    if (!saveSuccess) {
                        return false
                    }
                }
                return true
            },
            async handleSaveDraft () {
                this.savingDraft = true
                try {
                    if (this.currentStepIndex === 0) {
                        await this.saveConfigScopeDraft()
                    } else {
                        await this.saveResourceDependencyDraft()
                    }
                } finally {
                    this.savingDraft = false
                }
            },
            /**
             * 保存配置范围草稿
             * @returns {Boolean} 保存是否成功
             */
            async saveConfigScopeDraft () {
                try {
                    const res = await this.saveConfigDraft({
                        projectId: this.projectId,
                        taskId: this.taskId,
                        params: { ...this.configScopeData }
                    })
                    if (res) {
                        this.$bkMessage({ theme: 'success', message: this.$t('saveDraftSuc') })
                    }
                    return !!res
                } catch (error) {
                    this.$bkMessage({ theme: 'error', message: error.message || error })
                    return false
                }
            },
            /**
             * 保存资源依赖草稿
             * @returns {Boolean} 保存是否成功
             */
            async saveResourceDependencyDraft () {
                try {
                    const params = this.transformResourceData()
                    const res = await this.saveResourceDraft({
                        projectId: this.projectId,
                        taskId: this.taskId,
                        params
                    })
                    if (res) {
                        this.$bkMessage({ theme: 'success', message: this.$t('saveDraftSuc') })
                    }
                    return !!res
                } catch (error) {
                    this.$bkMessage({ theme: 'error', message: error.message || error })
                    return false
                }
            },
            handleCancel () {
                if (this.fromSource === 'batchHistoricalTask') {
                    this.$router.push({
                        name: 'batchHistoricalTask',
                        params: {
                            projectId: this.projectId
                        }
                    })
                } else {
                    this.$router.push({
                        name: 'PipelineManageList',
                        params: {
                            projectId: this.projectId
                        }
                    })
                }

                sessionStorage.removeItem('crossProjectCopyFrom')
            },
            handleErrorClick (item) {
                // 如果错误项不在当前步骤,先跳转到对应步骤
                if (item.stepIndex !== undefined && item.stepIndex !== this.currentStepIndex) {
                    const targetStep = this.steps[item.stepIndex]
                    this.$router.push({
                        name: 'crossProjectCopy',
                        params: {
                            projectId: this.projectId,
                            taskId: this.taskId,
                            tab: targetStep.name
                        }
                    })
                }
                
                // 等待组件渲染完成后,触发指定字段的校验
                this.$nextTick(() => {
                    const currentComponent = this.$refs.currentComponent
                    if (currentComponent && typeof currentComponent.validateField === 'function') {
                        currentComponent.validateField(item.field)
                    }
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/scss/conf';
    @import "../../scss/mixins/scroller";

    .cross-project-copy {
        height: 100%;
        display: flex;
        flex-direction: column;
        background: #f5f7fa;
    }

    /* ========== 吸顶头部 ========== */
    .sticky-header {
        position: sticky;
        top: 0;
        height: 116px;
        z-index: 100;
        background: white;
        padding: 24px;
        border-bottom: 1px solid #DCDEE5;
        // transition: all 0.3s ease;
        
        &.is-sticky {
            border-bottom: none;
            height: 68px;
        }

        .header-full {
            display: flex;
            align-items: center;
            justify-content: space-between;

            &__top {
                display: flex;
                align-items: center;
                margin-bottom: 8px;
                
                .task-name {
                    display: flex;
                    align-items: center;

                    &__logo {
                        color: #1768EF;
                        vertical-align: middle;
                    }

                    &__text {
                        font-size: 20px;
                        color: #313238;
                        font-weight: 600;
                        margin-left: 12px;
                    }

                    &__status {
                        margin-left: 24px;
                        display: inline-block;
                        padding: 2px 8px;
                        color: #1768EF;
                        border-radius: 11px;
                        font-size: 12px;
                        background-color: #E1ECFF;
                    }
                }

                .auto-save-time {
                    font-size: 12px;
                    color: #979BA5;
                    margin-left: 18px;
                }
            }

            &__meta {
                height: 48px;
                font-size: 12px;
                color: $fontColor;
                padding: 16px 0;
                margin-left: 30px;

                .meta-item {
                    color: #313238;
                    font-size: 12px;

                    .meta-label {
                        color: #979BA5;
                    }
                }

                .meta-separator {
                    margin: 0 15px;
                    color: #DCDEE5;
                }
            }

            &__project {
                font-size: 12px;
                display: flex;
                align-items: center;
                padding-bottom: 12px;

                .project-info {
                    padding: 2px 12px 2px 3px;
                    align-items: center;
                    gap: 8px;
                    border-radius: 20px;
                    border: 1px solid #EAEBF0;
                    background: #F0F1F5;

                    .source-project {
                        display: inline-block;
                        padding: 2px 8px;
                        align-items: center;
                        align-content: center;
                        flex-wrap: wrap;
                        border-radius: 9px;
                        background: #D2F0FF;
                        color: #0A8CCD;
                    }

                    svg {
                        vertical-align: middle;
                    }

                    .target-project {
                        display: inline-block;
                        padding: 2px 8px;
                        align-items: center;
                        align-content: center;
                        flex-wrap: wrap;
                        border-radius: 9px;
                        background: #DCE1FF;
                        color: #2319D2;
                    }
    
                }

                .project-name {
                    color: #979BA5;
                    font-size: 12px;

                    &.is-empty {
                        color: #E71818;
                    }
                }

                .pipeline-count {
                    margin-left: 16px;
                    color: $fontColor;
                }
            }
        }
    }

    /* ========== 步骤条 ========== */
    .step-bar {
        position: sticky;
        top: 116px;
        display: flex;
        justify-content: center;
        align-items: center;
        margin: 16px 0;
        width: 100%;
        height: 48px;
        line-height: 48px;
        padding: 16px 15%;
        gap: 16px;
        z-index: 99;
        // transition: all 0.3s ease;
        transition: all 0.3s ease;
        
        &.is-sticky {
            top: 68px;
            background: #FFF;
            border-bottom: 1px solid #DCDEE5;
            margin: 0;
        }

        .step-item {
            display: flex;
            align-items: center;
            position: relative;
            flex: 1;
            cursor: default;
            border-bottom: 5px solid #DCDEE5;
            
            &.is-completed {
                border-color: #979BA5;

                .step-circle {
                    background-color: #979BA5;
                }
            }

            &.is-current {
                border-color: #3A84FF;

                .step-circle {
                    background-color: #3A84FF;
                }
            }

            .step-circle {
                width: 24px;
                height: 24px;
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 14px;
                color: #FFFFFF;
                background-color: #DCDEE5;
                flex-shrink: 0;
                z-index: 1;
            }
    
            .step-info {
                display: flex;
                align-items: center;
                margin-left: 8px;
                flex-shrink: 0;

                .step-title {
                    color: #313238;
                }
                .step-desc {
                    font-size: 12px;
                    color: #979BA5;
                    margin-left: 16px;
                }
            }
        }
    }

    /* ========== 主内容区 ========== */
    .main-content {
        padding: 0 24px;
        display: flex;
        flex: 1;
        overflow: hidden;
        gap: 16px;

        .left-content {
            flex: 1;
            border-radius: 2px;
            overflow-y: auto;
            height: 100%;
            @include scroller();
        }
    
        .right-sidebar {
            width: 320px;
            flex-shrink: 0;
            align-self: flex-start;
            border-radius: 2px;
            overflow-y: auto;
            transition: padding-top 0.3s ease;
            @include scroller();
            
            &.is-sticky {
                padding-top: 20px;
            }
        }
    
        .sidebar-section {
            padding: 20px;
            margin-bottom: 16px;
            
            &.submit-check-section {
                box-shadow: 0 2px 4px 0 #1919290d;
                background-color: #fff;
                border-left: 6px solid #FF9C01;
                .section-header {
                    display: flex;
                    align-items: center;
                    margin-bottom: 16px;
                }
                
                .error-count-badge {
                    display: inline-block;
                    padding: 0px 8px;
                    background: #FF9C01;
                    margin-left: 8px;
                    color: #FFFFFF;
                    font-size: 12px;
                    border-radius: 2px;
                    font-weight: 500;
                }
            }

            &.task-execution-result {
                box-shadow: 0 2px 4px 0 #1919290d;
                background-color: #fff;

                .section-title {
                    margin-bottom: 16px;
                }

                .guidance-item {
                    margin-bottom: 16px;

                    &:last-child {
                        margin-bottom: 0;
                    }

                    .guidance-header {
                        display: flex;
                        align-items: center;
                        gap: 8px;
                        margin-bottom: 4px;

                        .guidance-title {
                            font-size: 12px;
                            font-weight: 700;
                            color: #313238;
                            line-height: 20px;
                        }
                    }

                    .guidance-desc {
                        font-size: 12px;
                        color: #63656E;
                        line-height: 20px;
                        padding-left: 24px;
                        margin: 0;

                        .view-resource-link {
                            color: #3A84FF;
                            cursor: pointer;

                            &:hover {
                                color: #1768EF;
                            }
                        }
                    }
                }
            }
        }
    
        .section-title {
            font-size: 14px;
            font-weight: 600;
            color: #313238;
            margin: 0;
        }
        
        .check-list {
            .check-item {
                display: flex;
                align-items: center;
                padding: 4px;
                margin-bottom: 8px;
                cursor: pointer;
                transition: background 0.2s;
                
                &.check-item-error:hover {
                    background: #FFF0F0;
                }
                
                &:last-child {
                    margin-bottom: 0;
                }
                
                .error-icon {
                    color: #EA3636;
                    background-color: #FFEBEB;
                    border-radius: 50%;
                    font-size: 14px;
                    flex-shrink: 0;
                    margin-right: 8px;
                }
                
                .success-icon {
                    color: #65C389;
                    background-color: #EBFAF0;
                    border-radius: 50%;
                    font-size: 8px;
                    padding: 3px;
                    flex-shrink: 0;
                    margin-right: 8px;
                }
                
                .error-text {
                    flex: 1;
                    font-size: 12px;
                    color: #EA3636;
                    line-height: 20px;
                }
                
                .success-text {
                    flex: 1;
                    font-size: 12px;
                    color: #299E56;
                    line-height: 20px;
                }
            }
        }
        
        .empty-placeholder {
            display: flex;
            align-items: center;
            padding: 12px;
            background: #F0F8F0;
            border-radius: 2px;
            
            .success-icon {
                color: #2DCB56;
                font-size: 14px;
                margin-right: 8px;
            }
            
            span {
                font-size: 12px;
                color: #63656E;
            }
        }
        
        .task-summary-section {
            box-shadow: 0 2px 4px 0 #1919290d;
            background-color: #fff;
            .section-title {
                margin-bottom: 16px;
            }
        }

        .task-execution {
            box-shadow: 0 2px 4px 0 #1919290d;
            background-color: #fff;

            .section-title {
                margin-bottom: 4px;
            }

            .task-executionlist {
                display: flex;
                gap: 8px;
                padding-top: 12px;
                font-size: 12px;
                color: #4d4f56;

                p {
                    display: flex;
                    flex-direction: column;
                    gap: 4px;

                    .title {
                        font-size: 12px;
                        font-weight: 700;
                        line-height: 20px;
                    }
                }
            }
        }
        
        .summary-list {
            .summary-item {
                display: flex;
                align-items: center;
                justify-content: space-between;
                padding: 6px 0;
                
                &:last-child {
                    border-bottom: none;
                }
                
                .summary-label {
                    font-size: 12px;
                    color: #979BA5;
                    flex-shrink: 0;
                }
                
                .summary-value {
                    font-size: 12px;
                    color: #313238;
                    font-weight: 500;
                    text-align: right;
                    
                    .number-bold {
                        font-weight: 700;
                    }

                    &.is-error, .high-risk-number {
                        color: #EA3636;
                    }
                    
                    &.is-warning {
                        color: #FF9C01;
                    }
                    
                    &.is-success, .none-high-risk {
                        color: #2DCB56;
                    }
                }
            }
        }
    }

    /* ========== 底部操作栏 ========== */
    .footer-actions {
        background: #FAFBFD;
        box-shadow: inset 0 1px 0 0 #EAEBF0;
        display: flex;
        flex-direction: column;
        gap: 8px;
        align-items: start;
        justify-content: center;
        padding: 8px 24px;
        width: 100%;
        position: fixed;
        bottom: 0;
        z-index: 99;
        
        .high-risk-confirm {
            flex: 1;
            margin-right: 16px;
            margin-bottom: 8px;
            font-size: 14px;
            line-height: 22px;
            display: flex;
            align-items: center;
            gap: 8px;
            
            &__text {
                font-size: 12px;
                color: #63656E;
            }
            
            .high-risk-count {
                color: #EA3636;
                font-weight: 700;
                font-size: 14px;
            }
            
            .high-risk-link {
                color: #3A84FF;
                cursor: pointer;
                font-weight: 500;
                
                &:hover {
                    text-decoration: underline;
                }
            }
            
            ::v-deep .bk-checkbox-text {
                font-size: 12px;
                color: #FF9C01;
                font-weight: 500;
            }
        }
        
        .footer-buttons {
            display: flex;
            gap: 8px;
            align-items: center;
        }
    }

    .high-risk-dialog {
        ::v-deep .bk-tab-section {
            padding: 0;
        }
        ::v-deep .bk-tab-label-item {
            padding: 0 16px 0 0 !important;
        }
        ::v-deep .bk-tab-label {
            padding: 0 8px;
        }
    }
</style>
