<template>
    <article class="trend-common-home">
        <header class="common-head">
            <bk-tab :active.sync="chartTab" type="unborder-card">
                <bk-tab-panel v-for="(panel, index) in storeChartTabs" v-bind="panel" :key="index"></bk-tab-panel>
            </bk-tab>

            <bk-select class="common-time" v-model="time" :clearable="false">
                <bk-option v-for="item in timeList"
                    :key="item.value"
                    :id="item.value"
                    :name="item.name"
                ></bk-option>
            </bk-select>
        </header>

        <bk-exception class="exception-wrap-item exception-part" type="empty" v-if="isEmpty"></bk-exception>
        <canvas class="store-chart" v-else></canvas>
    </article>
</template>

<script>
    import BKChart from '@blueking/bkcharts'
    import moment from 'moment'
    import api from '@/api'

    export default {
        props: {
            detail: Object,
            type: String
        },

        data () {
            return {
                storeChartTabs: [
                    { name: 'totalDownloads', label: this.$t('store.安装量'), count: 10 }
                ],
                storeChart: {},
                timeList: [
                    { name: this.$t('store.周'), value: 'weeks' },
                    { name: this.$t('store.月'), value: 'months' },
                    { name: this.$t('store.年'), value: 'years' }
                ],
                time: 'weeks',
                chartTab: 'totalDownloads',
                chartData: {},
                isEmpty: false
            }
        },

        watch: {
            chartTab () {
                this.getChartData().then(this.paintAgain)
            },

            time () {
                this.getChartData().then(this.paintAgain)
            }
        },

        mounted () {
            this.getChartData().then(this.paintAgain)
        },

        methods: {
            getChartData () {
                return new Promise((resolve, reject) => {
                    const chartData = this.chartData[this.time]
                    if (chartData) {
                        resolve(chartData)
                    } else {
                        const code = this.detail.serviceCode
                        const now = moment(moment().format('YYYY-MM-DD')).subtract(1, 'days')
                        const params = {
                            endTime: now.format('YYYY-MM-DD HH:mm:ss'),
                            startTime: now.subtract(1, this.time).format('YYYY-MM-DD HH:mm:ss')
                        }
                        return api.requestStaticChartData(this.type.toUpperCase(), code, params).then((res) => {
                            this.chartData[this.time] = res
                            resolve(res)
                        }).catch((err) => {
                            resolve()
                            this.$bkMessage({ theme: 'error', message: err.message || err })
                        })
                    }
                })
            },

            paintAgain (data) {
                if (!data) return
                if (this.storeChart && this.storeChart.destroy) this.storeChart.destroy()
                let paintData = []
                let method = ''
                switch (this.chartTab) {
                    case 'totalDownloads':
                        paintData = data.dailyStatisticList || []
                        method = this.paintInstall
                        break
                    case 'execTrend':
                        paintData = data.dailyStatisticList || []
                        method = this.paintTrend
                        break
                    case 'failDetail':
                        for (const key in data.totalFailDetail || {}) {
                            paintData.push(data.totalFailDetail[key])
                        }
                        method = this.paintError
                        break
                }
                this.isEmpty = paintData.length <= 0
                if (!this.isEmpty) this.$nextTick(() => method(paintData))
            },

            paintInstall (dailyStatisticList) {
                const context = document.querySelector('.store-chart')
                this.storeChart = new BKChart(context, {
                    type: 'line',
                    data: {
                        labels: dailyStatisticList.map(x => x.statisticsTime),
                        datasets: [
                            {
                                label: this.$t('store.安装量'),
                                backgroundColor: 'rgba(43, 124, 255,0.3)',
                                borderColor: 'rgba(43, 124, 255,1)',
                                lineTension: 0,
                                borderWidth: 2,
                                pointRadius: 0,
                                pointHitRadius: 3,
                                pointHoverRadius: 3,
                                data: dailyStatisticList.map(x => x.dailyDownloads)
                            }
                        ]
                    },
                    options: {
                        maintainAspectRatio: false,
                        responsive: true,
                        plugins: {
                            tooltip: {
                                mode: 'x',
                                intersect: false,
                                singleInRange: true
                            },
                            legend: {
                                display: false
                            },
                            crosshair: {
                                enabled: true,
                                mode: 'x',
                                style: {
                                    x: {
                                        enabled: true,
                                        color: '#cde0ff',
                                        weight: 1,
                                        borderStyle: 'solid'
                                    },
                                    y: {
                                        enabled: false
                                    }
                                }
                            }
                        },
                        layout: {
                            padding: {
                                left: 0,
                                right: 0,
                                top: 20,
                                bottom: 0
                            }
                        },
                        scales: {
                            yAxes: {
                                scaleLabel: {
                                    display: true,
                                    padding: 0
                                },
                                gridLines: {
                                    drawTicks: false,
                                    borderDash: [5, 5]
                                },
                                ticks: {
                                    padding: 10
                                },
                                min: 0
                            },
                            xAxes: {
                                scaleLabel: {
                                    display: true,
                                    padding: 0
                                },
                                gridLines: {
                                    drawTicks: false,
                                    display: false
                                },
                                ticks: {
                                    padding: 10,
                                    sampleSize: 10,
                                    autoSkip: true,
                                    maxRotation: 0
                                }
                            }
                        }
                    }
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    .trend-common-home {
        margin-top: 5px;
        height: calc(100% - 39px);
        padding-bottom: 20px;
    }

    .common-head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        .common-time {
            width: 250px;
        }
    }

    .store-chart {
        height: calc(100% - 32px);
    }

    ::v-deep .bk-tab-header {
        background-color: #fff;
        height: 32px;
        line-height: 32px;
        background-image: none;
        .bk-tab-label-wrapper .bk-tab-label-list {
            height: 32px;
            .bk-tab-label-item {
                line-height: 32px;
                color: #63656e;
                min-width: 36px;
                padding: 0;
                margin-right: 20px;
                &:last-child {
                    margin: 0;
                }
                &::after {
                    height: 2px;
                    left: 0px;
                    width: 100%;
                }
                &.active {
                    color: #3a84ff;
                }
            }
        }
        .bk-tab-header-setting {
            height: 32px;
            line-height: 32px;
        }
    }
    ::v-deep .bk-tab-section {
        padding: 0;
    }
    ::v-deep .bk-exception-text {
        margin-top: -40px;
    }
</style>
