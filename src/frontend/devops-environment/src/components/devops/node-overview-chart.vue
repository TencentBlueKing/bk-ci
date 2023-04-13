<template>
    <div class="node-overview-chart-wrapper">
        <div class="node-overview-chart">
            <div class="part top-left">
                <div class="info">
                    <div class="left">{{ $t('environment.nodeInfo.cpuUsageRate') }}</div>
                    <div class="right">
                        <bk-dropdown-menu :align="'right'" ref="cpuDropdown">
                            <div style="cursor: pointer;" slot="dropdown-trigger">
                                <span>{{cpuToggleRangeStr}}</span>
                                <button class="dropdown-button">
                                    <i class="devops-icon icon-angle-down"></i>
                                </button>
                            </div>
                            <ul class="bk-dropdown-list" slot="dropdown-content">
                                <li>
                                    <a href="javascript:;" @click.stop="toggleRange('cpuDropdown', 'cpuToggleRangeStr', 'cpu_summary', '1')">{{ $t('environment.nodeInfo.oneHour') }}</a>
                                </li>
                                <li>
                                    <a href="javascript:;" @click.stop="toggleRange('cpuDropdown', 'cpuToggleRangeStr', 'cpu_summary', '2')">{{ $t('environment.nodeInfo.oneDay') }}</a>
                                </li>
                                <li>
                                    <a href="javascript:;" @click.stop="toggleRange('cpuDropdown', 'cpuToggleRangeStr', 'cpu_summary', '3')">{{ $t('environment.nodeInfo.oneWeek') }}</a>
                                </li>
                            </ul>
                        </bk-dropdown-menu>
                    </div>
                </div>
                <chart :option="cpuLine" ref="cpuLine1" autoresize :loading="cpuChartLoading" :loading-options="chartLoadingOption" v-show="!isEmptyCpu"></chart>
                <bk-exception
                    v-show="isEmptyCpu"
                    class="exception-wrap-item exception-part" type="empty" scene="part"
                />
            </div>
            <div class="part top-right">
                <div class="info">
                    <div class="left">{{ $t('environment.nodeInfo.ramUsageRate') }}</div>
                    <div class="right">
                        <bk-dropdown-menu :align="'right'" ref="memoryDropdown">
                            <div style="cursor: pointer;" slot="dropdown-trigger">
                                <span>{{memToggleRangeStr}}</span>
                                <button class="dropdown-button">
                                    <i class="devops-icon icon-angle-down"></i>
                                </button>
                            </div>
                            <ul class="bk-dropdown-list" slot="dropdown-content">
                                <li>
                                    <a href="javascript:;" @click.stop="toggleRange('memoryDropdown', 'memToggleRangeStr', 'mem', '1')">{{ $t('environment.nodeInfo.oneHour') }}</a>
                                </li>
                                <li>
                                    <a href="javascript:;" @click.stop="toggleRange('memoryDropdown', 'memToggleRangeStr', 'mem', '2')">{{ $t('environment.nodeInfo.oneDay') }}</a>
                                </li>
                                <li>
                                    <a href="javascript:;" @click.stop="toggleRange('memoryDropdown', 'memToggleRangeStr', 'mem', '3')">{{ $t('environment.nodeInfo.oneWeek') }}</a>
                                </li>
                            </ul>
                        </bk-dropdown-menu>
                    </div>
                </div>
                <chart :option="memoryLine" ref="memoryLine1" :loading="memChartLoading" :loading-options="chartLoadingOption" autoresize v-show="!isEmptyMemory"></chart>
                <bk-exception
                    v-show="isEmptyMemory"
                    class="exception-wrap-item exception-part" type="empty" scene="part"
                />
            </div>
        </div>
        <div class="node-overview-chart">
            <div class="part bottom-left">
                <div class="info">
                    <div class="left">{{ $t('environment.nodeInfo.networkIo') }}</div>
                    <div class="right">
                        <bk-dropdown-menu :align="'right'" ref="networkDropdown">
                            <div style="cursor: pointer;" slot="dropdown-trigger">
                                <span>{{networkToggleRangeStr}}</span>
                                <button class="dropdown-button">
                                    <i class="devops-icon icon-angle-down"></i>
                                </button>
                            </div>
                            <ul class="bk-dropdown-list" slot="dropdown-content">
                                <li>
                                    <a href="javascript:;" @click.stop="toggleRange('networkDropdown', 'networkToggleRangeStr', 'net', '1')">{{ $t('environment.nodeInfo.oneHour') }}</a>
                                </li>
                                <li>
                                    <a href="javascript:;" @click.stop="toggleRange('networkDropdown', 'networkToggleRangeStr', 'net', '2')">{{ $t('environment.nodeInfo.oneDay') }}</a>
                                </li>
                                <li>
                                    <a href="javascript:;" @click.stop="toggleRange('networkDropdown', 'networkToggleRangeStr', 'net', '3')">{{ $t('environment.nodeInfo.oneWeek') }}</a>
                                </li>
                            </ul>
                        </bk-dropdown-menu>
                    </div>
                </div>
                <chart :option="networkLine" ref="networkLine1" :loading="netChartLoading" :loading-options="chartLoadingOption" autoresize v-show="!isEmptyNetwork"></chart>
                <bk-exception
                    v-show="isEmptyNetwork"
                    class="exception-wrap-item exception-part" type="empty" scene="part"
                />
            </div>
            <div class="part">
                <div class="info">
                    <div class="left">{{ $t('environment.nodeInfo.diskIo') }}</div>
                    <div class="right">
                        <bk-dropdown-menu :align="'right'" ref="storageDropdown">
                            <div style="cursor: pointer;" slot="dropdown-trigger">
                                <span>{{storageToggleRangeStr}}</span>
                                <button class="dropdown-button">
                                    <i class="devops-icon icon-angle-down"></i>
                                </button>
                            </div>
                            <ul class="bk-dropdown-list" slot="dropdown-content">
                                <li>
                                    <a href="javascript:;" @click.stop="toggleRange('storageDropdown', 'storageToggleRangeStr', 'io', '1')">{{ $t('environment.nodeInfo.oneHour') }}</a>
                                </li>
                                <li>
                                    <a href="javascript:;" @click.stop="toggleRange('storageDropdown', 'storageToggleRangeStr', 'io', '2')">{{ $t('environment.nodeInfo.oneDay') }}</a>
                                </li>
                                <li>
                                    <a href="javascript:;" @click.stop="toggleRange('storageDropdown', 'storageToggleRangeStr', 'io', '3')">{{ $t('environment.nodeInfo.oneWeek') }}</a>
                                </li>
                            </ul>
                        </bk-dropdown-menu>
                    </div>
                </div>
                <chart :option="storageLine" ref="storageLine1" :loading="ioChartLoading" :loading-options="chartLoadingOption" autoresize v-show="!isEmptyDiskio"></chart>
                <bk-exception
                    v-show="isEmptyDiskio"
                    class="exception-wrap-item exception-part" type="empty" scene="part"
                />
            </div>
        </div>
    </div>
</template>

<script>
    import { use } from 'echarts/core'
    import VChart from 'vue-echarts'
    import { CanvasRenderer } from 'echarts/renderers'
    import { LineChart } from 'echarts/charts'
    import {
        GridComponent,
        TooltipComponent,
        LegendComponent
    } from 'echarts/components'
    import { nodeOverview } from '@/utils/chart-option'
    import { bus } from '@/utils/bus'

    use([
        CanvasRenderer,
        LineChart,
        GridComponent,
        TooltipComponent,
        LegendComponent
    ])

    export default {
        components: {
            chart: VChart
        },
        data () {
            return {
                isEmptyCpu: false,
                isEmptyMemory: false,
                isEmptyNetwork: false,
                isEmptyDiskio: false,
                cpuToggleRangeStr: this.$t('environment.nodeInfo.oneHour'),
                memToggleRangeStr: this.$t('environment.nodeInfo.oneHour'),
                networkToggleRangeStr: this.$t('environment.nodeInfo.oneHour'),
                storageToggleRangeStr: this.$t('environment.nodeInfo.oneHour'),
                cpuLine: nodeOverview.cpu,
                memoryLine: nodeOverview.memory,
                networkLine: nodeOverview.network,
                storageLine: nodeOverview.storage,
                cpuChartLoading: false,
                memChartLoading: false,
                netChartLoading: false,
                ioChartLoading: false
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            nodeHashId () {
                return this.$route.params.nodeHashId
            },
            chartLoadingOption () {
                return {
                    text: this.$t('environment.loading'),
                    color: '#30d878',
                    maskColor: 'rgba(255, 255, 255, 0.8)'
                }
            }
        },
        created () {
            bus.$off('refreshCharts')
            bus.$on('refreshCharts', () => {
                this.fetchData('cpu_summary', this.cpuToggleRangeStr === this.$t('environment.nodeInfo.oneHour') ? '1' : this.cpuToggleRangeStr === this.$t('environment.nodeInfo.oneDay') ? '2' : '3')
                this.fetchData('mem', this.memToggleRangeStr === this.$t('environment.nodeInfo.oneHour') ? '1' : this.memToggleRangeStr === this.$t('environment.nodeInfo.oneDay') ? '2' : '3')
                this.fetchData('net', this.networkToggleRangeStr === this.$t('environment.nodeInfo.oneHour') ? '1' : this.networkToggleRangeStr === this.$t('environment.nodeInfo.oneDay') ? '2' : '3')
                this.fetchData('io', this.storageToggleRangeStr === this.$t('environment.nodeInfo.oneHour') ? '1' : this.storageToggleRangeStr === this.$t('environment.nodeInfo.oneDay') ? '2' : '3')
            })
        },
        mounted () {
            this.fetchData('cpu_summary', '1')
            this.fetchData('mem', '1')
            this.fetchData('net', '1')
            this.fetchData('io', '1')
        },
        methods: {
            toggleRange (dropdownRef, toggleRangeStr, idx, range) {
                if (range === '1') {
                    this[toggleRangeStr] = this.$t('environment.nodeInfo.oneHour')
                } else if (range === '2') {
                    this[toggleRangeStr] = this.$t('environment.nodeInfo.oneDay')
                } else if (range === '3') {
                    this[toggleRangeStr] = this.$t('environment.nodeInfo.oneWeek')
                }

                this.$refs[dropdownRef].hide()
                this.fetchData(idx, range)
            },
            async fetchData (idx, range) {
                const params = {
                    projectId: this.projectId,
                    nodeHashId: this.nodeHashId,
                    timeRange: range === '1' ? 'HOUR' : range === '2' ? 'DAY' : 'WEEK'
                }

                // 设置图表数据的方法名
                let hookFuncName
                if (idx === 'cpu_summary') {
                    hookFuncName = 'setCpuData'
                    this.cpuChartLoading = true
                } else if (idx === 'mem') {
                    hookFuncName = 'setMemData'
                    this.memChartLoading = true
                } else if (idx === 'io') {
                    hookFuncName = 'setStorageData'
                    this.ioChartLoading = true
                } else if (idx === 'net') {
                    hookFuncName = 'setNetworkData'
                    this.netChartLoading = true
                }

                if (hookFuncName) {
                    this[hookFuncName](params)
                }
            },
            async setCpuData (params) {
                const chartData = []
                const emptyData = []

                try {
                    const res = await this.$store.dispatch('environment/getNodeCpuMetrics', { params })
                    console.log(res)
                    if (res.usage_user.length) {
                        this.isEmptyCpu = false
                        res.usage_user.forEach(item => {
                            chartData.push({
                                value: [item.time, item.usage_user && item.usage_user.toFixed(2)]
                            })
                            emptyData.push(0)
                        })

                        this.$nextTick(() => {
                            this.cpuLine.series[0].data.splice(0, this.cpuLine.series[0].data.length, ...chartData)
                            this.cpuChartLoading = false
                        })
                    } else {
                        this.isEmptyCpu = true
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            async setMemData (params) {
                const chartData = []
                const emptyData = []

                try {
                    const res = await this.$store.dispatch('environment/getNodeMemoryMetrics', { params })
                    if (res.used_percent.length) {
                        this.isEmptyMemory = false
                        res.used_percent.forEach(item => {
                            chartData.push({
                                value: [item.time, item.used_percent && item.used_percent.toFixed(2)]
                            })
                            emptyData.push(0)
                        })
                        this.$nextTick(() => {
                            this.memoryLine.series[0].data.splice(0, this.memoryLine.series[0].data.length, ...chartData)
                            this.memChartLoading = false
                        })
                    } else {
                        this.isEmptyMemory = true
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            async setNetworkData (params) {
                try {
                    const res = await this.$store.dispatch('environment/getNodeNetworkMetrics', { params })
                    if (JSON.stringify(res) === '{}') {
                        this.isEmptyNetwork = true
                    } else {
                        const readChartData = []
                        this.isEmptyNetwork = false
                        Object.keys(res).forEach(item => {
                            const data = []
                            res[item].forEach(val => {
                                data.push({
                                    value: [val.time, val[item] && val[item].toFixed(2)]
                                })
                            })
                            readChartData.push(
                                {
                                    type: 'line',
                                    name: item,
                                    showSymbol: false,
                                    data: data,
                                    itemStyle: {
                                        normal: {
                                            lineStyle: {
                                                width: 1 // 设置线条粗细
                                            }
                                        }
                                    }
                                }
                            )
                        })

                        this.$nextTick(() => {
                            this.networkLine.series.splice(0, this.networkLine.series.length, ...readChartData || [])
                            this.netChartLoading = false
                        })
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            async setStorageData (params) {
                try {
                    const res = await this.$store.dispatch('environment/getNodeDiskioMetrics', { params })

                    if (JSON.stringify(res) === '{}') {
                        this.isEmptyDiskio = true
                    } else {
                        const readChartData = []
                        this.isEmptyDiskio = false
                        Object.keys(res).forEach(item => {
                            const data = []
                            res[item].forEach(val => {
                                data.push({
                                    value: [val.time, val[item] && val[item].toFixed(2)]
                                })
                            })
                            readChartData.push(
                                {
                                    type: 'line',
                                    name: item,
                                    showSymbol: false,
                                    data: data,
                                    itemStyle: {
                                        normal: {
                                            lineStyle: {
                                                width: 1 // 设置线条粗细
                                            }
                                        }
                                    }
                                }
                            )
                        })
                        this.$nextTick(() => {
                            this.storageLine.series.splice(0, this.storageLine.series.length, ...readChartData || [])
                            this.ioChartLoading = false
                        })
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';
    .node-overview-chart-wrapper {
        margin-top: 20px;
        background-color: #FFF;
        border: 1px solid $borderWeightColor;
        font-size: 0;
        border-radius: 2px;
        .node-overview-chart {
            display: inline-block;
            width: 100%;
            .part {
                width: 50%;
                float: left;
                height: 250px;
                &.top-left {
                    border-right: 1px solid $borderWeightColor;
                    border-bottom: 1px solid $borderWeightColor;
                }
                &.top-right {
                    border-bottom: 1px solid $borderWeightColor;
                }
                &.bottom-left {
                    border-right: 1px solid $borderWeightColor;
                }
                .info {
                    font-size: 14px;
                    display: -webkit-box;
                    display: flex;
                    padding: 20px 30px;
                    .left,
                    .right {
                        -webkit-flex: 1;
                        flex: 1;
                    }

                    .left {
                        font-weight: 700;
                    }

                    .right {
                        text-align: right;
                    }
                }
            }
            .dropdown-button {
                width: 16px;
                height: 16px;
                line-height: 16px;
                text-align: center;
                border: 1px solid #c3cdd7;
                color: #7b7d8a;
                font-size: 0;
                display: inline-block;
                padding: 0;
                background: #fff;
                border-radius: 2px;
                outline: none;
                i {
                    font-size: 12px;
                    transform: scale(.7);
                    display: inline-block;
                }
            }
            .echarts {
                width: 100%;
                height: 180px;
            }
        }
    }
</style>
