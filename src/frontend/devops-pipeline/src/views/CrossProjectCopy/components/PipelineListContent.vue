<template>
    <div class="pipeline-list-content">
        <!-- 上方: CustomTabs + 搜索框 -->
        <div class="pipeline-header">
            <custom-tabs
                :active-tab="currentStatusTab"
                :tabs="statusTabs"
                @tab-change="handleStatusTabChange"
            />
            <search-select
                class="search-input"
                :data="searchData"
                :values="searchValues"
                :placeholder="searchPlaceholder"
                @change="handleSearchChange"
            />
        </div>
        
        <!-- 下方: 流水线表格 -->
        <bk-table
            :data="pipelineTableData"
            :pagination="pagination"
            :outer-border="false"
            :header-border="false"
            :max-height="740"
            v-bkloading="{ isLoading: tableLoading }"
            class="pipeline-table"
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
            ></bk-table-column>
            <bk-table-column
                :label="$t('execStatus')"
            >
                <template slot-scope="{ row }">
                    <div class="status-cell">
                        <div class="status-left">
                            <span :class="['status-text', `status-text--${row.status}`]">
                                {{ getStatusText(row.status) }}
                            </span>
                            <bk-popover
                                placement="top"
                                :tippy-options="{
                                    theme: 'light'
                                }"
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
                        </div>
                        <bk-button
                            v-if="row.status === PipelineBatchTaskDetailStatus.FAILED"
                            text
                            theme="primary"
                            class="status-right"
                            @click="handleRetry(row)"
                        >
                            {{ $t('retry') }}
                        </bk-button>
                    </div>
                </template>
            </bk-table-column>
        </bk-table>
    </div>
</template>

<script>
    import { mapActions } from 'vuex'
    import SearchSelect from '@blueking/search-select'
    import CustomTabs from './CustomTabs.vue'
    import '@blueking/search-select/dist/styles/index.css'
    import { PipelineBatchTaskDetailStatus, PipelineBatchTaskDetailErrorType } from '@/store/modules/crossProjectCopy/constants'
    
    export default {
        name: 'PipelineListContent',
        components: {
            CustomTabs,
            SearchSelect
        },
        props: {
            statusSummary: {
                type: Object,
                default: () => ({
                    successCount: 0,
                    failedCount: 0,
                    totalCount: 0
                })
            }
        },
        data () {
            return {
                currentStatusTab: 'all',
                tabs: [
                    { name: 'all', label: this.$t('all'), totalCount: 0 },
                    { name: 'SUCCESS', label: this.$t('successItems'), totalCount: 0 },
                    { name: 'FAILED', label: this.$t('failedItems'), totalCount: 0 }
                ],
                searchValues: [],
                pipelineTableData: [],
                // 表格 loading 状态
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
            taskId () {
                return this.$route.params.taskId
            },
            statusTabs () {
                return this.tabs.map(tab => ({
                    ...tab,
                    label: `${tab.label}（${tab.totalCount}）`
                }))
            },
            searchData () {
                return [
                    {
                        id: 'pipelineName',
                        name: this.$t('pipelineName'),
                        default: true
                    },
                    {
                        id: 'creator',
                        name: this.$t('creator'),
                        default: true
                    }
                ]
            },
            searchPlaceholder () {
                return `${this.$t('search')} ${this.searchData.map(item => item.name).join('/')}`
            }
        },
        watch: {
            // 监听父组件传递的statusSummary数据，更新tabs的totalCount
            statusSummary: {
                immediate: true,
                handler (newVal) {
                    const { successCount = 0, failedCount = 0 } = newVal || {}
                    this.tabs = this.tabs.map(tab => {
                        if (tab.name === 'SUCCESS') return { ...tab, totalCount: successCount }
                        if (tab.name === 'FAILED') return { ...tab, totalCount: failedCount }
                        if (tab.name === 'all') return { ...tab, totalCount: successCount + failedCount }
                        return tab
                    })
                }
            }
        },
        created () {
            this.PipelineBatchTaskDetailStatus = PipelineBatchTaskDetailStatus
            this.PipelineBatchTaskDetailErrorType = PipelineBatchTaskDetailErrorType
            
            // 从query参数读取statusTab，如果指定了失败项则切换到失败项标签页
            const { statusTab } = this.$route.query
            if (statusTab && statusTab === 'FAILED') {
                this.currentStatusTab = 'FAILED'
            }
        },
        async mounted () {
            this.fetchPipelineList()
        },
        methods: {
            ...mapActions('crossProjectCopy', [
                'getTaskDetails',
                'retryFailedTaskDetail'
            ]),
            getStatusText (status) {
                const statusMap = {
                    [PipelineBatchTaskDetailStatus.EXCLUDED]: this.$t('excluded'),
                    [PipelineBatchTaskDetailStatus.SUCCESS]: this.$t('success'),
                    [PipelineBatchTaskDetailStatus.FAILED]: this.$t('failed'),
                    [PipelineBatchTaskDetailStatus.WAIT_COPY]: this.$t('pendingCopy')
                }
                return statusMap[status] || '-'
            },
            handleStatusTabChange (tabName) {
                this.currentStatusTab = tabName
                this.pagination.current = 1
                this.fetchPipelineList()
            },
            handleSearchChange (values) {
                this.searchValues = values
                this.pagination.current = 1
                this.fetchPipelineList()
            },
            handlePageChange (page) {
                this.pagination.current = page
                this.fetchPipelineList()
            },
            handlePageLimitChange (limit) {
                this.pagination.limit = limit
                this.pagination.current = 1
                this.fetchPipelineList()
            },
            getSearchParams () {
                return this.searchValues.reduce((acc, item) => {
                    if (item.values && item.values.length > 0) {
                        acc[item.id] = item.values.map(v => v.id).join(',')
                    }
                    return acc
                }, {})
            },
            async fetchPipelineList () {
                this.tableLoading = true
                try {
                    const searchParams = this.getSearchParams()
                    
                    const params = {
                        ...searchParams,
                        ...(this.currentStatusTab === 'all' ? {} : {status: this.currentStatusTab}),
                        page: this.pagination.current,
                        pageSize: this.pagination.limit
                    }
                    const data = await this.getTaskDetails({
                        projectId: this.projectId,
                        taskId: this.taskId,
                        params
                    })
                    this.pipelineTableData = data?.records || []
                    this.pagination.count = data?.count || 0
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                } finally {
                    this.tableLoading = false
                }
            },
            handleJump (resourceType, err) {
                this.$emit('jump', {...err, resourceType})
            },
            handleRetry (row) {
                this.$emit('retry', row)
            }
        }
    }
</script>

<style lang="scss" scoped>
.pipeline-list-content {
    .pipeline-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 16px;
        
        .search-input {
            width: 400px;
        }
    }
    
    .pipeline-table {
        background: white;
        
        .pipeline-name-cell {
            display: flex;
            align-items: center;
            gap: 8px;

            .pipeline-name-text {
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            }

            .sub-tag {
                padding: 2px 6px;
                border-radius: 2px;
                font-size: 10px;
                color: #1768EF;
                background: #E1ECFF;
            }
            
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
                flex-shrink: 0;
                
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
            
            .template-tag,
            .sub-pipeline-tag {
                padding: 2px 6px;
                border-radius: 2px;
                font-size: 10px;
                color: #4D4F56;
                border: 1px solid #DCDEE5;
                background-color: #F0F1F5;
                flex-shrink: 0;
            }
        }
        
        .status-cell {
            display: flex;
            align-items: center;
            justify-content: space-between;

            .status-left {
                display: flex;
                align-items: center;
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
