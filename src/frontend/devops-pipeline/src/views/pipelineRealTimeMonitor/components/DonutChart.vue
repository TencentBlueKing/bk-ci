<template>
    <div class="donut-chart-wrapper">
        <div
            ref="chartRef"
            class="donut-chart-container"
        ></div>
    </div>
</template>

<script>
    import { defineComponent, ref, onMounted, watch, onBeforeUnmount } from 'vue'
    import * as echarts from 'echarts'

    export default defineComponent({
        name: 'DonutChart',
        emits: ['update:selectedItem', 'clear'],
        props: {
            data: {
                type: Array,
                default: () => []
            },
            centerText: {
                type: [String, Number],
                default: ''
            },
            centerSubText: {
                type: String,
                default: ''
            },
            radius: {
                type: Array,
                default: () => ['60%', '85%']
            }
        },
        setup (props, { emit }) {
            const chartRef = ref(null)
            let chartInstance = null
            const selectedIndex = ref(-1) // 当前选中的索引
            const selectedItem = ref(null) // 当前选中的项数据

            // 清除选中状态
            const clearSelection = () => {
                const currentIndex = selectedIndex.value
                if (chartInstance && currentIndex >= 0) {
                    // 先取消高亮
                    chartInstance.dispatchAction({
                        type: 'downplay',
                        seriesIndex: 0,
                        dataIndex: currentIndex
                    })
                    // 再取消选中
                    chartInstance.dispatchAction({
                        type: 'unselect',
                        seriesIndex: 0,
                        dataIndex: currentIndex
                    })
                }
                selectedIndex.value = -1
                selectedItem.value = null
                // 恢复默认中心文字
                updateCenterText(-1)
                // 通知父组件
                emit('update:selectedItem', null)
            }

            const updateCenterText = (index) => {
                if (!chartInstance) return

                let centerText = props.centerText
                let centerSubText = props.centerSubText

                if (index >= 0 && props.data[index]) {
                    const item = props.data[index]
                    centerText = item.value
                    centerSubText = item.name
                }

                chartInstance.setOption({
                    graphic: [
                        {
                            type: 'text',
                            left: 'center',
                            top: 'center',
                            style: {
                                text: centerText,
                                fontSize: 32,
                                fontWeight: 600,
                                fill: '#313238',
                                textAlign: 'center'
                            }
                        },
                        {
                            type: 'text',
                            left: 'center',
                            top: '58%',
                            style: {
                                text: centerSubText,
                                fontSize: 12,
                                fill: '#979BA5',
                                textAlign: 'center'
                            }
                        }
                    ]
                })
            }

            const initChart = () => {
                if (!chartRef.value) return

                // 如果图表已存在，只更新配置，不重新初始化
                if (!chartInstance) {
                    chartInstance = echarts.init(chartRef.value)
                }

                const option = {
                    tooltip: {
                        trigger: 'item',
                        backgroundColor: '#fff',
                        borderColor: '#DCDEE5',
                        borderWidth: 1,
                        padding: 0,
                        textStyle: {
                            color: '#63656E',
                            fontSize: 12
                        },
                        formatter: (params) => {
                            return `
                                <div style="padding: 8px 12px;width: 150px;color:#63656E; font-size: 12px;">
                                    <div style="display: flex; align-items: center; margin-bottom: 4px;">
                                        <span style="display: inline-block; width: 8px; height: 8px; background: ${params.color}; border-radius: 50%; margin-right: 6px;"></span>
                                        <span >${params.name}</span>
                                    </div>
                                    <div style="display: flex; flex-direction: column; align-items: flex-start; margin-top: 8px; width: 100%;margin-left: 12px;padding-right: 12px;">
                                        <div style="display: flex; flex-direction: row; justify-content: space-between; align-items: center; width: 100%; margin-bottom: 4px;">
                                            <span>数量</span>
                                            <span>${params.value}</span>
                                        </div>
                                        <div style="display: flex; flex-direction: row; justify-content: space-between; align-items: center; width: 100%;">
                                            <span>占比</span>
                                            <span>${params.percent}%</span>
                                        </div>
                                    </div>
                                </div>
                            `
                        }
                    },
                    legend: {
                        show: false
                    },
                    graphic: [
                        {
                            type: 'text',
                            left: 'center',
                            top: 'center',
                            style: {
                                text: props.centerText,
                                fontSize: 32,
                                fontWeight: 600,
                                fill: '#313238',
                                textAlign: 'center'
                            }
                        },
                        {
                            type: 'text',
                            left: 'center',
                            top: '58%',
                            style: {
                                text: props.centerSubText,
                                fontSize: 12,
                                fill: '#979BA5',
                                textAlign: 'center'
                            }
                        }
                    ],
                    series: [
                        {
                            name: '告警分布',
                            type: 'pie',
                            radius: props.radius,
                            avoidLabelOverlap: false,
                            itemStyle: {
                                borderRadius: 0,
                                borderWidth: 0
                            },
                            emphasis: {
                                // hover 时的样式
                                scale: true,
                                scaleSize: 10,
                                itemStyle: {
                                    shadowBlur: 10,
                                    shadowOffsetX: 0,
                                    shadowColor: 'rgba(0, 0, 0, 0.2)',
                                    borderWidth: 0
                                }
                            },
                            select: {
                                // 选中时的样式
                                itemStyle: {
                                    shadowBlur: 10,
                                    shadowOffsetX: 0,
                                    shadowColor: 'rgba(0, 0, 0, 0.3)',
                                    borderWidth: 0
                                }
                            },
                            selectedMode: 'single', // 单选模式
                            selectedOffset: 15, // 选中时的偏移距离
                            label: {
                                show: false
                            },
                            labelLine: {
                                show: false
                            },
                            data: props.data
                        }
                    ]
                }

                chartInstance.setOption(option)
            }
            
            // 绑定事件监听器（只在初始化时调用一次）
            const bindEvents = () => {
                if (!chartInstance) return

                // 监听点击事件
                chartInstance.on('click', (params) => {
                    if (params.componentType === 'series') {
                        const clickedIndex = params.dataIndex
                        
                        // 如果点击的是当前选中的，则取消选中
                        if (selectedIndex.value === clickedIndex) {
                            clearSelection()
                        } else {
                            // 选中新的项
                            selectedIndex.value = clickedIndex
                            selectedItem.value = props.data[clickedIndex]
                            // 更新中心文字
                            updateCenterText(clickedIndex)
                            // 通知父组件
                            emit('update:selectedItem', selectedItem.value)
                        }
                    }
                })

                // 监听 hover 事件
                chartInstance.on('mouseover', (params) => {
                    if (params.componentType === 'series' && selectedIndex.value === -1) {
                        // 只有在没有选中项时，hover 才更新中心文字
                        updateCenterText(params.dataIndex)
                    }
                })

                chartInstance.on('mouseout', () => {
                    if (selectedIndex.value === -1) {
                        // 只有在没有选中项时，鼠标移出才恢复默认文字
                        updateCenterText(-1)
                    }
                })
            }

            // 监听数据变化
            watch(
                () => [props.data, props.centerText, props.centerSubText],
                () => {
                    if (chartInstance) {
                        console.log(props.data,'props.data')
                        
                        // 清除之前的选中状态
                        if (selectedIndex.value >= 0) {
                            chartInstance.dispatchAction({
                                type: 'unselect',
                                seriesIndex: 0,
                                dataIndex: selectedIndex.value
                            })
                        }
                        
                        // 重置选中状态
                        selectedIndex.value = -1
                        selectedItem.value = null
                        
                        // 更新图表数据
                        initChart()
                        
                        // 通知父组件选中状态已清除
                        emit('update:selectedItem', null)
                    }
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
                initChart()
                bindEvents()
                window.addEventListener('resize', handleResize)
            })

            onBeforeUnmount(() => {
                window.removeEventListener('resize', handleResize)
                if (chartInstance) {
                    chartInstance.dispose()
                }
            })

            return {
                chartRef,
                clearSelection
            }
        }
    })
</script>

<style lang="scss" scoped>
.donut-chart-wrapper {
    position: relative;
}

.donut-chart-container {
    width: 100%;
    height: 320px;
}
</style>
