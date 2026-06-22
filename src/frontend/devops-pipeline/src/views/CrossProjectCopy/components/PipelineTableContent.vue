<template>
    <div class="pipeline-table-content">
        <!-- 标题 -->
        <p
            v-if="showTitle"
            class="pipeline-title"
        >
            {{ title }}
        </p>

        <div class="pipeline-header">
            <CustomTabs
                :active-tab="innerActiveTab"
                :tabs="tabList"
                @tab-change="handleTabChange"
            />
            <div class="header-right">
                <bk-input
                    v-model="innerSearchKeyword"
                    :placeholder="$t('searchPipelineName')"
                    :right-icon="'bk-icon icon-search'"
                    :clearable="true"
                    class="pipeline-search"
                    @clear="handleSearchClear"
                ></bk-input>
            </div>
        </div>

        <bk-table
            v-bkloading="{ isLoading: innerLoading }"
            :data="innerPipelines"
            :pagination="innerPagination"
            :outer-border="false"
            :header-border="false"
            :max-height="tableMaxHeight"
            size="small"
            @page-change="handlePageChange"
            @page-limit-change="handlePageLimitChange"
        >
            <bk-table-column
                :label="$t('pipelineName')"
                prop="pipelineName"
            >
                <template slot-scope="{ row }">
                    {{ row.pipelineName }}
                </template>
            </bk-table-column>
            <bk-table-column
                :label="$t('status')"
                width="150"
            >
                <template slot-scope="{ row }">
                    <span class="status-indicator">
                        <span
                            class="status-dot"
                            :class="{ used: row.locked, unused: !row.locked }"
                        ></span>
                        <span
                            v-if="row.locked"
                            class="status-text"
                        >{{ $t('used') }}</span>
                        <span
                            v-else
                            class="status-text"
                        >{{ $t('unused') }}</span>
                    </span>
                </template>
            </bk-table-column>
            <bk-table-column
                :label="$t('operate')"
                width="150"
            >
                <template slot-scope="{ row }">
                    <a
                        class="view-detail-link"
                        @click="handleViewDetail(row)"
                    >
                        <Logo
                            name="tiaozhuan"
                            size="12"
                        />
                        {{ $t('checkPipeline') }}
                    </a>
                </template>
            </bk-table-column>
            <template #empty>
                <bk-exception
                    type="search-empty"
                    scene="part"
                ></bk-exception>
            </template>
        </bk-table>
    </div>
</template>

<script>
    import Logo from '@/components/Logo'
    import CustomTabs from './CustomTabs.vue'

    export default {
        name: 'PipelineTableContent',
        components: {
            Logo,
            CustomTabs
        },
        props: {
            // 资源数据项
            item: {
                type: Object,
                required: true
            },
            // 项目 ID
            projectId: {
                type: String,
                required: true
            },
            // 任务 ID
            taskId: {
                type: String,
                required: true
            },
            // 表格最大高度
            tableMaxHeight: {
                type: Number,
                default: 400
            },
            // 是否显示标题
            showTitle: {
                type: Boolean,
                default: false
            },
            // 标题文案
            title: {
                type: String,
                default: ''
            }
        },
        data () {
            return {
                innerPipelines: [],
                innerLoading: false,
                innerActiveTab: 'all',
                innerSearchKeyword: '',
                innerPagination: {
                    current: 1,
                    limit: 10,
                    count: 0,
                    showLimit: true,
                    showTotalCount: true
                }
            }
        },
        computed: {
            tabList () {
                return [
                    { name: 'all', label: this.$t('all') },
                    { name: 'unused', label: this.$t('unused') },
                    { name: 'used', label: this.$t('used') }
                ]
            }
        },
        created () {
            this.fetchPipelinesData()
        },
        methods: {
            async fetchPipelinesData () {
                this.innerLoading = true
                try {
                    const res = await this.$store.dispatch('crossProjectCopy/getResourcePipelines', {
                        projectId: this.projectId,
                        taskId: this.taskId,
                        resourceType: this.item.resourceType,
                        resourceId: this.item.resourceId,
                        params: {
                            ...(this.innerActiveTab === 'used' ? { locked: true } : this.innerActiveTab === 'unused' ? { locked: false } : {}),
                            ...(this.innerSearchKeyword ? { pipelineName: this.innerSearchKeyword } : {}),
                            page: this.innerPagination.current,
                            pageSize: this.innerPagination.limit
                        }
                    })
                    if (res) {
                        this.innerPipelines = res.records || res
                        this.innerPagination.count = res.count || 0
                    }
                } catch (error) {
                    this.innerPipelines = []
                    this.innerPagination.count = 0
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                } finally {
                    this.innerLoading = false
                }
            },
            handleTabChange (name) {
                this.innerActiveTab = name
                this.innerPagination.current = 1
                this.fetchPipelinesData()
            },
            handleSearchClear () {
                this.innerSearchKeyword = ''
                this.innerPagination.current = 1
                this.fetchPipelinesData()
            },
            handlePageChange (page) {
                this.innerPagination.current = page
                this.fetchPipelinesData()
            },
            handlePageLimitChange (limit) {
                this.innerPagination.limit = limit
                this.innerPagination.current = 1
                this.fetchPipelinesData()
            },
            handleViewDetail (row) {
                const url = `/console/pipeline/${this.projectId}/${row.pipelineId}/history/pipeline`
                window.open(url, '_blank')
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/scss/resource-dependency';

    .pipeline-table-content {
        .pipeline-title {
            color: #4D4F56;
            font-size: 12px;
            margin-bottom: 16px;
        }

        .pipeline-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 16px;

            .header-right {
                display: flex;
                justify-content: flex-end;
                flex: 1;
                margin-left: 20%;
            }
        }

        .status-indicator {
            display: inline-flex;
            align-items: center;
            gap: 8px;

            .status-dot {
                display: inline-block;
                width: 6px;
                height: 6px;
                border-radius: 50%;
                position: relative;

                &::before {
                    content: '';
                    position: absolute;
                    top: 50%;
                    left: 50%;
                    transform: translate(-50%, -50%);
                    width: 12px;
                    height: 12px;
                    border-radius: 50%;
                    opacity: 0.2;
                }

                &.unused {
                    background: #2DCB56;

                    &::before {
                        background: #2DCB56;
                    }
                }

                &.used {
                    background: #C4C6CC;

                    &::before {
                        background: #C4C6CC;
                    }
                }
            }

            .status-text {
                color: #4D4F56;

                &.unused {
                    color: #2DCB56;
                }

                &.used {
                    color: #979BA5;
                }
            }
        }

        .view-detail-link {
            display: inline-flex;
            align-items: center;
            color: #3A84FF;
            font-size: 12px;
            cursor: pointer;
            gap: 8px;

            &:hover {
                color: #699df4;
            }
        }
    }
</style>
