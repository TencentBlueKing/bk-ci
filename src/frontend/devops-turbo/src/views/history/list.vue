<template>
    <article class="history-list-home">
        <main class="g-turbo-box history-list-main" v-if="hasPermission">
            <header class="filter-area">
                <bk-select multiple
                    :value="filter.value"
                    @change="(options) => chooseFilter(filter.key, options)"
                    searchable
                    show-select-all
                    class="single-width"
                    :loading="isLoadingSearch"
                    v-for="filter in filterList"
                    :key="filter.key"
                    :placeholder="filter.placeholder"
                >
                    <bk-option v-for="(value, key) in filter.list"
                        :key="key"
                        :id="key"
                        :name="value">
                    </bk-option>
                </bk-select>
                <bk-date-picker class="single-width" :placeholder="$t('turbo.选择日期范围')" type="daterange" :value="timeRange" @change="(options) => chooseFilter('timeRange', options)"></bk-date-picker>
                <bk-button @click="clearFilter" class="clear-btn"> {{ $t('turbo.重置') }} </bk-button>
            </header>

            <bk-table class="history-records g-turbo-scroll-table"
                :data="historyList"
                :outer-border="false"
                :header-border="false"
                :header-cell-style="{ background: '#f5f6fa' }"
                :pagination="pagination"
                v-bkloading="{ isLoading }"
                @page-change="pageChanged"
                @page-limit-change="pageLimitChange"
                @row-click="goToDetail"
                @sort-change="sortChange"
            >
                <bk-table-column :label="$t('turbo.编号')" prop="executeNum" width="80" sortable></bk-table-column>
                <bk-table-column :label="$t('turbo.流水线/构建机')" prop="pipeline_name" sortable show-overflow-tooltip>
                    <template slot-scope="props">
                        <span v-if="props.row.pipelineName">
                            {{ props.row.pipelineName }}
                            <a @click.stop :href="`/console/pipeline/${projectId}/${props.row.pipelineId}/detail/${props.row.devopsBuildId}`" target="_blank" class="g-turbo-click-text"><logo name="cc-jump-link" class="jump-link" size="14"></logo></a>
                        </span>
                        <span v-else>{{ props.row.clientIp }}</span>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('turbo.状态')" prop="status" sortable>
                    <template slot-scope="props">
                        <task-status :status="props.row.status" :message="props.row.message"></task-status>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('turbo.开始时间')" prop="startTime" sortable></bk-table-column>
                <bk-table-column :label="$t('turbo.未加速耗时')" prop="estimateTimeValue" sortable></bk-table-column>
                <bk-table-column :label="$t('turbo.实际耗时')" prop="executeTimeValue" sortable></bk-table-column>
                <bk-table-column :label="$t('turbo.节省率')" prop="turboRatio" sortable></bk-table-column>
                <template #empty>
                    <EmptyTableStatus :type="emptyType" @clear="clearFilter" />
                </template>
            </bk-table>
        </main>
        <permission-exception v-else :message="errMessage" />
    </article>
</template>

<script>
    import { getHistoryList, getHistorySearchList } from '@/api'
    import taskStatus from '../../components/task-status.vue'
    import logo from '../../components/logo'
    import permissionException from '../../components/exception/permission.vue'
    import EmptyTableStatus from '../../components/empty-table-status.vue'

    export default {
        components: {
            taskStatus,
            logo,
            permissionException,
            EmptyTableStatus
        },

        data () {
            return {
                pagination: {
                    current: 1,
                    count: 1,
                    limit: 10
                },
                historyList: [],
                turboPlanId: [],
                pipelineId: [],
                clientIp: [],
                status: [],
                startTime: '',
                endTime: '',
                timeRange: [],
                isLoading: false,
                isLoadingSearch: false,
                planInfo: {},
                pipelineInfo: {},
                statusInfo: {},
                clientIpInfo: [],
                sortField: undefined,
                sortType: undefined,
                hasPermission: true,
                errMessage: ''
            }
        },

        computed: {
            filterList () {
                const clientIpList = {};
                (this.clientIpInfo || []).forEach((clientIp) => {
                    clientIpList[clientIp] = clientIp
                })
                return [
                    { key: 'turboPlanId', list: this.planInfo, value: this.turboPlanId, placeholder: this.$t('turbo.请选择加速方案') },
                    { key: 'pipelineId', list: this.pipelineInfo, value: this.pipelineId, placeholder: this.$t('turbo.请选择流水线') },
                    { key: 'clientIp', list: clientIpList, value: this.clientIp, placeholder: this.$t('turbo.请选择构建机') },
                    { key: 'status', list: this.statusInfo, value: this.status, placeholder: this.$t('turbo.请选择状态') }
                ]
            },

            projectId () {
                return this.$route.params.projectId
            },
            
            emptyType () {
                return (
                    this.turboPlanId.length
                    || this.pipelineId.length
                    || this.status.length
                    || this.timeRange.length
                    || this.startTime
                    || this.endTime
                )
                    ? 'search-empty'
                    : 'empty'
            }
        },

        watch: {
            projectId: {
                handler () {
                    this.initFilter()
                    this.getHistoryList()
                    this.getHistorySearchList()
                },
                immediate: true
            }
        },

        methods: {
            sortChange (sort) {
                const sortMap = {
                    ascending: 'ASC',
                    descending: 'DESC'
                }
                this.sortField = sort.prop
                this.sortType = sortMap[sort.order]
                this.getHistoryList()
            },

            initFilter () {
                const query = this.$route.query || {}
                if (query.planId) this.turboPlanId = [query.planId]
                if (query.pipelineId) this.pipelineId = [query.pipelineId]
                if (query.clientIp) this.clientIp = [query.clientIp]
            },

            getHistorySearchList () {
                this.isLoadingSearch = true
                getHistorySearchList(this.projectId).then((res) => {
                    this.planInfo = res.planInfo || {}
                    this.pipelineInfo = res.pipelineInfo || {}
                    this.statusInfo = res.statusInfo || {}
                    this.clientIpInfo = res.clientIpInfo || []
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoadingSearch = false
                })
            },

            getHistoryList () {
                this.isLoading = true
                const postData = {
                    startTime: this.startTime,
                    endTime: this.endTime,
                    pipelineId: this.pipelineId,
                    status: this.status,
                    turboPlanId: this.turboPlanId,
                    clientIp: this.clientIp,
                    projectId: this.projectId
                }
                const queryData = {
                    pageNum: this.pagination.current,
                    pageSize: this.pagination.limit,
                    sortField: this.sortField,
                    sortType: this.sortType
                }
                getHistoryList(queryData, postData).then((res = {}) => {
                    this.historyList = res.records || []
                    this.pagination.count = res.count
                }).catch((err) => {
                    if (err.code === 2300017) {
                        this.hasPermission = false
                        this.errMessage = err.message
                    } else {
                        this.$bkMessage({
                            message: err.message || err,
                            theme: 'error'
                        })
                    }
                }).finally(() => {
                    this.isLoading = false
                })
            },

            clearFilter () {
                this.turboPlanId = []
                this.pipelineId = []
                this.status = []
                this.timeRange = []
                this.startTime = ''
                this.endTime = ''
                this.getHistoryList()
            },

            chooseFilter (key, value) {
                this[key] = value
                this.startTime = this.timeRange[0]
                this.endTime = this.timeRange[1]
                this.getHistoryList()
            },

            goToDetail (row) {
                this.$router.push({
                    name: 'historyDetail',
                    params: {
                        id: row.id
                    }
                })
            },

            pageChanged (page) {
                if (page) this.pagination.current = page
                this.getHistoryList()
            },

            pageLimitChange (currentLimit) {
                if (currentLimit === this.pagination.limit) return

                this.pagination.current = 1
                this.pagination.limit = currentLimit
                this.getHistoryList()
            }
        }
    }
</script>

<style lang="scss" scoped>
    .history-list-home {
        padding: 20px;
        margin: 0 auto;
    }
    .history-list-main {
        padding: 20px;
    }
    .filter-area {
        display: flex;
        align-items: center;
        margin-bottom: 20px;
    }
    .single-width {
        width: 1.82rem;
        margin-right: 8px;
    }
    .clear-btn {
        margin-left: 8px;
    }
    .jump-link {
        vertical-align: sub;
    }
    ::v-deep .bk-table-row {
        cursor: pointer;
    }
</style>
