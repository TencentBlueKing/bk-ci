<template>
    <div class="config-scope">
        <!-- 配置任务信息表单 -->
        <div class="config-form-section">
            <h3 class="section-title">{{ $t('configTaskInfo') }}</h3>
            <bk-form
                :model="formData.configScope"
                :rules="formRules"
                ref="configForm"
                form-type="vertical"
            >
                <!-- 目标项目和任务名称在同一行 -->
                <div class="form-row">
                    <bk-form-item
                        :label="$t('targetProject')"
                        :required="true"
                        property="targetProjectId"
                        class="form-item-half"
                    >
                        <bk-select
                            :value="formData.configScope.targetProjectId"
                            @change="handleUpdate('configScope', 'targetProjectId', $event)"
                            :placeholder="$t('pleaseSelect')"
                            searchable
                        >
                            <bk-option
                                v-for="project in projectList"
                                :key="project.id"
                                :id="project.id"
                                :name="project.name"
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
                            :value="formData.configScope.taskName"
                            @change="handleUpdate('configScope', 'taskName', $event)"
                            :placeholder="$t('enterTaskName')"
                        />
                    </bk-form-item>
                </div>
                <!-- 流水线ID处理策略单独一行 -->
                <bk-form-item
                    :label="$t('pipelineIdStrategy')"
                    :required="true"
                    property="pipelineIdStrategy"
                >
                    <bk-radio-group
                        :value="formData.configScope.pipelineIdStrategy"
                        @change="handleUpdate('configScope', 'pipelineIdStrategy', $event)"
                    >
                        <bk-radio value="auto">{{ $t('autoGenerateNewId') }}</bk-radio>
                        <bk-radio value="keep">{{ $t('keepSourceId') }}</bk-radio>
                    </bk-radio-group>
                </bk-form-item>
            </bk-form>
        </div>

        <!-- 提示栏区域 -->
        <div
            class="alert-bars-section"
            v-if="pacPipelineCount > 0 || subPipelineCount > 0"
        >
            <div
                class="alert-bar alert-bar--pac"
                v-if="pacPipelineCount > 0"
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
                            <span class="alert-bar__count">{{ pacPipelineCount }}</span>
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
                v-if="subPipelineCount > 0"
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
                            <span class="alert-bar__count">{{ subPipelineCount }}</span>
                        </i18n>
                        <span class="desc">{{ $t('autoAddedSubPipelineDesc') }}</span>
                    </p>
                </div>
                <div class="alert-bar__right">
                    <bk-button
                        text
                        @click="handleViewDetails('sub')"
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
                    <div class="custom-tabs">
                        <div
                            v-for="tab in tabList"
                            :key="tab.name"
                            :class="['custom-tab-item', { 'is-active': activeTab === tab.name }]"
                            @click="handleTabChange(tab.name)"
                        >
                            <span class="tab-label">{{ tab.label }}</span>
                            <span class="tab-count">{{ tab.count }}555</span>
                        </div>
                    </div>
                </div>
                <div class="toolbar-right">
                    <bk-input
                        v-model="searchKeyword"
                        :placeholder="$t('searchPipelineNameOrGroup')"
                        :right-icon="'bk-icon icon-search'"
                        :clearable="true"
                        @change="handleSearch"
                        style="width: 300px; margin-right: 8px;"
                    />
                    <bk-button @click="handleRestoreAll">
                        {{ $t('restoreAll') }}
                    </bk-button>
                </div>
            </div>

            <!-- 表格 -->
            <bk-table
                :data="filteredPipelineList"
                :pagination="tablePagination"
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
                            <bk-tag
                                v-if="row.type === 'sub'"
                                theme="success"
                                type="filled"
                            >
                                {{ $t('subPipeline') }}
                            </bk-tag>
                            <bk-tag
                                v-else-if="row.type === 'pac'"
                                theme="info"
                                type="filled"
                            >
                                {{ $t('pacPipeline') }}
                            </bk-tag>
                        </div>
                    </template>
                </bk-table-column>
                <bk-table-column
                    :label="$t('pipelineGroups')"
                    prop="groupName"
                    min-width="150"
                />
                <bk-table-column
                    :label="$t('creator')"
                    prop="creator"
                    width="120"
                />
                <bk-table-column
                    :label="$t('status')"
                    prop="status"
                    width="120"
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
                                v-if="row.status === 'excluded' && row.canRestore"
                                text
                                @click="handleRestore(row)"
                            >
                                {{ $t('restore.restore') }}
                            </bk-button>
                            <bk-button
                                v-else-if="row.status === 'pending' && row.canExclude"
                                text
                                @click="handleExclude(row)"
                            >
                                {{ $t('exclude') }}
                            </bk-button>
                            <span
                                v-else-if="row.autoAction"
                                class="auto-action-text"
                            >
                                {{ row.autoAction }}
                            </span>
                            <span
                                v-else
                                class="not-support-text"
                            >
                                {{ $t('notSupportMigration') }}
                            </span>
                        </div>
                    </template>
                </bk-table-column>
            </bk-table>
        </div>
    </div>
</template>

<script>
    export default {
        name: 'ConfigScope',
        props: {
            formData: {
                type: Object,
                required: true
            }
        },
        data () {
            return {
                // 当前激活的 Tab
                activeTab: 'all',
                // 搜索关键词
                searchKeyword: '',
                // 从提示栏点击「查看明细」时的类型筛选
                typeFilter: '',
                // 项目列表
                projectList: [
                    { id: 'proj-001', name: '项目A' },
                    { id: 'proj-002', name: '项目B' },
                    { id: 'proj-003', name: '项目C' }
                ],
                // 流水线列表（Mock 数据）
                pipelineList: [
                    { pipelineId: 'p-001', pipelineName: 'yamb1-trigger', groupName: '构建发布，精品服务', creator: 'fayewang', status: 'pending', type: 'sub', canExclude: true, autoAction: '系统自动添加' },
                    { pipelineId: 'p-002', pipelineName: 'frontend-app / 灰度流水线', groupName: '质量测试', creator: 'fayewang', status: 'pending', type: 'pac', canExclude: false, autoAction: '' },
                    { pipelineId: 'p-003', pipelineName: 'data-pipeline / ETL任务', groupName: '核心服务', creator: 'fayewang', status: 'pending', type: 'normal', canExclude: true, autoAction: '' },
                    { pipelineId: 'p-004', pipelineName: 'mobile-sdk / 质量扫描', groupName: '质量扫描', creator: 'fayewang', status: 'pending', type: 'sub', canExclude: true, autoAction: '系统自动添加' },
                    { pipelineId: 'p-005', pipelineName: 'gateway / 回归测试', groupName: '自动化测试', creator: 'fayewang', status: 'pending', type: 'normal', canExclude: true, autoAction: '' },
                    { pipelineId: 'p-006', pipelineName: 'infra / 基础设施监控', groupName: '基础设施', creator: 'fayewang', status: 'pending', type: 'normal', canExclude: true, autoAction: '' },
                    { pipelineId: 'p-007', pipelineName: 'infra / 测试环境', groupName: '基础设施', creator: 'fayewang', status: 'excluded', type: 'pac', canRestore: true, autoAction: '' },
                    { pipelineId: 'p-008', pipelineName: 'security / 安全扫描', groupName: '质量扫描', creator: 'fayewang', status: 'pending', type: 'normal', canExclude: true, autoAction: '' },
                    { pipelineId: 'p-009', pipelineName: 'nightly-regression / 夜间回归', groupName: '自动化测试', creator: 'fayewang', status: 'pending', type: 'normal', canExclude: true, autoAction: '' },
                    { pipelineId: 'p-010', pipelineName: 'asset-center / 资产构建 #098', groupName: '质量扫描', creator: 'fayewang', status: 'excluded', type: 'pac', canRestore: false, autoAction: '暂不支持迁移' }
                ],
                // 表格分页
                tablePagination: {
                    current: 1,
                    limit: 10,
                    count: 0,
                    'show-total-count': true
                },
                // 表单验证规则
                formRules: {
                    targetProjectId: [
                        { required: true, message: this.$t('notSelectedTargetProject'), trigger: 'blur' }
                    ],
                    taskName: [
                        { required: true, message: this.$t('pleaseEnterTaskName'), trigger: 'blur' }
                    ],
                    pipelineIdStrategy: [
                        { required: true, message: this.$t('pleaseSelectPipelineIdStrategy'), trigger: 'change' }
                    ]
                }
            }
        },
        computed: {
            // Tab 列表（包含动态数量）
            tabList () {
                const allCount = this.pipelineList.length
                const pendingCount = this.pipelineList.filter(item => item.status === 'pending').length
                const excludedCount = this.pipelineList.filter(item => item.status === 'excluded').length

                return [
                    { name: 'all', label: this.$t('all'), count: allCount },
                    { name: 'pending', label: this.$t('pendingCopy'), count: pendingCount },
                    { name: 'excluded', label: this.$t('excluded'), count: excludedCount }
                ]
            },
            // 已排除的 PAC 流水线数量
            pacPipelineCount () {
                return this.pipelineList.filter(item => item.type === 'pac' && item.status === 'excluded').length
            },
            // 已添加的子流水线数量
            subPipelineCount () {
                return this.pipelineList.filter(item => item.type === 'sub' && item.status === 'pending').length
            },
            // 筛选后的流水线列表
            filteredPipelineList () {
                let list = this.pipelineList

                // 1. 按 Tab 筛选状态
                if (this.activeTab !== 'all') {
                    list = list.filter(item => item.status === this.activeTab)
                }

                // 2. 按类型筛选（从提示栏点击「查看明细」）
                if (this.typeFilter) {
                    list = list.filter(item => item.type === this.typeFilter)
                }

                // 3. 按搜索关键词筛选
                if (this.searchKeyword) {
                    const keyword = this.searchKeyword.toLowerCase()
                    list = list.filter(item => {
                        return item.pipelineName.toLowerCase().includes(keyword)
                            || item.groupName.toLowerCase().includes(keyword)
                    })
                }

                // 更新分页总数
                this.tablePagination.count = list.length

                return list
            }
        },
        methods: {
            handleUpdate (stepName, field, value) {
                this.$emit('update-form-data', stepName, field, value)
            },
            handleTabChange (name) {
                this.activeTab = name
                this.typeFilter = '' // 切换 Tab 时清除类型筛选
            },
            handleSearch () {
                // 搜索时重置分页
                this.tablePagination.current = 1
            },
            handleViewDetails (type) {
                // 点击提示栏的「查看明细」，筛选对应类型
                this.typeFilter = type
                if (type === 'pac') {
                    this.activeTab = 'excluded'
                } else if (type === 'sub') {
                    this.activeTab = 'pending'
                }
            },
            handleRestoreAll () {
                // 恢复全部已排除的流水线
                this.pipelineList.forEach(item => {
                    if (item.status === 'excluded' && item.canRestore) {
                        item.status = 'pending'
                    }
                })
                this.$bkMessage({
                    theme: 'success',
                    message: this.$t('restoreAllSuccess')
                })
            },
            handleRestore (row) {
                // 恢复单个流水线
                row.status = 'pending'
                this.$bkMessage({
                    theme: 'success',
                    message: this.$t('restoreSuccess')
                })
            },
            handleExclude (row) {
                // 排除单个流水线
                row.status = 'excluded'
                this.$bkMessage({
                    theme: 'success',
                    message: this.$t('excludeSuccess')
                })
            },
            getStatusText (status) {
                const statusMap = {
                    pending: this.$t('pendingCopy'),
                    excluded: this.$t('excluded')
                }
                return statusMap[status] || '-'
            },
            handlePageChange (page) {
                this.tablePagination.current = page
            },
            handlePageLimitChange (limit) {
                this.tablePagination.limit = limit
                this.tablePagination.current = 1
            },
            /**
             * 表单验证方法，供父组件调用
             */
            validate () {
                return this.$refs.configForm?.validate?.() || Promise.resolve()
            }
        }
    }
</script>

<style lang="scss" scoped>
    .config-form-section {
        background: #FFFFFF;
        border-radius: 2px;
        margin-bottom: 16px;

        .section-title {
            font-size: 14px;
            font-weight: 600;
            color: #313238;
            margin: 0 0 16px 0;
        }

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

        ::v-deep .bk-form-item__label {
            font-size: 12px;
            color: #63656E;
            margin-bottom: 6px;
        }

        ::v-deep .bk-radio-group {
            display: flex;
            gap: 24px;
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

                        &:hover {
                            background: rgba(255, 255, 255, 0.5);
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
                gap: 8px;
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
                font-size: 12px;

                &--pending {
                    color: #FF9C01;

                    &::before {
                        content: '';
                        display: inline-block;
                        width: 6px;
                        height: 6px;
                        border-radius: 50%;
                        background: #FF9C01;
                        margin-right: 6px;
                    }
                }

                &--excluded {
                    color: #979BA5;

                    &::before {
                        content: '';
                        display: inline-block;
                        width: 6px;
                        height: 6px;
                        border-radius: 50%;
                        background: #979BA5;
                        margin-right: 6px;
                    }
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
