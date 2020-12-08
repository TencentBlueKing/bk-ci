<template>
    <div class="charts-dupc">
        <div class="breadcrumb">
            <div class="breadcrumb-name">
                <bk-tab :active.sync="active" @tab-change="handleTableChange" type="unborder-card">
                    <bk-tab-panel
                        v-for="(panel, index) in panels"
                        v-bind="panel"
                        :key="index">
                    </bk-tab-panel>
                </bk-tab>
                <span class="codecc-icon icon-export-excel excel-download" @click="downloadExcel" v-bk-tooltips="$t('导出Excel')"></span>
            </div>
        </div>
        <div class="main-container">
            <div>
                <bk-form :label-width="60">
                    <container class="cc-container">
                        <div class="cc-col">
                            <bk-form-item :label="$t('日期')">
                                <bk-date-picker v-model="searchParams.daterange" :type="'daterange'" :options="pickerOptions"></bk-date-picker>
                            </bk-form-item>
                        </div>
                    </container>
                </bk-form>
            </div>
            <div class="authors-wrapper">
                <div class="authors-charts">
                    <div id="authorsChart" ref="authorsChart"></div>
                </div>
                <div class="authors-table">
                    <table>
                        <thead>
                            <tr class="table-tr">
                                <th>{{$t('风险级别')}}</th>
                                <th>{{$t('文件个数')}}</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><i class="block-sup"></i>{{$t('极高风险(>=20%)')}}</td>
                                <td><a href="javascript:;" @click="handleHref({ severity: 1 })">{{ dupcData[0] }}</a></td>
                            </tr>
                            <tr>
                                <td><i class="block-high"></i>{{$t('高风险11%20%')}}</td>
                                <td><a href="javascript:;" @click="handleHref({ severity: 2 })">{{ dupcData[1] }}</a></td>
                            </tr>
                            <tr>
                                <td><i class="block-mid"></i>{{$t('中风险5%11%')}}</td>
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
                    <div class="tips">{{$t('注代码重复率')}}</div>
                </div>
            </div>
        </div>
    </div>
</template>
<script>
    import chart from '@/mixins/chart'
    import echarts from 'echarts/lib/echarts'
    import { format } from 'date-fns'
    // eslint-disable-next-line
    import { export_json_to_excel } from 'vendor/export2Excel'

    export default {
        components: {
        },
        mixins: [chart],
        data () {
            const query = this.$route.query

            return {
                panels: [
                    { name: 'defect', label: this.$t('重复文件') },
                    { name: 'report', label: this.$t('数据报表') }
                ],
                chartRiskList: {},
                chartTrendList: {
                    ducpChartList: []
                },
                dupcData: [],
                searchParams: {
                    taskId: this.$route.params.taskId,
                    toolId: 'DUPC',
                    daterange: [query.startTime, query.endTime]
                },
                toolId: 'DUPC',
                authorsChart: undefined,
                trendChart: undefined
            }
        },
        computed: {
            pickerOptions () {
                return {
                    disabledDate (time) {
                        return time.getTime() > Date.now()
                    }
                }
            }
        },
        watch: {
            searchParams: {
                handler () {
                    this.fetchLintList().then(res => {
                        this.chartRiskList = res.chartRiskList || {}
                        this.chartTrendList = res.chartTrendList || {}
                        this.initAuthors()
                        this.initTrend()
                    })
                },
                deep: true
            }
        },
        created () {
        },
        mounted () {
            // this.fetchLintList()
        },
        methods: {
            formatTime (date, token, options = {}) {
                return date ? format(Number(date), token, options) : ''
            },
            async fetchLintList () {
                this.searchParams.daterange[0] = this.searchParams.daterange[0] > Date.now() ? Date.now() : this.searchParams.daterange[0]
                this.searchParams.daterange[1] = this.searchParams.daterange[1] > Date.now() ? Date.now() : this.searchParams.daterange[1]
                this.searchParams.daterange[0] = this.searchParams.daterange[0] < this.searchParams.daterange[1] ? this.searchParams.daterange[0] : this.searchParams.daterange[1]
                const daterange = this.searchParams.daterange
                let startTime = this.formatTime(daterange[0], 'YYYY-MM-DD')
                startTime = startTime === 'Invalid Date' ? '' : startTime
                let endTime = this.formatTime(daterange[1], 'YYYY-MM-DD')
                endTime = endTime === 'Invalid Date' ? '' : endTime
                const params = { ...this.searchParams, startTime, endTime, showLoading: true }
                const res = await this.$store.dispatch('defect/report', params)
                return res
            },
            async initAuthors () {
                const { superHighCount, highCount, mediumCount, totalCount } = this.chartRiskList
                this.dupcData = [superHighCount, highCount, mediumCount, totalCount]
                const dupcFileData = [superHighCount, highCount, mediumCount]

                const option = {
                    title: {
                        text: this.$t('重复文件分布')
                    },
                    xAxis: {
                        axisLabel: {
                            rotate: 0
                        },
                        data: [this.$t('极高风险'), this.$t('高风险'), this.$t('中风险')]
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
                            name: this.$t('文件'),
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
                            data: dupcFileData,
                            label: {
                                normal: {
                                    show: true,
                                    position: 'top',
                                    fontSize: 16,
                                    distance: 10
                                }
                            }
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
                        text: this.$t('代码重复率趋势')
                    },
                    legend: {
                        data: [
                            {
                                name: this.$t('上限建议值')
                            },
                            {
                                name: this.$t('重复率')
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
                            name: this.$t('重复率'),
                            type: 'line',
                            data: dupc
                        },
                        {
                            name: this.$t('上限建议值'),
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
                this.resolveHref('defect-dupc-list', query)
            },
            downloadExcel () {
                const data = [
                    {
                        riskLevel: this.$t('极高风险(>=20%)'),
                        fileNum: this.dupcData[0]
                    },
                    {
                        riskLevel: this.$t('高风险11%20%'),
                        fileNum: this.dupcData[1]
                    },
                    {
                        riskLevel: this.$t('中风险5%11%'),
                        fileNum: this.dupcData[2]
                    },
                    {
                        riskLevel: this.$t('Total'),
                        fileNum: this.dupcData[3]
                    }
                ]
                const excelData1 = this.getExcelData([this.$t('风险级别'), this.$t('文件个数')], ['riskLevel', 'fileNum'], data, '重复文件分布')
                const excelData = [excelData1]
                const title = `${this.taskDetail.nameCn}-${this.taskDetail.taskId}-${this.toolId}-${this.$t('数据报表')}-${new Date().toISOString()}`
                const sheets = ['重复文件分布']
                export_json_to_excel('', excelData, title, sheets)
            },
            getExcelData (tHeader, filterVal, list, sheetName) {
                const data = this.formatJson(filterVal, list)
                return { tHeader, data, sheetName }
            },
            formatJson (filterVal, list) {
                return list.map(item => filterVal.map(j => {
                    return item[j]
                }))
            }
        }
    }
</script>

<style lang="postcss" scoped>
    @import '../../css/variable.css';
    @import '../../css/charts.css';

    .charts-dupc {
        padding: 16px 20px 0px 16px;
        .breadcrumb {
            padding: 0px!important;
            .breadcrumb-name {
                background: white;
            }
        }
        .main-container {
            /* padding: 20px 33px 0!important;
            margin: 0 -13px!important; */
            border-top: 1px solid #dcdee5;
            margin: 0px!important;
            background: white;
        }
    }
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
                font-size: 12px;

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
    .excel-download {
        position: absolute;
        right: 20px;
        top: 29px;
        cursor: pointer;
        padding-right: 10px;
        &:hover {
            color: #3a84ff;
        }
    }
</style>
