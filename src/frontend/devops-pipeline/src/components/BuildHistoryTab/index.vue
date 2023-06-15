<template>
    <div>
        <empty-tips v-if="hasNoPermission" :show-lock="true" v-bind="emptyTipsConfig"></empty-tips>
        <infinite-scroll v-else class="build-history-tab-content" ref="infiniteScroll" :data-fetcher="requestHistory" :page-size="pageSize" :scroll-box-class-name="scrollBoxCls" v-slot="slotProps">
            <filter-bar v-if="showFilterBar" @query="slotProps.queryList" :set-history-page-status="setHistoryPageStatus" :reset-query-condition="resetQueryCondition" v-bind="historyPageStatus.queryMap"></filter-bar>
            <build-history-table v-if="!slotProps.isLoading" :loading-more="slotProps.isLoadingMore" :current-pipeline-version="currentPipelineVersion" @update-table="updateBuildHistoryList" :build-list="slotProps.list" :columns="shownColumns" :empty-tips-config="isEmptyList ? emptyTipsConfig : null" :show-log="showLog"></build-history-table>
            <bk-dialog
                width="567"
                :title="$t('history.settingCols')"
                ext-cls="create-view-dialog"
                :value="isColumnsSelectPopupVisible"
                @confirm="updateTableColumns"
                @cancel="resetColumns">
                <bk-transfer :source-list="sourceColumns" display-key="label" setting-key="prop" :sortable="true" :target-list="shownColumns" :title="[$t('history.canChooseList'), $t('history.choosedList')]" @change="handleColumnsChange"></bk-transfer>
            </bk-dialog>
        </infinite-scroll>
    </div>
</template>

<script>
    import BuildHistoryTable from '@/components/BuildHistoryTable/'
    import FilterBar from '@/components/BuildHistoryTable/FilterBar'
    import emptyTips from '@/components/devops/emptyTips'
    import { BUILD_HISTORY_TABLE_DEFAULT_COLUMNS } from '@/utils/pipelineConst'
    import { mapGetters, mapActions, mapState } from 'vuex'
    import { coverStrTimer } from '@/utils/util'
    import { PROCESS_API_URL_PREFIX, AUTH_URL_PREFIX } from '@/store/constants'
    import pipelineConstMixin from '@/mixins/pipelineConstMixin'
    import InfiniteScroll from '@/components/InfiniteScroll'
    import webSocketMessage from '@/utils/webSocketMessage'

    const LS_COLUMNS_KEYS = 'shownColumns'
    export default {
        name: 'build-history-tab',
        components: {
            BuildHistoryTable,
            InfiniteScroll,
            FilterBar,
            emptyTips
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
                hasNoPermission: false,
                pageSize: 24,
                currentPipelineVersion: '',
                currentBuildNo: '',
                currentBuildNum: '',
                currentShowStatus: false,
                triggerList: [],
                queryStrMap: ['status', 'materialAlias', 'materialBranch', 'startTimeStartTime', 'endTimeEndTime']
            }
        },

        computed: {
            ...mapGetters({
                historyPageStatus: 'pipelines/getHistoryPageStatus'
            }),
            ...mapState('atom', [
                'isPropertyPanelVisible'
            ]),
            scrollBoxCls () {
                return 'bkdevops-pipeline-history'
            },
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
                const historyTableColumns = Object.values(this.BUILD_HISTORY_TABLE_COLUMNS_MAP).sort((c1, c2) => c1.index > c2.index)
                return historyTableColumns.filter(x => !x.hiddenInHistory)
            },
            isEmptyList () {
                const list = this.$refs.infiniteScroll ? this.$refs.infiniteScroll.list : []
                const isLoading = this.$refs.infiniteScroll ? this.$refs.infiniteScroll.isLoading : []
                const { historyPageStatus: { isQuerying } } = this
                return list.length === 0 && !isLoading && !isQuerying
            },
            emptyTipsConfig () {
                const { hasNoPermission } = this
                const title = hasNoPermission ? this.$t('noPermission') : this.$t('history.noBuildRecords')
                const desc = hasNoPermission ? this.$t('history.noPermissionTips') : this.$t('history.buildEmptyDesc')
                const btns = hasNoPermission
                    ? [{
                        theme: 'primary',
                        size: 'normal',
                        handler: this.changeProject,
                        text: this.$t('changeProject')
                    }, {
                        theme: 'success',
                        size: 'normal',
                        handler: this.toApplyPermission,
                        text: this.$t('applyPermission')
                    }]
                    : [{
                        theme: 'primary',
                        size: 'normal',
                        disabled: this.executeStatus,
                        loading: this.executeStatus,
                        handler: () => {
                            !this.executeStatus && this.$router.push({
                                name: 'pipelinesPreview',
                                ...this.$route.params
                            })
                        },
                        text: this.$t('history.startBuildTips')
                    }]
                return {
                    title,
                    desc,
                    btns
                }
            }
        },

        watch: {
            pipelineId () {
                if (this.$refs.infiniteScroll) {
                    this.$refs.infiniteScroll.setScrollTop(0)
                    this.$nextTick(async () => {
                        this.$refs.infiniteScroll.animateScroll(0)
                        await this.$refs.infiniteScroll.queryList(1)
                        // this.initWebSocket()
                    })
                }
            },
            queryStr (newStr) {
                let hashParam = ''
                if (this.$route.hash && /^#b-+/.test(this.$route.hash)) hashParam = this.$route.hash
                const url = `${this.$route.path}${newStr ? `?${newStr}` : ''}${hashParam}`
                console.log(url, this.$route.fullPath)
                if (url !== this.$route.fullPath) {
                    this.$router.push(url)
                }
            }
        },

        async created () {
            const { pageSize } = this
            this.pageSize = document.body.scrollHeight > 42 * pageSize ? Math.ceil(document.body.scrollHeight / 42) : pageSize
            await this.handlePathQuery()
        },

        async mounted () {
            await this.handleRemoteMethod()
            if (this.$route.hash) { // 带上buildId时，弹出日志弹窗
                const isBuildId = /^#b-+/.test(this.$route.hash) // 检查是否是合法的buildId
                isBuildId && this.showLog(this.$route.hash.slice(1), '', true)
            }
            webSocketMessage.installWsMessage(this.refreshBuildHistoryList)
        },

        updated () {
            if (!this.isPropertyPanelVisible) {
                this.currentBuildNo = ''
                this.currentBuildNum = ''
                this.currentShowStatus = false
            }
        },

        beforeDestroy () {
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

            changeProject () {
                this.$toggleProjectMenu(true)
            },
            async toApplyPermission () {
                try {
                    const { projectId } = this.$route.params
                    const redirectUrl = await this.$ajax.post(`${AUTH_URL_PREFIX}/user/auth/permissionUrl`, [{
                        actionId: this.$permissionActionMap.view,
                        resourceId: this.$permissionResourceMap.pipeline,
                        instanceId: [{
                            id: projectId,
                            type: this.$permissionResourceTypeMap.PROJECT
                        }, {
                            id: this.pipelineId,
                            name: this.pipelineId,
                            type: this.$permissionResourceTypeMap.PIPELINE_DEFAULT
                        }]
                    }])
                    console.log('redirectUrl', redirectUrl)
                    window.open(redirectUrl, '_blank')
                    this.$bkInfo({
                        title: this.$t('permissionRefreshtitle'),
                        subTitle: this.$t('permissionRefreshSubtitle'),
                        okText: this.$t('permissionRefreshOkText'),
                        cancelText: this.$t('close'),
                        confirmFn: () => {
                            location.reload()
                        }
                    })
                } catch (e) {
                    console.error(e)
                }
            },

            resetQueryCondition () {
                this.resetHistoryFilterCondition()
                this.$refs.infiniteScroll.queryList()
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
                // TODO 筛选参数目前不支持带#字符串回填
                const { $route, historyPageStatus: { queryMap } } = this
                const pathQuery = $route.query
                const newSearchKey = []
                const queryArr = Object.keys(pathQuery)
                const searchKeyArr = queryArr.filter(item => !this.queryStrMap.includes(item))

                if (queryArr.includes('trigger')) await this.handleRemoteMethod()
                if (queryArr.length) {
                    const newQuery = {}
                    queryArr.forEach(item => {
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

                    searchKeyArr.forEach(val => {
                        const newItem = this.filterData.filter(item => item.id === val)
                        if (newItem[0]) {
                            newItem[0].values = [{ id: pathQuery[val] }]
                            if (val === 'trigger') {
                                newItem[0].values = []
                                pathQuery[val].split(',').forEach(item => {
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

            refreshBuildHistoryList () {
                this.updateBuildHistoryList(true)
            },

            async updateBuildHistoryList (isRefresh = false) {
                try {
                    if (!this.pipelineId || !this.projectId || !this.$refs.infiniteScroll) {
                        webSocketMessage.unInstallWsMessage()
                        return
                    }
                    const res = await this.$refs.infiniteScroll.updateList(isRefresh)
                    this.currentPipelineVersion = res.pipelineVersion || ''
                } catch (err) {
                    if (err.code === 403) {
                        this.hasNoPermission = true
                    } else {
                        this.$showTips({
                            message: err.message || err,
                            theme: 'error'
                        })
                        if ((err.code === 404 || err.httpStatus === 404) && this.$route.name !== 'PipelineManageList') {
                            this.$router.push({
                                name: 'PipelineManageList'
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
                    this.currentPipelineVersion = res.pipelineVersion || ''
                    return res
                } catch (err) {
                    if (err.code === 403) {
                        this.hasNoPermission = true
                    } else {
                        this.$showTips({
                            message: err.message || err,
                            theme: 'error'
                        })
                        if ((err.code === 404 || err.httpStatus === 404) && this.$route.name !== 'PipelineManageList') {
                            this.$router.push({
                                name: 'PipelineManageList'
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
