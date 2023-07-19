<template>
    <bk-table
        v-bkloading="{ isLoading }"
        ref="pipelineTable"
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
        :row-style="{ height: '56px' }"
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
                <span
                    v-if="!props.row.delete && !props.row.hasPermission && !isDeleteView"
                    class="pointer"
                    @click="applyPermission(props.row)"
                >
                    {{props.row.pipelineName}}
                </span>
                <router-link
                    v-else-if="!props.row.delete && !isDeleteView && props.row.historyRoute"
                    class="pipeline-cell-link"
                    :disabled="!props.row.hasPermission"
                    :to="props.row.historyRoute">
                    {{props.row.pipelineName}}
                </router-link>
                <span v-else>{{props.row.pipelineName}}</span>
            </template>
        </bk-table-column>
        <bk-table-column v-if="isAllPipelineView || isPatchView || isDeleteView" width="250" :label="$t('ownGroupName')" prop="viewNames">
            <div :ref="`belongsGroupBox_${props.$index}`" class="pipeline-group-box-cell" slot-scope="props">
                <template v-if="pipelineGroups[props.$index].visibleGroups">
                    <bk-tag
                        ext-cls="pipeline-group-name-tag"
                        :ref="`groupName_${props.$index}`"
                        v-for="(viewName, index) in pipelineGroups[props.$index].visibleGroups"
                        :key="index"
                        v-bk-tooltips="{ content: viewName, delay: [300, 0], allowHTML: false }"
                        @click="goGroup(viewName)"
                    >
                        {{viewName}}
                    </bk-tag>
                    <bk-popover
                        theme="light"
                        placement="bottom-end"
                        max-width="250"
                        v-if="pipelineGroups[props.$index].showMore"
                    >
                        <bk-tag
                            :ref="`groupNameMore_${props.$index}`"
                        >
                            +{{ pipelineGroups[props.$index].showMore }}
                        </bk-tag>
                        <div slot="content">
                            <bk-tag
                                v-for="hiddenGroup in pipelineGroups[props.$index].hiddenGroups"
                                ext-cls="pipeline-group-name-tag"
                                :key="hiddenGroup"
                                v-bk-tooltips="{ content: hiddenGroup, delay: [300, 0], allowHTML: false }"
                                @click="goGroup(hiddenGroup)"
                            >
                                {{hiddenGroup}}
                            </bk-tag>
                        </div>
                    </bk-popover>
                </template>
            </div>
        </bk-table-column>
        <template v-if="isPatchView">
            <bk-table-column width="150" :label="$t('latestExec')" prop="latestBuildNum">
                <span slot-scope="props">{{ props.row.latestBuildNum ? `#${props.row.latestBuildNum}` : '--' }}</span>
            </bk-table-column>
            <bk-table-column width="200" sortable="custom" :label="$t('lastExecTime')" prop="latestBuildStartDate" />
            <bk-table-column width="200" sortable="custom" :label="$t('restore.createTime')" prop="createTime" :formatter="formatTime" />
            <bk-table-column width="200" :label="$t('creator')" prop="creator" />
        </template>
        <template v-else-if="isDeleteView">
            <bk-table-column key="createTime" :label="$t('restore.createTime')" sortable="custom" prop="createTime" sort :formatter="formatTime" />
            <bk-table-column key="updateTime" :label="$t('restore.deleteTime')" sortable="custom" prop="updateTime" :formatter="formatTime" />
            <bk-table-column key="lastModifyUser" :label="$t('restore.deleter')" prop="lastModifyUser"></bk-table-column>
        </template>
        <template v-else>
            <bk-table-column :label="$t('latestExec')">
                <span v-if="props.row.delete" slot-scope="props">
                    {{$t('deleteAlready')}}
                </span>
                <div v-else slot-scope="props" class="pipeline-latest-exec-cell">
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
            <bk-table-column width="200" sortable="custom" :label="$t('lastExecTime')" prop="latestBuildStartDate">
                <div class="latest-build-multiple-row" v-if="!props.row.delete" slot-scope="props">
                    <p>{{ props.row.latestBuildStartDate }}</p>
                    <p v-if="props.row.progress" class="primary">{{ props.row.progress }}</p>
                    <p v-else class="desc">{{props.row.duration}}</p>
                </div>
            </bk-table-column>
            <bk-table-column width="200" :label="$t('lastModify')" sortable="custom" prop="updateTime" sort>
                <div class="latest-build-multiple-row" v-if="!props.row.delete" slot-scope="props">
                    <p>{{ props.row.updater }}</p>
                    <p class="desc">{{props.row.updateDate}}</p>
                </div>
            </bk-table-column>
        </template>
        <bk-table-column v-if="!isPatchView" width="150" :label="$t('operate')" prop="pipelineId">
            <div class="pipeline-operation-cell" slot-scope="props">
                <bk-button
                    v-if="isDeleteView"
                    text
                    theme="primary"
                    @click="handleRestore(props.row)">
                    {{ $t('restore.restore') }}
                </bk-button>
                <bk-button
                    v-else-if="props.row.delete && !isRecentView"
                    text
                    theme="primary"
                    :disabled="!isManage"
                    @click="removeHandler(props.row)"
                >
                    {{ $t('removeFromGroup') }}
                </bk-button>
                <bk-button
                    v-else-if="!props.row.hasPermission && !props.row.delete"
                    outline
                    theme="primary"
                    @click="applyPermission(props.row)">
                    {{ $t('apply') }}
                </bk-button>
                <template
                    v-else-if="props.row.hasPermission && !props.row.delete"
                >
                    <bk-button
                        text
                        theme=""
                        class="pipeline-exec-btn"
                        :disabled="props.row.disabled"
                        @click="execPipeline(props.row)"
                    >
                        <span class="exec-btn-span" v-bk-tooltips="props.row.tooltips">
                            <logo v-if="props.row.lock" name="minus-circle"></logo>
                            <logo
                                v-else
                                name="play"
                            />
                        </span>
                    </bk-button>
                    <bk-button
                        text
                        :theme="props.row.hasCollect ? 'warning' : ''"
                        class="pipeline-collect-btn"
                        @click="collectHandler(props.row)">
                        <i :class="{
                            'devops-icon': true,
                            'icon-star': !props.row.hasCollect,
                            'icon-star-shape': props.row.hasCollect
                        }" />
                    </bk-button>
                    <ext-menu :data="props.row" :config="props.row.pipelineActions"></ext-menu>
                </template>
            </div>
        </bk-table-column>
    </bk-table>
</template>

<script>
    import Logo from '@/components/Logo'
    import PipelineStatusIcon from '@/components/PipelineStatusIcon'
    import PipelineListEmpty from '@/components/pipelineList/PipelineListEmpty'
    import ExtMenu from '@/components/pipelineList/extMenu'
    import piplineActionMixin from '@/mixins/pipeline-action-mixin'
    import {
        ALL_PIPELINE_VIEW_ID,
        DELETED_VIEW_ID,
        RECENT_USED_VIEW_ID
    } from '@/store/constants'
    import { ORDER_ENUM, PIPELINE_SORT_FILED } from '@/utils/pipelineConst'
    import { convertTime, isShallowEqual } from '@/utils/util'
    import { mapGetters, mapState } from 'vuex'

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
            }
        },
        data () {
            return {
                isLoading: false,
                pipelineList: [],
                selectionLength: 0,
                pagination: {
                    current: parseInt(this.$route.query.page ?? 1),
                    limit: parseInt(this.$route.query.pageSize ?? 50),
                    count: 0
                },
                visibleTagCountList: {}
            }
        },
        computed: {
            ...mapGetters('pipelines', [
                'groupNamesMap'
            ]),
            ...mapState('pipelines', [
                'isManage'
            ]),
            isAllPipelineView () {
                return this.$route.params.viewId === ALL_PIPELINE_VIEW_ID
            },
            maxheight () {
                return this.$refs?.pipelineTable?.$el?.parent?.clientHeight
            },
            isDeleteView () {
                return this.$route.params.viewId === DELETED_VIEW_ID
            },
            isRecentView () {
                return this.$route.params.viewId === RECENT_USED_VIEW_ID
            },
            pipelineGroups () {
                const res = this.pipelineList.map((pipeline, index) => {
                    const { viewNames } = pipeline
                    const visibleCount = this.visibleTagCountList[index]

                    if (visibleCount >= 1) {
                        return {
                            visibleGroups: viewNames.slice(0, visibleCount),
                            hiddenGroups: viewNames.slice(visibleCount),
                            showMore: viewNames.length - visibleCount
                        }
                    }

                    return {
                        visibleGroups: viewNames,
                        hiddenGroups: [],
                        showMore: viewNames?.length ?? 0
                    }
                })
                return res
            },
            sortField () {
                const { sortType, collation } = this.$route.query
                const prop = sortType ?? localStorage.getItem('pipelineSortType') ?? PIPELINE_SORT_FILED.createTime
                const order = collation ?? localStorage.getItem('pipelineSortCollation') ?? ORDER_ENUM.descending
                return {
                    prop: this.getkeyByValue(PIPELINE_SORT_FILED, prop),
                    order: this.getkeyByValue(ORDER_ENUM, order)
                }
            }
        },

        watch: {
            '$route.params.viewId': function (viewId) {
                this.$refs?.pipelineTable?.clearSort?.()
                this.requestList({
                    viewId,
                    page: 1
                })
            },
            sortField: function (newSortField, oldSortField) {
                if (!isShallowEqual(newSortField, oldSortField)) {
                    this.$refs?.pipelineTable?.clearSort?.()
                    this.$refs?.pipelineTable?.sort?.(newSortField.prop, newSortField.order)
                    this.$nextTick(this.requestList)
                }
            },
            filterParams: function (filterMap, oldFilterMap) {
                if (!isShallowEqual(filterMap, oldFilterMap)) {
                    this.requestList({
                        ...filterMap,
                        page: 1
                    })
                }
            }
        },
        mounted () {
            this.requestList()
        },
        methods: {
            getkeyByValue (obj, value) {
                return Object.keys(obj).find(key => obj[key] === value)
            },
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
                const sortType = PIPELINE_SORT_FILED[prop]
                if (sortType) {
                    const collation = prop ? ORDER_ENUM[order] : ORDER_ENUM.descending
                    localStorage.setItem('pipelineSortType', sortType)
                    localStorage.setItem('pipelineSortCollation', collation)
                    this.$router.push({
                        ...this.$route,
                        query: {
                            ...this.$route.query,
                            sortType: sortType,
                            collation
                        }
                    })
                }
            },
            async requestList (query = {}) {
                this.isLoading = true

                try {
                    const { count, page, records } = await this.getPipelines({
                        page: this.pagination.current,
                        pageSize: this.pagination.limit,
                        viewId: this.$route.params.viewId,
                        ...this.filterParams,
                        ...query
                    })
                    Object.assign(this.pagination, {
                        count,
                        current: page
                    })
                    this.pipelineList = records
                    if (this.isAllPipelineView || this.isPatchView || this.isDeleteView) {
                        this.visibleTagCountList = {}
                        setTimeout(this.calcOverPos, 100)
                    }
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
            },
            calcOverPos () {
                const tagMargin = 6

                this.visibleTagCountList = this.pipelineList.reduce((acc, pipeline, index) => {
                    if (Array.isArray(pipeline.viewNames)) {
                        const groupNameBoxWidth = this.$refs[`belongsGroupBox_${index}`].clientWidth * 2
                        const groupNameLength = pipeline.viewNames.length
                        const moreTag = this.$refs?.[`groupNameMore_${index}`]?.$el
                        const moreTagWidth = (moreTag?.clientWidth ?? 0) + tagMargin
                        const viewPortWidth = groupNameBoxWidth - (groupNameLength > 1 ? moreTagWidth : 0)
                        let sumTagWidth = 0
                        let tagVisbleCount = 0

                        this.$refs[`groupName_${index}`]?.every((groupName) => {
                            sumTagWidth += groupName.$el.offsetWidth + tagMargin

                            const isOverSize = sumTagWidth > viewPortWidth
                            !isOverSize && tagVisbleCount++
                            return !isOverSize
                        })

                        acc[index] = tagVisbleCount
                    }
                    return acc
                }, {})
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
        background-color: #FAFBFD;
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
    .latest-build-multiple-row {
        display: flex;
        flex-direction: column;
    }
</style>
