<template>
    <bk-table
        v-bkloading="{ isLoading }"
        ref="pipelineTable"
        size="large"
        height="100%"
        :data="pipelineList"
        :pagination="pagination"
        @page-change="handlePageChange"
        @page-limit-change="handlePageLimitChange"
        v-on="$listeners"
    >
        <bk-table-column v-if="isPatchView" type="selection" width="60"></bk-table-column>
        <bk-table-column width="250" sortable :label="$t('pipelineName')" prop="pipelineName">
            <template slot-scope="props">
                <span @click="goHistory(props.row.pipelineId)">{{props.row.pipelineName}}</span>
            </template>
        </bk-table-column>
        <bk-table-column v-if="isAllPipelineView || isPatchView" width="250" :label="$t('ownGroupName')" prop="viewNames">
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
                <span v-if="isPatchView">
                    <b>#{{ props.row.latestBuildNum }}</b>
                </span>
                <div v-else class="pipeline-latest-exec-cell">
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
        <template v-if="isPatchView">
            <bk-table-column width="250" sortable :label="$t('restore.createTime')" prop="createTime"></bk-table-column>
            <bk-table-column width="250" sortable :label="$t('creator')" prop="creator"></bk-table-column>
        </template>
        <bk-table-column v-if="!isPatchView" width="150" :label="$t('operate')">
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
        ALL_PIPELINE_VIEW_ID
    } from '@/store/constants'

    export default {
        components: {
            Logo,
            ExtMenu,
            PipelineStatusIcon
        },
        mixins: [piplineActionMixin],
        props: {
            isPatchView: Boolean,
            filterParams: {
                type: Object,
                default: () => ({})
            },
            sortType: {
                type: String,
                default: 'CREATE_TIME'
            }
        },
        data () {
            return {
                isLoading: false,
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
            }
        },
        watch: {
            '$route.params.viewId': function (viewId) {
                this.requestList({
                    viewId
                })
            },
            sortType: function (newSort) {
                this.requestList({
                    sortType: newSort
                })
            },
            filterParams: function (filterMap) {
                this.requestList(filterMap)
            }
        },
        created () {
            this.requestList()
        },
        methods: {
            clearSelection () {
                console.log(this.$refs.pipelineTable)
                this.$refs.pipelineTable?.clearSelection?.()
            },
            handlePageLimitChange (limit) {
                this.pagination.limit = limit
                this.$nextTick(this.getPipelines)
            },
            handlePageChange (page) {
                this.pagination.current = page
                this.$nextTick(this.getPipelines)
            },
            async requestList (query = {}) {
                this.isLoading = true
                const { count, page, records } = await this.getPipelines({
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
            },
            refresh () {
                this.requestList()
            }
        }
    }

</script>
