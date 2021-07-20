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
                isLoading: false
            }
        },

        created () {
            this.initData()
        },

        methods: {
            initData () {
                const methodGenerator = {
                    atom: this.getAtomData
                }

                const currentMethod = methodGenerator[this.type]
                currentMethod().catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                })
            },

            getAtomData () {
                return this.$store.dispatch('store/requestVersionList', {
                    atomCode: this.detail.atomCode
                }).then((res) => {
                    const records = res.records || []
                    this.list = records.map((x) => ({
                        tag: x.createTime,
                        content: `${x.creator} ${this.$t('store.新增版本')} ${x.version}`
                    }))
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    .over-view-news {
        overflow: auto;
        height: calc(100% - .28rem);
        padding: 15px 32px 0 37px;
        /deep/ .bk-timeline-dot::before {
            border-color: #1592ff;
        }
    }
</style>
