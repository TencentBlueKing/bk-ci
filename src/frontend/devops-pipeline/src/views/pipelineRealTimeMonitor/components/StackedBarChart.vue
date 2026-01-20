<template>
    <div class="stacked-bar-chart-wrapper">
        <div
            ref="chartRef"
            class="stacked-bar-chart"
        >
        </div>
    </div>
</template>

<script>
    import { defineComponent, ref, onMounted, watch, onBeforeUnmount, nextTick } from 'vue'
    import * as echarts from 'echarts'

    export default defineComponent({
        name: 'StackedBarChart',
        props: {
            data: {
                type: Object,
                default: () => ({
                    xAxisData: [],
                    series: []
                })
            },
            height: {
                type: String,
                default: '300px'
            }
        },
        setup (props) {
            const chartRef = ref(null)
            let chartInstance = null

            const initChart = () => {
                if (!chartRef.value) return

                // 销毁旧实例
                if (chartInstance) {
                    chartInstance.dispose()
                }

                chartInstance = echarts.init(chartRef.value)

                const option = {
                    tooltip: {
                        trigger: 'axis',
                        axisPointer: {
                            type: 'shadow'
                        }
                    },
                    legend: {
                        data: props.data.series.map(item => item.name),
                        bottom: 0,
                        itemWidth: 14,
                        itemHeight: 14,
                        textStyle: {
                            fontSize: 12,
                            color: '#63656E'
                        }
                    },
                    grid: {
                        left: '3%',
                        right: '4%',
                        bottom: '15%',
                        top: '5%',
                        containLabel: true
                    },
                    xAxis: {
                        type: 'category',
                        data: props.data.xAxisData,
                        axisLine: {
                            lineStyle: {
                                color: '#DCDEE5'
                            }
                        },
                        axisLabel: {
                            color: '#63656E',
                            fontSize: 12
                        },
                        axisTick: {
                            show: false
                        }
                    },
                    yAxis: {
                        type: 'value',
                        splitLine: {
                            lineStyle: {
                                color: '#F0F1F5',
                                type: 'dashed'
                            }
                        },
                        axisLine: {
                            show: false
                        },
                        axisLabel: {
                            color: '#979BA5',
                            fontSize: 12
                        },
                        axisTick: {
                            show: false
                        }
                    },
                    series: props.data.series.map((item, index) => ({
                        name: item.name,
                        type: 'bar',
                        stack: 'total',
                        barWidth: '40%',
                        data: item.data,
                        itemStyle: {
                            color: item.color,
                            // 最后一个系列（最顶部）添加圆角
                            borderRadius: index === props.data.series.length - 1 ? [4, 4, 0, 0] : 0
                        }
                    }))
                }

                chartInstance.setOption(option)
            }

            // 监听数据变化
            watch(
                () => props.data,
                () => {
                    nextTick(() => {
                        initChart()
                    })
                },
                { deep: true }
            )

            // 监听窗口大小变化
            const handleResize = () => {
                if (chartInstance) {
                    chartInstance.resize()
                }
            }

            onMounted(() => {
                nextTick(() => {
                    initChart()
                    window.addEventListener('resize', handleResize)
                })
            })

            onBeforeUnmount(() => {
                window.removeEventListener('resize', handleResize)
                if (chartInstance) {
                    chartInstance.dispose()
                }
            })

            return {
                chartRef
            }
        }
    })
</script>

<style lang="scss" scoped>
.stacked-bar-chart-wrapper {
    width: 100%;
    height: v-bind(height);
}

.stacked-bar-chart {
    width: 100%;
    height: 100%;
    min-height: 280px;
}
</style>
