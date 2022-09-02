<template>
    <bk-table
        v-bkloading="{ isLoading }"
        size="large"
        :data="pipelineList"
        :pagination="pagination"
        @page-change="handlePageChange"
        @page-limit-change="handlePageLimitChange">
        <bk-table-column v-if="isPatchOperate" type="selection" width="60"></bk-table-column>
        <bk-table-column width="250" sortable :label="$t('pipelineName')" prop="pipelineName"></bk-table-column>
        <bk-table-column v-if="isAllPipelineView" width="250" :label="$t('ownGroupName')" prop="viewNames">
            <div class="pipeline-group-box-cell" slot-scope="props">
                <div class="group-name-tag-box">
                    <bk-tag v-bk-tooltips="viewName" ext-cls="group-name-tag" v-for="viewName in props.row.viewNames" :key="viewName">
                        {{viewName}}
                    </bk-tag>
                </div>
            </div>
        </bk-table-column>
        <bk-table-column :label="$t('latestExec')">
            <template slot-scope="props">
                <div class="pipeline-latest-exec-cell">
                    <pipeline-status-icon :status="props.row.latestBuildStatus" />
                    <div class="pipeline-exec-msg">
                        <template v-if="props.row.latestBuildNum">
                            <p class="pipeline-exec-msg-title">
                                <b>#{{ props.row.latestBuildNum }}</b>
                                |
                                <span>{{ props.row.lastBuildMsg }}</span>
                            </p>
                            <p class="flex-row">
                                <span class="desc flex-row">
                                    <logo name="manualTrigger" size="16" />
                                    {{ props.row.latestBuildUserId }}
                                </span>
                                <span v-if="props.row.webhookRepoUrl" class="desc flex-row">
                                    <logo :name="props.row.trigger" size="16" />
                                    {{ props.row.webhookRepoUrl }}
                                </span>
                            </p>
                        </template>
                        <p v-else class="desc">{{$t('unexecute')}}</p>
                    </div>
                </div>
            </template>
        </bk-table-column>
        <bk-table-column width="150" :label="$t('lastExecTime')" prop="latestBuildStartDate">
            <template slot-scope="props">
                <p>{{ props.row.latestBuildStartDate }}</p>
                <p>{{ props.row.progress }}</p>
            </template>
        </bk-table-column>
        <bk-table-column width="150" :label="$t('operate')">
            <div class="pipeline-operation-cell" slot-scope="props">

                <bk-button
                    v-if="isDeleteView"
                    text
                    theme="primary"
                    class="pipeline-exec-btn"
                    @click="restore(props.row)">
                    {{ $t('restore.restore') }}
                </bk-button>
                <bk-button
                    v-else
                    text
                    theme="primary"
                    class="pipeline-exec-btn"
                    :disabled="props.row.lock || !props.row.canManualStartup"
                    @click="execPipeline(props.row)">
                    {{ $t(props.row.lock ? 'disabled' : 'exec') }}
                </bk-button>
                <ext-menu v-if="!isDeleteView" :data="props.row" :config="props.row.pipelineActions"></ext-menu>
            </div>
        </bk-table-column>
    </bk-table>
</template>

<script>
    import piplineActionMixin from '@/mixins/pipeline-action-mixin'
    import Logo from '@/components/Logo'
    import ExtMenu from '@/components/pipelineList/extMenu'
    import PipelineStatusIcon from '@/components/PipelineStatusIcon'
    import {
        ALL_PIPELINE_VIEW_ID,
        DELETED_VIEW_ID
    } from '@/store/constants'

    export default {
        components: {
            Logo,
            ExtMenu,
            PipelineStatusIcon
        },
        mixins: [piplineActionMixin],
        props: {
            filterParams: {
                type: Object,
                default: () => ({})
            },
            sortType: {
                type: String,
                default: 'CREATE_TIME'
            },
            fetchPipelines: {
                type: Function,
                required: true
            }
        },
        data () {
            return {
                isLoading: false,
                isPatchOperate: false,
                pipelineList: [],
                pagination: {
                    current: 1,
                    limit: 20,
                    count: 0
                },
                activePipeline: null
            }
        },
        computed: {
            isAllPipelineView () {
                return this.$route.params.viewId === ALL_PIPELINE_VIEW_ID
            },
            isDeleteView () {
                return this.$route.params.viewId === DELETED_VIEW_ID
            }
        },
        watch: {
            '$route.params.viewId': function (viewId) {
                this.getPipelines({
                    viewId
                })
            },
            sortType: function (newSort) {
                this.getPipelines({
                    sortType: newSort
                })
            },
            filterParams: function (filterMap) {
                this.getPipelines(filterMap)
            }
        },
        created () {
            this.getPipelines()
        },
        methods: {

            handlePageLimitChange (limit) {
                this.pagination.limit = limit
                this.$nextTick(this.getPipelines)
            },
            handlePageChange (page) {
                this.pagination.current = page
                this.$nextTick(this.getPipelines)
            },
            async getPipelines (query = {}) {
                this.isLoading = true
                const { count, page, records } = await this.fetchPipelines({
                    page: this.pagination.current,
                    pageSize: this.pagination.limit,
                    sortType: this.sortType,
                    viewId: this.$route.params.viewId,
                    ...this.filterParams,
                    ...query
                })
                Object.assign(this.pagination, {
                    count,
                    current: page
                })
                this.pipelineList = records

                this.isLoading = false
            }
        }
    }

</script>

<style lang="scss">
    @import '@/scss/mixins/ellipsis';
    @import '@/scss/conf';

    .pipeline-group-section {
        flex: 1;
        display: flex;
        .flex-row {
            display: flex;
            align-items: center;
        }
        .pipeline-list-main {
            flex: 1;
            padding: 24px;
            .current-pipeline-group-name {
                font-size: 14px;
                line-height: 22px;
                margin: 0 0 16px 0;

            }
            .pipeline-list-main-header {
                display: flex;
                justify-content: space-between;
                margin-bottom: 16px;
                .pipeline-list-main-header-left-area {
                    width: 300px
                }
                .pipeline-list-main-header-right-area {
                    flex: 1;
                    display: flex;
                    .search-pipeline-input {
                        flex: 1;
                        margin-right: 16px;
                    }
                }
            }
            .pipeline-latest-exec-cell {
                display: flex;
                align-items: center;
                .pipeline-exec-status-icon {
                    display: inline-flex;
                    font-size: 22px;
                    margin-right: 10px;

                }
                .pipeline-exec-msg {
                    display: flex;
                    flex-direction: column;
                    font-size: 12px;
                    line-height: 20px;
                    margin-left: 12px;
                    overflow: hidden;
                    .desc {
                        color: #979BA5;
                    }
                    .pipeline-exec-msg-title {
                        @include ellipsis();
                        flex: 1;
                    }
                }
            }
            .hidden {
                visibility: hidden;
            }
            .pipeline-operation-cell {
                display: flex;
                align-items: center;
                .pipeline-exec-btn {
                    width: 60px;
                }
                .more-action-menu {
                    font-size: 0;
                    cursor: pointer;
                    .more-action-menu-trigger {
                        font-size: 18px;
                        padding: 0 6px;
                    }
                }
            }
        }
    }

    .more-action-menu-list {
        .more-action-menu-item {
            line-height: 32px;
            cursor: pointer;
            &:hover {
                background: #E1ECFF;
            }
        }
    }
    .pipeline-group-visible-range-group {
        > :first-child {
            margin-right: 48px;
        }
    }
    .pipeline-group-box-cell {
        display: flex;
        .group-name-tag {
            @include ellipsis();
            max-width: 100px;
        }
    }
</style>
