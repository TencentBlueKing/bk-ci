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

export const trendOptions = (i18n) => ({
    tooltip: {
        trigger: 'axis',
        axisPointer: {
            type: 'shadow'
        }
    },
    legend: {
        padding: [10, 0, 0, 0],
        data: ['effectPipelineExecCount', 'intercepts'],
        formatter: (name) => {
            return i18n('quality.' + name)
        }
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
            name: 'effectPipelineExecCount',
            data: [],
            type: 'line',
            itemStyle: {
                normal: {
                    color: '#3c96ff',
                    lineStyle: {
                        color: '#3c96ff'
                    }
                }
            },
            label: 'aaaa'
        },
        {
            name: 'Intercepts',
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
})
