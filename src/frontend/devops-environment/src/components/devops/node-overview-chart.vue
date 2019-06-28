<template>
    <div class="node-overview-chart-wrapper">
        <div class="node-overview-chart">
            <div class="part top-left">
                <div class="info">
                    <div class="left">CPU使用率</div>
                    <div class="right">
                        <bk-dropdown-menu :align="'right'" ref="cpuDropdown">
                            <div style="cursor: pointer;" slot="dropdown-trigger">
                                <span>{{cpuToggleRangeStr}}</span>
                                <button class="dropdown-button">
                                    <i class="bk-icon icon-angle-down"></i>
                                </button>
                            </div>
                            <ul class="bk-dropdown-list" slot="dropdown-content">
                                <li>
                                    <a href="javascript:;" @click.stop="toggleRange('cpuDropdown', 'cpuToggleRangeStr', 'cpu_summary', '1')">1小时</a>
                                </li>
                                <li>
                                    <a href="javascript:;" @click.stop="toggleRange('cpuDropdown', 'cpuToggleRangeStr', 'cpu_summary', '2')">24小时</a>
                                </li>
                                <li>
                                    <a href="javascript:;" @click.stop="toggleRange('cpuDropdown', 'cpuToggleRangeStr', 'cpu_summary', '3')">近7天</a>
                                </li>
                            </ul>
                        </bk-dropdown-menu>
                    </div>
                </div>
                <chart :options="cpuLine" ref="cpuLine1" auto-resize v-show="!isEmptyCpu"></chart>
                <div class="paas-ci-empty" v-show="isEmptyCpu">
                    <img :src="calcSrc" alt="暂无数据" class="empty-pic">
                </div>
            </div>
            <div class="part top-right">
                <div class="info">
                    <div class="left">内存使用率</div>
                    <div class="right">
                        <bk-dropdown-menu :align="'right'" ref="memoryDropdown">
                            <div style="cursor: pointer;" slot="dropdown-trigger">
                                <span>{{memToggleRangeStr}}</span>
                                <button class="dropdown-button">
                                    <i class="bk-icon icon-angle-down"></i>
                                </button>
                            </div>
                            <ul class="bk-dropdown-list" slot="dropdown-content">
                                <li>
                                    <a href="javascript:;" @click.stop="toggleRange('memoryDropdown', 'memToggleRangeStr', 'mem', '1')">1小时</a>
                                </li>
                                <li>
                                    <a href="javascript:;" @click.stop="toggleRange('memoryDropdown', 'memToggleRangeStr', 'mem', '2')">24小时</a>
                                </li>
                                <li>
                                    <a href="javascript:;" @click.stop="toggleRange('memoryDropdown', 'memToggleRangeStr', 'mem', '3')">近7天</a>
                                </li>
                            </ul>
                        </bk-dropdown-menu>
                    </div>
                </div>
                <chart :options="memoryLine" ref="memoryLine1" auto-resize v-show="!isEmptyMemory"></chart>
                <div class="paas-ci-empty" v-show="isEmptyMemory">
                    <img :src="calcSrc" alt="暂无数据" class="empty-pic">
                </div>
            </div>
        </div>
        <div class="node-overview-chart">
            <div class="part bottom-left">
                <div class="info">
                    <div class="left">网络IO</div>
                    <div class="right">
                        <bk-dropdown-menu :align="'right'" ref="networkDropdown">
                            <div style="cursor: pointer;" slot="dropdown-trigger">
                                <span>{{networkToggleRangeStr}}</span>
                                <button class="dropdown-button">
                                    <i class="bk-icon icon-angle-down"></i>
                                </button>
                            </div>
                            <ul class="bk-dropdown-list" slot="dropdown-content">
                                <li>
                                    <a href="javascript:;" @click.stop="toggleRange('networkDropdown', 'networkToggleRangeStr', 'net', '1')">1小时</a>
                                </li>
                                <li>
                                    <a href="javascript:;" @click.stop="toggleRange('networkDropdown', 'networkToggleRangeStr', 'net', '2')">24小时</a>
                                </li>
                                <li>
                                    <a href="javascript:;" @click.stop="toggleRange('networkDropdown', 'networkToggleRangeStr', 'net', '3')">近7天</a>
                                </li>
                            </ul>
                        </bk-dropdown-menu>
                    </div>
                </div>
                <chart :options="networkLine" ref="networkLine1" auto-resize v-show="!isEmptyNetwork"></chart>
                <div class="paas-ci-empty" v-show="isEmptyNetwork">
                    <img :src="calcSrc" alt="暂无数据" class="empty-pic">
                </div>
            </div>
            <div class="part">
                <div class="info">
                    <div class="left">磁盘IO</div>
                    <div class="right">
                        <bk-dropdown-menu :align="'right'" ref="storageDropdown">
                            <div style="cursor: pointer;" slot="dropdown-trigger">
                                <span>{{storageToggleRangeStr}}</span>
                                <button class="dropdown-button">
                                    <i class="bk-icon icon-angle-down"></i>
                                </button>
                            </div>
                            <ul class="bk-dropdown-list" slot="dropdown-content">
                                <li>
                                    <a href="javascript:;" @click.stop="toggleRange('storageDropdown', 'storageToggleRangeStr', 'io', '1')">1小时</a>
                                </li>
                                <li>
                                    <a href="javascript:;" @click.stop="toggleRange('storageDropdown', 'storageToggleRangeStr', 'io', '2')">24小时</a>
                                </li>
                                <li>
                                    <a href="javascript:;" @click.stop="toggleRange('storageDropdown', 'storageToggleRangeStr', 'io', '3')">近7天</a>
                                </li>
                            </ul>
                        </bk-dropdown-menu>
                    </div>
                </div>
                <chart :options="storageLine" ref="storageLine1" auto-resize v-show="!isEmptyDiskio"></chart>
                <div class="paas-ci-empty" v-show="isEmptyDiskio">
                    <img :src="calcSrc" alt="暂无数据" class="empty-pic">
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import ECharts from 'vue-echarts/components/ECharts.vue'
    import 'echarts/lib/chart/line'
    import 'echarts/lib/component/tooltip'
    import 'echarts/lib/component/legend'
    import { nodeOverview } from '@/utils/chart-option'
    import { bus } from '@/utils/bus'

    export default {
        components: {
            chart: ECharts
        },
        data () {
            return {
                isEmptyCpu: false,
                isEmptyMemory: false,
                isEmptyNetwork: false,
                isEmptyDiskio: false,
                cpuToggleRangeStr: '1小时',
                memToggleRangeStr: '1小时',
                networkToggleRangeStr: '1小时',
                storageToggleRangeStr: '1小时',
                cpuLine: nodeOverview.cpu,
                memoryLine: nodeOverview.memory,
                networkLine: nodeOverview.network,
                storageLine: nodeOverview.storage,
                calcSrc: require('@/images/no_data.png')
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            nodeHashId () {
                return this.$route.params.nodeHashId
            }
        },
        created () {
            bus.$off('refreshCharts')
            bus.$on('refreshCharts', () => {
                this.fetchData('cpu_summary', this.cpuToggleRangeStr === '1小时' ? '1' : this.cpuToggleRangeStr === '24小时' ? '2' : '3')
                this.fetchData('mem', this.memToggleRangeStr === '1小时' ? '1' : this.memToggleRangeStr === '24小时' ? '2' : '3')
                this.fetchData('net', this.networkToggleRangeStr === '1小时' ? '1' : this.networkToggleRangeStr === '24小时' ? '2' : '3')
                this.fetchData('io', this.storageToggleRangeStr === '1小时' ? '1' : this.storageToggleRangeStr === '24小时' ? '2' : '3')
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
                    this[toggleRangeStr] = '1小时'
                } else if (range === '2') {
                    this[toggleRangeStr] = '24小时'
                } else if (range === '3') {
                    this[toggleRangeStr] = '近7天'
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

                // 图表组件 ref
                let ref
                // 设置图表数据的方法名
                let hookFuncName
                if (idx === 'cpu_summary') {
                    ref = this.$refs.cpuLine1
                    hookFuncName = 'setCpuData'
                } else if (idx === 'mem') {
                    ref = this.$refs.memoryLine1
                    hookFuncName = 'setMemData'
                } else if (idx === 'io') {
                    ref = this.$refs.storageLine1
                    hookFuncName = 'setStorageData'
                } else if (idx === 'net') {
                    ref = this.$refs.networkLine1
                    hookFuncName = 'setNetworkData'
                }

                ref && ref.showLoading({
                    text: '正在加载',
                    color: '#30d878',
                    maskColor: 'rgba(255, 255, 255, 0.8)'
                })
                
                if (hookFuncName) {
                    this[hookFuncName](ref, params)
                }
            },
            async setCpuData (ref, params) {
                if (!ref) {
                    return
                }
                const chartData = []
                const emptyData = []

                try {
                    const res = await this.$store.dispatch('environment/getNodeCpuMetrics', { params })
                    if (res.usage_user.length) {
                        this.isEmptyCpu = false
                        res.usage_user.forEach(item => {
                            chartData.push({
                                value: [item.time, item.usage_user && item.usage_user.toFixed(2)]
                            })
                            emptyData.push(0)
                        })
                        
                        this.cpuLine.series[0].data.splice(0, this.cpuLine.series[0].data.length, ...chartData)
                        
                        ref.hideLoading()
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
            async setMemData (ref, params) {
                if (!ref) {
                    return
                }
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
                        this.memoryLine.series[0].data.splice(0, this.memoryLine.series[0].data.length, ...chartData)
                        ref.hideLoading()
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
            async setNetworkData (ref, params) {
                if (!ref) {
                    return
                }
                
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
                        this.networkLine.series.splice(0, this.networkLine.series.length, ...readChartData || [])
                        this.$refs.networkLine1.hideLoading()
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
            async setStorageData (ref, params) {
                if (!ref) {
                    return
                }
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
                        this.storageLine.series.splice(0, this.storageLine.series.length, ...readChartData || [])
                        this.$refs.storageLine1.hideLoading()
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
        .paas-ci-empty {
            position: relative;
            width: 100%;
            height: 180px;
            text-align: center;
            .empty-pic {
                position: relative;
                top: 36px;
                width: 80px;
                height: 80px;
            }
        }
    }
</style>
