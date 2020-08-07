export default {
    data () {
        return {
            chartBarOption: {
                color: ['#7572dc', '#ff9c01', '#ff5656', '#ff5656'],
                title: {
                    text: '',
                    textStyle: {
                        fontSize: 14,
                        color: '#333'
                    }
                },
                grid: {
                    left: '5%',
                    right: '20'
                    // top: '50',
                    // bottom: '50'
                },
                tooltip: {
                    trigger: 'axis',
                    // backgroundColor: '#fda85d',
                    transitionDuration: 0,
                    axisPointer: { // 坐标轴指示器，坐标轴触发有效
                        type: 'shadow', // 默认为直线，可选为：'line' | 'shadow'
                        shadowStyle: {
                            // color: '#87c1e6',
                            // opacity: 0.15
                        }
                    }
                },
                legend: {
                    x: 'right',
                    show: true,
                    align: 'right',
                    textStyle: {
                        color: '#777',
                        fontSize: 12
                    },
                    selectedMode: false,
                    data: [{
                        name: this.$t('严重')
                    }, {
                        name: this.$t('一般')
                    }, {
                        name: this.$t('提示')
                    }]
                },
                xAxis: {
                    type: 'category',
                    axisLine: {
                        show: false
                    },
                    axisTick: {
                        show: false
                    },
                    data: [],
                    axisLabel: {
                        interval: 0,
                        rotate: 30,
                        show: true,
                        textStyle: {
                            fontSize: 12,
                            color: '#777'
                        }
                    }
                },
                yAxis: {
                    type: 'value',
                    axisLine: {
                        show: false
                    },
                    axisTick: {
                        show: false
                    },
                    axisLabel: {
                        textStyle: {
                            fontSize: 12,
                            color: '#555555'
                        }
                    },
                    splitLine: {
                        show: true,
                        lineStyle: {
                            color: '#d0eef6',
                            type: 'dashed'
                        }
                    }
                },
                animation: false,
                series: [
                    {
                        name: this.$t('提示'),
                        type: 'bar',
                        stack: this.$t('状态'),
                        data: [],
                        barWidth: 42,
                        barGap: '50',
                        itemStyle: {
                            normal: {
                                // color: '#7572dc'
                            }
                        }
                    },
                    {
                        name: this.$t('一般'),
                        type: 'bar',
                        stack: this.$t('状态'),
                        data: [],
                        itemStyle: {
                            normal: {
                                // color: '#ff9c01'
                            }
                        }
                    },
                    {
                        name: this.$t('严重'),
                        type: 'bar',
                        stack: this.$t('状态'),
                        data: [],
                        itemStyle: {
                            normal: {
                                // color: '#ff5656'
                            }
                        }
                    },
                    {
                        name: this.$t('极高风险'),
                        type: 'bar',
                        stack: this.$t('状态'),
                        data: [],
                        itemStyle: {
                            normal: {
                                // color: '#ff5656'
                            }
                        }
                    }
                ]
            }
        }
    }
}
