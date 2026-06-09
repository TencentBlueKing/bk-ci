<template>
    <div class="batch-historical-task">
        <div class="task-header">
            <div class="header-left">
                <Logo
                    name="arrows-left"
                    size="14"
                    @click.native="goBack"
                />
                <span class="header-title">{{ $t('batchTaskHistory') }}</span>
                <span class="header-desc">{{ $t('viewBatchTaskHistory') }}</span>
            </div>
        </div>

        <bk-alert type="warning">
            <div slot="title">
                {{ $t('taskingAlert', [1]) }}
                <span
                    @click="handleViewExecuting"
                    class="task-tips"
                >{{ $t('viewExecutingTask') }}</span>
            </div>
        </bk-alert>

        <div class="task-filters">
            <div class="filter-tabs">
                <div class="filter-left">
                    <custom-tabs
                        :active-tab="activeTab"
                        :tabs="statusTabs"
                        @tab-change="changeTab"
                    />
                    <bk-checkbox
                        v-model="onlyMyTask"
                        class="only-my-task-checkbox"
                        @change="handleFilterChange"
                    >
                        {{ $t('onlyMyTask') }}
                    </bk-checkbox>
                </div>
            </div>
            <div class="filter-search">
                <search-select
                    v-model="searchValue"
                    :data="searchData"
                    :placeholder="$t('taskSearchPlaceholder')"
                    @change="handleSearchChange"
                />
            </div>
        </div>

        <div class="task-content">
            <bk-table
                :data="taskList"
                :pagination="pagination"
                v-bkloading="{ isLoading: tableLoading }"
                @page-change="handlePageChange"
                @page-limit-change="handlePageLimitChange"
            >
                <bk-table-column
                    :label="$t('task')"
                    min-width="500"
                >
                    <template slot-scope="{ row }">
                        <div class="task-info">
                            <span
                                class="task-link"
                                @click="viewDetail(row)"
                            >
                                {{ row.taskName }}
                            </span>
                            <div class="task-id">{{ row.taskId }}</div>
                        </div>
                    </template>
                </bk-table-column>
                <bk-table-column
                    :label="$t('taskType')"
                    prop="taskType"
                >
                    <template slot-scope="{ row }">
                        <span>{{ row.taskType === 'PIPELINE_COPY' ? $t('pipelineCopy') : '' }}</span>
                    </template>
                </bk-table-column>
                <bk-table-column
                    :label="$t('execProgress')"
                >
                    <template slot-scope="{ row }">
                        <div class="progress-info">
                            <p class="progress-text">{{ row.progress }}%</p>
                            <p class="progress-count">{{ row.totalCount > 0 ? `${row.executedCount}/${row.totalCount}` : '--' }}</p>
                        </div>
                    </template>
                </bk-table-column>
                <bk-table-column
                    :label="$t('execResult')"
                >
                    <template slot-scope="{ row }">
                        <div class="status-wrapper">
                            <p class="status-info">
                                <Logo
                                    v-if="row.status === 'EXECUTING'"
                                    name="spinner"
                                    size="16"
                                />
                                <span
                                    v-else
                                    :class="['status-icon', `icon-${row.status}`]"
                                />
                                <span class="status-text">
                                    {{ getStatusLabel(row.status) }}
                                </span>
                            </p>
                            <div
                                v-if="row.progress > 0"
                                class="status-count"
                            >
                                <span
                                    v-if="row.successCount"
                                    class="count-badge success-badge"
                                >
                                    {{ row.successCount }}
                                </span>
                                <span
                                    v-if="row.failedCount"
                                    class="count-badge failed-badge"
                                >
                                    {{ row.failedCount }}
                                </span>
                                <span v-if="!row.successCount && !row.failedCount">--</span>
                            </div>
                            <div
                                v-else
                                class="status-count"
                            >
                                --
                            </div>
                        </div>
                    </template>
                </bk-table-column>
                <bk-table-column
                    :label="$t('submitter')"
                    prop="creator"
                />
                <bk-table-column
                    :label="$t('updateTime')"
                    prop="updateTime"
                    :formatter="formatTime"
                />
                <bk-table-column
                    :label="$t('operation')"
                >
                    <template slot-scope="{ row }">
                        <div class="task-btns">
                            <template v-if="row.status === 'DRAFT'">
                                <bk-button
                                    text
                                    size="small"
                                    @click="continueEdit(row)"
                                >
                                    {{ $t('continueEdit') }}
                                </bk-button>
                                <bk-button
                                    text
                                    size="small"
                                    @click="discardDraft(row)"
                                >
                                    {{ $t('discardDraft') }}
                                </bk-button>
                            </template>
                            <bk-button
                                v-if="row.status !== 'DRAFT'"
                                text
                                size="small"
                                @click="viewDetail(row)"
                            >
                                {{ $t('detail') }}
                            </bk-button>
                            <bk-button
                                v-if="['FAILED', 'PARTIAL_FAILED'].includes(row.status)"
                                text
                                size="small"
                                @click="viewRetryRecord(row)"
                            >
                                {{ $t('retryFailed') }}
                            </bk-button>
                        </div>
                    </template>
                </bk-table-column>
            </bk-table>
        </div>
    </div>
</template>

<script>
    import Logo from '@/components/Logo'
    import CustomTabs from '@/views/CrossProjectCopy/components/CustomTabs.vue'
    import { mapActions } from 'vuex'
    import SearchSelect from '@blueking/search-select'
    import '@blueking/search-select/dist/styles/index.css'
    import { convertTime } from '@/utils/util'
    import { PipelineBatchTaskStep } from '@/store/modules/crossProjectCopy/constants'

    export default {
        name: 'BatchHistoricalTask',
        components: {
            Logo,
            CustomTabs,
            SearchSelect
        },
        data () {
            return {
                activeTab: 'all',
                onlyMyTask: false,
                searchValue: [],
                taskList: [],
                tableLoading: false,
                pagination: {
                    current: 1,
                    count: 0,
                    limit: 10
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            statusTabs () {
                return [
                    { name: 'all', label: this.$t('allTask'), count: 0 },
                    { name: 'EXECUTING', label: this.$t('executing'), count: 0 },
                    { name: 'SUCCESS', label: this.$t('success'), count: 0 },
                    { name: 'FAILED', label: this.$t('failed'), count: 0 },
                    { name: 'DRAFT', label: this.$t('draft'), count: 0 }
                ]
            },
            searchData () {
                const usedIds = (this.searchValue || []).map(v => v.id)
                const list = [
                    {
                        id: 'taskName',
                        name: this.$t('taskName'),
                        default: true
                    },
                    {
                        id: 'taskId',
                        name: this.$t('taskId'),
                        default: true
                    },
                    {
                        id: 'type',
                        name: this.$t('taskType'),
                        children: [
                            { id: 'PIPELINE_COPY', name: this.$t('pipelineCopy') }
                        ]
                    },
                    {
                        id: 'targetProject',
                        name: this.$t('targetProject'),
                        default: true
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
        mounted () {
            this.fetchTaskList()
        },
        methods: {
            ...mapActions('crossProjectCopy', ['getTaskList', 'deleteTask']),
            formatTime (row, cell, value) {
                return convertTime(value)
            },
            /**
             * 把 SearchSelect 的搜索条件转换为接口可用的参数
             */
            getSearchParams () {
                return (this.searchValue || []).reduce((acc, item) => {
                    if (item.values && item.values.length > 0) {
                        acc[item.id] = item.values[0]
                    }
                    return acc
                }, {})
            },
            handleViewExecuting () {
                // 切换activeTab到执行中
                this.activeTab = 'EXECUTING'
                this.pagination.current = 1
                this.fetchTaskList()
            },
            changeTab (tabId) {
                this.activeTab = tabId
                this.pagination.current = 1
                this.fetchTaskList()
            },
            handleFilterChange () {
                this.pagination.current = 1
                this.fetchTaskList()
            },
            handleSearchChange () {
                this.pagination.current = 1
                this.fetchTaskList()
            },
            async fetchTaskList () {
                this.tableLoading = true
                try {
                    const params = {
                        type: 'PIPELINE_COPY',
                        page: this.pagination.current,
                        pageSize: this.pagination.limit,
                        ...(this.activeTab === 'all' ? {} : { status: this.activeTab })
                    }

                    if (this.onlyMyTask) {
                        const currentUser = this.$userInfo.username || ''
                        if (currentUser) {
                            params.creator = currentUser
                        }
                    }

                    const searchParams = this.getSearchParams()
                    Object.assign(params, searchParams)

                    const res = await this.getTaskList({
                        projectId: this.projectId,
                        params
                    })

                    this.taskList = (res.records || []).map(row => {
                        const successCount = row.successCount || 0
                        const failedCount = row.failedCount || 0
                        const totalCount = row.totalCount || 0
                        const executedCount = successCount + failedCount
                        const progress = totalCount > 0 ? Math.round(executedCount / totalCount * 100) : 0
                        
                        return {
                            ...row,
                            successCount,
                            failedCount,
                            executedCount,
                            progress
                        }
                    })
                    this.pagination.count = res.count || 0
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                } finally {
                    this.tableLoading = false
                }
            },
            handlePageChange (page) {
                this.pagination.current = page
                this.fetchTaskList()
            },
            handlePageLimitChange (limit) {
                this.pagination.limit = limit
                this.pagination.current = 1
                this.fetchTaskList()
            },
            getStatusLabel (status) {
                const labelMap = {
                    DRAFT: this.$t('draft'),
                    EXECUTING: this.$t('executing'),
                    SUCCESS: this.$t('success'),
                    FAILED: this.$t('failed'),
                    PARTIAL_FAILED: this.$t('failed')
                }
                return labelMap[status] || status
            },
            discardDraft (row) {
                const h = this.$createElement
                this.$bkInfo({
                    title: this.$t('confirmDiscardDraft'),
                    okText: this.$t('discard'),
                    theme: 'danger',
                    width: 480,
                    confirmLoading: true,
                    subHeader: h('div', [
                        h('p', {
                            class: 'discard-draft-task-name'
                        }, `${this.$t('task')}：${row.taskName}`),
                        h('p', {
                            class: 'discard-draft-warning'
                        }, this.$t('discardDraftWarning'))
                    ]),
                    extCls: 'discard-draft-info',
                    confirmFn: async () => {
                        try {
                            await this.deleteTask({
                                projectId: this.projectId,
                                taskId: row.taskId
                            })
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('discardDraftSuccess')
                            })
                            this.fetchTaskList()
                        } catch (error) {
                            this.$bkMessage({
                                theme: 'error',
                                message: error.message || error
                            })
                        }
                    }
                })
            },
            /**
             * 继续编辑草稿
             */
            continueEdit (row) {
                this.$router.push({
                    name: 'crossProjectCopy',
                    params: {
                        projectId: this.projectId,
                        taskId: row.taskId,
                        tab: PipelineBatchTaskStep[row.step]
                    }
                })
            },
            /**
             * 查看任务详情
             */
            viewDetail (row) {
                this.$router.push({
                    name: 'crossProjectCopy',
                    params: {
                        projectId: this.projectId,
                        taskId: row.taskId,
                        tab: PipelineBatchTaskStep.EXECUTE
                    }
                })
            },
            /**
             * 查看重试记录
             */
            viewRetryRecord (row) {
                this.$router.push({
                    name: 'crossProjectCopy',
                    params: {
                        projectId: this.projectId,
                        taskId: row.taskId,
                        tab: PipelineBatchTaskStep.EXECUTE
                    },
                    query: {
                        statusTab: 'FAILED' // 跳转到失败项标签页
                    }
                })
            },
            goBack () {
                this.$router.push({
                    name: 'PipelineManageList',
                    params: {
                        projectId: this.projectId
                    }
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
.batch-historical-task {
    height: 100%;
    display: flex;
    flex-direction: column;
    background: #f5f7fa;

    .task-header {
        padding: 16px 24px;
        background: #fff;
        border-bottom: 1px solid #dcdee5;

        .header-left {
            display: flex;
            align-items: center;
            gap: 8px;

            .header-title {
                font-size: 16px;
                color: #313238;
            }

            .header-desc {
                font-size: 12px;
                color: #979ba5;
                padding-left: 8px;
                border-left: 1px solid #C4C6CC;
            }
        }
    }

    .task-tips {
        color: #3A84FF;
        cursor: pointer;
        margin-left: 8px;
    }

        .task-filters {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 24px 24px 16px;

            .filter-tabs {
                display: flex;
                align-items: center;
                gap: 24px;

                .filter-left {
                    display: flex;
                    align-items: center;

                    .custom-tabs {
                        background-color: #EAEBF0;
                    }
        
                    .only-my-task-checkbox {
                        margin-left: 24px;
                    }
                }
            }

        .filter-search {
            margin-left: 30%;
            flex: 1;
            
            ::v-deep .search-select-wrap {
                background-color: #fff;
            }
        }
    }

    .task-content {
        flex: 1;
        overflow: hidden;
        padding: 0 24px 16px;
        border-radius: 2px;

        ::v-deep .bk-table {
            background-color: #fff;
        }

        .task-info {
            display: flex;
            flex-direction: column;
            margin: 8px 0;
            gap: 2px;

            .task-link {
                color: #3A84FF;
                cursor: pointer;

                &:hover {
                    color: #699df4;
                }
            }

            .task-id {
                font-size: 12px;
                color: #979ba5;
                margin-top: 4px;
            }
        }

        .progress-info {
            display: flex;
            flex-direction: column;
            margin: 8px 0;
            gap: 4px;

            .progress-text {
                font-size: 12px;
                color: #63656e;
            }

            .progress-count {
                font-size: 10px;
                border-radius: 2px;
                padding: 2px 4px;
                background-color: #F0F1F5;
                align-self: flex-start;
            }
        }

        .status-wrapper {
            display: flex;
            flex-direction: column;
            margin: 8px 0;
            gap: 4px;

            .status-info {
                display: flex;
                align-items: center;
                gap: 4px;

                .status-icon {
                    display: inline-block;
                    width: 8px;
                    height: 8px;
                    border: 1px solid transparent;
                    border-radius: 50%;
    
                    &.icon-DRAFT {
                        background-color: #F5F7FA;
                        border-color: #C4C6CC;
                    }
    
                    &.icon-SUCCESS {
                        background-color: #DAF6E5;
                        border-color: #2CAF5E;
                    }
    
                    &.icon-FAILED, &.icon-PARTIAL_FAILED {
                        background-color: #FFEBEB;
                        border-color: #EA3636;
                    }
                }
    
                .status-text {
                    font-size: 12px;
                    color: #4D4F56;
                    align-self: flex-start;
                }
            }

            .status-count {
                display: flex;
                align-items: center;
                gap: 4px;
                font-size: 10px;
                color: #63656e;
                margin-left: 10px;

                .count-badge {
                    padding: 0 6px;
                    border-radius: 2px;
                }

                .success-badge {
                    color: #299e56;
                    background-color: #DAF6E5;
                }

                .failed-badge {
                    color: #e71818;
                    background-color: #FFEBEB;
                }
            }
        }
        
        .task-btns {
            display: flex;
            align-items: center;
            text-wrap: nowrap;
        }
    }
}
.discard-draft-info {
    font-size: 14px;

    .discard-draft-task-name {
        margin-bottom: 16px;
        color: #313238;
    }

    .discard-draft-warning {
        background-color: #F5F7FA;
        padding: 12px 16px;
        color: #4D4F56;
    }
    ::v-deep .bk-dialog-wrapper .bk-info-box .bk-dialog-sub-header {
        padding: 0 32px !important;
    }
}
</style>
