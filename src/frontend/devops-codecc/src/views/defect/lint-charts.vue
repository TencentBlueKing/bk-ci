<template>
    <div class="charts-lint">
        <div class="trend-wrapper">
            <div class="trend-box">
                <div class="trend-charts">
                    <div id="newTrendChart" ref="newTrendChart"></div>
                </div>
                <div class="trend-table">
                    <div class="table">
                        <bk-table :empty-text="$t('st.暂无数据')" :data="trendData" :outer-border="false">
                            <bk-table-column :label="$t('charts.日期')" prop="tips" align="center"></bk-table-column>
                            <bk-table-column :label="$t('charts.新告警数')" align="center">
                                <template slot-scope="{ row, column, $index }">
                                    <a v-if="$index === 6" @click="handleHref({ fileType: 1 })" href="javascript:;">{{row.newCount}}</a>
                                    <span v-else>{{row.newCount}}</span>
                                </template>
                            </bk-table-column>
                        </bk-table>
                    </div>
                </div>
            </div>
            <div class="trend-box">
                <div class="trend-charts">
                    <div id="hisTrendChart" ref="hisTrendChart"></div>
                </div>
                <div class="trend-table">
                    <div class="table">
                        <bk-table :empty-text="$t('st.暂无数据')" :data="trendData" :outer-border="false">
                            <bk-table-column :label="$t('charts.日期')" prop="tips" align="center"></bk-table-column>
                            <bk-table-column :label="$t('charts.历史告警数')" align="center">
                                <template slot-scope="{ row, column, $index }">
                                    <a v-if="$index === 6" @click="handleHref({ fileType: 2 })" href="javascript:;">{{row.historyCount}}</a>
                                    <span v-else>{{row.historyCount}}</span>
                                </template>
                            </bk-table-column>
                        </bk-table>
                    </div>
                </div>
            </div>
        </div>
        <div class="authors-wrapper">
            <div class="authors-charts">
                <div id="newAuthorsChart" ref="newAuthorsChart"></div>
            </div>
            <div class="authors-table">
                <div class="table">
                    <bk-table :empty-text="$t('st.暂无数据')" :data="newAuthorsData" :outer-border="false">
                        <bk-table-column :label="$t('defect.告警处理人')" prop="authorName" align="center"></bk-table-column>
                        <bk-table-column :label="$t('charts.总数')" prop="total" align="center">
                            <template slot-scope="{ row }">
                                <a v-if="row.authorName !== 'Total'" href="javascript:;" @click="handleHref({ author: row.authorName, fileType: 1 })">{{row.total}}</a>
                                <a v-else href="javascript:;" @click="handleHref({ fileType: 1 })">{{row.total}}</a>
                            </template>
                        </bk-table-column>
                        <bk-table-column :label="$t('defect.严重')" prop="serious" align="center">
                            <template slot-scope="{ row }">
                                <a v-if="row.authorName !== 'Total'" href="javascript:;" @click="handleHref({ author: row.authorName, severity: 1, fileType: 1 })">{{row.serious}}</a>
                                <a v-else href="javascript:;" @click="handleHref({ severity: 1, fileType: 1 })">{{row.serious}}</a>
                            </template>
                        </bk-table-column>
                        <bk-table-column :label="$t('defect.一般')" prop="normal" align="center">
                            <template slot-scope="{ row }">
                                <a v-if="row.authorName !== 'Total'" href="javascript:;" @click="handleHref({ author: row.authorName, severity: 2, fileType: 1 })">{{row.normal}}</a>
                                <a v-else href="javascript:;" @click="handleHref({ severity: 2, fileType: 1 })">{{row.normal}}</a>
                            </template>
                        </bk-table-column>
                        <bk-table-column :label="$t('defect.提示')" prop="prompt" align="center">
                            <template slot-scope="{ row }">
                                <a v-if="row.authorName !== 'Total'" href="javascript:;" @click="handleHref({ author: row.authorName, severity: 4, fileType: 1 })">{{row.prompt}}</a>
                                <a v-else href="javascript:;" @click="handleHref({ severity: 4, fileType: 1 })">{{row.prompt}}</a>
                            </template>
                        </bk-table-column>
                    </bk-table>
                </div>
            </div>
        </div>
        <div class="authors-wrapper">
            <div class="authors-charts">
                <div id="hisAuthorsChart" ref="hisAuthorsChart"></div>
            </div>
            <div class="authors-table">
                <div class="table">
                    <bk-table :empty-text="$t('st.暂无数据')" :data="hisAuthorsData" :outer-border="false">
                        <bk-table-column :label="$t('defect.告警处理人')" prop="authorName" align="center"></bk-table-column>
                        <bk-table-column :label="$t('charts.总数')" prop="total" align="center">
                            <template slot-scope="{ row }">
                                <a v-if="row.authorName !== 'Total'" href="javascript:;" @click="handleHref({ author: row.authorName, fileType: 2 })">{{row.total}}</a>
                                <a v-else href="javascript:;" @click="handleHref({ fileType: 2 })">{{row.total}}</a>
                            </template>
                        </bk-table-column>
                        <bk-table-column :label="$t('defect.严重')" prop="serious" align="center">
                            <template slot-scope="{ row }">
                                <a v-if="row.authorName !== 'Total'" href="javascript:;" @click="handleHref({ author: row.authorName, severity: 1, fileType: 2 })">{{row.serious}}</a>
                                <a v-else href="javascript:;" @click="handleHref({ severity: 1, fileType: 2 })">{{row.serious}}</a>
                            </template>
                        </bk-table-column>
                        <bk-table-column :label="$t('defect.一般')" prop="normal" align="center">
                            <template slot-scope="{ row }">
                                <a v-if="row.authorName !== 'Total'" href="javascript:;" @click="handleHref({ author: row.authorName, severity: 2, fileType: 2 })">{{row.normal}}</a>
                                <a v-else href="javascript:;" @click="handleHref({ severity: 2, fileType: 2 })">{{row.normal}}</a>
                            </template>
                        </bk-table-column>
                        <bk-table-column :label="$t('defect.提示')" prop="prompt" align="center">
                            <template slot-scope="{ row }">
                                <a v-if="row.authorName !== 'Total'" href="javascript:;" @click="handleHref({ author: row.authorName, severity: 4, fileType: 2 })">{{row.prompt}}</a>
                                <a v-else href="javascript:;" @click="handleHref({ severity: 4, fileType: 2 })">{{row.prompt}}</a>
                            </template>
                        </bk-table-column>
                    </bk-table>
                </div>
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
                chartLegacys: {
                    legacyList: []
                },
                chartAuthors: {
                    newAuthorList: [],
                    historyAuthorList: []
                },
                trendData: [],
                newAuthorsData: [],
                hisAuthorsData: []
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
                const toolId = this.$route.params.toolId
                const res = await this.$store.dispatch('defect/report', { toolId }, { showLoading: true })
                this.chartLegacys = res.chartLegacys || {}
                this.chartAuthors = res.chartAuthors || {}
                this.initTrend()
                this.initNewAuthors()
                this.initHisAuthors()
            },
            initTrend () {
                const elemList = this.chartLegacys.legacyList || []
                const xAxisData = []
                const newCount = []
                const historyCount = []
                elemList.forEach((element, index) => {
                    xAxisData.unshift(element['tips'])
                    newCount.unshift(element['newCount'])
                    historyCount.unshift(element['historyCount'])
                })
                this.trendData = elemList.reverse()

                const option1 = {
                    title: {
                        text: this.$t('charts.新告警遗留趋势')
                    },
                    legend: {
                        data: [{
                            name: this.$t('charts.新告警数')
                        }]
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
                    series: [{
                        name: this.$t('charts.新告警数'),
                        data: newCount
                    }]
                }
                
                const newTrendChart = echarts.init(this.$refs.newTrendChart)
                newTrendChart.setOption(this.chartLineOption)
                newTrendChart.setOption(option1)
                window.addEventListener('resize', () => {
                    newTrendChart.resize()
                })

                const option2 = {
                    title: {
                        text: this.$t('charts.历史告警遗留趋势')
                    },
                    legend: {
                        data: [{
                            name: this.$t('charts.历史告警数')
                        }]
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
                    series: [{
                        name: this.$t('charts.历史告警数'),
                        data: historyCount,
                        itemStyle: {
                            normal: {
                                color: '#2dcb56'
                            }
                        },
                        lineStyle: {
                            normal: {
                                color: '#2dcb56'
                            }
                        }
                    }]
                }

                const hisTrendChart = echarts.init(this.$refs.hisTrendChart)
                hisTrendChart.setOption(this.chartLineOption)
                hisTrendChart.setOption(option2)
                window.addEventListener('resize', () => {
                    hisTrendChart.resize()
                })
            },
            initNewAuthors () {
                if (!this.chartAuthors.newAuthorList) return
                const authorList = this.chartAuthors.newAuthorList.authorList
                const authorName = []
                const serious = []
                const normal = []
                const prompt = []

                if (authorList.length) {
                    authorList.forEach((author, index) => {
                        authorName.push(author['authorName'])
                        serious.push(author['serious'])
                        normal.push(author['normal'])
                        prompt.push(author['prompt'])
                    })
                } else {
                    serious.push(0) // 没有数据时，加一条零数据显示图表
                }

                authorList.push(this.chartAuthors.newAuthorList.totalAuthor)
                this.newAuthorsData = authorList

                const option = {
                    title: {
                        text: this.$t('charts.新告警处理人分布')
                    },
                    xAxis: {
                        data: authorName
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
                            data: prompt
                        },
                        {
                            data: normal
                        },
                        {
                            data: serious
                        }
                    ]
                }

                const newAuthorsChart = echarts.init(this.$refs.newAuthorsChart)
                newAuthorsChart.setOption(this.chartBarOption)
                newAuthorsChart.setOption(option)
                window.addEventListener('resize', () => {
                    newAuthorsChart.resize()
                })
            },
            initHisAuthors () {
                if (!this.chartAuthors.historyAuthorList) return
                const authorList = this.chartAuthors.historyAuthorList.authorList
                const authorName = []
                const serious = []
                const normal = []
                const prompt = []

                if (authorList.length) {
                    authorList.forEach((author, index) => {
                        authorName.push(author['authorName'])
                        serious.push(author['serious'])
                        normal.push(author['normal'])
                        prompt.push(author['prompt'])
                    })
                } else {
                    serious.push(0) // 没有数据时，加一条零数据显示图表
                }
                
                authorList.push(this.chartAuthors.historyAuthorList.totalAuthor)
                this.hisAuthorsData = authorList

                const option = {
                    title: {
                        text: this.$t('charts.历史告警处理人分布')
                    },
                    xAxis: {
                        data: authorName
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
                            data: prompt
                        },
                        {
                            data: normal
                        },
                        {
                            data: serious
                        }
                    ]
                }

                const hisAuthorsChart = echarts.init(this.$refs.hisAuthorsChart)
                hisAuthorsChart.setOption(this.chartBarOption)
                hisAuthorsChart.setOption(option)
                window.addEventListener('resize', () => {
                    hisAuthorsChart.resize()
                })
            },
            handleHref (query) {
                const resolved = this.$router.resolve({
                    name: 'defect-lint-list',
                    params: this.$route.params,
                    query
                })
                const href = `${window.DEVOPS_SITE_URL}/console${resolved.href}`
                window.open(href, '_blank')
            }
        }
    }
</script>

<style lang="postcss">
</style>

<style lang="postcss" scoped>
    @import '../../css/variable.css';

    .charts-lint {
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
                height: 467px;
                width: calc(50% - 7.5px);
                background: #fff;
                border: 1px solid $borderColor;
                margin-bottom: 15px;

                .trend-charts, .trend-table {
                    height: 200px;
                    width: 100%;
                    padding: 15px;
                }

                &:first-of-type {
                    margin-right: 15px;
                }
                #newTrendChart, #hisTrendChart {
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
            #newAuthorsChart, #hisAuthorsChart {
                width: 100%;
                height: 215px;
            }
        }
    }
</style>
