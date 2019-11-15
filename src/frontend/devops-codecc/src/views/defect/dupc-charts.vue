<template>
    <div class="charts-dupc">
        <div class="authors-wrapper">
            <div class="authors-charts">
                <div id="authorsChart" ref="authorsChart"></div>
            </div>
            <div class="authors-table">
                <table>
                    <thead>
                        <tr class="table-tr">
                            <th>{{$t('defect.风险级别')}}</th>
                            <th>{{$t('charts.文件个数')}}</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td><i class="block-sup"></i>{{$t('charts.极高风险(>=20%)')}}</td>
                            <td><a href="javascript:;" @click="handleHref({ severity: 1 })">{{ dupcData[0] }}</a></td>
                        </tr>
                        <tr>
                            <td><i class="block-high"></i>{{$t('charts.高风险11%20%')}}</td>
                            <td><a href="javascript:;" @click="handleHref({ severity: 2 })">{{ dupcData[1] }}</a></td>
                        </tr>
                        <tr>
                            <td><i class="block-mid"></i>{{$t('charts.中风险5%11%')}}</td>
                            <td><a href="javascript:;" @click="handleHref({ severity: 4 })">{{ dupcData[2] }}</a></td>
                        </tr>
                        <tr>
                            <td style="font-weight: bold;">Total</td>
                            <td><a href="javascript:;" @click="handleHref()">{{ dupcData[3] }}</a></td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
        <div class="trend-wrapper">
            <div class="trend-charts">
                <div id="trendChart" ref="trendChart"></div>
                <div class="tips">{{$t('charts.注项目代码重复率')}}</div>
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
                chartRiskList: {},
                chartTrendList: {
                    ducpChartList: []
                },
                dupcData: []
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
                const toolId = 'DUPC'
                const res = await this.$store.dispatch('defect/report', { toolId }, { showLoading: true })
                this.chartRiskList = res.chartRiskList || {}
                this.chartTrendList = res.chartTrendList || {}
                this.initAuthors()
                this.initTrend()
            },
            async initAuthors () {
                const { superHighCount, highCount, mediumCount, totalCount } = this.chartRiskList
                this.dupcData = [superHighCount, highCount, mediumCount, totalCount]

                const option = {
                    title: {
                        text: this.$t('charts.重复文件分布')
                    },
                    xAxis: {
                        axisLabel: {
                            rotate: 0
                        },
                        data: [this.$t('charts.极高风险'), this.$t('charts.高风险'), this.$t('charts.中风险')]
                    },
                    yAxis: {
                        splitNumber: 4,
                        minInterval: 1
                    },
                    grid: {
                        left: '50'
                    },
                    legend: {
                        data: []
                    },
                    series: [
                        {
                            name: this.$t('charts.文件'),
                            type: 'bar',
                            barWidth: '30%',
                            itemStyle: {
                                normal: {
                                    color: params => {
                                        const colorList = ['#ff5656', '#ff9c01', '#7572dc']
                                        return colorList[params.dataIndex]
                                    }
                                }
                            },
                            data: this.dupcData
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
            async initTrend () {
                const elemList = this.chartTrendList.ducpChartList || []
                const xAxisData = []
                const dupc = []
                const recommendValue = []
                elemList.forEach((element, index) => {
                    xAxisData.unshift(element['tips'])
                    dupc.unshift(element['dupc'])
                    recommendValue.unshift(5) // 重复率上限建议值为5%
                })

                const option = {
                    title: {
                        text: this.$t('charts.项目代码重复率趋势')
                    },
                    legend: {
                        data: [
                            {
                                name: this.$t('charts.上限建议值')
                            },
                            {
                                name: this.$t('charts.重复率')
                            }
                        ]
                    },
                    tooltip: {
                        formatter: '{b0}<br />{a0}: {c0}%<br />{a1}: {c1}%'
                    },
                    xAxis: {
                        data: xAxisData
                    },
                    yAxis: {
                        axisLabel: {
                            formatter: '{value}%'
                        }
                    },
                    series: [
                        {
                            name: this.$t('charts.重复率'),
                            type: 'line',
                            data: dupc
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
                    name: 'defect-dupc-list',
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

    .authors-wrapper {
        width: 100%;
        height: 225px;
        background: #fff;
        margin-bottom: 15px;
        border: 1px solid $borderColor;

        .authors-charts, .authors-table {
            float: left;
            height: 215px;
            padding: 20px 15px;
        }
        .authors-charts {
            width: 68%;
            height: 224px;
            border-right: 1px solid $borderColor;
        }
        .authors-table {
            width: 32%;
            padding-left: 40px;

            table {
                width: 100%;
                text-align: left;
                font-size: 14px;

                tr {
                    height: 35px;

                    i {
                        display: inline-block;
                        width: 20px;
                        height: 12px;
                        background: red;
                        position: relative;
                        top: 1px;
                        margin-right: 8px;

                        &.block-sup {
                            background: #ff5656;
                        }
                        &.block-high {
                            background: #ff9c01;
                        }
                        &.block-mid {
                            background: #7572dc;
                        }
                    }
                }
            }
        }
        #authorsChart {
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
</style>
