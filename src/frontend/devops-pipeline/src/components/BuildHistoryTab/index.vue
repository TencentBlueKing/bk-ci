<template>
    <div class="build-history-tab-content" v-bkloading="{ isLoading }">
        <filter-bar v-if="showFilterBar" @query="queryBuildHistory" :set-history-page-status="setHistoryPageStatus" :reset-query-condition="resetQueryCondition" v-bind="historyPageStatus.queryMap"></filter-bar>
        <build-history-table :loading-more="isLoadingMore" :current-pipeline-version="currentPipelineVersion" @update-table="updateBuildHistoryList" :build-list="buildList" :columns="shownColumns" :empty-tips-config="emptyTipsConfig" :show-log="showLog"></build-history-table>
        <bk-dialog
            width="567"
            title="设置显示列"
            ext-cls="create-view-dialog"
            :value="isColumnsSelectPopupVisible"
            @confirm="updateTableColumns"
            @cancel="resetColumns">
            <bk-transfer :source-list="sourceColumns" display-key="label" setting-key="prop" :sortable="true" :target-list="shownColumns" :title="['可选列表', '已选列表']" @change="handleColumnsChange"></bk-transfer>
        </bk-dialog>
        <bk-sideslider
            class="pipeline-history-side-slider"
            :is-show.sync="isLogSliderShow"
            :title="`查看日志${currentBuildNum ? `（#${currentBuildNum}）` : ''}`"
            :quick-close="true"
            :width="820">
            <template slot="content">
                <pipeline-log v-if="currentBuildNo" :build-no="currentBuildNo" :build-num="currentBuildNum" :show-export="currentShowStatus" />
            </template>
        </bk-sideslider>

    </div>
</template>

<script>
    import pipelineWebsocket from '@/utils/pipelineWebSocket'
    import PipelineLog from '@/components/Log'
    import BuildHistoryTable from '@/components/BuildHistoryTable/'
    import FilterBar from '@/components/BuildHistoryTable/FilterBar'
    import { BUILD_HISTORY_TABLE_DEFAULT_COLUMNS, BUILD_HISTORY_TABLE_COLUMNS_MAP } from '@/utils/pipelineConst'
    import { mapGetters, mapActions } from 'vuex'
    import { throttle } from '@/utils/util'
    import { bus } from '@/utils/bus'
    import pipelineOperateMixin from '@/mixins/pipeline-operate-mixin'

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

        mixins: [pipelineOperateMixin],

        props: {
            isColumnsSelectPopupVisible: Boolean,
            showFilterBar: Boolean
        },

        data () {
            const lsColumns = localStorage && localStorage.getItem(LS_COLUMNS_KEYS)
            const initShownColumns = lsColumns ? JSON.parse(lsColumns) : BUILD_HISTORY_TABLE_DEFAULT_COLUMNS
            return {
                sourceColumns: Object.values(BUILD_HISTORY_TABLE_COLUMNS_MAP).sort((c1, c2) => c1.index > c2.index),
                shownColumns: initShownColumns,
                tempColumns: initShownColumns,
                isLoading: false,
                isLoadingMore: false,
                hasNoPermission: false,
                currentPipelineVersion: '',
                currentBuildNo: '',
                currentBuildNum: '',
                currentShowStatus: false,
                isLogSliderShow: false,
                buildList: []
            }
        },

        computed: {
            ...mapGetters({
                'historyPageStatus': 'pipelines/getHistoryPageStatus'
            }),
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            emptyTipsConfig () {
                const { hasNoPermission, buildList, isLoading, historyPageStatus: { isQuerying } } = this
                const title = hasNoPermission ? '没有权限' : '构建记录为空'
                const desc = hasNoPermission ? '你没有查看该流水线的权限，请切换项目或申请相应权限' : '定义了流水线之后，你可以手动触发执行一次构建任务，我们会给每个构建分配一个唯一ID，所有的构建记录都会在这里'
                const btns = hasNoPermission ? [{
                    theme: 'primary',
                    size: 'normal',
                    handler: this.changeProject,
                    text: '切换项目'
                }, {
                    theme: 'success',
                    size: 'normal',
                    handler: this.goToApplyPerm,
                    text: '申请权限'
                }] : [{
                    theme: 'primary',
                    size: 'normal',
                    disabled: this.executeStatus,
                    loading: this.executeStatus,
                    handler: () => {
                        !this.executeStatus && bus.$emit('trigger-excute')
                    },
                    text: '开始构建流水线'
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
                this.$nextTick(() => {
                    const scrollTable = document.querySelector(`.${SCROLL_BOX_CLASS_NAME}`)
                    if (scrollTable) {
                        scrollTable.scrollTo(0, 0)
                    }
                    this.requestHistory(1)
                    this.initWebSocket()
                })
            },
            buildList (list, oldList) {
                if (list.length !== oldList.length) {
                    this.$nextTick(() => {
                        const { historyPageStatus: { scrollTop } } = this
                        this.animateScroll(scrollTop)
                    })
                }
            }
        },

        created () {
            const { historyPageStatus: { currentPage, pageSize } } = this
            const len = currentPage * pageSize
            this.queryBuildHistory(1, len)
        },

        mounted () {
            const scrollTable = document.querySelector(`.${SCROLL_BOX_CLASS_NAME}`)
            this.throttleScroll = throttle(this.handleScroll, 500)
            if (scrollTable) {
                scrollTable.addEventListener('scroll', this.throttleScroll)
            }
            if (this.$route.hash) { // 带上buildId时，弹出日志弹窗
                const isBuildId = /^#b-+/.test(this.$route.hash) // 检查是否是合法的buildId
                isBuildId && this.showLog(this.$route.hash.slice(1), '', true)
            }
            this.initWebSocket()
        },

        updated () {
            if (!this.isLogSliderShow) {
                this.currentBuildNo = ''
                this.currentBuildNum = ''
                this.currentShowStatus = false
            }
        },

        beforeDestroy () {
            pipelineWebsocket.disconnect()
            const scrollTable = document.querySelector(`.${SCROLL_BOX_CLASS_NAME}`)
            if (scrollTable) {
                scrollTable.removeEventListener('scroll', this.throttleScroll)
            }
            this.resetHistoryFilterCondition()
        },

        methods: {
            ...mapActions('pipelines', [
                'requestPipelinesHistory',
                'requestExecPipeline',
                'setHistoryPageStatus',
                'resetHistoryFilterCondition'
            ]),
            animateScroll (scrollTop, speed = 0) {
                const scrollTable = document.querySelector(`.${SCROLL_BOX_CLASS_NAME}`)
                if (scrollTable && scrollTop !== scrollTable.scrollTop) {
                    scrollTable.scrollTo(0, scrollTop)
                }
            },
            handleColumnsChange (source, target, tagetValueList) {
                this.tempColumns = tagetValueList.sort((v1, v2) => BUILD_HISTORY_TABLE_COLUMNS_MAP[v1].index - BUILD_HISTORY_TABLE_COLUMNS_MAP[v2].index)
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

            initWebSocket () {
                const subscribe = `/topic/pipelineHistory/${this.pipelineId}`

                pipelineWebsocket.connect(this.projectId, subscribe, {
                    success: () => this.updateBuildHistoryList(),
                    error: (message) => this.$showTips({ message, theme: 'error' })
                })
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
                this.isLogSliderShow = true
                this.currentBuildNo = buildId
                this.currentBuildNum = buildNum
                this.currentShowStatus = status
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
                    this.$showTips({
                        message: '加载出错',
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
