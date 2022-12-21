import { throttle } from '@/utils/util'

const SCROLL_BOX_CLASS_NAME = 'bkdevops-pipeline-history'
const SCROLL_THRESHOLD = 250
export default {

    data () {
        return {
            isLoading: false,
            isLoadingMore: false,
            list: []
        }
    },

    computed: {
        projectId () {
            return this.$route.params.projectId
        },
        pipelineId () {
            return this.$route.params.pipelineId
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
        list (list, oldList) {
            if (list.length !== oldList.length) {
                this.$nextTick(() => {
                    const { historyPageStatus: { scrollTop } } = this
                    this.animateScroll(scrollTop)
                })
            }
        }
    },

    created () {
        const { currentPage, pageSize } = this
        const len = currentPage * pageSize
        this.queryBuildHistory(1, len)
    },

    mounted () {
        const scrollTable = document.querySelector(`.${SCROLL_BOX_CLASS_NAME}`)
        this.throttleScroll = throttle(this.handleScroll, 500)
        if (scrollTable) {
            scrollTable.addEventListener('scroll', this.throttleScroll)
        }
    },

    beforeDestroy () {
        const scrollTable = document.querySelector(`.${SCROLL_BOX_CLASS_NAME}`)
        if (scrollTable) {
            scrollTable.removeEventListener('scroll', this.throttleScroll)
        }
    },

    methods: {

        animateScroll (scrollTop, speed = 0) {
            const scrollTable = document.querySelector(`.${SCROLL_BOX_CLASS_NAME}`)
            if (scrollTable && scrollTop !== scrollTable.scrollTop) {
                scrollTable.scrollTo(0, scrollTop)
            }
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
