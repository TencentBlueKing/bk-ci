<template>
    <div class="chart-slider">
        <div class="chart-head clearfix">
            <div class="chart-title">报表数据视图</div>
            <bk-dropdown-menu class="fr" :align="'center'">
                <a type="primary" slot="dropdown-trigger">
                    <span>{{ fileterText }}</span>
                    <i class="bk-icon icon-angle-down bk-selector-icon"></i>
                </a>
                <ul class="bk-dropdown-list" slot="dropdown-content">
                    <li v-for="(filiter, index) in fileters" :key="index">
                        <a href="javascript: void(0);" @click.stop="fileterHandler(filiter)">{{ filiter.name }}</a>
                    </li>
                </ul>
            </bk-dropdown-menu>
            <bk-dropdown-menu v-if="task.machineType === '3'" class="fr" :align="'center'">
                <a type="primary" slot="dropdown-trigger">
                    <span>{{ ipText }}</span>
                    <i class="bk-icon icon-angle-down bk-selector-icon"></i>
                </a>
                <ul class="bk-dropdown-list" slot="dropdown-content">
                    <li><a href="javascript: void(0);" @click.stop="fileterIp()">全部IP</a></li>
                    <li v-for="(machine, index) in ipList" :key="index">
                        <a href="javascript: void(0);" @click.stop="fileterIp(machine)">{{ machine }}</a>
                    </li>
                </ul>
            </bk-dropdown-menu>
        </div>
        <div class="chart-container">
            <div class="tasking-wrapper">
                <chart :options="buildOption" auto-resize></chart>
            </div>
            <div class="percentage-wrapper" v-if="task.ccacheEnabled === 'true'">
                <chart :options="ccacheOption" auto-resize></chart>
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
    import { echartsOpt } from '@/utils/chart-option'

    export default {
        name: 'chart-slider',
        components: {
            chart: ECharts
        },
        mixins: [echartsOpt],
        props: {
            task: {
                type: Object,
                default () {
                    return {
                        taskId: '',
                        taskName: ''
                    }
                }
            }
        },
        data () {
            return {
                fileterText: '按日期',
                fileterType: 'date',
                fileters: [
                    {
                        name: '按日期',
                        type: 'date'
                    },
                    {
                        name: '按次数',
                        type: 'count'
                    }
                ],
                ipText: '全部IP',
                ipList: [],
                machineIp: '',
                taskId: '',
                taskInfos: {} // numList timeList
            }
        },
        watch: {
            'task.taskId' (newVal, oldVal) {
                // console.log(newVal, oldVal)
                if (newVal) {
                    this.fileterType = 'date'
                    this.fileterText = '按日期'
                    this.requestTaskChart(newVal)
                }
            }
        },
        created () {
            // console.log(this.task.taskId)
            
            this.task.taskId && this.getIpList(this.task.taskId) && this.requestTaskChart(this.task.taskId)
        },
        methods: {
            fileterHandler (filter) {
                if (filter.name === this.fileterText) {
                    return false
                }
                this.fileterText = filter.name
                this.fileterType = filter.type
                this.chartChanged()
            },
            async requestTaskChart (newVal) {
                try {
                    const res = await this.$store.dispatch('turbo/requestTaskChart', {
                        taskId: newVal,
                        machineIp: this.machineIp
                    })
                    if (res) {
                        this.taskInfos = res
                        this.chartChanged()
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                }
            },
            chartChanged () {
                const { numberTrans, taskInfos } = this
                const key = this.fileterType === 'date' ? 'timeList' : 'numList'
                // console.log(this.taskInfos, type, key)
                const xAxis = []
                // let legend = []
                const originalTime = [] // 加速前
                const accelerateTime = [] // 加速后
                const ccache = [] // 命中率
                taskInfos[key].forEach((item, index) => {
                    originalTime.push(numberTrans(item.originalTime))
                    accelerateTime.push(numberTrans(item.accelerateTime))
                    ccache.push(numberTrans(item.cacheHitRate))
                    if (key === 'numList') {
                        if (item.buildNum) {
                            xAxis.push('#' + item.buildNum)
                        } else {
                            xAxis.push('#' + (index + 1))
                        }
                    } else {
                        xAxis.push(item.compileStartTime ? item.compileStartTime.slice(5) : '')
                    }
                })
                this.buildOption.xAxis.data = this.ccacheOption.xAxis.data = xAxis
                this.buildOption.series[0].data = originalTime
                this.buildOption.series[1].data = accelerateTime
                this.ccacheOption.series[0].data = ccache
            },
            async getIpList (taskId) {
                try {
                    const res = await this.$store.dispatch('turbo/requestIpList', {
                        taskId: taskId
                    })
                    if (res) {
                        this.ipList = res
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                }
            },
            fileterIp (machineIp) {
                this.ipText = machineIp || '全部IP'
                this.machineIp = machineIp || ''
                this.requestTaskChart(this.task.taskId)
            },
            numberTrans (num, leng) {
                if (num <= 0) {
                    return 0
                }
                const result = (num.toString()).indexOf('.')
                if (result !== -1) {
                    return num.toFixed(leng || 2)
                } else {
                    return num
                }
            }
        }
    }
</script>

<style lang="scss">
.chart-slider {
    .bk-dropdown-menu {
        margin-left: 50px;
    }
}
</style>
