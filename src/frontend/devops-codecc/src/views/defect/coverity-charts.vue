<template>
    <div class="charts-lint">
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
                            <bk-form-item :label="$t('工具')">
                                <bk-select v-model="toolId" @selected="handleSelectTool" :clearable="false" searchable>
                                    <bk-option-group
                                        v-for="group in toolList"
                                        :name="group.name"
                                        :key="group.key">
                                        <bk-option v-for="option in group.toolList"
                                            :key="option.toolName"
                                            :id="option.toolName"
                                            :name="option.toolDisplayName">
                                        </bk-option>
                                    </bk-option-group>
                                </bk-select>
                            </bk-form-item>
                        </div>
                        <div class="cc-col">
                            <bk-form-item :label="$t('日期')">
                                <bk-date-picker v-model="searchParams.daterange" :type="'daterange'" :options="pickerOptions"></bk-date-picker>
                            </bk-form-item>
                        </div>
                    </container>
                </bk-form>
            </div>
            <div class="trend-wrapper">
                <div class="trend-box small">
                    <div class="trend-charts">
                        <div class="trend-charts-item" id="existTrendChart" ref="existTrendChart"></div>
                    </div>
                </div>
            </div>
            <div class="authors-wrapper">
                <div class="authors-charts">
                    <div class="authors-charts-item" id="existAuthorsChart" ref="existAuthorsChart"></div>
                </div>
                <div class="authors-table">
                    <div class="table">
                        <bk-table :data="existAuthorsTableData" :outer-border="false">
                            <bk-table-column :label="$t('问题处理人')" prop="authorName" align="center"></bk-table-column>
                            <bk-table-column :label="$t('总数')" prop="total" align="center">
                                <template slot-scope="{ row }">
                                    <a v-if="row.authorName !== 'Total'" href="javascript:;" @click="handleHref({ author: row.authorName })">{{row.total}}</a>
                                    <a v-else href="javascript:;" @click="handleHref({})">{{row.total}}</a>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('严重')" prop="serious" align="center">
                                <template slot-scope="{ row }">
                                    <a v-if="row.authorName !== 'Total'" href="javascript:;" @click="handleHref({ author: row.authorName, severity: 1 })">{{row.serious}}</a>
                                    <a v-else href="javascript:;" @click="handleHref({ severity: 1 })">{{row.serious}}</a>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('一般')" prop="normal" align="center">
                                <template slot-scope="{ row }">
                                    <a v-if="row.authorName !== 'Total'" href="javascript:;" @click="handleHref({ author: row.authorName, severity: 2 })">{{row.normal}}</a>
                                    <a v-else href="javascript:;" @click="handleHref({ severity: 2 })">{{row.normal}}</a>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('提示')" prop="prompt" align="center">
                                <template slot-scope="{ row }">
                                    <a v-if="row.authorName !== 'Total'" href="javascript:;" @click="handleHref({ author: row.authorName, severity: 4 })">{{row.prompt}}</a>
                                    <a v-else href="javascript:;" @click="handleHref({ severity: 4 })">{{row.prompt}}</a>
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
                <div class="trend-box">
                    <div class="trend-charts">
                        <div class="trend-charts-item" id="newTrendChart" ref="newTrendChart"></div>
                    </div>
                    <div class="trend-table small">
                        <div class="table">
                            <bk-table :data="trendNewTableData.slice(0, midLength)" :outer-border="false">
                                <bk-table-column :label="$t('日期')" prop="tips" align="center"></bk-table-column>
                                <bk-table-column :label="$t('新增数')" align="center">
                                    <template slot-scope="{ row }">
                                        <span>{{row.newCount}}</span>
                                    </template>
                                </bk-table-column>
                                <bk-table-column :label="$t('待修复数')" align="center">
                                    <template slot-scope="{ row }">
                                        <a @click="handleHref({ startTime: row.date, endTime: row.date })" href="javascript:;">{{row.existCount}}</a>
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
                    <div class="trend-table small">
                        <div class="table">
                            <bk-table :data="trendNewTableData.slice(midLength, trendNewTableData.length)" :outer-border="false">
                                <bk-table-column :label="$t('日期')" prop="tips" align="center"></bk-table-column>
                                <bk-table-column :label="$t('新增数')" align="center">
                                    <template slot-scope="{ row }">
                                        <span>{{row.newCount}}</span>
                                    </template>
                                </bk-table-column>
                                <bk-table-column :label="$t('待修复数')" align="center">
                                    <template slot-scope="{ row }">
                                        <a @click="handleHref({ startTime: row.date, endTime: row.date })" href="javascript:;">{{row.existCount}}</a>
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
            </div>
            <div class="trend-wrapper">
                <div class="trend-box">
                    <div class="trend-charts">
                        <div class="trend-charts-item" id="fixedTrendChart" ref="fixedTrendChart"></div>
                    </div>
                    <div class="trend-table small">
                        <div class="table">
                            <bk-table :data="trendFixedTableData.slice(0, midLength)" :outer-border="false">
                                <bk-table-column :label="$t('日期')" prop="tips" align="center"></bk-table-column>
                                <bk-table-column :label="$t('关闭总数')" align="center">
                                    <template slot-scope="{ row }">
                                        <span>{{row.closedCount}}</span>
                                    </template>
                                </bk-table-column>
                                <bk-table-column :label="$t('修复数')" align="center">
                                    <template slot-scope="{ row }">
                                        <a @click="handleHref({ status: 2, dateType: 'fixTime', startTime: row.date, endTime: row.date })" href="javascript:;">{{row.repairedCount}}</a>
                                    </template>
                                </bk-table-column>
                                <bk-table-column :label="$t('忽略数')" align="center">
                                    <template slot-scope="{ row }">
                                        <span>{{row.ignoreCount}}</span>
                                    </template>
                                </bk-table-column>
                                <bk-table-column :label="$t('过滤屏蔽数')" align="center">
                                    <template slot-scope="{ row }">
                                        <span>{{row.excludedCount}}</span>
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
                    <div class="trend-table small">
                        <div class="table">
                            <bk-table :data="trendFixedTableData.slice(midLength, trendFixedTableData.length)" :outer-border="false">
                                <bk-table-column :label="$t('日期')" prop="tips" align="center"></bk-table-column>
                                <bk-table-column :label="$t('关闭总数')" align="center">
                                    <template slot-scope="{ row }">
                                        <span>{{row.closedCount}}</span>
                                    </template>
                                </bk-table-column>
                                <bk-table-column :label="$t('修复数')" align="center">
                                    <template slot-scope="{ row }">
                                        <a @click="handleHref({ status: 2, dateType: 'fixTime', startTime: row.date, endTime: row.date })" href="javascript:;">{{row.repairedCount}}</a>
                                    </template>
                                </bk-table-column>
                                <bk-table-column :label="$t('忽略数')" align="center">
                                    <template slot-scope="{ row }">
                                        <span>{{row.ignoreCount}}</span>
                                    </template>
                                </bk-table-column>
                                <bk-table-column :label="$t('过滤屏蔽数')" align="center">
                                    <template slot-scope="{ row }">
                                        <span>{{row.excludedCount}}</span>
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
            </div>
        </div>
    </div>
</template>
<script>
    import chart from '@/mixins/chart'
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
                    { name: 'defect', label: this.$t('问题管理') },
                    { name: 'report', label: this.$t('数据报表') }
                ],
                trendNewTableData: [],
                trendFixedTableData: [],
                existAuthorsTableData: [],
                searchParams: {
                    taskId: this.$route.params.taskId,
                    toolId: this.$route.params.toolId,
                    daterange: [query.startTime, query.endTime]
                },
                existTrendChart: undefined,
                existAuthorsChart: undefined,
                newTrendChart: undefined,
                fixedTrendChart: undefined
            }
        },
        computed: {
            midLength () {
                const midLength = (this.trendNewTableData.length + 1) / 2 || 0
                return midLength
            },
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
                        this.initTrend('exist', res.newCloseFixChart && res.newCloseFixChart.elemList) // 待修复
                        this.initAuthor('exist', res.authorChart) // 待修复作者
                        this.initTrend('new', res.newCloseFixChart && res.newCloseFixChart.elemList) // 新增
                        this.initTrend('fixed', res.newCloseFixChart && res.newCloseFixChart.elemList) // 关闭/修复
                    })
                },
                deep: true
            }
        },
        created () {
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
            initTrend (trendType, list = []) {
                const trendTypeMap = {
                    exist: {
                        table: '',
                        title: this.$t('待修复问题趋势'),
                        name: this.$t('遗留问题数'),
                        data: 'unFixCount',
                        chart: 'existTrendChart'
                    },
                    new: {
                        table: 'trendNewTableData',
                        title: this.$t('每日新增问题'),
                        name: this.$t('新增问题数'),
                        data: 'newCount',
                        chart: 'newTrendChart'
                    },
                    fixed: {
                        table: 'trendFixedTableData',
                        title: this.$t('每日关闭/修复问题'),
                        name: this.$t('关闭问题总数'),
                        name2: this.$t('修复问题数'),
                        data: 'closedCount',
                        data2: 'repairedCount',
                        chart: 'fixedTrendChart'
                    }
                }
                const elemList = [...list]
                elemList.reverse()
                if (trendTypeMap[trendType]['table']) {
                    this[trendTypeMap[trendType]['table']] = elemList
                }
                const xAxisData = elemList.map(item => item.tips)
                const seriesData = elemList.map(item => item[trendTypeMap[trendType]['data']])
                const seriesData2 = elemList.map(item => item[trendTypeMap[trendType]['data2']])
                let legend = []
                let series = []
                if (trendTypeMap[trendType]['name2']) {
                    legend = [{ name: trendTypeMap[trendType]['name'] }, { name: trendTypeMap[trendType]['name2'] }]
                    series = [{ name: trendTypeMap[trendType]['name'], type: 'line', data: seriesData },
                              { name: trendTypeMap[trendType]['name2'], type: 'line', data: seriesData2 }]
                } else {
                    legend = [{ name: trendTypeMap[trendType]['name'] }]
                    series = [{ name: trendTypeMap[trendType]['name'], data: seriesData }]
                }
                // console.log('initTrend -> legend', legend)
                // console.log('initTrend -> series', series)
                // console.log('initTrend -> xAxisData', xAxisData)

                const option = {
                    title: {
                        text: trendTypeMap[trendType]['title']
                    },
                    legend: {
                        data: legend
                    },
                    xAxis: {
                        data: xAxisData
                    },
                    yAxis: {
                        splitNumber: 4,
                        minInterval: 1
                    },
                    grid: {
                        left: '50'
                    },
                    series: series
                }
                this.handleChartOption(trendTypeMap[trendType]['chart'], option, 'chartLineOption')
            },
            initAuthor (authorType, list = {}) {
                const authorTypeMap = {
                    exist: {
                        data: 'existAuthorsTableData',
                        title: this.$t('待修复问题处理人分布'),
                        chart: 'existAuthorsChart'
                    }
                }
                this.handleInitAuthor(authorTypeMap, authorType, list)
            },
            handleHref (query) {
                this.resolveHref('defect-coverity-list', query)
            },
            downloadExcel () {
                const excelData1 = this.getExcelData([this.$t('问题处理人'), this.$t('总数'), this.$t('严重'), this.$t('一般'), this.$t('提示')], ['authorName', 'total', 'serious', 'normal', 'prompt'], this.existAuthorsTableData, '待修复问题处理人分布')
                const excelData2 = this.getExcelData([this.$t('日期'), this.$t('新增数'), this.$t('待修复数')], ['tips', 'newCount', 'existCount'], this.trendNewTableData, this.$t('每日新增问题'))
                const excelData3 = this.getExcelData([this.$t('日期'), this.$t('关闭总数'), this.$t('修复数'), this.$t('忽略数'), this.$t('过滤屏蔽数')], ['tips', 'closedCount', 'repairedCount', 'ignoreCount', 'excludedCount'], this.trendFixedTableData, this.$t('每日关闭或修复问题'))
                const excelData = [excelData1, excelData2, excelData3]
                const title = `${this.taskDetail.nameCn}-${this.taskDetail.taskId}-${this.toolId}-数据报表-${new Date().toISOString()}`
                const sheets = ['待修复问题处理人分布', '每日新增问题', '每日关闭或修复问题']
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

    .charts-lint {
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
        .layout-inner .main-content {
            background: #f5f7fa;
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
        .trend-wrapper {
            display: flex;

            .trend-box {
                width: 100%;
                background: #fff;
                border: 1px solid $borderColor;
                margin-bottom: 15px;

                &.small {
                    height: 233px;
                }

                .trend-charts, .trend-table {
                    display: inline-block;
                    vertical-align: top;
                    width: 100%;
                    padding: 15px;
                    &.small {
                        width: calc(50% - 15px);
                    }
                }
              
                .trend-charts-item {
                    width: 100%;
                    height: 200px;
                }
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
            .authors-charts-item {
                width: 100%;
                height: 215px;
            }
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
