<template>
    <ul class="total-static-list" v-bkloading="{ isLoading }">
        <li v-for="(statistic, index) in statisticList" :key="index" class="static-item">
            <icon :name="statistic.name" class="item-icon" size="64"></icon>
            <h5 class="item-title">
                <span class="item-name">{{ statistic.label }}</span>
                <p class="item-value">{{ statistic.value }}</p>
            </h5>
        </li>
    </ul>
</template>

<script>
    export default {
        props: {
            detail: Object,
            type: String
        },

        data () {
            return {
                statisticList: [],
                isLoading: false
            }
        },

        created () {
            this.initData()
        },

        methods: {
            initData () {
                this.isLoading = true
                const methodGenerator = {
                    atom: this.getAtomData
                }

                const currentMethod = methodGenerator[this.type]
                currentMethod().catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => {
                    this.isLoading = false
                })
            },

            getAtomData () {
                return this.$store.dispatch('store/requestAtomStatistic', {
                    atomCode: this.detail.atomCode
                }).then((res) => {
                    this.statisticList = [
                        { name: 'install-num', label: this.$t('store.安装量'), value: res.downloads },
                        { name: 'pipeline-count', label: this.$t('store.流水线个数'), value: res.pipelineCnt },
                        { name: 'comment-num', label: this.$t('store.评论数'), value: res.commentCnt },
                        { name: 'rate', label: this.$t('store.评分'), value: res.score }
                    ]
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    .total-static-list {
        height: calc(100% - .28rem);
        display: flex;
        align-items: center;
        .static-item {
            display: flex;
            align-items: center;
            &:not(:last-child):after {
                content: '';
                height: 26px;
                width: 1px;
                margin: 0 .48rem 0 0.52rem;
                background: #dcdee5;
            }
            .item-icon {
                margin-right: .24rem;
            }
            .item-title {
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;
                font-weight: normal;
                .item-name {
                    font-size: 14px;
                    color: #999;
                    line-height: 20px;
                }
                .item-value {
                    font-weight: 600;
                    font-size: 36px;
                    line-height: 36px;
                }
            }
        }
    }
</style>
