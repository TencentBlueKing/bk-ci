<template>
    <ul
        class="total-static-list"
        v-bkloading="{ isLoading }"
    >
        <li
            v-for="(statistic, index) in statisticList"
            :key="index"
            class="static-item"
        >
            <icon
                :name="statistic.name"
                class="item-icon"
                size="64"
            ></icon>
            <h5 class="item-title">
                <span
                    :class="['item-name', { 'g-store-text-underline': statistic.tips }]"
                    v-bk-tooltips="{ content: statistic.tips, disabled: !statistic.tips }"
                >{{ statistic.label }}</span>
                <p
                    :class="{ 'item-value': true, 'g-text-link': statistic.linkName }"
                    @click="goToLink(statistic.linkName)"
                >
                    {{ statistic.value }}
                </p>
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
                this.getStatisticData().catch((err) => {
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

            getStatisticData () {
                const configMap = {
                    atom: {
                        storeCode: this.detail.atomCode,
                        storeType: 'ATOM',
                        extraItems: [
                            {
                                name: 'pipeline-count',
                                label: this.$t('store.流水线个数'),
                                value: 'pipelineCnt',
                                linkName: 'statisticPipeline'
                            },
                            { name: 'icon-success-rate', label: this.$t('store.成功率'), value: (res) => ![undefined, null].includes(res.successRate) ? `${res.successRate}%` : '--', tips: this.$t('store.最近三个月内的执行成功率') }
                        ]
                    },
                    service: {
                        storeCode: this.detail.serviceCode,
                        storeType: 'SERVICE',
                        extraItems: [
                            { name: 'rate', label: this.$t('store.星级'), value: (res) => res.score || '--' }
                        ]
                    },
                    devx: {
                        storeCode: this.detail.storeCode,
                        storeType: 'DEVX',
                        extraItems: [
                            { name: 'install-num', label: this.$t('store.装机量'), value: 'downloads' }
                        ]
                    }
                }

                const config = configMap[this.type]

                const baseItems = [
                    { name: 'install-num', label: this.$t('store.安装量'), value: 'downloads' },
                    { name: 'comment-num', label: this.$t('store.评论数'), value: 'commentCnt' },
                    { name: 'rate', label: this.$t('store.评分'), value: (res) => res.score || '--' }
                ]

                const statisticConfig = [...baseItems, ...config.extraItems]

                return this.$store.dispatch('store/requestAtomStatistic', {
                    storeCode: config.storeCode,
                    storeType: config.storeType
                }).then((res) => {
                    this.statisticList = statisticConfig.map(item => ({
                        name: item.name,
                        label: item.label,
                        value: typeof item.value === 'function' ? item.value(res) : (res[item.value] || '--'),
                        ...(item.linkName && { linkName: item.linkName }),
                        ...(item.tips && { tips: item.tips })
                    }))
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
                    white-space: nowrap;
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
