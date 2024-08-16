<template>
    <section class="over-view-news" v-bkloading="{ isLoading }">
        <bk-timeline :list="list"></bk-timeline>
    </section>
</template>

<script>
    export default {
        props: {
            detail: Object,
            type: String
        },

        data () {
            return {
                list: [],
                isLoading: false,
                current: 1,
                limit: 100,
                loadEnd: false,
                isLoadingMore: false
            }
        },

        created () {
            this.initData()
        },

        mounted () {
            this.addScrollLoadMore()
        },

        beforeDestroy () {
            this.removeScrollLoadMore()
        },

        methods: {
            initData () {
                this.isLoadingMore = true
                const methodGenerator = {
                    atom: this.getAtomData,
                    service: this.getImageData
                }

                const currentMethod = methodGenerator[this.type]
                currentMethod().then(() => {
                    this.current += 1
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => {
                    this.isLoadingMore = false
                })
            },

            getAtomData () {
                return this.$store.dispatch('store/requestVersionList', {
                    atomCode: this.detail.atomCode,
                    page: this.current,
                    pageSize: this.limit
                }).then((res) => {
                    const records = res.records || []
                    this.list = [...this.list, ...records.map((x) => ({
                        tag: x.createTime,
                        content: `${x.creator} ${this.$t('store.新增版本')} ${x.version}`
                    }))]
                    this.loadEnd = res.count <= this.list.length
                })
            },

            getImageData () {
                return this.$store.dispatch('store/requestVersionLog', this.detail.serviceCode).then((res) => {
                    const records = res.records || []
                    this.list = records.map((x) => ({
                        tag: x.createTime,
                        content: `${x.creator} ${this.$t('store.新增版本')} ${x.version}`
                    }))
                })
            },
            scrollLoadMore (event) {
                const target = event.target
                const bottomDis = target.scrollHeight - target.clientHeight - target.scrollTop
                if (bottomDis <= 500 && !this.loadEnd && !this.isLoadingMore) this.initData()
            },

            addScrollLoadMore () {
                const mainBody = document.querySelector('.over-view-news')
                if (mainBody) mainBody.addEventListener('scroll', this.scrollLoadMore, { passive: true })
            },

            removeScrollLoadMore () {
                const mainBody = document.querySelector('.over-view-news')
                if (mainBody) mainBody.removeEventListener('scroll', this.scrollLoadMore, { passive: true })
            }
        }
    }
</script>

<style lang="scss" scoped>
    .over-view-news {
        overflow: auto;
        height: calc(100% - .28rem);
        padding: 15px 32px 0 37px;
        ::v-deep .bk-timeline-dot::before {
            border-color: #1592ff;
        }
    }
</style>
