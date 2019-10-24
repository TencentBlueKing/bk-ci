<template>
    <div class="build-history-tab-content" v-bkloading="{ isLoading }">
        <filter-bar v-if="showFilterBar" @query="queryBuildHistory" :set-history-page-status="setHistoryPageStatus" :reset-query-condition="resetQueryCondition" v-bind="historyPageStatus.queryMap"></filter-bar>
        <build-history-table :loading-more="isLoadingMore" :current-pipeline-version="currentPipelineVersion" @update-table="updateBuildHistoryList" :build-list="buildList" :columns="shownColumns" :empty-tips-config="emptyTipsConfig" :show-log="showLog"></build-history-table>
        <bk-dialog
            width="567"
            :title="$t('history.settingCols')"
            ext-cls="create-view-dialog"
            :value="isColumnsSelectPopupVisible"
            @confirm="updateTableColumns"
            @cancel="resetColumns">
            <bk-transfer :source-list="sourceColumns" display-key="label" setting-key="prop" :sortable="true" :target-list="shownColumns" :title="[$t('history.canChooseList'), $t('history.choosedList')]" @change="handleColumnsChange"></bk-transfer>
        </bk-dialog>
        <template v-if="currentBuildNo">
            <pipeline-log :title="`$t('history.viewLog')${currentBuildNum ? `（#${currentBuildNum}）` : ''}`" :build-no="currentBuildNo" :build-num="currentBuildNum" :show-export="currentShowStatus" />
        </template>
    </div>
</template>

<script>
    import webSocketMessage from '@/utils/webSocketMessage'
    import PipelineLog from '@/components/Log'
    import BuildHistoryTable from '@/components/BuildHistoryTable/'
    import FilterBar from '@/components/BuildHistoryTable/FilterBar'
    import { BUILD_HISTORY_TABLE_DEFAULT_COLUMNS } from '@/utils/pipelineConst'
    import { mapGetters, mapActions, mapState } from 'vuex'
    import { throttle, coverStrTimer } from '@/utils/util'
    import { bus } from '@/utils/bus'
    import { PROCESS_API_URL_PREFIX } from '@/store/constants'
    import pipelineConstMixin from '@/mixins/pipelineConstMixin'

    const LS_COLUMNS_KEYS = 'shownColumns'
    const SCROLL_BOX_CLASS_NAME = 'bkdevops-pipeline-history'
    const SCROLL_THRESHOLD = 250
    export default {
        name: 'build-history-tab',
        components: {
            BuildHistoryTable,
            FilterBar,
            PipelineLog
        },

        mixins: [pipelineConstMixin],

        props: {
            isColumnsSelectPopupVisible: Boolean,
            showFilterBar: Boolean,
            toggleFilterBar: Function
        },

        data () {
            const lsColumns = localStorage && localStorage.getItem(LS_COLUMNS_KEYS)
            const initShownColumns = lsColumns ? JSON.parse(lsColumns) : BUILD_HISTORY_TABLE_DEFAULT_COLUMNS
            return {
                shownColumns: initShownColumns,
                tempColumns: initShownColumns,
                isLoading: false,
                isLoadingMore: false,
                hasNoPermission: false,
                currentPipelineVersion: '',
                currentBuildNo: '',
                currentBuildNum: '',
                currentShowStatus: false,
                buildList: [],
                triggerList: [],
                queryStrMap: ['status', 'materialAlias', 'materialBranch', 'startTimeStartTime', 'endTimeEndTime']
            }
        },

        computed: {
            ...mapGetters({
                'historyPageStatus': 'pipelines/getHistoryPageStatus'
            }),
            ...mapState('atom', [
                'isPropertyPanelVisible'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            queryStr () {
                return this.historyPageStatus.queryStr
            },
            filterData () {
                return [
                    {
                        value: 'commitid',
                        id: 'materialCommitId'
                    },
                    {
                        value: 'commitMessage',
                        id: 'materialCommitMessage'
                    },
                    {
                        value: this.$t('history.triggerType'),
                        id: 'trigger',
                        remote: true,
                        multiable: true,
                        children: this.triggerList
                    },
                    {
                        value: this.$t('history.remark'),
                        id: 'remark'
                    }
                ]
            },
            sourceColumns () {
                return Object.values(this.BUILD_HISTORY_TABLE_COLUMNS_MAP).sort((c1, c2) => c1.index > c2.index)
            },
            emptyTipsConfig () {
                const { hasNoPermission, buildList, isLoading, historyPageStatus: { isQuerying } } = this
                const title = hasNoPermission ? this.$t('noPermission') : this.$t('history.noBuildRecords')
                const desc = hasNoPermission ? this.$t('history.noPermissionTips') : this.$t('history.buildEmptyDesc')
                const btns = hasNoPermission ? [{
                    theme: 'primary',
                    size: 'normal',
                    handler: this.changeProject,
                    text: this.$t('changeProject')
                }, {
                    theme: 'success',
                    size: 'normal',
                    handler: this.goToApplyPerm,
                    text: this.$t('applyPermission')
                }] : [{
                    theme: 'primary',
                    size: 'normal',
                    disabled: this.executeStatus,
                    loading: this.executeStatus,
                    handler: () => {
                        !this.executeStatus && bus.$emit('trigger-excute')
                    },
                    text: this.$t('history.startBuildTips')
                }]

                return buildList.length === 0 && !isLoading && !isQuerying ? {
                    title,
                    desc,
                    btns
                } : null
            }
        },

        watch: {
            pipelineId () {
                this.setHistoryPageStatus({
                    scrollTop: 0
                })
                this.$nextTick(async () => {
                    const scrollTable = document.querySelector(`.${SCROLL_BOX_CLASS_NAME}`)
                    if (scrollTable) {
                        scrollTable.scrollTo(0, 0)
                    }
                    this.isLoading = true
                    await this.requestHistory(1)
                    this.isLoading = false
                    // this.initWebSocket()
                })
            },
            buildList (list, oldList) {
                if (list.length !== oldList.length) {
                    this.$nextTick(() => {
                        const { historyPageStatus: { scrollTop } } = this
                        this.animateScroll(scrollTop)
                    })
                }
            },
            queryStr (newStr) {
                let hashParam = ''
                if (this.$route.hash && /^#b-+/.test(this.$route.hash)) hashParam = this.$route.hash
                this.$router.push(`${this.$route.path}?${newStr}${hashParam}`)
            }
        },

        async created () {
            await this.handlePathQuery()
            const { currentPage, pageSize } = this
            const len = currentPage * pageSize
            this.queryBuildHistory(1, len)
        },

        async mounted () {
            await this.handleRemoteMethod()
            const scrollTable = document.querySelector(`.${SCROLL_BOX_CLASS_NAME}`)
            this.throttleScroll = throttle(this.handleScroll, 500)
            if (scrollTable) {
                scrollTable.addEventListener('scroll', this.throttleScroll)
            }
            if (this.$route.hash) { // 带上buildId时，弹出日志弹窗
                const isBuildId = /^#b-+/.test(this.$route.hash) // 检查是否是合法的buildId
                isBuildId && this.showLog(this.$route.hash.slice(1), '', true)
            }
            webSocketMessage.installWsMessage(this.updateBuildHistoryList)
        },

        updated () {
            if (!this.isPropertyPanelVisible) {
                this.currentBuildNo = ''
                this.currentBuildNum = ''
                this.currentShowStatus = false
            }
        },

        beforeDestroy () {
            const scrollTable = document.querySelector(`.${SCROLL_BOX_CLASS_NAME}`)
            if (scrollTable) {
                scrollTable.removeEventListener('scroll', this.throttleScroll)
            }
            this.resetHistoryFilterCondition()
            webSocketMessage.unInstallWsMessage()
        },

        methods: {
            ...mapActions('pipelines', [
                'requestPipelinesHistory',
                'requestExecPipeline',
                'setHistoryPageStatus',
                'resetHistoryFilterCondition'
            ]),
            ...mapActions('atom', [
                'togglePropertyPanel'
            ]),
            animateScroll (scrollTop, speed = 0) {
                const scrollTable = document.querySelector(`.${SCROLL_BOX_CLASS_NAME}`)
                if (scrollTable && scrollTop !== scrollTable.scrollTop) {
                    scrollTable.scrollTo(0, scrollTop)
                }
            },
            handleColumnsChange (source, target, tagetValueList) {
                this.tempColumns = tagetValueList.sort((v1, v2) => this.BUILD_HISTORY_TABLE_COLUMNS_MAP[v1].index - this.BUILD_HISTORY_TABLE_COLUMNS_MAP[v2].index)
            },

            updateTableColumns () {
                localStorage.setItem(LS_COLUMNS_KEYS, JSON.stringify(this.tempColumns))
                this.shownColumns = [...this.tempColumns]
                this.$emit('hideColumnPopup')
            },

            resetColumns () {
                this.tempColumns = [...this.shownColumns]
                this.shownColumns = [...this.shownColumns]
                this.$emit('hideColumnPopup')
            },

            handleScroll (e) {
                const { target } = e
                const { historyPageStatus, setHistoryPageStatus, scrollLoadMore, isLoadingMore } = this
                setHistoryPageStatus({
                    scrollTop: e.target.scrollTop
                })

                const offset = e.target.scrollHeight - (e.target.offsetHeight + e.target.scrollTop)
                if (offset <= SCROLL_THRESHOLD && historyPageStatus.hasNext && !isLoadingMore) { // scroll to end
                    scrollLoadMore(target.scrollTop)
                }
            },

            changeProject () {
                this.$toggleProjectMenu(true)
            },

            goToApplyPerm () {
                const url = `${PERM_URL_PIRFIX}/backend/api/perm/apply/subsystem/?client_id=pipeline&project_code=${this.projectId}&service_code=pipeline&role_viewer=pipeline:${this.pipelineId}`
                window.open(url, '_blank')
            },

            setPermissionConfig (resource, option) {
                this.$showAskPermissionDialog({
                    noPermissionList: [{
                        resource,
                        option
                    }],
                    applyPermissionUrl: `${PERM_URL_PIRFIX}/backend/api/perm/apply/subsystem/?client_id=pipeline&project_code=${this.projectId}&service_code=pipeline&role_executor=pipeline:${this.pipelineId}`
                })
            },

            resetQueryCondition () {
                this.resetHistoryFilterCondition()
                this.queryBuildHistory()
            },

            showLog (buildId, buildNum, status) {
                this.togglePropertyPanel({
                    isShow: true
                })

                this.currentBuildNo = buildId
                this.currentBuildNum = buildNum
                this.currentShowStatus = status
            },

            async handlePathQuery () {
                const { $route, historyPageStatus: { queryMap } } = this
                const pathQuery = $route.query
                const newSearchKey = []
                const queryArr = Object.keys(pathQuery)
                const searchKeyArr = queryArr.filter(item => !this.queryStrMap.includes(item))

                if (queryArr.includes('trigger')) await this.handleRemoteMethod()
                if (queryArr.length) {
                    const newQuery = {}
                    queryArr.map(item => {
                        if (['status', 'materialAlias'].includes(item)) {
                            newQuery[item] = pathQuery[item].split(',')
                        } else if (pathQuery.startTimeStartTime && pathQuery.endTimeEndTime) {
                            newQuery.startTimeStartTime = pathQuery.startTimeStartTime
                            newQuery.endTimeEndTime = pathQuery.endTimeEndTime
                            newQuery.dateTimeRange = [coverStrTimer(parseInt($route.query.startTimeStartTime)), coverStrTimer(parseInt($route.query.endTimeEndTime))]
                        } else {
                            newQuery[item] = pathQuery[item]
                        }
                    })

                    searchKeyArr.map(val => {
                        const newItem = this.filterData.filter(item => item.id === val)
                        if (newItem[0]) {
                            newItem[0].values = [{ id: pathQuery[val] }]
                            if (val === 'trigger') {
                                newItem[0].values = []
                                pathQuery[val].split(',').map(item => {
                                    newItem[0].values.push({
                                        id: item,
                                        value: this.triggerList.find(val => val.id === item) && this.triggerList.find(val => val.id === item).value
                                    })
                                })
                            } else {
                                newItem[0].values[0].value = pathQuery[val]
                            }
                            newSearchKey.push(newItem[0])
                        }
                    })
                    this.setHistoryPageStatus({
                        queryMap: {
                            ...queryMap,
                            query: {
                                ...queryMap.query,
                                ...newQuery
                            },
                            searchKey: newSearchKey
                        }
                    })
                    this.toggleFilterBar()
                }
            },

            async handleRemoteMethod () {
                try {
                    const { $route: { params }, $ajax } = this
                    const url = `${PROCESS_API_URL_PREFIX}/user/builds/${params.projectId}/${params.pipelineId}/historyCondition/trigger`
                    const res = await $ajax.get(url)
                    this.triggerList = res.data
                } catch (e) {

                }
            },

            async queryBuildHistory (page = 1, pageSize) {
                try {
                    this.isLoading = true
                    await this.requestHistory(page, pageSize)
                } catch (e) {
                    console.error(e)
                } finally {
                    this.isLoading = false
                }
            },

            async scrollLoadMore () {
                try {
                    this.isLoadingMore = true
                    await this.requestHistory(this.historyPageStatus.currentPage + 1)
                } catch (e) {
                    console.log(e)
                    this.$showTips({
                        message: this.$t('history.loadingErr'),
                        theme: 'error'
                    })
                } finally {
                    this.isLoadingMore = false
                }
            },

            async updateBuildHistoryList () {
                try {
                    const { projectId, pipelineId, buildList, historyPageStatus: { pageSize } } = this
                    const oldlen = buildList.length
                    const res = await this.requestPipelinesHistory({
                        projectId,
                        pipelineId,
                        page: 1,
                        pageSize: oldlen > pageSize ? oldlen : pageSize
                    })

                    this.buildList = res.records
                    this.currentPipelineVersion = res.pipelineVersion || ''
                } catch (err) {
                    if (err.code === 403) {
                        this.hasNoPermission = true
                    } else {
                        this.$showTips({
                            message: err.message || err,
                            theme: 'error'
                        })
                        if (err.code === 404) {
                            this.$router.push({
                                name: 'pipelinesList'
                            })
                        }
                    }
                }
            },

            async requestHistory (page, pageSize) {
                try {
                    const { projectId, pipelineId, historyPageStatus: { pageSize: defaultPageSize } } = this
                    const pageLen = pageSize || defaultPageSize
                    const res = await this.requestPipelinesHistory({
                        projectId,
                        pipelineId,
                        page,
                        pageSize: pageLen
                    })

                    this.buildList = page === 1 ? res.records : [
                        ...this.buildList,
                        ...res.records
                    ]
                    const currentPage = Math.ceil(this.buildList.length / defaultPageSize)
                    this.setHistoryPageStatus({
                        currentPage,
                        hasNext: currentPage < res.totalPages
                    })
                    this.currentPipelineVersion = res.pipelineVersion || ''
                } catch (err) {
                    if (err.code === 403) {
                        this.hasNoPermission = true
                    } else {
                        this.$showTips({
                            message: err.message || err,
                            theme: 'error'
                        })
                        if (err.code === 404) {
                            this.$router.push({
                                name: 'pipelinesList'
                            })
                        }
                    }
                }
            }
        }
    }
</script>

<style lang="scss">
    .build-history-tab-content {
        overflow: visible;
        .bk-loading {
            background-color: rgba(250, 251, 253, .8)
        }
        .bk-sideslider-wrapper {
            top: 0;
            padding-bottom: 0;
             .bk-sideslider-content {
                height: calc(100% - 60px);
            }
        }
    }
</style>
