<template>
    <div class="charts-ccn">
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
                    <div class="table">
                        <bk-table :data="authorsData" :outer-border="false">
                            <bk-table-column :label="$t('问题处理人')" prop="authorName" align="center"></bk-table-column>
                            <bk-table-column :label="$t('总数')" prop="total" align="center">
                                <template slot-scope="{ row }">
                                    <a v-if="row.authorName !== 'Total'" href="javascript:;" @click="handleHref({ author: row.authorName })">{{row.total}}</a>
                                    <a v-else href="javascript:;" @click="handleHref()">{{row.total}}</a>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('极高风险(>=60)')" prop="superHigh" align="center">
                                <template slot-scope="{ row }">
                                    <a v-if="row.authorName !== 'Total'" href="javascript:;" @click="handleHref({ author: row.authorName, severity: 1 })">{{row.superHigh}}</a>
                                    <a v-else href="javascript:;" @click="handleHref({ severity: 1 })">{{row.superHigh}}</a>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('高风险(40-59)')" prop="high" align="center">
                                <template slot-scope="{ row }">
                                    <a v-if="row.authorName !== 'Total'" href="javascript:;" @click="handleHref({ author: row.authorName, severity: 2 })">{{row.high}}</a>
                                    <a v-else href="javascript:;" @click="handleHref({ severity: 2 })">{{row.high}}</a>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('中风险(20-39)')" prop="medium" align="center">
                                <template slot-scope="{ row }">
                                    <a v-if="row.authorName !== 'Total'" href="javascript:;" @click="handleHref({ author: row.authorName, severity: 4 })">{{row.medium}}</a>
                                    <a v-else href="javascript:;" @click="handleHref({ severity: 4 })">{{row.medium}}</a>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('低风险(1-19)')" prop="low" align="center">
                                <template slot-scope="{ row }">
                                    <a v-if="row.authorName !== 'Total'" href="javascript:;" @click="handleHref({ author: row.authorName, severity: 8 })">{{row.low}}</a>
                                    <a v-else href="javascript:;" @click="handleHref({ severity: 8 })">{{row.low}}</a>
                                </template>
                            </bk-table-column>
                            <div slot="empty">
                                <div class="codecc-table-empty-text">
                                    <img src="../../images/empty.png" class="empty-img">
                                    <div>{{$t('暂无数据')}}</div>
                                </div>
                            </div>
                        </bk-table>
                    </div>
                </div>
            </div>
            <div class="trend-wrapper">
                <div class="trend-charts">
                    <div id="trendChart" ref="trendChart"></div>
                    <div class="tips">{{$t('注函数平均圈复杂度')}}</div>
                </div>
            </div>
            <div class="trend-wrapper">
                <div class="trend-charts">
                    <div id="overTrendChart" ref="overTrendChart"></div>
                    <div class="tips">{{$t('注超标圈复杂度总和')}}</div>
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
                    { name: 'defect', label: this.$t('风险函数') },
                    { name: 'report', label: this.$t('数据报表') }
                ],
                chartAuthorList: {
                    authorList: []
                },
                chartBeyondThresholdList: {
                    averageList: []
                },
                authorsData: [],
                searchParams: {
                    taskId: this.$route.params.taskId,
                    toolId: 'CCN',
                    daterange: [query.startTime, query.endTime]
                },
                authorsChart: undefined,
                trendChart: undefined,
                overTrendChart: undefined
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
                        this.chartAuthorList = res.chartAuthorList || {}
                        this.chartAverageList = res.chartAverageList || {}
                        this.chartBeyondThresholdList = res.chartBeyondThresholdList || {}
                        this.initAuthors()
                        this.initTrend()
                        this.initOverTrend()
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
            initAuthors () {
                const authorList = this.chartAuthorList.authorList
                const name = []
                const superHighN = []
                const highN = []
                const mediumN = []
                const lowN = []

                if (authorList) {
                    authorList.forEach((author, index) => {
                        name.push(author['authorName'])
                        superHighN.push(author['superHigh'])
                        highN.push(author['high'])
                        mediumN.push(author['medium'])
                        lowN.push(author['low'])
                    })
                    authorList.push(this.chartAuthorList.totalAuthor)
                }
                this.authorsData = authorList

                const option = {
                    color: ['#32e396', '#7572dc', '#ff9c01', '#ff5656'],
                    title: {
                        text: this.$t('待修复函数处理人分布')
                    },
                    legend: {
                        data: [{
                            name: this.$t('极高风险')
                        }, {
                            name: this.$t('高风险')
                        }, {
                            name: this.$t('中风险')
                        }, {
                            name: this.$t('低风险')
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
                            name: this.$t('低风险'),
                            data: lowN
                        },
                        {
                            name: this.$t('中风险'),
                            data: mediumN
                        },
                        {
                            name: this.$t('高风险'),
                            data: highN
                        },
                        {
                            name: this.$t('极高风险'),
                            data: superHighN
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
                        text: this.$t('函数平均圈复杂度趋势')
                    },
                    legend: {
                        data: [
                            {
                                name: this.$t('上限建议值')
                            },
                            {
                                name: this.$t('圈复杂度')
                            }
                        ]
                    },
                    xAxis: {
                        data: xAxisData
                    },
                    series: [
                        {
                            name: this.$t('圈复杂度'),
                            type: 'line',
                            data: averageCCN
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
            initOverTrend () {
                const elemList = this.chartBeyondThresholdList.averageList || []
                const xAxisData = []
                const averageCCN = []
                elemList.forEach((element, index) => {
                    xAxisData.unshift(element['tips'])
                    averageCCN.unshift(element['ccnBeyondThresholdSum'])
                })

                const option = {
                    title: {
                        text: this.$t('超标圈复杂度总和趋势')
                    },
                    legend: {
                        data: [
                            {
                                name: this.$t('超标圈复杂度总和')
                            }
                        ]
                    },
                    xAxis: {
                        data: xAxisData
                    },
                    series: [
                        {
                            name: this.$t('超标圈复杂度总和'),
                            type: 'line',
                            data: averageCCN
                        }
                    ]
                }
                
                const overTrendChart = echarts.init(this.$refs.overTrendChart)
                overTrendChart.setOption(this.chartLineOption)
                overTrendChart.setOption(option)
                window.addEventListener('resize', () => {
                    overTrendChart.resize()
                })
            },
            handleHref (query) {
                this.resolveHref('defect-ccn-list', query)
            },
            downloadExcel () {
                const excelData1 = this.getExcelData([this.$t('问题处理人'), this.$t('总数'), this.$t('极高风险(>=60)'), this.$t('高风险(40-59)'), this.$t('中风险(20-39)'), this.$t('低风险(1-19)')], ['authorName', 'total', 'superHigh', 'high', 'medium', 'low'], this.authorsData, '待修复函数处理人分布')
                const excelData = [excelData1]
                const title = `${this.taskDetail.nameCn}-${this.taskDetail.taskId}-${this.toolId}-${this.$t('数据报表')}-${new Date().toISOString()}`
                const sheets = ['待修复函数处理人分布']
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

    .charts-ccn {
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
            #overTrendChart {
                width: 100%;
                height: 233px;
            }
        }

        .tips {
            font-size: 12px;
        }
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
