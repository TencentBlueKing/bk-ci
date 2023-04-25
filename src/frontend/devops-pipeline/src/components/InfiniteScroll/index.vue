<template>
    <div style="height: 100%;" v-bkloading="{ isLoading }">
        <slot v-bind="{ list, isLoading, isLoadingMore, queryList, setScrollTop, animateScroll, totals }"></slot>
        <div v-if="isLoadingMore" class="loading-more" slot="append"><i class="devops-icon icon-circle-2-1 spin-icon"></i><span>{{ $t('loadingTips') }}</span></div>
    </div>
</template>

<script>
    import { throttle } from '@/utils/util'
    const SCROLL_THRESHOLD = 250
    export default {

        props: {
            dataFetcher: {
                type: Function,
                deafult: () => () => {}
            },
            scrollBoxClassName: {
                type: String,
                isRequired: true
            },
            pageSize: {
                type: Number,
                default: 24
            }
        },
        data () {
            return {
                isLoading: true,
                isLoadingMore: false,
                list: [],
                currentPage: 1,
                scrollTop: 0,
                hasNext: false,
                totals: 0
            }
        },

        mounted () {
            const { currentPage, pageSize, scrollBoxClassName } = this
            const scrollTable = document.querySelector(`.${scrollBoxClassName}`)
            const len = currentPage * pageSize
            this.queryList(1, len)

            this.throttleScroll = throttle(this.handleScroll, 500)
            if (scrollTable) {
                scrollTable.addEventListener('scroll', this.throttleScroll)
            }
        },

        beforeDestroy () {
            const scrollTable = document.querySelector(`.${this.scrollBoxClassName}`)
            if (scrollTable) {
                scrollTable.removeEventListener('scroll', this.throttleScroll)
            }
        },

        methods: {
            setScrollTop (scrollTop) {
                this.scrollTop = scrollTop
            },
            animateScroll (scrollTop, speed = 0) {
                const scrollTable = document.querySelector(`.${this.scrollBoxClassName}`)
                if (scrollTable && scrollTop !== scrollTable.scrollTop) {
                    scrollTable.scrollTo(0, scrollTop)
                }
            },

            handleScroll (e) {
                const { target } = e
                const { hasNext, setScrollTop, scrollLoadMore, isLoadingMore } = this
                setScrollTop(e.target.scrollTop)
                const offset = e.target.scrollHeight - (e.target.offsetHeight + e.target.scrollTop)
                if (offset <= SCROLL_THRESHOLD && hasNext && !isLoadingMore) { // scroll to end
                    scrollLoadMore(target.scrollTop)
                }
            },

            async fetchData (page = 1, pageSize = this.pageSize) {
                const res = await this.dataFetcher(page, pageSize)
                if (res) {
                    this.list = page === 1
                        ? res.records
                        : [
                            ...this.list,
                            ...res.records
                        ]

                    this.currentPage = Math.ceil(this.list.length / pageSize)
                    this.hasNext = this.list.length < res.count
                    this.totals = res.count
                    return res
                }
            },

            async queryList (page = 1, pageSize, isRefresh = false) {
                try {
                    this.isLoading = !isRefresh
                    const res = await this.fetchData(page, pageSize)
                    return res
                } catch (e) {
                    console.error(e)
                } finally {
                    this.isLoading = false
                }
            },

            async scrollLoadMore () {
                try {
                    this.isLoadingMore = true
                    await this.fetchData(this.currentPage + 1, this.pageSize)
                } catch (e) {
                    console.log(e)
                    this.$showTips({
                        message: this.$t('history.loadingErr'),
                        theme: 'error'
                    })
                } finally {
                    this.isLoadingMore = false
                    this.animateScroll(this.scrollTop)
                }
            },

            async updateList (isRefresh = false) {
                const { list, pageSize } = this
                const len = list.length
                const res = await this.queryList(1, len > pageSize ? len : pageSize, isRefresh)

                this.list = res.records

                return res
            }
        }
    }
</script>

<style lang="scss">
    .loading-more {
        display: flex;
        height: 36px;
        justify-content: center;
        align-items: center;
        .devops-icon {
            margin-right: 8px;
        }
    }
</style>
