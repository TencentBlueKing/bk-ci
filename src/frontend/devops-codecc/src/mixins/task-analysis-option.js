export default {
    data () {
        return {
            taskAnalysisOption: {
                grid: {
                    x: 0,
                    y: 0,
                    x2: 0,
                    y2: 0
                },
                legend: {
                    show: false
                },
                xAxis: {
                    type: 'category',
                    boundaryGap: false,
                    axisLine: {
                        show: false
                    },
                    axisTick: {
                        show: false
                    },
                    splitLine: {
                        show: false
                    },
                    data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
                },
                yAxis: {
                    type: 'value',
                    show: false,
                    axisLine: {
                        show: false
                    },
                    axisTick: {
                        show: false
                    },
                    splitLine: {
                        show: false
                    }
                },
                series: [{
                    data: [820, 932, 901, 934, 1290, 1330, 1320],
                    type: 'line',
                    symbol: 'none',
                    areaStyle: {
                        // color: '#e4f1fd'
                        color: '#f5f7fa'
                    },
                    lineStyle: {
                        color: '#e4f1fd',
                        width: 0
                    }
                }]
            }
        }
    }
}
