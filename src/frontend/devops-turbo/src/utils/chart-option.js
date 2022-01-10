export const echartsOpt = {
    data () {
        return {
            buildOption: {
                title: {
                    text: '编译耗时趋势图（单位min）',
                    padding: 12,
                    textStyle: {
                        fontSize: 14,
                        align: 'center',
                        color: '#333c48',
                        fontWeight: '400'
                    }
                },
                grid: {
                    x: 60,
                    x2: 30,
                    y: 50,
                    y2: 30
                },
                legend: {
                    data: ['未加速耗时（估算）', '加速后耗时'],
                    top: 9,
                    right: 12
                },
                xAxis: {
                    type: 'category',
                    boundaryGap: false,
                    data: [],
                    axisTick: {
                        show: false
                    },
                    axisLine: {
                        lineStyle: {
                            color: '#C3CDD7'
                        }
                    },
                    axisLabel: {
                        color: '#737987'
                    }
                },
                yAxis: {
                    type: 'value',
                    axisTick: {
                        show: false
                    },
                    axisLine: {
                        lineStyle: {
                            color: '#C3CDD7'
                        }
                    },
                    axisLabel: {
                        formatter: function (value) {
                            return value
                        },
                        color: '#737987'
                    },
                    splitLine: {
                        lineStyle: {
                            color: '#DDE4EB'
                        }
                    }
                },
                tooltip: {
                    trigger: 'axis',
                    axisPointer: {
                        type: 'cross',
                        label: {
                            backgroundColor: '#6a7985'
                        }
                    },
                    formatter ([param1, param2]) {
                        const numToDate = num => {
                            const h = Math.floor(num)
                            const m = Math.round((num - h) * 60)
                            const s = h + 'min ' + m + 's'
                            return s
                        }
                        let res = ''
                        if (param2) {
                            res = `<div>${param1.name}</br>${param1.marker}${param1.seriesName}: ${numToDate(param1.value)}</br>
                            ${param2.marker}${param2.seriesName}: ${numToDate(param2.value)}</div>`
                        } else {
                            res = `<div>${param1.name}</br>${param1.marker}${param1.seriesName}: ${numToDate(param1.value)}</div>`
                        }
                        return res
                    }
                },
                series: [
                    {
                        name: '未加速耗时（估算）',
                        type: 'line',
                        data: [],
                        lineStyle: {
                            color: '#3C96FF',
                            width: 2
                        },
                        itemStyle: {
                            color: '#3C96FF',
                            borderColor: '#3C96FF'
                        }
                    },
                    {
                        name: '加速后耗时',
                        type: 'line',
                        data: [],
                        lineStyle: {
                            color: '#FF9600',
                            width: 2
                        },
                        itemStyle: {
                            color: '#FF9600',
                            borderColor: '#FF9600'
                        }
                    }
                ]
            },
            ccacheOption: {
                title: {
                    text: 'Ccache的Cache命中率',
                    padding: 12,
                    textStyle: {
                        fontSize: 14,
                        align: 'center',
                        color: '#333c48',
                        fontWeight: '400'
                    }
                },
                grid: {
                    x: 60,
                    x2: 30,
                    y: 50,
                    y2: 30
                },
                legend: [],
                xAxis: {
                    type: 'category',
                    boundaryGap: false,
                    data: [],
                    axisTick: {
                        show: false
                    },
                    axisLine: {
                        lineStyle: {
                            color: '#C3CDD7'
                        }
                    },
                    axisLabel: {
                        color: '#737987'
                    }
                },
                yAxis: {
                    type: 'value',
                    axisLabel: {
                        formatter: function (val) {
                            return val + '%'
                        },
                        color: '#737987'
                    },
                    axisTick: {
                        show: false
                    },
                    axisLine: {
                        lineStyle: {
                            color: '#C3CDD7'
                        }
                    },
                    splitLine: {
                        lineStyle: {
                            color: '#DDE4EB'
                        }
                    }
                },
                tooltip: {
                    trigger: 'axis',
                    formatter (params) {
                        const res = '<div>' + params[0].name
                                + '<p>命中率: ' + Number(params[0].value).toFixed(2) + '%</p></div>'
                        return res
                    }
                },
                series: [
                    {
                        name: '命中率',
                        type: 'line',
                        data: [],
                        lineStyle: {
                            color: '#3C96FF',
                            width: 2
                        },
                        itemStyle: {
                            color: '#3C96FF',
                            borderColor: '#3C96FF'
                        }
                    }
                ]
            }
        }
    }
}
