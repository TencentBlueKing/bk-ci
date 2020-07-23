export default {
    data () {
        return {
            chartLineOption: {
                title: {
                    text: '',
                    textStyle: {
                        fontSize: 14,
                        color: '#333'
                    }
                },
                grid: {
                    left: '5%',
                    right: '20',
                    top: '50',
                    bottom: '50'
                },
                legend: {
                    right: '15',
                    align: 'right',
                    textStyle: {
                        color: '#777',
                        fontSize: 12
                    },
                    itemHeight: 10,
                    selectedMode: false,
                    data: []
                },
                tooltip: {
                    show: true,
                    // backgroundColor: '#fda85d',
                    transitionDuration: 0,
                    padding: 8,
                    textStyle: {
                        color: '#fff',
                        fontSize: 14
                    },
                    trigger: 'axis',
                    axisPointer: { // 坐标轴指示器，坐标轴触发有效
                        type: 'shadow', // 默认为直线，可选为：'line' | 'shadow'
                        shadowStyle: {
                            // color: '#87c1e6',
                            // opacity: 0.15
                        }
                    }
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
                        textStyle: {
                            fontSize: 12,
                            color: '#777',
                            align: 'center'
                        }
                    }
                },
                yAxis: {
                    min: 0,
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
                series: [{
                    type: 'line',
                    data: [],
                    itemStyle: {
                        normal: {
                            color: '#3a84ff'
                        }
                    },
                    lineStyle: {
                        normal: {
                            color: '#3a84ff'
                        }
                    }
                }]
            }
        }
    }
}
