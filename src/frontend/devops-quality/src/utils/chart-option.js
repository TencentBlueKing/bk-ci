export const rankOptions = {
    legend: {
        data: []
    },
    grid: {
        top: '12%',
        left: '0',
        right: '2%',
        bottom: '3%',
        containLabel: true
    },
    xAxis: {
        type: 'value',
        boundaryGap: [0, 0.01],
        splitLine: { show: false },
        axisLine: {
            lineStyle: {
                color: '#DDE4EB'
            }
        },
        axisLabel: {
            textStyle: {
                color: '#737987',
                fontSize: '12'
            }
        }
    },
    yAxis: {
        type: 'category',
        data: [],
        splitLine: { show: false },
        axisLine: {
            lineStyle: {
                color: '#DDE4EB'
            }
        },
        axisLabel: {
            textStyle: {
                color: '#737987',
                fontSize: '12'
            },
            show: true,
            interval: 0
        }
    },
    series: [
        {
            type: 'bar',
            barWidth: 18,
            itemStyle: {
                normal: {}
            },
            data: []
        }
    ],
    tooltip: {
        trigger: 'axis',
        axisPointer: {
            type: 'shadow'
        }
    }
}

export const trendOptions = {
    tooltip: {
        trigger: 'axis',
        axisPointer: {
            type: 'shadow'
        }
    },
    legend: {
        padding: [10, 0, 0, 0],
        data: ['生效流水线执行数', '拦截数']
    },
    grid: {
        top: '12%',
        left: '0',
        right: '0%',
        bottom: '3%',
        containLabel: true
    },
    xAxis: {
        type: 'category',
        data: [],
        splitLine: { show: false },
        axisLine: {
            lineStyle: {
                color: '#DDE4EB'
            }
        },
        axisLabel: {
            textStyle: {
                color: '#737987',
                fontSize: '12'
            }
        }
    },
    yAxis: {
        type: 'value',
        splitLine: { show: false },
        axisLine: {
            lineStyle: {
                color: '#DDE4EB'
            }
        },
        axisLabel: {
            textStyle: {
                color: '#737987',
                fontSize: '12'
            }
        }
    },
    series: [
        {
            name: '生效流水线执行数',
            data: [],
            type: 'line',
            itemStyle: {
                normal: {
                    color: '#3c96ff',
                    lineStyle: {
                        color: '#3c96ff'
                    }
                }
            }
        },
        {
            name: '拦截数',
            data: [],
            type: 'line',
            itemStyle: {
                normal: {
                    color: '#FF5656',
                    lineStyle: {
                        color: '#FF5656'
                    }
                }
            }
        }
    ]
}
