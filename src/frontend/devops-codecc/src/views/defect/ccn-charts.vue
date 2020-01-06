<template>
    <div class="charts-ccn">
        <div class="authors-wrapper">
            <div class="authors-charts">
                <div id="authorsChart" ref="authorsChart"></div>
            </div>
            <div class="authors-table">
                <div class="table">
                    <bk-table :empty-text="$t('st.暂无数据')" :data="authorsData" :outer-border="false">
                        <bk-table-column :label="$t('defect.告警处理人')" prop="authorName" align="center"></bk-table-column>
                        <bk-table-column :label="$t('charts.总数')" prop="total" align="center">
                            <template slot-scope="{ row }">
                                <a v-if="row.authorName !== 'Total'" href="javascript:;" @click="handleHref({ author: row.authorName })">{{row.total}}</a>
                                <a v-else href="javascript:;" @click="handleHref()">{{row.total}}</a>
                            </template>
                        </bk-table-column>
                        <bk-table-column :label="$t('charts.极高风险(>=60)')" prop="serious" align="center">
                            <template slot-scope="{ row }">
                                <a v-if="row.authorName !== 'Total'" href="javascript:;" @click="handleHref({ author: row.authorName, severity: 1 })">{{row.serious}}</a>
                                <a v-else href="javascript:;" @click="handleHref({ severity: 1 })">{{row.serious}}</a>
                            </template>
                        </bk-table-column>
                        <bk-table-column :label="$t('charts.高风险(40-59)')" prop="normal" align="center">
                            <template slot-scope="{ row }">
                                <a v-if="row.authorName !== 'Total'" href="javascript:;" @click="handleHref({ author: row.authorName, severity: 2 })">{{row.normal}}</a>
                                <a v-else href="javascript:;" @click="handleHref({ severity: 2 })">{{row.normal}}</a>
                            </template>
                        </bk-table-column>
                        <bk-table-column :label="$t('charts.中风险(20-39)')" prop="prompt" align="center">
                            <template slot-scope="{ row }">
                                <a v-if="row.authorName !== 'Total'" href="javascript:;" @click="handleHref({ author: row.authorName, severity: 4 })">{{row.prompt}}</a>
                                <a v-else href="javascript:;" @click="handleHref({ severity: 4 })">{{row.prompt}}</a>
                            </template>
                        </bk-table-column>
                    </bk-table>
                </div>
            </div>
        </div>
        <div class="trend-wrapper">
            <div class="trend-charts">
                <div id="trendChart" ref="trendChart"></div>
                <div class="tips">{{$t('charts.注项目函数平均圈复杂度')}}</div>
            </div>
        </div>
    </div>
</template>
<script>
    import chartBarOption from '@/mixins/chart-bar-option'
    import chartLineOption from '@/mixins/chart-line-option'
    import echarts from 'echarts/lib/echarts'
    import 'echarts/lib/chart/bar'
    import 'echarts/lib/chart/line'
    import 'echarts/lib/component/tooltip'
    import 'echarts/lib/component/title'
    import 'echarts/lib/component/legend'

    export default {
        components: {
        },
        mixins: [chartBarOption, chartLineOption],
        data () {
            return {
                chartAuthorList: {
                    authorList: []
                },
                chartAverageList: {
                    averageList: []
                },
                authorsData: []
            }
        },
        computed: {
        },
        created () {
        },
        mounted () {
            this.init()
        },
        methods: {
            async init () {
                const toolId = 'CCN'
                const res = await this.$store.dispatch('defect/report', { toolId }, { showLoading: true })
                this.chartAuthorList = res.chartAuthorList || {}
                this.chartAverageList = res.chartAverageList || {}
                this.initAuthors()
                this.initTrend()
            },
            initAuthors () {
                const authorList = this.chartAuthorList.authorList
                const name = []
                const highN = []
                const mediumN = []
                const lowN = []

                if (authorList) {
                    authorList.forEach((author, index) => {
                        name.push(author['authorName'])
                        highN.push(author['serious'])
                        mediumN.push(author['normal'])
                        lowN.push(author['prompt'])
                    })
                    authorList.push(this.chartAuthorList.totalAuthor)
                }
                this.authorsData = authorList

                const option = {
                    title: {
                        text: this.$t('charts.待修复函数处理人分布')
                    },
                    legend: {
                        data: [{
                            name: this.$t('charts.极高风险')
                        }, {
                            name: this.$t('charts.高风险')
                        }, {
                            name: this.$t('charts.中风险')
                        }]
                    },
                    xAxis: {
                        data: name
                    },
                    yAxis: {
                        splitNumber: 4,
                        minInterval: 1
                    },
                    grid: {
                        left: '50'
                    },
                    series: [
                        {
                            name: this.$t('charts.中风险'),
                            data: lowN
                        },
                        {
                            name: this.$t('charts.高风险'),
                            data: mediumN
                        },
                        {
                            name: this.$t('charts.极高风险'),
                            data: highN
                        }
                    ]
                }

                const authorsChart = echarts.init(this.$refs.authorsChart)
                authorsChart.setOption(this.chartBarOption)
                authorsChart.setOption(option)
                window.addEventListener('resize', () => {
                    authorsChart.resize()
                })
            },
            initTrend () {
                const elemList = this.chartAverageList.averageList || []
                const xAxisData = []
                const averageCCN = []
                const recommendValue = []
                elemList.forEach((element, index) => {
                    xAxisData.unshift(element['tips'])
                    averageCCN.unshift(element['averageCCN'])
                    recommendValue.unshift(2) // 圈复杂度上限建议值为2
                })

                const option = {
                    title: {
                        text: this.$t('charts.函数平均圈复杂度趋势')
                    },
                    legend: {
                        data: [
                            {
                                name: this.$t('charts.上限建议值')
                            },
                            {
                                name: this.$t('charts.圈复杂度')
                            }
                        ]
                    },
                    xAxis: {
                        data: xAxisData
                    },
                    series: [
                        {
                            name: this.$t('charts.圈复杂度'),
                            type: 'line',
                            data: averageCCN
                        },
                        {
                            name: this.$t('charts.上限建议值'),
                            type: 'line',
                            symbol: 'none',
                            itemStyle: {
                                normal: {
                                    color: '#2DCB56'
                                }
                            },
                            areaStyle: {
                                normal: {
                                    color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{
                                        offset: 0,
                                        color: 'rgba(45,203,86,0.03)'
                                    }, {
                                        offset: 1,
                                        color: 'rgba(45,203,86,0.03)'
                                    }])
                                }
                            },
                            data: recommendValue
                        }
                    ]
                }
                
                const trendChart = echarts.init(this.$refs.trendChart)
                trendChart.setOption(this.chartLineOption)
                trendChart.setOption(option)
                window.addEventListener('resize', () => {
                    trendChart.resize()
                })
            },
            handleHref (query) {
                const resolved = this.$router.resolve({
                    name: 'defect-ccn-list',
                    params: this.$route.params,
                    query
                })
                const href = `${window.DEVOPS_SITE_URL}/console${resolved.href}`
                window.open(href, '_blank')
            }
        }
    }
</script>

<style lang="postcss" scoped>
    @import '../../css/variable.css';

    .charts-ccn {
        >>> .bk-table {
            th, td {
                height: 28px;
                
                &>.cell {
                    height: 28px;
                    line-height: 28px;
                }
            }
        }
        .authors-wrapper {
            >>> .bk-table-row-last {
                font-weight: bold;
            }
        }
        .authors-wrapper {
            width: 100%;
            background: #fff;
            margin-bottom: 15px;
            border: 1px solid $borderColor;

            .authors-charts, .authors-table {
                padding: 20px 15px;
            }
            #authorsChart {
                width: 100%;
                height: 215px;
            }
        }

        .trend-wrapper {
            width: 100%;
            background: #fff;
            margin-bottom: 15px;
            border: 1px solid $borderColor;

            .trend-charts {
                padding: 20px 15px;
            }
            #trendChart {
                width: 100%;
                height: 233px;
            }
        }

        .tips {
            font-size: 12px;
        }
    }
</style>
