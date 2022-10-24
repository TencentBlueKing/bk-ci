<template>
    <bk-table
        v-bkloading="{ isLoading }"
        ref="pipelineTable"
        size="large"
        height="100%"
        row-key="pipelineId"
        :data="pipelineList"
        :pagination="pagination"
        @page-change="handlePageChange"
        @page-limit-change="handlePageLimitChange"
        :row-class-name="setRowCls"
        @sort-change="handleSort"
        :default-sort="sortField"
        v-on="$listeners"
    >
        <PipelineListEmpty slot="empty"></PipelineListEmpty>
        <bk-table-column v-if="isPatchView" type="selection" width="60" :selectable="checkSelecteable"></bk-table-column>
        <bk-table-column width="250" sortable="custom" :label="$t('pipelineName')" prop="pipelineName">
            <template slot-scope="props">
                <span class="pipeline-name-cell-link primary" @click="goHistory(props.row.pipelineId)">
                    {{props.row.pipelineName}}
                </span>
            </template>
        </bk-table-column>
        <bk-table-column v-if="isAllPipelineView || isPatchView || isDeleteView" width="250" :label="$t('ownGroupName')" prop="viewNames">
            <div class="pipeline-group-box-cell" slot-scope="props">
                <div class="group-name-tag-box">
                    <bk-tag v-bk-tooltips="viewName" ext-cls="pipeline-group-name-tag" v-for="viewName in props.row.viewNames" :key="viewName">
                        {{viewName}}
                    </bk-tag>
                </div>
            </div>
        </bk-table-column>
        <template v-if="isPatchView">
            <bk-table-column width="250" :label="$t('latestExec')" prop="latestBuildNum">
                <span slot-scope="props">{{ props.row.latestBuildNum ? `#${props.row.latestBuildNum}` : '--' }}</span>
            </bk-table-column>
            <bk-table-column width="150" sortable="custom" :label="$t('lastExecTime')" prop="latestBuildStartDate" />
            <bk-table-column width="250" sortable="custom" :label="$t('restore.createTime')" prop="createTime" :formatter="formatTime" />
            <bk-table-column width="250" :label="$t('creator')" prop="creator" />
        </template>
        <template v-else-if="isDeleteView">
            <bk-table-column :label="$t('restore.createTime')" sortable="custom" prop="createTime" sort :formatter="formatTime" />
            <bk-table-column :label="$t('restore.deleteTime')" sortable="custom" prop="updateTime" :formatter="formatTime" />
            <bk-table-column :label="$t('restore.deleter')" prop="lastModifyUser" column-key="lastModifyUser">
                <span slot-scope="props">
                    {{ props.row.lastModifyUser }}
                </span>
            </bk-table-column>
        </template>
        <template v-else>
            <bk-table-column :label="$t('latestExec')" key="latestBuildStatus">
                <div slot-scope="props" class="pipeline-latest-exec-cell">
                    <pipeline-status-icon :status="props.row.latestBuildStatus" />
                    <div class="pipeline-exec-msg">
                        <template v-if="props.row.latestBuildNum">
                            <p class="pipeline-exec-msg-title">
                                <b>#{{ props.row.latestBuildNum }}</b>
                                |
                                <span>{{ props.row.lastBuildMsg }}</span>
                            </p>
                            <p class="pipeline-exec-msg-desc">
                                <span class="desc">
                                    <logo :name="props.row.trigger" size="16" />
                                    <span>{{ props.row.latestBuildUserId }}</span>
                                </span>
                                <span v-if="props.row.webhookAliasName" class="desc">
                                    <logo name="branch" size="16" />
                                    <span>{{ props.row.webhookAliasName }}</span>
                                </span>
                                <span v-if="props.row.webhookMessage" class="desc">
                                    <span>{{ props.row.webhookMessage }}</span>
                                </span>
                            </p>
                        </template>
                        <p v-else class="desc">{{$t('unexecute')}}</p>
                    </div>
                </div>
            </bk-table-column>
            <bk-table-column width="150" :label="$t('lastExecTime')" prop="latestBuildStartDate" key="latestBuildStatus">
                <template slot-scope="props">
                    <p>{{ props.row.latestBuildStartDate }}</p>
                    <p v-if="props.row.progress" class="primary">{{ props.row.progress }}</p>
                    <p v-else class="desc">{{props.row.duration}}</p>
                </template>
            </bk-table-column>
        </template>
        <bk-table-column v-if="!isPatchView" width="150" :label="$t('operate')" prop="pipelineId">
            <div class="pipeline-operation-cell" slot-scope="props">
                <bk-button
                    v-if="isDeleteView"
                    text
                    theme="primary"
                    class="pipeline-exec-btn"
                    @click="handleRestore(props.row)">
                    {{ $t('restore.restore') }}
                </bk-button>
                <bk-button
                    v-else-if="props.row.delete"
                    text
                    theme="primary"
                    class="pipeline-exec-btn"
                    @click="removeHandler(props.row)">
                    {{ $t('removeFromGroup') }}
                </bk-button>
                <bk-button
                    v-else-if="!props.row.hasPermission"
                    text
                    theme="primary"
                    class="pipeline-exec-btn"
                    @click="applyPermission(props.row)">
                    {{ $t('applyPermission') }}
                </bk-button>
                <template
                    v-else
                >
                    <bk-button
                        text
                        theme="primary"
                        class="pipeline-exec-btn"
                        :disabled="props.row.lock || !props.row.canManualStartup"
                        @click="execPipeline(props.row)">
                        {{ $t(props.row.lock ? 'disabled' : 'exec') }}
                    </bk-button>
                    <ext-menu :data="props.row" :config="props.row.pipelineActions"></ext-menu>
                </template>
            </div>
        </bk-table-column>
    </bk-table>
</template>

<script>
    import piplineActionMixin from '@/mixins/pipeline-action-mixin'
    import Logo from '@/components/Logo'
    import PipelineListEmpty from '@/components/pipelineList/PipelineListEmpty'
    import ExtMenu from '@/components/pipelineList/extMenu'
    import PipelineStatusIcon from '@/components/PipelineStatusIcon'
    import {
        ALL_PIPELINE_VIEW_ID
    } from '@/store/constants'
    import { convertTime } from '@/utils/util'
    import { ORDER_ENUM, PIPELINE_SORT_FILED } from '@/utils/pipelineConst'

    export default {
        components: {
            Logo,
            ExtMenu,
            PipelineStatusIcon,
            PipelineListEmpty
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
                sortField: {
                    prop: this.sortType,
                    order: this.$route.query.collation ?? ORDER_ENUM.ascending
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
                this.sortField.prop = newSort
                this.requestList({
                    sortType: newSort
                })
            },
            filterParams: function (filterMap) {
                this.requestList({
                    ...filterMap,
                    page: 1
                })
            }
        },
        created () {
            this.requestList()
        },
        methods: {
            setRowCls ({ row }) {
                const clsObj = {
                    'has-delete': row.delete,
                    'no-permission': !row.hasPermission
                }
                return Object.keys(clsObj).filter(key => clsObj[key]).join(' ')
            },
            checkSelecteable (row) {
                return !row.delete
            },
            clearSelection () {
                this.$refs.pipelineTable?.clearSelection?.()
            },
            handlePageLimitChange (limit) {
                this.pagination.limit = limit
                this.$nextTick(this.requestList)
            },
            handlePageChange (page) {
                this.pagination.current = page
                this.$nextTick(this.requestList)
            },
            handleSort ({ prop, order }) {
                console.log('sort change', prop, order)
                Object.assign(this.sortField, {
                    order: prop ? ORDER_ENUM[order] : undefined,
                    prop: PIPELINE_SORT_FILED[prop]
                })
                this.$nextTick(this.requestList)
            },
            async requestList (query = {}) {
                this.isLoading = true
                const { count, page, records } = await this.getPipelines({
                    page: this.pagination.current,
                    pageSize: this.pagination.limit,
                    sortType: this.sortField.prop,
                    collation: this.sortField.order,
                    viewId: this.$route.params.viewId,
                    ...this.filterParams,
                    ...query
                })
                Object.assign(this.pagination, {
                    count,
                    current: page
                })
                this.pipelineList = records
                console.log(records)
                this.isLoading = false
            },
            refresh () {
                this.requestList()
            },
            formatTime (row, cell, value) {
                return convertTime(value)
            },
            async handleRestore (...args) {
                const res = await this.restore(...args)
                if (res) {
                    this.refresh()
                }
            }
        }
    }

</script>

<style lang="scss">
    @import '@/scss/conf.scss';
    .pipeline-name-cell-link {
        cursor: pointer;
    }
    .primary {
        color: $primaryColor;
    }
    .desc {
        color: #979BA5;
    }
    tr.no-permission {
        background-color: #F5F7FA;
    }
    tr.has-delete {
        color: #C4C6CC;
    }
</style>
