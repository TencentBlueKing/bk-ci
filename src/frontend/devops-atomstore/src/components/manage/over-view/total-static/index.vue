<template>
    <ul class="total-static-list" v-bkloading="{ isLoading }">
        <li v-for="(statistic, index) in statisticList" :key="index" class="static-item">
            <icon :name="statistic.name" class="item-icon" size="64"></icon>
            <h5 class="item-title">
                <span :class="['item-name', { 'g-store-text-underline': statistic.tips }]"
                    v-bk-tooltips="{ content: statistic.tips, disabled: !statistic.tips }"
                >{{ statistic.label }}</span>
                <p :class="{ 'item-value': true, 'g-text-link': statistic.linkName }" @click="goToLink(statistic.linkName)">{{ statistic.value }}</p>
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
                    atom: this.getAtomData,
                    service: this.getServiceData
                }

                const currentMethod = methodGenerator[this.type]
                currentMethod().catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => {
                    this.isLoading = false
                })
            },

            goToLink (name) {
                if (name) {
                    this.$router.push({ name, params: { code: this.detail.atomCode, type: 'atom' } })
                }
            },

            getAtomData () {
                return this.$store.dispatch('store/requestAtomStatistic', {
                    storeCode: this.detail.atomCode,
                    storeType: 'ATOM'
                }).then((res) => {
                    this.statisticList = [
                        { name: 'install-num', label: this.$t('store.安装量'), value: res.downloads },
                        {
                            name: 'pipeline-count',
                            label: this.$t('store.流水线个数'),
                            value: res.pipelineCnt,
                            linkName: 'statisticPipeline'
                        },
                        { name: 'comment-num', label: this.$t('store.评论数'), value: res.commentCnt },
                        { name: 'rate', label: this.$t('store.评分'), value: res.score || '--' },
                        { name: 'icon-success-rate', label: this.$t('store.成功率'), value: ![undefined, null].includes(res.successRate) ? `${res.successRate}%` : '--', tips: this.$t('store.最近三个月内的执行成功率') }
                    ]
                })
            },

            getServiceData () {
                return this.$store.dispatch('store/requestAtomStatistic', {
                    storeCode: this.detail.serviceCode,
                    storeType: 'SERVICE'
                }).then((res) => {
                    this.statisticList = [
                        { name: 'install-num', label: this.$t('store.安装量'), value: res.downloads },
                        { name: 'comment-num', label: this.$t('store.评论数'), value: res.commentCnt },
                        { name: 'rate', label: this.$t('store.星级'), value: res.score || '--' }
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
            min-width: 20%;
            max-width: 20%;
            &:after {
                content: '';
                height: 26px;
                width: 1px;
                margin: 0 .28rem 0 0.24rem;
                background: #dcdee5;
            }
            &:last-child:after {
                width: 0;
            }
            .item-icon {
                margin-right: .1rem;
            }
            .item-title {
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;
                font-weight: normal;
                min-width: .75rem;
                .item-name {
                    font-size: .14rem;
                    color: #999;
                    line-height: .2rem;
                }
                .item-value {
                    font-weight: 600;
                    font-size: .26rem;
                    line-height: .3rem;
                    margin-top: .03rem;
                    box-sizing: border-box;
                    height: .3rem;
                }
            }
        }
    }
</style>
