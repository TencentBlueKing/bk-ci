<template>
    <div class="trigger-wrapper">
        <bk-search-select
            class="search-select"
            v-model="searchValue"
            :data="searchList"
            clearable
            :key="repoId"
            :show-condition="false"
            :placeholder="$t('codelib.触发器类型/事件类型')"
        >
        </bk-search-select>
        <bk-table
            v-bkloading="{ isLoading }"
            class="trigger-table"
            :data="triggerData"
            :pagination="pagination"
            max-height="615"
            @page-change="handlePageChange"
            @page-limit-change="handlePageLimitChange"
        >
            <bk-table-column :label="$t('codelib.事件')" prop="eventType">
                <template slot-scope="{ row }">
                    <div class="event-name">
                        <img class="logo" :src="`https:${row.atomLogo}`" alt="">
                        {{ row.eventTypeDesc }}
                    </div>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('codelib.触发条件')" prop="eventType" show-overflow-tooltip>
                <template slot-scope="{ row }">
                    <div
                        v-if="Object.keys(row.triggerCondition).length"
                        :class="{
                            'trigger-condition-content': true,
                            'expand-condition': row.isExpand
                        }"
                    >
                        <div
                            v-for="(item, key, index) in row.triggerCondition" :key="index" class="condition-item"
                            v-show="(index <= 2 && !row.isExpand) || row.isExpand"
                        >
                            <bk-popover
                                :disabled="!Array.isArray(item) || (Array.isArray(item) && item.length <= 2)"
                            >
                                <span>- {{ key }}:</span>
                                <template v-if="Array.isArray(item) && item.length === 1">
                                    <span>{{ item.join(',') }}</span>
                                </template>
                                <template
                                    v-else-if="Array.isArray(item)"
                                >
                                    <div
                                        v-for="(i, itemIndex) in item"
                                        v-show="itemIndex <= 1"
                                        :key="i"
                                        class="array-item"
                                    >
                                        - {{ i }}
                                    </div>
                                    <div
                                        v-if="item.length > 2"
                                        class="array-item"
                                    >
                                        ...
                                    </div>
                                </template>
                                <span v-else>{{ key }}</span>
                                <div class="trigger-expand-btn" v-if="!row.isExpand && Object.keys(row.triggerCondition).length > 3 && index === 2" text @click="row.isExpand = true">{{ $t('codelib.展开') }}</div>
                                <div slot="content" style="white-space: normal;">
                                    <span>- {{ key }}:</span>
                                    <div
                                        v-for="i in item"
                                        :key="i"
                                        class="array-item"
                                    >
                                        - {{ i }}
                                    </div>
                                </div>
                            </bk-popover>

                        </div>
                        <div class="trigger-expand-btn" v-if="row.isExpand && Object.keys(row.triggerCondition).length > 3 " text @click="row.isExpand = false">{{ $t('codelib.收起') }}</div>
                    </div>
                    <div v-else>--</div>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('codelib.流水线数量')" prop="pipelineCount">
                <template slot-scope="{ row }">
                    <bk-button text @click="handleShowPipelineList(row)">{{ row.pipelineRefCount }}</bk-button>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('codelib.操作')" width="150">
                <template slot-scope="{ row }">
                    <bk-button
                        class="mr10"
                        theme="primary"
                        text
                        @click="handelShowDetail(row)"
                    >
                        {{ $t('codelib.详情') }}
                    </bk-button>
                </template>
            </bk-table-column>
            <template #empty>
                <EmptyTableStatus
                    :type="!!searchValue.length ? 'search-empty' : 'empty'"
                    @clear="clearFilter"
                />
            </template>
        </bk-table>
        <atom-detail
            ref="atomDetailRef"
            :atom="curAtom">
        </atom-detail>

        <bk-sideslider
            :is-show.sync="showPipelineSideslider"
            :width="600"
            quick-close
            :title="$t('codelib.关联的流水线')"
            ext-cls="pipeline-list-sideslider"
        >
            <template slot="content">
                <bk-table
                    :data="pipelineList"
                    :pagination="pipelineListPagination"
                    @page-change="handlePipelinePageChange">
                    <bk-table-column :label="$t('codelib.流水线名称')" prop="pipelineName">
                        <template slot-scope="{ row }">
                            <a @click="handleToPipeline(row)">{{ row.pipelineName }}</a>
                        </template>
                    </bk-table-column>
                </bk-table>
            </template>
        </bk-sideslider>
    </div>
</template>

<script>
    import {
        mapActions
    } from 'vuex'
    import atomDetail from '../atom-detail.vue'
    import EmptyTableStatus from '../empty-table-status.vue'
    export default {
        components: {
            atomDetail,
            EmptyTableStatus
        },
        props: {
            curRepo: {
                type: Object,
                default: () => {}
            },
            triggerTypeList: {
                type: Object,
                default: () => {}
            },
            eventTypeList: {
                type: Object,
                default: () => {}
            }
        },
        data () {
            return {
                isLoading: false,
                triggerData: [],
                searchValue: [],
                pagination: {
                    current: 1,
                    count: 0,
                    limit: 20
                },
                eventType: '',
                triggerType: '',
                curAtom: {},
                catchRepoId: '',
                triggerConditionMd5: '',
                triggerEventType: '',
                showPipelineSideslider: false,
                pipelineListPagination: {
                    current: 1,
                    count: 0,
                    limit: 20
                },
                pipelineList: []
            }
        },
        computed: {
            repoId () {
                return this.$route.query.id
            },
            projectId () {
                return this.$route.params.projectId
            },
            searchList () {
                const list = [
                    {
                        name: this.$t('codelib.触发器类型'),
                        id: 'triggerType',
                        children: this.triggerTypeList
                    },
                    {
                        name: this.$t('codelib.事件类型'),
                        id: 'eventType',
                        children: this.eventTypeList
                    }
                ]
                return list.filter((data) => {
                    return !this.searchValue.find(val => val.id === data.id)
                })
            },
            scmType () {
                return this.$route.query.scmType || ''
            }
        },
        watch: {
            async repoId (id) {
                await this.triggerRepo()
                this.catchRepoId = id
            },
            searchValue (val) {
                if (this.catchRepoId === this.repoId) {
                    const paramsMap = {}
                    val.forEach(item => {
                        const id = item.id
                        const value = item.values[0].id
                        paramsMap[id] = value
                    })
                    this.pagination.current = 1
                    this.triggerType = paramsMap.triggerType || ''
                    this.eventType = paramsMap.eventType || ''
                    this.getTriggerData()
                }
            },
            showPipelineSideslider (val) {
                if (val) {
                    this.fetchUsingPipelinesList({
                        projectId: this.projectId,
                        repositoryHashId: this.repoId,
                        triggerConditionMd5: this.triggerConditionMd5,
                        eventType: this.triggerEventType,
                        page: this.pipelineListPagination.current,
                        pageSize: this.pipelineListPagination.limit
                    }).then(res => {
                        this.pipelineList = res.records
                        this.pipelineListPagination.count = res.count
                    })
                }
            }
        },
        created () {
            this.catchRepoId = this.repoId
            this.getTriggerData()
        },
        methods: {
            ...mapActions('codelib', [
                'fetchUsingPipelinesList',
                'fetchTriggerData'
            ]),
            triggerRepo () {
                this.catchRepoId = ''
                this.searchValue = []
                this.pagination.current = 1
                this.triggerData = []
                this.getTriggerData()
            },
            /**
             * 获取触发器数据
             */
            async getTriggerData () {
                this.isLoading = true
                await this.fetchTriggerData({
                    projectId: this.projectId,
                    repositoryHashId: this.repoId,
                    page: this.pagination.current,
                    pageSize: this.pagination.limit,
                    triggerType: this.triggerType,
                    eventType: this.eventType
                }).then(res => {
                    this.pagination.count = res.count
                    this.triggerData = res.records.map(i => {
                        return {
                            isExpand: false,
                            ...i
                        }
                    })
                }).finally(() => {
                    this.isLoading = false
                })
            },
            /**
             * 获取事件类型
             */
            getEventTypeList () {
                this.fetchEventType({
                    scmType: this.scmType
                }).then(res => {
                    this.eventTypeList = res.map(i => {
                        return {
                            ...i,
                            name: i.value
                        }
                    })
                })
            },
            /**
             * 获取触发器类型
             */
            getTriggerTypeList () {
                this.fetchTriggerType({
                    scmType: this.scmType
                }).then(res => {
                    this.triggerTypeList = res.map(i => {
                        return {
                            ...i,
                            name: i.value
                        }
                    })
                })
            },
            handlePageChange (page) {
                this.pagination.current = page
                this.getTriggerData()
            },
            handlePageLimitChange (limit) {
                this.pagination.limit = limit
                this.getTriggerData()
            },
            handleShowPipelineList (row) {
                this.triggerConditionMd5 = row.triggerConditionMd5
                this.triggerEventType = row.eventType
                this.showPipelineSideslider = true
            },
            handelShowDetail (row) {
                this.curAtom = row
                this.$refs.atomDetailRef.isShow = true
            },
            clearFilter () {
                this.searchValue = []
            },
            handleToPipeline (row) {
                window.open(`/console/pipeline/${row.projectId}/${row.pipelineId}`, '__blank')
            }
        }
    }
</script>

<style lang="scss" scoped>
    .trigger-wrapper {
        .search-select {
            margin-bottom: 16px;
        }
        .trigger-table {
            ::v-deep .cell {
                max-height: 1500px !important;
                -webkit-line-clamp: 3 !important;
                padding: 10px 15px !important;
            }
            .event-name {
                display: flex;
                align-items: center;
                .logo {
                    width: 18px;
                    height: 18px;
                    margin-right: 5px;
                }
            }
            .condition-item {
                position: relative;
                line-height: 20px;
            }
            ::v-deep .part-img {
                margin-top: 0 !important;
            }
            .trigger-condition-content {
                overflow: hidden;
                text-overflow: ellipsis;
                display: -webkit-box;
                -webkit-box-orient: vertical;
                .array-item {
                    margin-left: 25px;
                    overflow: hidden;
                    white-space: nowrap;
                    text-overflow: ellipsis;
                }
            }
            .expand-condition {
                -webkit-line-clamp: 100 !important;
            }
            .trigger-expand-btn {
                color: #3A84FF;
                cursor: pointer;
            }
        }
    }
</style>

<style lang="scss">
    .array-item {
        margin-left: 25px;
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
    }
    .pipeline-list-sideslider {
        .bk-sideslider-content {
            padding: 20px;
            height: calc(100vh - 60px) !important;
        }
    }
</style>
