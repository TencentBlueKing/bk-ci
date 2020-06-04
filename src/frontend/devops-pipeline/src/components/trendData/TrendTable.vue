<template>
    <div class="trend-data-container" v-if="trendData.length">
        <div class="search-input">
            <bk-input
                :placeholder="$t('history.fiterPackageTips')"
                :clearable="true"
                right-icon="icon-search"
                @enter="filterData"
                @change="clearFilter"
                v-model="searchModel">
            </bk-input>
        </div>
        <div class="trend-chart-wrapper">
            <chart class="chart-wrapper rankchart-wrapper"
                :options="option"
                auto-resize>
            </chart>
        </div>
        <div class="trend-table-container">
            <bk-table :data="showList" size="small">
                <bk-table-column label="buildNum" prop="buildNum" width="100"></bk-table-column>
                <bk-table-column :label="$t('fileName')" prop="name"></bk-table-column>
                <bk-table-column :label="$t('versionNum')" prop="appVersion" width="150"></bk-table-column>
                <bk-table-column
                    width="150"
                    :label="$t('size')"
                    prop="size"
                    :formatter="formatSize"
                ></bk-table-column>
                <bk-table-column
                    width="200"
                    :label="$t('lastUpdateTime')"
                    prop="modifiedTime"
                    :formatter="formatTime"
                ></bk-table-column>
            </bk-table>
        </div>
    </div>
    <div v-else class="trends-empty">
        <div class="no-data-right">
            <img src="../../images/box.png">
            <p>
                <span>{{ $t('history.noPackages')}}</span>
            </p>
        </div>
    </div>
</template>

<script>
    import { convertTime, convertFileSize } from '@/utils/util'
    import ECharts from 'vue-echarts/components/ECharts.vue'
    import 'echarts/lib/chart/line'
    import 'echarts/lib/component/tooltip'
    import 'echarts/lib/component/title'
    import 'echarts/lib/component/legend'
    export default {
        name: 'trend-table',
        components: {
            chart: ECharts
        },
        props: {
            trendData: {
                type: Array,
                default: []
            }
        },
        data () {
            return {
                searchModel: '',
                searchValue: '',
                option: {
                    title: {
                        text: this.$t('history.chartTitle'),
                        padding: 20,
                        left: 'center',
                        textStyle: {
                            fontSize: 14,
                            fontWeight: 600,
                            color: '#737987'
                        }
                    },
                    totalCount: [],
                    legend: [],
                    xAxis: {
                        type: 'category',
                        boundaryGap: true,
                        data: [],
                        axisTick: {
                            show: false
                        },
                        axisLine: {
                            lineStyle: {
                                color: '#DDE4EB'
                            }
                        },
                        axisLabel: {
                            color: '#737987'
                        }
                    },
                    yAxis: {
                        type: 'value',
                        name: `${this.$t('history.unit')}：MB`,
                        axisLabel: {
                            formatter: '{value}',
                            color: '#737987'
                        },
                        minInterval: 1,
                        boundaryGap: [0.2, 0.2],
                        nameTextStyle: {
                            color: '#737987'
                        },
                        axisLine: {
                            show: false
                        },
                        axisTick: {
                            show: false
                        },
                        splitLine: {
                            lineStyle: {
                                color: '#DDE4EB'
                            }
                        }
                    },
                    axisPointer: {
                        lineStyle: {
                            color: '#DDE4EB',
                            type: 'dashed'
                        }
                    },
                    tooltip: {
                        trigger: 'axis',
                        padding: 0,
                        backgroundColor: '#fff',
                        formatter: (params) => {
                            const name = params[0].name
                            const data = `${params[0].data}MB`
                            const res = '<div style="width:auto;border:1px solid #dde4eb; font-size:12px;color:#737987;"><p style="padding:0 14px; line-height: 32px; border-bottom:1px solid #dde4eb;font-weight: bold;">'
                                + this.$t('buildNum') + '：' + name
                                + '</p><p style="padding: 8px 14px; line-height: 20px; font-weight: bold;">' + this.$t('history.packageSize') + ':' + data + '</p></div>'
                            return res
                        }
                    },
                    series: [
                        {
                            name: this.$t('history.packageSize'),
                            type: 'line',
                            data: [],
                            lineStyle: {
                                color: '#0082ff',
                                width: 2
                            },
                            itemStyle: {
                                normal: {
                                    color: '#0082ff',
                                    shadowBlur: 8,
                                    shadowColor: '#4ca7ff',
                                    borderColor: '#0082ff', // 00c1de
                                    borderWidth: 2,
                                    backgroundColor: 'transparent'
                                }
                            },
                            areaStyle: {
                                color: {
                                    type: 'linear',
                                    x: 0,
                                    y: 0,
                                    x2: 0,
                                    y2: 1,
                                    colorStops: [{
                                        offset: 0, color: 'rgb(203,226,255)' // 0% 处的颜色
                                    }, {
                                        offset: 1, color: '#fff' // 100% 处的颜色
                                    }],
                                    globalCoord: false // 缺省为 false
                                }
                            }
                        }
                    ]
                }
            }
        },
        computed: {
            showList () {
                if (this.searchValue) {
                    return this.trendData.filter(item => item.name.indexOf(this.searchValue) !== -1)
                } else {
                    return this.trendData
                }
            }
        },
        watch: {
            trendData () {
                this.searchModel = ''
                this.searchValue = ''
            },
            showList (val) {
                this.initData()
            }
        },
        created () {
            this.initData()
        },
        methods: {
            initData () {
                this.option.xAxis.data = this.showList.map(item => item.buildNum)
                this.option.series[0].data = this.showList.map(item => item.size ? (item.size / 1024 / 1024).toFixed(2) : 0)
            },
            formatTime (row) {
                return row.modifiedTime ? convertTime(row.modifiedTime * 1000) : '--'
            },
            formatSize (row) {
                return row.size ? convertFileSize(row.size, 'B') : ''
            },
            filterData (data) {
                this.searchValue = data
            },
            clearFilter () {
                if (!this.searchModel) this.searchValue = ''
            }
        }
    }
</script>

<style lang="scss">

    .trend-data-container {
        height: 100%;
        .prompt-tips {
            margin-bottom: 12px;
        }
        .search-input {
            width: 300px;
            float: right;
        }
    }
    .trend-chart-wrapper {
        width: 100%;
        overflow: hidden;
        position: relative;
        height: 352px;
        .echarts {
            width: 100%;
            height: 350px;
        }
    }
    .trend-table-container {
        height: calc(100% - 380px);
        margin: 10px 0;
        overflow: auto;
    }
</style>
