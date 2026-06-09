<template>
    <div
        class="config-scope"
        :style="{ height: isLoading ? '100%' : 'auto' }"
    >
        <!-- 配置任务信息表单 -->
        <div class="config-form-section">
            <div class="section-title">
                <p>
                    <span class="title-text">{{ $t('configTaskInfo') }}</span>
                    <span class="pipeline-count">
                        <span v-html="$t('pipelineCount', [taskData?.pipelineCount])"></span>
                        <template v-if="taskData?.subPipelineCount > 0">（{{ $t('includingAutoAdded', [taskData.subPipelineCount]) }}）</template>
                    </span>
                </p>
                <p class="pipeline-desc">{{ $t('configTaskInfoDesc') }}</p>
            </div>
        </div>
        
        <hr style="border: 0; border-top: 1px solid #DCDEE5; margin: 16px 0 24px;" />

        <div v-bkloading="{ isLoading: isLoading, title: $t('analyzingPipeline'), zIndex: 10 }">
            <bk-form
                :model="configScopeData"
                :rules="formRules"
                ref="configForm"
                form-type="vertical"
                class="config-form"
            >
                <div class="form-row">
                    <bk-form-item
                        :label="$t('targetProject')"
                        :required="true"
                        property="targetProjectId"
                        class="form-item-half"
                    >
                        <bk-select
                            v-model="configScopeData.targetProjectId"
                            @change="(value) => handleUpdate(value, 'targetProjectId')"
                            :placeholder="$t('pleaseSelect')"
                            :disabled="isReadOnly"
                            searchable
                        >
                            <bk-option
                                v-for="project in projectList"
                                :key="project.projectCode"
                                :id="project.projectCode"
                                :name="project.projectName"
                            />
                        </bk-select>
                    </bk-form-item>
                    <bk-form-item
                        :label="$t('taskName')"
                        :required="true"
                        property="taskName"
                        class="form-item-half"
                    >
                        <bk-input
                            v-model="configScopeData.taskName"
                            @change="(value) => handleUpdate(value, 'taskName')"
                            :placeholder="$t('enterTaskName')"
                            :disabled="isReadOnly"
                            :readonly="isReadOnly"
                        />
                    </bk-form-item>
                </div>
                <bk-form-item
                    :label="$t('pipelineIdStrategy')"
                    :required="true"
                    class="strategy-form"
                    property="pipelineCopyStrategy"
                >
                    <bk-radio-group
                        class="pipeline-strategy"
                        v-model="configScopeData.pipelineCopyStrategy"
                        @change="(value) => handleUpdate(value, 'pipelineCopyStrategy')"
                    >
                        <div
                            class="pipeline-strategy-item"
                            :class="{ active: configScopeData.pipelineCopyStrategy === PipelineIdStrategy.PIPELINE_CREATE_NEW_ID }"
                        >
                            <bk-radio
                                :value="PipelineIdStrategy.PIPELINE_CREATE_NEW_ID"
                                :disabled="isReadOnly"
                            >
                                {{ $t('autoGenerateNewId') }}
                                <span class="recommend">{{ $t('recommend') }}</span>
                            </bk-radio>
                            <p>{{ $t('autoGenerateNewIdDesc') }}</p>
                        </div>
                        <div
                            class="pipeline-strategy-item"
                            :class="{ active: configScopeData.pipelineCopyStrategy === PipelineIdStrategy.PIPELINE_REUSE_SOURCE_ID }"
                        >
                            <bk-radio
                                :value="PipelineIdStrategy.PIPELINE_REUSE_SOURCE_ID"
                                :disabled="isReadOnly"
                            >
                                {{ $t('keepSourceId') }}
                            </bk-radio>
                            <p>{{ $t('keepSourceIdDesc') }}</p>
                        </div>
                    </bk-radio-group>
                </bk-form-item>
            </bk-form>

            <hr style="border: 0; border-top: 1px solid #DCDEE5; margin: 24px 0 24px;" />

            <!-- 提示栏区域 -->
            <div
                class="alert-bars-section"
                v-if="taskData?.pacCount > 0 || taskData?.subPipelineCount > 0"
            >
                <div
                    class="alert-bar alert-bar--pac"
                    v-if="taskData?.pacCount > 0"
                >
                    <div class="alert-bar__left">
                        <Logo
                            name="excluded-pipeline"
                            size="20"
                        />
                        <p>
                            <i18n
                                path="autoExcludedPacPipeline"
                                tag="span"
                            >
                                <span class="alert-bar__count">{{ taskData?.pacCount }}</span>
                            </i18n>
                            <span class="desc">{{ $t('autoExcludedPacPipelineDesc') }}</span>
                        </p>
                    </div>
                    <div class="alert-bar__right">
                        <bk-button
                            text
                            @click="handleViewDetails('pac')"
                        >
                            {{ $t('viewDetails') }}
                        </bk-button>
                    </div>
                </div>
                <div
                    class="alert-bar alert-bar--sub"
                    v-if="taskData?.subPipelineCount > 0"
                >
                    <div class="alert-bar__left">
                        <Logo
                            name="sub-pipeline"
                            size="20"
                        />
                        <p>
                            <i18n
                                path="autoAddedSubPipeline"
                                tag="span"
                            >
                                <span class="alert-bar__count">{{ taskData?.subPipelineCount }}</span>
                            </i18n>
                            <span class="desc">{{ $t('autoAddedSubPipelineDesc') }}</span>
                        </p>
                    </div>
                    <div class="alert-bar__right">
                        <bk-button
                            text
                            @click="handleViewDetails('subPipeline')"
                        >
                            {{ $t('viewDetails') }}
                        </bk-button>
                    </div>
                </div>
            </div>

            <!-- 流水线表格区域 -->
            <div class="pipeline-table-section">
                <!-- Tab 筛选和操作栏 -->
                <div class="table-toolbar">
                    <div class="toolbar-left">
                        <custom-tabs
                            :active-tab="activeTab"
                            :tabs="tabList"
                            @tab-change="handleTabChange"
                        />
                    </div>
                    <div class="toolbar-right">
                        <search-select
                            :key="searchSelectKey"
                            class="pipeline-search-select"
                            :data="searchDropList"
                            :values="searchValues"
                            :placeholder="$t('searchPipelineNameOrGroup')"
                            @change="handleSearch"
                            style="width: 360px; margin-right: 8px;"
                        />
                        <bk-button @click="handleRestoreAll">
                            {{ $t('restoreAll') }}
                        </bk-button>
                    </div>
                </div>

                <!-- 表格 -->
                <bk-table
                    :data="pipelineList"
                    :pagination="tablePagination"
                    :outer-border="false"
                    :header-border="false"
                    v-bkloading="{ isLoading: tableLoading }"
                    @page-change="handlePageChange"
                    @page-limit-change="handlePageLimitChange"
                >
                    <bk-table-column
                        :label="$t('pipelineName')"
                        prop="pipelineName"
                        min-width="250"
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
                        prop="creator"
                    />
                    <bk-table-column
                        :label="$t('status')"
                        prop="status"
                    >
                        <template slot-scope="{ row }">
                            <span :class="['status-text', `status-text--${row.status}`]">
                                {{ getStatusText(row.status) }}
                            </span>
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        :label="$t('operation')"
                        width="180"
                    >
                        <template slot-scope="{ row }">
                            <div class="operation-cell">
                                <bk-button
                                    v-if="row.status === PipelineBatchTaskDetailStatus.EXCLUDED"
                                    text
                                    @click="handleRestore(row)"
                                >
                                    {{ $t('restore.restore') }}
                                </bk-button>
                                <bk-button
                                    v-else-if="row.status === PipelineBatchTaskDetailStatus.WAIT_COPY"
                                    text
                                    @click="handleExclude(row)"
                                >
                                    {{ $t('exclude') }}
                                </bk-button>
                                <span
                                    v-else-if="row.pac"
                                    class="auto-action-text"
                                >
                                    {{ $t('notSupportMigration') }}
                                </span>
                                <span
                                    v-else-if="row.subPipeline"
                                    class="not-support-text"
                                >
                                    {{ $t('systemAutoAdded') }}
                                </span>
                            </div>
                        </template>
                    </bk-table-column>
                </bk-table>
            </div>
        </div>
    </div>
</template>

<script>
    import { mapActions } from 'vuex'
    import Logo from '@/components/Logo'
    import CustomTabs from '@/views/CrossProjectCopy/components/CustomTabs.vue'
    import SearchSelect from '@blueking/search-select'
    import '@blueking/search-select/dist/styles/index.css'
    import { getTimestamp } from '@/utils/util'
    import { PipelineIdStrategy, PipelineBatchTaskDetailStatus } from '@/store/modules/crossProjectCopy/constants'

    export default {
        name: 'ConfigScope',
        components: {
            Logo,
            CustomTabs,
            SearchSelect
        },
        props: {
            configScopeData: {
                type: Object,
                required: true,
                default: () => ({
                    targetProjectId: '',
                    taskName: '',
                    pipelineCopyStrategy: ''
                })
            },
            taskData: {
                type: Object,
                default: null
            },
            analyzingPipeline: {
                type: Boolean,
                default: false
            },
            isReadOnly: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                // 当前激活的 Tab
                activeTab: 'all',
                // 搜索条件值（SearchSelect 的值结构）
                searchValues: [],
                searchSelectKey: 0,
                // 内部 loading 状态（用于接口加载）
                isLoadingData: false,
                // 表格 loading 状态
                tableLoading: false,
                // 项目列表
                projectList: [],
                // 流水线列表
                pipelineList: [],
                // 状态汇总数据
                statusSummary: [],
                // 表格分页
                tablePagination: {
                    current: 1,
                    limit: 10,
                    count: 0,
                },
                // 表单验证规则
                formRules: {
                    targetProjectId: [
                        { required: true, message: this.$t('notSelectedTargetProject'), trigger: 'change' }
                    ],
                    taskName: [
                        { required: true, message: this.$t('pleaseEnterTaskName'), trigger: 'blur' }
                    ],
                    pipelineCopyStrategy: [
                        { required: true, message: this.$t('pleaseSelectPipelineIdStrategy'), trigger: 'change' }
                    ]
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            taskId () {
                return this.$route.params.taskId
            },
            // 统一的 loading 状态 包括：父组件轮询阶段 + 子组件数据加载阶段
            isLoading () {
                return this.analyzingPipeline || this.isLoadingData
            },
            // Tab 列表
            tabList () {
                const waitCopyItem = this.statusSummary.find(item => item.status === PipelineBatchTaskDetailStatus.WAIT_COPY)
                const excludedItem = this.statusSummary.find(item => item.status === PipelineBatchTaskDetailStatus.EXCLUDED)

                const pendingCount = waitCopyItem ? waitCopyItem.count : 0
                const excludedCount = excludedItem ? excludedItem.count : 0
                // 全部数量 = 待复制 + 已排除（或从statusSummary中计算全部）
                const allCount = this.statusSummary.reduce((sum, item) => sum + (item.count || 0), 0)

                return [
                    { name: 'all', label: this.$t('all'), count: allCount },
                    { name: PipelineBatchTaskDetailStatus.WAIT_COPY, label: this.$t('pendingCopy'), count: pendingCount },
                    { name: PipelineBatchTaskDetailStatus.EXCLUDED, label: this.$t('excluded'), count: excludedCount }
                ]
            },
            searchDropList () {
                const usedIds = (this.searchValues || []).map(v => v.id)
                const list = [
                    {
                        id: 'pipelineName',
                        name: this.$t('pipelineName'),
                        default: true
                    },
                    {
                        id: 'type',
                        name: this.$t('pipelineType'),
                        children: [
                            { id: 'pac', name: this.$t('pacPipeline') },
                            { id: 'subPipeline', name: this.$t('subPipeline') }
                        ]
                    },
                    {
                        id: 'creator',
                        name: this.$t('creator'),
                        default: true
                    }
                ]
                return list.filter(item => !usedIds.includes(item.id))
            }
        },
        watch: {
            // 监听父组件的 analyzingPipeline 状态
            // 当从非 false（null 或 true）变为 false 时，说明轮询完成，自动加载数据
            async analyzingPipeline (newVal, oldVal) {
                if (oldVal !== false && newVal === false) {
                    await this.loadPipelineListWithLoading()
                }
            }
        },
        created () {
            this.PipelineIdStrategy = PipelineIdStrategy
            this.PipelineBatchTaskDetailStatus = PipelineBatchTaskDetailStatus
            this.loadProjectList()
            
            // 如果已经不在分析中状态，主动加载流水线列表
            if (!this.analyzingPipeline) {
                this.loadPipelineListWithLoading()
            }
        },
        methods: {
            ...mapActions('crossProjectCopy', [
                'getTaskDetails',
                'getTaskStatusSummary',
                'restoreTaskDetail',
                'excludeTaskDetail',
                'restoreAllExcludedTaskDetail',
                'getProjectList'
            ]),
            /**
             * 加载项目列表
             */
            async loadProjectList () {
                try {
                    this.projectList  = await this.getProjectList()
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                }
            },
            /**
             * 加载配置复制范围数据 — 首次加载，需要同时拉取列表数据和状态汇总数据
             */
            async loadPipelineListWithLoading () {
                this.isLoadingData = true
                try {
                    await Promise.all([
                        this.loadPipelineList(),
                        this.loadStatusSummary()
                    ])
                } finally {
                    this.isLoadingData = false
                }
            },
            /**
             * 加载流水线列表数据
             */
            async loadPipelineList () {
                this.tableLoading = true
                try {
                    const queryParams = {
                        projectId: this.projectId,
                        taskId: this.taskId,
                        params: {
                            page: this.tablePagination.current,
                            pageSize: this.tablePagination.limit,
                            ...this.getStatusParam(),
                            ...this.getSearchParams()
                        }
                    }
                    const detailsData = await this.getTaskDetails(queryParams)
                    
                    this.pipelineList = detailsData.records || []
                    this.tablePagination.count = detailsData.count || 0
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
             * 当前 Tab 对应的 status 参数
             */
            getStatusParam () {
                const status = this.activeTab !== 'all' ? this.activeTab : ''
                return status ? { status } : {}
            },
            /**
             * 加载状态汇总数据（仅 Tab 数量等场景需要）
             */
            async loadStatusSummary () {
                try {
                    const summaryData = await this.getTaskStatusSummary({
                        projectId: this.projectId,
                        taskId: this.taskId
                    })
                    this.statusSummary = summaryData || []
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                }
            },
            handleUpdate (value, field) {
                // 当选择目标项目时，如果任务名称为空，则自动填充默认名称
                if (field === 'targetProjectId' && value) {
                    if (!this.configScopeData.taskName || !this.configScopeData.taskName.trim()) {
                        // 获取目标项目名称
                        const targetProject = this.projectList.find(p => p.projectCode === value)
                        const projectName = targetProject ? targetProject.projectName : value

                        const timestamp = getTimestamp()

                        // 生成默认任务名称
                        const defaultTaskName = `copy-to-${projectName}-${timestamp}`
                        this.$emit('update-form-data', 'taskName', defaultTaskName)
                        this.validateField('taskName')
                    }
                }
                
                this.$emit('update-form-data', field, value)
            },
            handleTabChange (name) {
                this.activeTab = name

                this.tablePagination.current = 1
                this.loadPipelineList()
            },
            /**
             * 搜索框搜索
             */
            handleSearch (value) {
                this.searchValues = value || []
                this.tablePagination.current = 1
                this.loadPipelineList()
            },
            /**
             * 把 SearchSelect 的搜索条件转换为接口可用的参数
             */
            getSearchParams () {
                return (this.searchValues || []).reduce((acc, item) => {
                    if (item.id === 'type') {
                        item.values.forEach(v => {
                            acc[v.id] = true
                        })
                    } else {
                        acc[item.id] = item.values.map(v => v.id).join(',')
                    }
                    return acc
                }, {})
            },
            /**
             * 点击「查看明细」
             */
            handleViewDetails (type) {
                const typeName = type === 'pac' ? this.$t('pacPipeline') : this.$t('subPipeline')
                
                const newSearchValues = [
                    {
                        id: 'type',
                        name: this.$t('pipelineType'),
                        values: [{ id: type, name: typeName }]
                    }
                ]
                this.activeTab = 'all'
                this.searchSelectKey++
                
                this.handleSearch(newSearchValues)
            },
            /**
             * 恢复全部已排除的流水线
             */
            async handleRestoreAll () {
                try {
                    await this.restoreAllExcludedTaskDetail({
                        projectId: this.projectId,
                        taskId: this.taskId
                    })
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('restoreSuccess')
                    })
                    this.loadPipelineListWithLoading()
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                }
            },
            /**
             * 恢复单个流水线
             */
            async handleRestore (row) {
                try {
                    await this.restoreTaskDetail({
                        projectId: this.projectId,
                        taskId: this.taskId,
                        pipelineId: row.pipelineId
                    })
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('restoreSuccess')
                    })
                    this.loadPipelineListWithLoading()
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                }
            },
            /**
             *  排除单个流水线
             */
            async handleExclude (row) {
                try {
                    await this.excludeTaskDetail({
                        projectId: this.projectId,
                        taskId: this.taskId,
                        pipelineId: row.pipelineId
                    })
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('excludeSuccess')
                    })
                    this.loadPipelineListWithLoading()
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
            handlePageChange (page) {
                this.tablePagination.current = page
                this.loadPipelineList()
            },
            handlePageLimitChange (limit) {
                this.tablePagination.limit = limit
                this.tablePagination.current = 1
                this.loadPipelineList()
            },
            /**
             * 表单验证方法,供父组件调用
             */
            validate () {
                return this.$refs.configForm?.validate?.()
            },
            /**
             * 校验指定字段
             * @param {string} fieldName - 字段名称
             */
            validateField (fieldName) {
                if (this.$refs.configForm && typeof this.$refs.configForm.validateField === 'function') {
                    this.$refs.configForm.validateField(fieldName)
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/scss/conf.scss';

    .config-scope {
        
        padding: 16px 24px 0;
    }
    
    .config-form-section {
        background: #FFFFFF;
        border-radius: 2px;
        margin-bottom: 16px;

        .section-title {

            .title-text {
                color: #313238;
                font-size: 14px;
                font-weight: 700;
                line-height: 22px;
                margin-right: 16px;
            }

            .pipeline-count {
                display: inline-block;
                padding: 2px 8px;
                font-size: 12px;
                flex-wrap: wrap;
                border-radius: 2px;
                font-weight: 400;
                background:#F0F1F5;
            }
            .pipeline-desc {
                color: #979ba5;
                font-size: 12px;
                line-height: 20px;
                margin-top: 9px;
            }
        }

    }
    .config-form {
        .form-row {
            display: flex;
            gap: 20px;
            align-items: flex-start;
    
            .form-item-half {
                flex: 1;
            }
    
            ::v-deep .bk-form-item {
                margin: 0;
            }
    
            ::v-deep .bk-select-dropdown{
                margin-top: 1px;
            }
    
            ::v-deep .bk-input-text {
                height: 33px;
            }
        }
    
        ::v-deep .bk-form-item {
            margin-bottom: 20px;
    
            &:last-child {
                margin-bottom: 0;
            }
        }
    
        ::v-deep .bk-label-text {
            font-size: 12px;
            color: #4D4F56;
            margin-bottom: 6px;
            font-weight: 400;
        }

        .strategy-form {
            margin-top: 24px;

            .pipeline-strategy {
                display: flex;
                align-items: center;
                gap: 10px;
    
                .pipeline-strategy-item {
                    display: flex;
                    flex-direction: column;
                    flex: 1;
                    padding: 12px 16px;
                    align-items: flex-start;
                    gap: 8px;
                    height: 90px;
                    border-radius: 2px;
                    background: #F5F7FA;
                    border: 1px solid transparent;
    
                    .recommend {
                        padding: 2px 8px;
                        margin-left: 8px;
                        border-radius: 2px;
                        border: 1px #A1E3BA solid;
                        background: #DAF6E5;
                        color: #299e56;
                        font-size: 12px;
                        line-height: 20px;
                    }
    
                    ::v-deep .bk-radio-text {
                        margin-left: 4px;
                        color: #313238;
                    }
    
                    &.active {
                        border: 1px solid #C4C6CC;
                    }
    
                    p {
                        color: #979ba5;
                        font-size: 12px;
                        line-height: 20px;
                        margin-left: 18px;
                    }
                }
            }
        }
    }

    .alert-bars-section {
        margin-bottom: 16px;

        .alert-bar {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 10px 16px;
            border-radius: 2px;
            margin-bottom: 8px;
            line-height: 20px;
            font-size: 12px;
            transition: all 0.2s ease;

            &:last-child {
                margin-bottom: 0;
            }

            &__left {
                display: flex;
                flex: 1;
                align-items: center;
                gap: 8px;
                p {
                    display: flex;
                    flex-direction: column;
                    color: #313238;
                    font-size: 14px;

                    .desc {
                        font-size: 12px;
                        color: #979BA5;
                        margin-top: 4px;
                    }
                }
            }

            &__icon {
                font-size: 16px;
                color: #FF9C01;
            }

            &__count {
                font-weight: 700;
                color: #313238;
                margin: 0 2px;
            }

            &__right {
                width: 50px;
                margin-left: 16px;
                .bk-button-text {
                    color: #3A84FF;
                    font-size: 12px;
                }
            }

            &--sub {
                border-radius: 2px;
                background: #F0F5FF;
                border: 1px solid #A3C5FD;
            }

            &--pac {
                border-radius: 2px;
                background: #FDF4E8;
                border: 1px solid #F9D090;
            }
        }
    }

    .pipeline-table-section {
        background: #FFFFFF;
        border-radius: 2px;

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

        .table-toolbar {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 16px;

            .toolbar-left {
                flex: 1;

                .custom-tabs {
                    display: inline-flex;
                    align-items: center;
                    background: #F0F1F5;
                    border-radius: 2px;
                    padding: 4px;
                    gap: 4px;

                    .custom-tab-item {
                        display: inline-flex;
                        align-items: center;
                        padding: 5px 12px;
                        border-radius: 2px;
                        font-size: 12px;
                        cursor: pointer;
                        transition: all 0.2s ease;
                        white-space: nowrap;

                        .tab-label {
                            color: #4D4F56;
                        }

                        .tab-count {
                            margin-left: 4px;
                            color: #4D4F56;
                            padding: 0px 6px;
                            border-radius: 8px;
                            background-color: #fff;
                        }

                        &.is-active {
                            background: #FFFFFF;
                            box-shadow: 0 2px 4px 0 #0000001a;

                            .tab-label {
                                color: #3A84FF;
                            }

                            .tab-count {
                                color: #3A84FF;
                                background: #E1ECFF;
                            }
                        }
                    }
                }
            }

            .toolbar-right {
                display: flex;
                align-items: center;

                .pipeline-search-select {
                    background-color: white;
                    ::placeholder {
                        color: #c4c6cc;
                    }
                }
            }
        }

        ::v-deep .bk-table {
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

            .operation-cell {
                .auto-action-text {
                    font-size: 12px;
                    color: #63656E;
                }

                .not-support-text {
                    font-size: 12px;
                    color: #C4C6CC;
                }
            }
        }
    }
</style>
