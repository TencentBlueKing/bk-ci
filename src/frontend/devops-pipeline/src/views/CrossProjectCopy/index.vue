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
                        <Logo
                            name="arrows-left"
                            class="task-name__logo"
                            size="18"
                        />
                        <span class="task-name__text">{{ taskName }}</span>
                        <span class="task-name__status">{{ $t('draft') }}</span>
                    </div>
                    <span class="auto-save-time">{{ $t('autoSave') }} {{ autoSaveTime }}</span>
                </div>
                <div class="header-full__project">
                    <p class="project-info">
                        <span class="source-project">{{ $t('source') }}</span>
                        <span class="project-name">{{ sourceProjectName || '--' }}</span>
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
                    <span class="pipeline-count">{{ $t('totalPipelines', [pipelineCount]) }}</span>
                </div>
            </div>
            <div
                class="header-full__meta"
                v-show="!isSticky"
            >
                <span class="meta-item">
                    <span class="meta-label">ID：</span>
                    <span>{{ taskId || '--' }}</span>
                </span>
                <span class="meta-separator">|</span>
                <span class="meta-item">
                    <span class="meta-label">{{ $t('taskType') }}：</span>
                    <span>{{ $t('crossProjectCopy') }}</span>
                </span>
                <span class="meta-separator">|</span>
                <span class="meta-item">
                    <span class="meta-label">{{ $t('submitter') }}：</span>
                    <span>{{ submitter || '--' }}</span>
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
        <div class="main-content">
            <!-- 左侧内容区 -->
            <div
                class="left-content"
                ref="leftContent"
            >
                <component
                    :is="currentTabComponent"
                    :form-data="formData"
                    @update-form-data="handleUpdateFormData"
                    ref="currentComponent"
                />
            </div>

            <!-- 右侧提交检查/任务摘要面板 -->
            <div
                class="right-sidebar"
                :class="{ 'is-sticky': isSticky }"
            >
                <!-- 提交检查区域 -->
                <div class="sidebar-section submit-check-section">
                    <div class="section-header">
                        <h3 class="section-title">{{ $t('submitCheck') }}</h3>
                        <span
                            v-if="validationErrors.length > 0"
                            class="error-count-badge"
                        >
                            {{ $t('waitingForHandle', [validationErrors.length]) }}
                        </span>
                    </div>
                    
                    <!-- 检查项列表 -->
                    <div
                        class="check-list"
                    >
                        <div
                            v-for="(error, index) in validationErrors"
                            :key="index"
                            class="check-item"
                            @click="handleErrorClick(error)"
                        >
                            <i class="devops-icon icon-close-small error-icon"></i>
                            <span class="error-text">{{ error.message }}</span>
                        </div>
                    </div>
                </div>

                <!-- 任务摘要区域 -->
                <div class="sidebar-section task-summary-section">
                    <h3 class="section-title">{{ $t('taskSummary') }}</h3>
                    
                    <div class="summary-list">
                        <div class="summary-item">
                            <span class="summary-label">{{ $t('sourceProject') }}</span>
                            <span class="summary-value">{{ sourceProjectName || '--' }}</span>
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
                            <span class="summary-value"><span class="number-bold">{{ pipelineCount }}</span> {{ $t('items') }}</span>
                        </div>
                        <div class="summary-item">
                            <span class="summary-label">{{ $t('autoAddSubPipeline') }}</span>
                            <span class="summary-value"><span class="number-bold">{{ autoAddSubPipelineCount }}</span> {{ $t('items') }}</span>
                        </div>
                        <div class="summary-item">
                            <span class="summary-label">{{ $t('autoRemovePacPipeline') }}</span>
                            <span class="summary-value"><span class="number-bold">{{ autoRemovePacCount }}</span> {{ $t('items') }}</span>
                        </div>
                        <div class="summary-item">
                            <span class="summary-label">{{ $t('pipelineIdStrategy') }}</span>
                            <span class="summary-value">{{ $t('autoGenerateNewId') }}</span>
                        </div>
                        <div class="summary-item">
                            <span class="summary-label">{{ $t('unhandledResources') }}</span>
                            <span
                                class="summary-value"
                                :class="{ 'is-warning': unhandledResourceCount > 0 }"
                            >
                                <span class="number-bold">{{ unhandledResourceCount }}</span>
                            </span>
                        </div>
                        <div class="summary-item">
                            <span class="summary-label">{{ $t('highRiskOperations') }}</span>
                            <span
                                class="summary-value"
                                :class="{ 'is-success': highRiskOperationCount === 0 }"
                            >
                                <span
                                    class="number-bold"
                                    v-if="highRiskOperationCount !== 0"
                                >{{ highRiskOperationCount }}</span>
                                <template v-else>{{ $t('none') }}</template>
                            </span>
                        </div>
                        <div class="summary-item">
                            <span class="summary-label">{{ $t('autoExecuteAfterCopy') }}</span>
                            <span class="summary-value"><span class="number-bold">{{ autoExecuteCount }}</span> {{ $t('items') }}</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- 底部操作栏 -->
        <div class="footer-actions">
            <bk-button
                v-for="btn in footerButtons"
                :key="btn.action"
                :theme="btn.theme"
                :disabled="btn.disabled"
                v-bk-tooltips="btn.tooltip || ''"
                @click="btn.handler"
            >
                {{ btn.text }}
            </bk-button>
        </div>
    </div>
</template>

<script>
    import ConfigScope from './ConfigScope.vue'
    import ResourceDependency from './ResourceDependency.vue'
    import TaskExecution from './TaskExecution.vue'
    import Logo from '@/components/Logo'

    export default {
        name: 'CrossProjectCopy',
        components: {
            ConfigScope,
            ResourceDependency,
            TaskExecution,
            Logo,
        },
        data () {
            return {
                isSticky: false,
                scrollThrottle: null,
                shouldEnableSticky: true, // 是否应该启用吸顶效果
                resizeObserver: null, // ResizeObserver实例
                taskName: this.$t('newCrossProjectCopyTask'),
                isDraft: true,
                autoSaveTime: '--',
                taskId: '',
                submitter: '',
                sourceProjectName: '',
                targetProjectName: '',
                pipelineCount: 0,
                autoAddSubPipelineCount: 1,
                autoRemovePacCount: 1,
                unhandledResourceCount: 15,
                highRiskOperationCount: 0,
                autoExecuteCount: 32,
                currentStepIndex: 0,
                formData: {
                    configScope: {
                        targetProjectId: '',
                        taskName: '',
                        pipelineIdStrategy: 'auto', // 'auto' | 'keep'
                        selectedPipelines: [],
                        copyOptions: []
                    },
                    resourceDependency: {
                        selectedResources: []
                    },
                    taskExecution: {
                        executeOption: '',
                        scheduleTime: ''
                    }
                },
                steps: [
                    {
                        name: 'configScope',
                        title: this.$t('configScope'),
                        desc: this.$t('configScopeDesc'),
                        completed: false
                    },
                    {
                        name: 'resourceDependency',
                        title: this.$t('resourceDependency'),
                        desc: this.$t('resourceDependencyDesc'),
                        completed: false
                    },
                    {
                        name: 'taskExecution',
                        title: this.$t('taskExecution'),
                        desc: this.$t('taskExecutionDesc'),
                        completed: false
                    }
                ],
                tabs: [
                    { name: 'configScope', component: 'ConfigScope' },
                    { name: 'resourceDependency', component: 'ResourceDependency' },
                    { name: 'taskExecution', component: 'TaskExecution' }
                ]
            }
        },
        computed: {
            activeTab () {
                return this.tabs[this.currentStepIndex].name
            },
            currentTabComponent () {
                return this.tabs[this.currentStepIndex].component
            },
            validationErrors () {
                const errors = []
                // 检查目标项目是否选择
                if (!this.targetProjectName) {
                    errors.push({
                        field: 'targetProject',
                        message: this.$t('notSelectedTargetProject'),
                        stepIndex: 0
                    })
                }
                // 可以根据当前步骤添加更多检查项
                return errors
            },
            footerButtons () {
                const buttons = []
                if (this.currentStepIndex > 0) {
                    buttons.push({
                        action: 'prev',
                        theme: 'default',
                        text: this.$t('previousStep'),
                        handler: this.handlePrev
                    })
                }
                if (this.currentStepIndex < this.steps.length - 1) {
                    const isStep2 = this.currentStepIndex === 1
                    const isResourceSelected = this.formData.resourceDependency.selectedResources.length > 0
                    buttons.push({
                        action: 'next',
                        theme: 'primary',
                        text: this.currentStepIndex === 0
                            ? this.$t('nextStepToResourceDependency')
                            : this.$t('nextStepToTaskExecution'),
                        disabled: isStep2 && !isResourceSelected,
                        tooltip: isStep2 && !isResourceSelected ? this.$t('pleaseSelectResourceHandle') : '',
                        handler: this.handleNext
                    })
                } else {
                    buttons.push({
                        action: 'start',
                        theme: 'primary',
                        text: this.$t('startCopy'),
                        handler: this.handleStartCopy
                    })
                }
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
            '$route.params.tab' (newTab) {
                const index = this.tabs.findIndex(t => t.name === newTab)
                if (index !== -1) {
                    this.currentStepIndex = index
                }
            },
            currentStepIndex () {
                // 切换步骤时重新检测内容高度和设置监听
                this.$nextTick(() => {
                    this.setupResizeObserver()
                    this.checkContentHeight()
                })
            }
        },
        mounted () {
            this.initData()
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
            // 清理ResizeObserver
            if (this.resizeObserver) {
                this.resizeObserver.disconnect()
                this.resizeObserver = null
            }
        },
        methods: {
            handleUpdateFormData (stepName, field, value) {
                if (this.formData[stepName]) {
                    this.$set(this.formData[stepName], field, value)
                }
            },
            initData () {
                this.sourceProjectName = this.$route.params.projectId || ''
                const pipelineIds = this.$route.query.pipelineIds
                this.pipelineCount = pipelineIds ? pipelineIds.split(',').length : 0
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
            handleNext () {
                if (this.currentStepIndex < this.steps.length - 1) {
                    this.steps[this.currentStepIndex].completed = true
                    this.currentStepIndex++
                    this.syncRoute()
                }
            },
            handlePrev () {
                if (this.currentStepIndex > 0) {
                    this.steps[this.currentStepIndex].completed = false
                    this.currentStepIndex--
                    this.syncRoute()
                }
            },
            handleStartCopy () {
                console.log('Start copy')
            },
            handleSaveDraft () {
                console.log('Save draft')
            },
            handleCancel () {
                this.$router.back()
            },
            handleErrorClick (error) {
                // 点击错误项，跳转到对应步骤
                if (error.stepIndex !== undefined && error.stepIndex !== this.currentStepIndex) {
                    this.currentStepIndex = error.stepIndex
                    this.syncRoute()
                }
            },
            syncRoute () {
                this.$router.push({
                    name: 'crossProjectCopy',
                    params: {
                        tab: this.tabs[this.currentStepIndex].name
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
        padding: 0 24px 48px; // 增加底部padding(48px操作栏)
        display: flex;
        flex: 1;
        overflow: hidden;
        gap: 16px;

        .left-content {
            flex: 1;
            background: white;
            border-radius: 2px;
            padding: 16px 24px 0;
            overflow-y: auto;
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
                
                &:hover {
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
                
                .error-text {
                    flex: 1;
                    font-size: 12px;
                    color: #EA3636;
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
                    
                    &.is-error {
                        color: #EA3636;
                    }
                    
                    &.is-warning {
                        color: #FF9C01;
                    }
                    
                    &.is-success {
                        color: #2DCB56;
                    }
                }
            }
        }
    }

    /* ========== 底部操作栏 ========== */
    .footer-actions {
        height: 48px;
        background: #FFFFFF;
        border-top: 1px solid #EAEBF0;
        display: flex;
        gap: 8px;
        align-items: center;
        justify-content: start;
        padding: 8px 24px;
        width: 100%;
        position: fixed;
        bottom: 0;
        z-index: 99;
    }
</style>
