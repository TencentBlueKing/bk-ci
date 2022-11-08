<template>
    <bk-table
        v-bkloading="{ isLoading }"
        ref="pipelineTable"
        size="large"
        row-key="pipelineId"
        height="100%"
        :data="pipelineList"
        :pagination="pagination"
        @page-change="handlePageChange"
        @page-limit-change="handlePageLimitChange"
        :row-class-name="setRowCls"
        @sort-change="handleSort"
        :default-sort="sortField"
        @selection-change="handleSelectChange"
        v-on="$listeners"
    >
        <PipelineListEmpty slot="empty" :is-patch="isPatchView"></PipelineListEmpty>
        <div v-if="selectionLength > 0" slot="prepend" class="selected-all-indicator">
            <span v-html="$t('selectedCount', [selectionLength])"></span>
            <bk-button theme="primary" text @click="clearSelection">
                {{$t('clearSelection')}}
            </bk-button>
        </div>
        <bk-table-column v-if="isPatchView" type="selection" width="60" :selectable="checkSelecteable"></bk-table-column>
        <bk-table-column width="250" sortable="custom" :label="$t('pipelineName')" prop="pipelineName">
            <template slot-scope="props">
                <!-- hack disabled event -->
                <router-link
                    v-if="!isDeleteView && props.row.historyRoute"
                    class="pipeline-cell-link"
                    :disabled="!props.row.hasPermission"
                    :event="props.row.hasPermission ? 'click' : ''"
                    :to="props.row.historyRoute">
                    {{props.row.pipelineName}}
                </router-link>
                <span v-else>{{props.row.pipelineName}}</span>
            </template>
        </bk-table-column>
        <bk-table-column v-if="isAllPipelineView || isPatchView || isDeleteView" width="250" :label="$t('ownGroupName')" prop="viewNames">
            <div class="pipeline-group-box-cell" slot-scope="props">
                <div class="group-name-tag-box">
                    <bk-tag
                        ext-cls="pipeline-group-name-tag"
                        v-for="(viewName, index) in props.row.viewNames"
                        :key="index"
                        @click="goGroup(viewName)"
                    >
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
                            <router-link
                                class="pipeline-cell-link pipeline-exec-msg-title"
                                :disabled="!props.row.hasPermission"
                                :event="props.row.hasPermission ? 'click' : ''"
                                :to="props.row.latestBuildRoute"
                            >
                                <b>#{{ props.row.latestBuildNum }}</b>
                                |
                                <span>{{ props.row.lastBuildMsg }}</span>
                            </router-link>
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
            <bk-table-column width="250" :label="$t('lastExecTime')" prop="latestBuildStartDate" key="latestBuildStatus">
                <template slot-scope="props">
                    <p>{{ props.row.latestBuildStartDate }}</p>
                    <p v-if="props.row.progress" class="primary">{{ props.row.progress }}</p>
                    <p v-else class="desc">{{props.row.duration}}</p>
                </template>
            </bk-table-column>
            <bk-table-column width="250" :label="$t('lastModify')" sortable="custom" prop="updateTime" sort>
                <template slot-scope="props">
                    <p>{{ props.row.updater }}</p>
                    <p class="desc">{{props.row.updateDate}}</p>
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
    import { mapGetters } from 'vuex'
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
                selectionLength: 0,
                pagination: {
                    current: 1,
                    limit: 20,
                    count: 0
                },
                sortField: {
                    prop: this.sortType,
                    order: this.$route.query.collation ?? ORDER_ENUM.descending
                }
            }
        },
        computed: {
            ...mapGetters('pipelines', [
                'groupNamesMap'
            ]),
            isAllPipelineView () {
                return this.$route.params.viewId === ALL_PIPELINE_VIEW_ID
            },
            maxheight () {
                console.log(this.$refs?.pipelineTable?.$el?.parent)
                return this.$refs?.pipelineTable?.$el?.parent?.clientHeight
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
            handleSelectChange (selection, ...args) {
                this.selectionLength = selection.length
                this.$emit('selection-change', selection, ...args)
            },
            goGroup (groupName) {
                const group = this.groupNamesMap[groupName]
                if (group) {
                    this.$router.push({
                        params: {
                            viewId: group?.id
                        }
                    })
                }
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
                Object.assign(this.sortField, {
                    order: prop ? ORDER_ENUM[order] : undefined,
                    prop: PIPELINE_SORT_FILED[prop]
                })
                this.$nextTick(this.requestList)
            },
            async requestList (query = {}) {
                this.isLoading = true

                try {
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
                } catch (e) {
                    console.error(e)
                } finally {
                    this.isLoading = false
                }
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
    .selected-all-indicator {
        display: flex;
        align-items: center;
        justify-content: center;
        background: #EAEBF0;
        height: 32px;
    }
</style>
