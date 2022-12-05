const titleStyle = {
    fontSize: 15,
    fontWeight: 'bold',
    color: '#63656E'
}

const title = {
    text: '',
    textStyle: titleStyle,
    x: 'center',
    top: 30,
    padding: [0, 0, 30, 0]
}

export const pieOption = {
    tooltip: {
        trigger: 'item',
        formatter: '{b}: {c} ({d}%)'
    },
    legend: {
        orient: 'vertical',
        y: 'bottom',
        right: 0,
        padding: [0, 50, 60, 0],
        itemGap: 20,
        selectedMode: false,
        data: []
    },
    series: [
        {
            name: '',
            type: 'pie',
            center: ['40%', '55%'],
            radius: ['15%', '40%'],
            avoidLabelOverlap: false,
            label: {
                normal: {
                    show: false,
                    position: 'center'
                },
                emphasis: {
                    show: false,
                    textStyle: {
                        fontSize: '30',
                        fontWeight: 'bold'
                    }
                }
            },
            labelLine: {
                normal: {
                    show: false
                }
            },
            data: []
        }
    ],
    title
}

export const barOption = {
    // color: ['#73c7cf', '#f7d359', '#e679ad'],
    color: ['#3c96ff', '#7fcaff', '#a1eaee'],
    tooltip: {
        trigger: 'axis',
        axisPointer: { // 坐标轴指示器，坐标轴触发有效
            type: 'shadow' // 默认为直线，可选为：'line' | 'shadow'
        }
    },
    grid: {
        left: '6%',
        right: '8%',
        bottom: '6%',
        containLabel: true
    },
    xAxis: [
        {
            type: 'category',
            data: [],
            axisTick: {
                alignWithLabel: true
            },
            axisLabel: {
                show: true,
                textStyle: {
                    fontSize: '12',
                    color: '#63656E'
                }
            },
            axisLine: {
                lineStyle: {
                    color: '#e0e0e0'
                }
            }
        }
    ],
    yAxis: [
        {
            type: 'value',
            splitLine: {
                show: false
            },
            axisLabel: {
                show: true,
                textStyle: {
                    fontSize: '12',
                    color: '#63656E'
                }
            },
            axisLine: {
                lineStyle: {
                    color: '#e0e0e0'
                }
            }
        }
    ],
    series: [
        {
            name: '',
            type: 'bar',
            data: [],
            itemStyle: {
                normal: {
                    color: '#3c96ff',
                    barBorderRadius: [5, 5, 0, 0]
                }
            },
            barMaxWidth: 50
        }
    ],
    title
}

export const lineOption = {
    color: ['#3c96ff'],
    tooltip: {
        trigger: 'item'
    },
    xAxis: {
        type: 'category',
        data: []
    },
    yAxis: {
        type: 'value',
        axisLabel: {}
    },
    series: [{
        data: [],
        type: 'line',
        itemStyle: {
            normal: {}
        }
    }],
    title
}

export const overViewOption = {
    color: ['#3c96ff', '#dde4eb'],
    tooltip: {
        trigger: 'item',
        formatter: '{b}: {c} ({d}%)'
    },
    legend: {
        show: false,
        data: []
    },
    series: [
        {
            name: '',
            type: 'pie',
            center: ['50%', '50%'],
            radius: ['62%', '70%'],
            avoidLabelOverlap: true,
            label: {
                normal: {
                    show: false,
                    position: 'center'
                },
                emphasis: {
                    show: false,
                    textStyle: {
                        fontSize: '16',
                        fontWeight: 'bold'
                    }
                }
            },
            labelLine: {
                normal: {
                    show: true
                }
            },
            data: []
        }
    ]
}
