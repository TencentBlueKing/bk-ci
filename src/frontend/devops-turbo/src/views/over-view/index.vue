<template>
    <article class="overview-home">
        <template v-if="hasPermission">
            <ul class="overview-cards">
                <li class="card g-turbo-box" v-for="card in taskCards" :key="card.label">
                    <logo :name="card.icon" size="42" class="card-logo"></logo>
                    <h5>
                        <p class="g-turbo-black-font">
                            <bk-animate-number :value="card.num" :digits="card.digit"></bk-animate-number>
                        </p>
                        <span class="g-turbo-gray-font">{{ card.label }}</span>
                    </h5>
                </li>
            </ul>

            <section class="g-turbo-chart-box chart">
                <header class="chart-head">
                    <span class="g-turbo-black-font"> {{ $t('turbo.耗时分布') }} </span>
                    <bk-tab :active.sync="takeTimeDateType" type="unborder-card">
                        <bk-tab-panel v-for="(panel, index) in timeGap" v-bind="panel" :key="index"></bk-tab-panel>
                    </bk-tab>
                </header>
                <div class="canvas-wrapper">
                    <canvas class="take-time"></canvas>
                </div>
            </section>

            <section class="g-turbo-chart-box chart">
                <header class="chart-head build-trend">
                    <span class="g-turbo-black-font"> {{ $t('turbo.编译次数趋势') }} </span>
                    <bk-tab :active.sync="buildNumDateType" type="unborder-card">
                        <bk-tab-panel v-for="(panel, index) in timeGap" v-bind="panel" :key="index"></bk-tab-panel>
                    </bk-tab>
                </header>
                <div class="canvas-wrapper">
                    <canvas class="build-num"></canvas>
                </div>
            </section>
        </template>
        <permission-exception v-else :message="errMessage" />
    </article>
</template>

<script>
    import { getOverViewStatData, getCompileNumberTrend, getTimeConsumingTrend } from '@/api'
    import BKChart from '@blueking/bkcharts'
    import logo from '../../components/logo'
    import permissionException from '../../components/exception/permission.vue'

    export default {
        components: {
            logo,
            permissionException
        },

        data () {
            return {
                taskCards: {
                    instanceNum: {
                        label: this.$t('turbo.加速方案数'),
                        num: 0,
                        icon: 'acceleration-plan',
                        digit: 0
                    },
                    executeCount: {
                        label: this.$t('turbo.加速次数'),
                        num: 0,
                        icon: 'accelerations-num',
                        digit: 0
                    },
                    executeTimeHour: {
                        label: this.$t('turbo.总耗时(h)'),
                        num: 0,
                        icon: 'total-time',
                        digit: 2
                    },
                    savingRate: {
                        label: this.$t('turbo.节省率(%)'),
                        num: 0,
                        icon: 'save-time',
                        digit: 2
                    }
                },
                timeGap: [
                    { name: 'week', label: this.$t('turbo.近一周'), count: 10 },
                    { name: 'month', label: this.$t('turbo.近一月'), count: 20 },
                    { name: 'year', label: this.$t('turbo.近一年'), count: 30 }
                ],
                takeTimeDateType: 'week',
                buildNumDateType: 'week',
                takeTimeChart: {},
                buildNumChart: {},
                hasPermission: true,
                errMessage: ''
            }
        },

        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },

        watch: {
            takeTimeDateType () {
                this.takeTimeChart.destroy()
                this.drawTakeTimeChart()
            },

            buildNumDateType () {
                this.buildNumChart.destroy()
                this.drawBuildNum()
            },

            projectId () {
                this.takeTimeChart.destroy()
                this.buildNumChart.destroy()
                this.getSummaryCount()
                this.drawTakeTimeChart()
                this.drawBuildNum()
            }
        },

        mounted () {
            this.getSummaryCount()
            this.drawTakeTimeChart()
            this.drawBuildNum()
        },

        methods: {
            handleError (err) {
                if (err.code === 2300017) {
                    this.hasPermission = false
                    this.errMessage = err.message
                } else {
                    this.$bkMessage({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
            },

            getSummaryCount () {
                getOverViewStatData(this.projectId).then((res) => {
                    this.taskCards.instanceNum.num = res.instanceNum || 0
                    this.taskCards.executeCount.num = res.executeCount || 0
                    this.taskCards.executeTimeHour.num = res.executeTimeHour || 0
                    this.taskCards.savingRate.num = res.savingRate || 0
                }).catch(this.handleError)
            },

            drawTakeTimeChart () {
                getTimeConsumingTrend(this.takeTimeDateType, this.projectId).then((res) => {
                    const context = document.querySelector('.take-time')
                    this.takeTimeChart = new BKChart(context, {
                        type: 'line',
                        data: {
                            labels: res.map(x => x.date),
                            datasets: [
                                {
                                    label: this.$t('turbo.未加速耗时'),
                                    fill: true,
                                    backgroundColor: 'rgba(43, 124, 255,0.3)',
                                    borderColor: 'rgba(43, 124, 255,1)',
                                    lineTension: 0,
                                    borderWidth: 2,
                                    pointRadius: 0,
                                    pointHitRadius: 3,
                                    pointHoverRadius: 3,
                                    data: res.map(x => x.estimateTime)
                                },
                                {
                                    label: this.$t('turbo.实际耗时'),
                                    fill: true,
                                    backgroundColor: 'rgba(0, 204, 158, 0.3)',
                                    borderColor: 'rgba(0, 204, 158, 1)',
                                    lineTension: 0,
                                    borderWidth: 2,
                                    pointRadius: 0,
                                    pointHitRadius: 3,
                                    pointHoverRadius: 3,
                                    data: res.map(x => x.executeTime)
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
                                    enableItemActive: true,
                                    singleInRange: true
                                },
                                legend: {
                                    position: 'top',
                                    legendIcon: 'arc',
                                    align: 'start',
                                    labels: {
                                        padding: 10,
                                        usePointStyle: true,
                                        pointStyle: 'dash'
                                    }
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
                                        sampleSize: 20,
                                        autoSkip: true,
                                        maxRotation: 0
                                    }
                                }
                            }
                        }
                    })
                }).catch(this.handleError)
            },

            drawBuildNum () {
                getCompileNumberTrend(this.buildNumDateType, this.projectId).then((res = []) => {
                    const context = document.querySelector('.build-num')
                    this.buildNumChart = new BKChart(context, {
                        type: 'line',
                        data: {
                            labels: res.map(x => x.date),
                            datasets: [
                                {
                                    label: this.$t('turbo.编译次数'),
                                    fill: false,
                                    backgroundColor: 'rgba(43, 124, 255,0.3)',
                                    borderColor: 'rgba(43, 124, 255,1)',
                                    lineTension: 0,
                                    borderWidth: 2,
                                    pointRadius: 1.5,
                                    pointHitRadius: 3,
                                    pointHoverRadius: 3,
                                    data: res.map(x => x.executeCount)
                                }
                            ]
                        },
                        options: {
                            responsive: true,
                            maintainAspectRatio: false,
                            plugins: {
                                tooltip: {
                                    mode: 'x',
                                    intersect: false,
                                    enableItemActive: true,
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
                                        sampleSize: 20,
                                        autoSkip: true,
                                        maxRotation: 0
                                    }
                                }
                            }
                        }
                    })
                }).catch(this.handleError)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .overview-home {
        padding: .2rem;
        margin: 0 auto;
    }
    .overview-cards {
        width: 100%;
        margin-bottom: 16.94px;
        &::after {
            content: '';
            display: table;
            clear: both;
        }
        .card {
            float: left;
            width: calc(25% - .075rem);
            height: 84.7px;
            margin-right: .1rem;
            display: flex;
            align-items: center;
            .card-logo {
                border-radius: 100px;
                background: #e1ecff;
                margin: 0 16px 0 30px;
            }
            .g-turbo-black-font {
                font-size: 24px;
                line-height: 32px;
            }
            .g-turbo-gray-font {
                font-size: 12px;
                line-height: 18px;
                font-weight: normal;
            }
            &:last-child {
                margin: 0;
            }
        }
    }
    .chart {
        margin-bottom: 10.59px;
        height: calc(50% - 56.115px);
        .canvas-wrapper {
            height: calc(100% - 25px);
            width: 100%;
        }
        &:last-child {
            margin-bottom: 0;
        }
    }
    .build-trend {
        margin-bottom: 10px;
    }
    .chart-head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 0 22px;
        ::v-deep .bk-tab-header {
            background-color: #fff;
            height: 25px !important;
            line-height: 25px !important;
            background-image: none !important;
            .bk-tab-label-wrapper .bk-tab-label-list {
                height: 25px !important;
                .bk-tab-label-item {
                    line-height: 25px !important;
                    color: #63656e;
                    min-width: 36px;
                    padding: 0 12px;
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
                    .bk-tab-label {
                        font-size: 12px;
                    }
                }
            }
            .bk-tab-header-setting {
                height: 25px !important;
                line-height: 25px !important;
            }
        }
        ::v-deep .bk-tab-section {
            padding: 0;
        }
    }
</style>
