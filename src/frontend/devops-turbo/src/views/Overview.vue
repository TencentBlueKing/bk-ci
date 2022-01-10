<template>
    <div class="turbo-overview-wrapper" v-bkloading="{ isLoading: loading.isloading }">
        <header-process :process-head="title"></header-process>
        <image-empty v-if="showContent && isEmptyTask"
            :title="emptyInfo.title"
            :desc="emptyInfo.desc"
            :btns="emptyInfo.btns">
        </image-empty>
        <div class="overview-container sub-view-port" v-if="showContent && !isEmptyTask">
            <h5 class="overview-title">两周内加速数据</h5>
            <div class="total-data-wrapper">
                <div class="total-item">
                    <p class="item-number">{{ totalTimeSaving }}</p>
                    <h6 class="item-title">累计节省编译时间</h6>
                </div>
                <div class="item-border"></div>
                <div class="total-item">
                    <p class="item-number">{{ acceTaskNum }}</p>
                    <h6 class="item-title">加速任务数量</h6>
                </div>
                <div class="item-border"></div>
                <div class="total-item">
                    <p class="item-number">{{ acceRecordNum }}</p>
                    <h6 class="item-title">编译加速次数</h6>
                </div>
            </div>

            <div class="total-chart-wrapper">
                <chart class="chart-wrapper rankchart-wrapper"
                    :options="option"
                    auto-resize>
                </chart>
            </div>

            <h5 class="overview-title">可用加速方案</h5>
            <div class="total-available-wrapper">
                <div class="avail-item">
                    <p class="item-number">Distcc</p>
                    <h6 class="item-title">平均可缩短编译时间</h6>
                    <div class="item-percent primary">
                        <span>60%</span>
                    </div>
                </div>
                <div class="avail-item">
                    <p class="item-number">Ccache</p>
                    <h6 class="item-title">平均可缩短编译时间</h6>
                    <div class="item-percent warning">
                        <span>30%</span>
                    </div>
                </div>
                <div class="avail-item">
                    <p class="item-number">Distcc+Ccache</p>
                    <h6 class="item-title">平均可缩短编译时间</h6>
                    <div class="item-percent success">
                        <span>80%</span>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import ECharts from 'vue-echarts/components/ECharts.vue'
    import 'echarts/lib/chart/line'
    import 'echarts/lib/chart/bar'
    import 'echarts/lib/chart/pie'
    import 'echarts/lib/component/tooltip'
    import 'echarts/lib/component/title'
    import 'echarts/lib/component/legend'
    import headerProcess from '@/components/turbo/headerProcess'
    import imageEmpty from '@/components/common/imageEmpty'

    export default {
        components: {
            headerProcess,
            chart: ECharts,
            'image-empty': imageEmpty
        },
        data () {
            return {
                isEmptyTask: false,
                showContent: false,
                loading: {
                    isloading: true,
                    title: ''
                },
                title: {
                    title: '总览',
                    list: [],
                    hasLink: true,
                    linkTitle: '了解更多编译加速',
                    linkHref: `${DOCS_URL_PREFIX}/x/nobm`
                },
                emptyInfo: {
                    title: '创建第一个编译加速任务',
                    desc: '通过使用不同的加速方案，可缩短30%~80%的编译时间',
                    btns: [
                        {
                            type: 'primary',
                            size: 'normal',
                            handler: () => this.toRegTask(),
                            text: '创建任务'
                        }
                    ]
                },
                totalTimeSaving: 0,
                acceTaskNum: 0,
                acceRecordNum: 0,
                option: {
                    title: {
                        text: '估算节省编译时间（分钟）',
                        padding: 20,
                        left: 'center',
                        textStyle: {
                            fontSize: 14,
                            fontWeight: 600,
                            color: '#737987'
                        }
                    },
                    totalCount: [],
                    grid: {
                        x: 50,
                        x2: 50,
                        y: 70,
                        y2: 50
                    },
                    legend: [],
                    xAxis: {
                        type: 'category',
                        boundaryGap: false,
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
                        axisLabel: {
                            color: '#737987',
                            formatter: '{value}' // '{value}h'
                        },
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
                            const dateArr = params[1].value.split('-')
                            const dataIndex = params[0].dataIndex
                            const dataSaveTime = Math.round(params[0].value * 60)
                            const dataSaveSec = dataSaveTime % 60
                            const dataSaveMin = parseInt(dataSaveTime / 60) % 60
                            const dataSaveHour = parseInt(dataSaveTime / 3600) % 60
                            // let dataSvaeTimeHour = Math.floor(params[0].value);
                            // let dataSvaeTimeMin = Math.round((dataSaveTime - dataSvaeTimeHour) * 60);
                            const dataSaveTimeTxt = dataSaveHour + 'h ' + dataSaveMin + 'min ' + dataSaveSec + 's'
                            const res = '<div style="width:auto;border:1px solid #dde4eb; font-size:12px;color:#737987;"><p style="padding:0 14px; line-height: 32px; border-bottom:1px solid #dde4eb;font-weight: bold;">'
                                + dateArr[0] + '年' + dateArr[1] + '月' + dateArr[2] + '日'
                                + '</p><p style="padding: 8px 14px; line-height: 20px">编译加速次数 ' + this.totalCount[dataIndex] + '次<br/>节省编译时间 ' + dataSaveTimeTxt + '</p></div>'
                            return res
                        }
                    },
                    series: [
                        {
                            name: '节省编译时间',
                            type: 'line',
                            data: [],
                            lineStyle: {
                                color: '#0082ff',
                                width: 2
                            },
                            markPoint: {
                                data: [
                                    { type: 'max', name: '最大值' },
                                    { type: 'min', name: '最小值' }
                                ]
                            },
                            markLine: {
                                data: [
                                    { type: 'average', name: '平均值' }
                                ]
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
                        },
                        {
                            name: '时间',
                            type: 'line',
                            data: [],
                            lineStyle: {
                                color: 'rgba(0, 0, 0, 0)'
                            },
                            itemStyle: {
                                opacity: 0
                            }
                        }
                    ]
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            projectId: async function () {
                await this.requestOverview()
            }
        },
        created () {
            this.requestOverview()
            console.log(this)
        },
        methods: {
            async requestOverview () {
                this.loading.isloading = true
                try {
                    const res = await this.$store.dispatch('turbo/requestOverview', this.projectId)
                    if (res) {
                        this.totalTimeSaving = res.totalTimeSaving
                        this.acceTaskNum = res.acceTaskNum || 0
                        this.acceRecordNum = res.acceRecordNum || 0
                        this.option.xAxis.data = []
                        const series = this.option.series
                        const dateList = series[1].data = []
                        this.totalCount = []
                        const saveTimeList = series[0].data = []
                        res.compileSavingList.forEach(item => {
                            this.option.xAxis.data.push(item.compileTime.slice(5).replace(/-/, '/'))
                            dateList.push(item.compileTime)
                            this.totalCount.push(item.acceNum)
                            saveTimeList.push(this.numberTrans(item.timeSaving / 60))
                        })
                        this.isEmptyTask = false
                    } else {
                        this.isEmptyTask = true
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    setTimeout(() => {
                        this.loading.isloading = false
                        this.showContent = true
                    }, 1)
                }
            },
            numberTrans (num) {
                if (num <= 0) {
                    return 0
                }
                const result = (num.toString()).indexOf('.')
                if (result !== -1) {
                    return num.toFixed(3)
                } else {
                    return num
                }
            },
            toRegTask () {
                this.$router.push({
                    name: 'registration',
                    params: {
                        projectId: this.projectId
                    }
                })
            },
            NumFormat (num) {
                const dataSvaeTimeHour = Math.floor(num)
                const dataSvaeTimeMin = Math.round((num - dataSvaeTimeHour) * 60)
                const dataSaveTimeTxt = dataSvaeTimeHour + 'h ' + dataSvaeTimeMin + 'min'
                return dataSaveTimeTxt
            }
        }
    }
</script>

<style lang="scss">
    @import '../assets/scss/conf.scss';

    .turbo-overview-wrapper {
        height: 100%;
        overflow: hidden;
        .overview-container {
            .overview-title {
                margin: 0 0 8px 0;
                line-height: 19px;
                font-size: 14px;
                font-weight: 400;
                color: $fontColorLabel;
            }
        }
        .echarts {
            width: 100%;
            height: 352px;
        }
        .total-data-wrapper {
            display: flex;
            margin-bottom: 10px;
            width: 100%;
            height: 80px;
            border: 1px solid $borderWeightColor;
            border-radius: 2px;
            background: #fff;
            .item-border {
                width: 1px;
                height: 60px;
                margin-top: 10px;
                background: $borderWeightColor;
            }
            .total-item {
                flex: 1;
                padding: 13px 20px 19px 20px;
                .item-number {
                    font-size: 24px;
                    line-height: 32px;
                    color: $fontColorLabel;
                }
                .item-title {
                    font-weight: 400;
                }
            }
        }
        .total-chart-wrapper {
            margin-bottom: 17px;
            width: 100%;
            height: 352px;
            border: 1px solid $borderWeightColor;
            border-radius: 2px;
            background: #fff;
        }
        .total-available-wrapper {
            display: flex;
            .avail-item {
                flex: 1;
                position: relative;
                padding: 13px 90px 19px 20px;
                border: 1px solid $borderWeightColor;
                border-radius: 2px;
                background: #fff;
                margin-right: 20px;
                &:last-child {
                    margin-right: 0;
                }
            }
            .item-number {
                font-size: 24px;
                line-height: 32px;
                color: $fontColorLabel;
            }
            .item-title {
                font-weight: 400;
            }
            .item-percent {
                position: absolute;
                top: 21px;
                right: 20px;
                line-height: 37px;
                font-size: 28px;
            }
        }
    }
</style>
