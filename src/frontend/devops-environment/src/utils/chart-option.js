/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import { convertTime } from '@/utils/util'

export const nodeOverview = {
    // cpu 使用率图表
    cpu: {
        tooltip: {
            trigger: 'axis',
            confine: true,
            axisPointer: {
                type: 'line',
                animation: false,
                label: {
                    backgroundColor: '#6a7985'
                }
            },
            formatter (params, ticket, callback) {
                let ret
                
                if (params[0].value[1] === undefined) {
                    ret = '<div>没有数据</div>'
                } else {
                    ret = `
                        <div>${convertTime(Date.parse(params[0].value[0]))}</div>
                        <div>${params[0].marker}CPU使用率：${params[0].value[1]}%</div>
                    `
                }

                return ret
            }
        },
        grid: {
            show: false,
            top: '4%',
            left: '4%',
            right: '5%',
            bottom: '3%',
            containLabel: true
        },
        xAxis: [
            {
                type: 'time',
                boundaryGap: false,
                axisLine: {
                    show: true,
                    lineStyle: {
                        color: '#dde4eb'
                    }
                },
                axisTick: {
                    alignWithLabel: true,
                    length: 5,
                    lineStyle: {
                        color: '#ebf0f5'
                    }
                },
                axisLabel: {
                    color: '#868b97'
                },
                splitLine: {
                    show: true,
                    lineStyle: {
                        color: ['#ebf0f5'],
                        type: 'dashed'
                    }
                }
            }
        ],
        yAxis: [
            {
                // min: 0,
                // max: 100,
                // interval: 25,
                boundaryGap: [0, '2%'],
                type: 'value',
                axisLine: {
                    show: true,
                    lineStyle: {
                        color: '#dde4eb'
                    }
                },
                axisTick: {
                    alignWithLabel: true,
                    length: 0,
                    lineStyle: {
                        color: 'red'
                    }
                },
                axisLabel: {
                    color: '#868b97',
                    formatter (value, index) {
                        return `${value}%`
                    }
                },
                splitLine: {
                    show: true,
                    lineStyle: {
                        color: ['#ebf0f5'],
                        type: 'dashed'
                    }
                }
            }
        ],
        series: [
            {
                type: 'line',
                name: 'CPU使用率',
                showSymbol: false,
                smooth: true,
                hoverAnimation: false,
                areaStyle: {
                    normal: {
                        color: {
                            type: 'linear',
                            x: 0,
                            y: 0,
                            x2: 0,
                            y2: 1,
                            colorStops: [
                                {
                                    offset: 0, color: '#30d878' // 0% 处的颜色
                                },
                                {
                                    offset: 1, color: '#c0f3d6' // 100% 处的颜色
                                }
                            ],
                            globalCoord: false
                        }
                    }
                },
                itemStyle: {
                    normal: {
                        color: '#30d878'
                    }
                },
                data: []
            }
        ]
    },
    // 内存使用率
    memory: {
        tooltip: {
            trigger: 'axis',
            axisPointer: {
                type: 'line',
                animation: false,
                label: {
                    backgroundColor: '#6a7985'
                }
            },
            formatter (params, ticket, callback) {
                let ret

                if (params[0].value[1] === undefined) {
                    ret = '<div>没有数据</div>'
                } else {
                    ret = `
                        <div>${convertTime(Date.parse(params[0].value[0]))}</div>
                        <div>${params[0].marker}内存使用率：${params[0].value[1]}%</div>
                    `
                }

                return ret
            }
        },
        grid: {
            show: false,
            top: '4%',
            left: '4%',
            right: '5%',
            bottom: '3%',
            containLabel: true
        },
        xAxis: [
            {
                type: 'time',
                boundaryGap: false,
                axisLine: {
                    show: true,
                    lineStyle: {
                        color: '#dde4eb'
                    }
                },
                axisTick: {
                    alignWithLabel: true,
                    length: 5,
                    lineStyle: {
                        color: '#ebf0f5'
                        // color: '#868b97'
                    }
                },
                axisLabel: {
                    color: '#868b97'
                },
                splitLine: {
                    show: true,
                    lineStyle: {
                        color: ['#ebf0f5'],
                        type: 'dashed'
                    }
                }
            }
        ],
        yAxis: [
            {
                boundaryGap: [0, '2%'],
                type: 'value',
                axisLine: {
                    show: true,
                    lineStyle: {
                        color: '#dde4eb'
                    }
                },
                axisTick: {
                    alignWithLabel: true,
                    length: 0,
                    lineStyle: {
                        color: 'red'
                    }
                },
                axisLabel: {
                    color: '#868b97',
                    formatter (value, index) {
                        return `${value}%`
                    }
                },
                splitLine: {
                    show: true,
                    lineStyle: {
                        color: ['#ebf0f5'],
                        type: 'dashed'
                    }
                }
            }
        ],
        series: [
            {
                type: 'line',
                name: 'total',
                smooth: true,
                showSymbol: false,
                hoverAnimation: false,
                areaStyle: {
                    normal: {
                        // color: {
                        //     type: 'linear',
                        //     x: 0,
                        //     y: 0,
                        //     x2: 0,
                        //     y2: 1,
                        //     colorStops: [
                        //         {
                        //             offset: 0, color: '#52a2ff' // 0% 处的颜色
                        //         },
                        //         {
                        //             offset: 1, color: '#a9d1ff' // 100% 处的颜色
                        //         }
                        //     ],
                        //     globalCoord: false
                        // }
                    }
                },
                // itemStyle: {
                //     normal: {
                //         color: '#52a2ff'
                //     }
                // }
                data: []
            }
            // {
            //     type: 'line',
            //     name: 'used',
            //     smooth: true,
            //     showSymbol: false,
            //     hoverAnimation: false,
            //     areaStyle: {
            //         normal: {
            //             // color: {
            //             //     type: 'linear',
            //             //     x: 0,
            //             //     y: 0,
            //             //     x2: 0,
            //             //     y2: 1,
            //             //     colorStops: [
            //             //         {
            //             //             offset: 0, color: '#52a2ff' // 0% 处的颜色
            //             //         },
            //             //         {
            //             //             offset: 1, color: '#a9d1ff' // 100% 处的颜色
            //             //         }
            //             //     ],
            //             //     globalCoord: false
            //             // }
            //         }
            //     }
            //     // itemStyle: {
            //     //     normal: {
            //     //         color: 'red'
            //     //     }
            //     // }
            // }
        ]
    },
    // 网络使用率
    network: {
        tooltip: {
            trigger: 'axis',
            position: function (point, params, dom, rect, size) {
                // 提示框高度
                const boxHeight = size.contentSize[1]
                return [point[0] + 20, point[1] - (boxHeight / 2)]
            },
            axisPointer: {
                type: 'line',
                animation: false,
                label: {
                    backgroundColor: '#6a7985'
                }
            },
            formatter (params, ticket, callback) {
                let ret
                
                ret = `<div>${convertTime(Date.parse(params[0].value[0]))}</div>`
                params.forEach(item => {
                    let displayTime
                    
                    if ((item.value[1]) / (8 * 1024 * 1024 * 1024) >= 1) {
                        displayTime = `${((item.value[1]) / (8 * 1024 * 1024 * 1024) || 0).toFixed(2)}G`
                    } else if ((item.value[1]) / (8 * 1024 * 1024) >= 1) {
                        displayTime = `${((item.value[1]) / (8 * 1024 * 1024) || 0).toFixed(2)}MB`
                    } else if ((item.value[1]) / (8 * 1024) >= 1) {
                        displayTime = `${((item.value[1]) / (8 * 1024) || 0).toFixed(2)}KB`
                    } else {
                        displayTime = `${(item.value[1] / 8 || 0).toFixed(2)}B`
                    }

                    ret += `<div>${item.marker}${item.seriesName}：${displayTime}</div>`
                })

                return ret
            }
        },
        // legend: {
        //     data: ['sent', 'recv']
        // },
        grid: {
            show: false,
            top: '4%',
            left: '4%',
            right: '5%',
            bottom: '3%',
            containLabel: true
        },
        xAxis: [
            {
                type: 'time',
                boundaryGap: false,
                axisLine: {
                    show: true,
                    lineStyle: {
                        color: '#dde4eb'
                    }
                },
                axisTick: {
                    alignWithLabel: true,
                    length: 5,
                    lineStyle: {
                        color: '#ebf0f5'
                        // color: '#868b97'
                    }
                },
                axisLabel: {
                    color: '#868b97'
                },
                splitLine: {
                    show: true,
                    lineStyle: {
                        color: ['#ebf0f5'],
                        type: 'dashed'
                    }
                }
            }
        ],
        yAxis: [
            {
                boundaryGap: [0, '2%'],
                type: 'value',
                axisLine: {
                    show: true,
                    lineStyle: {
                        color: '#dde4eb'
                    }
                },
                axisTick: {
                    alignWithLabel: true,
                    length: 0,
                    lineStyle: {
                        color: 'red'
                    }
                },
                axisLabel: {
                    color: '#868b97',
                    formatter (value, index) {
                        if ((value / (8 * 1024 * 1024 * 1024)) > 1) {
                            return `${(value / (8 * 1024 * 1024 * 1024)).toFixed(0)}G`
                        } else if ((value / (8 * 1024 * 1024)) > 1) {
                            return `${(value / (8 * 1024 * 1024)).toFixed(0)}MB`
                        } else if ((value / (8 * 1024)) > 1) {
                            return `${(value / (8 * 1024)).toFixed(0)}KB`
                        } else {
                            return `${value / 8}B`
                        }
                    }
                },
                splitLine: {
                    show: true,
                    lineStyle: {
                        color: ['#ebf0f5'],
                        type: 'dashed'
                    }
                }
            }
        ],
        series: [
            {
                type: 'line',
                // showSymbol: true,
                smooth: true,
                showSymbol: false,
                hoverAnimation: false,
                areaStyle: {
                    normal: {
                    }
                },
                itemStyle: {
                    normal: {
                        color: '#ffbe21'
                    }
                },
                data: []
            },
            {
                type: 'line',
                // showSymbol: true,
                smooth: true,
                showSymbol: false,
                hoverAnimation: false,
                areaStyle: {
                    normal: {
                    }
                },
                itemStyle: {
                    normal: {
                        color: 'red'
                    }
                },
                data: []
            }
        ]
    },
    // 存储使用率
    storage: {
        tooltip: {
            trigger: 'axis',
            // position: function (point, params, dom, rect, size) {
            //     // 提示框高度
            //     var boxHeight = size.contentSize[1]
            //     return [point[0] + 20, point[1] - (boxHeight / 2)]
            // },
            axisPointer: {
                type: 'line',
                animation: false,
                label: {
                    backgroundColor: '#6a7985'
                }
            },
            formatter (params, ticket, callback) {
                let ret

                ret = `<div>${convertTime(Date.parse(params[0].value[0]))}</div>`
                params.forEach(item => {
                    let displayTime

                    if ((item.value[1]) / (1024 * 1024 * 1024) >= 1) {
                        displayTime = `${((item.value[1]) / (1024 * 1024 * 1024) || 0).toFixed(2)}G`
                    } else if ((item.value[1]) / (1024 * 1024) >= 1) {
                        displayTime = `${((item.value[1]) / (1024 * 1024) || 0).toFixed(2)}MB`
                    } else if ((item.value[1]) / (1024) >= 1) {
                        displayTime = `${((item.value[1]) / (1024) || 0).toFixed(2)}KB`
                    } else {
                        displayTime = `${(item.value[1] || 0)}B`
                    }

                    ret += `<div>${item.marker}${item.seriesName}：${displayTime}</div>`
                })

                return ret
            }
        },
        // legend: {
        //     data: ['sent', 'recv']
        // },
        grid: {
            show: false,
            top: '4%',
            left: '4%',
            right: '5%',
            bottom: '3%',
            containLabel: true
        },
        xAxis: [
            {
                type: 'time',
                boundaryGap: false,
                axisLine: {
                    show: true,
                    lineStyle: {
                        color: '#dde4eb'
                    }
                },
                axisTick: {
                    alignWithLabel: true,
                    length: 5,
                    lineStyle: {
                        color: '#ebf0f5'
                        // color: '#868b97'
                    }
                },
                axisLabel: {
                    color: '#868b97'
                },
                splitLine: {
                    show: true,
                    lineStyle: {
                        color: ['#ebf0f5'],
                        type: 'dashed'
                    }
                }
            }
        ],
        yAxis: [
            {
                boundaryGap: [0, '2%'],
                type: 'value',
                axisLine: {
                    show: true,
                    lineStyle: {
                        color: '#dde4eb'
                    }
                },
                axisTick: {
                    alignWithLabel: true,
                    length: 0,
                    lineStyle: {
                        color: 'red'
                    }
                },
                axisLabel: {
                    color: '#868b97',
                    formatter (value, index) {
                        if ((value / (1024 * 1024 * 1024)) > 1) {
                            return `${(value / (1024 * 1024 * 1024)).toFixed(0)}G`
                        } else if ((value / (1024 * 1024)) > 1) {
                            return `${(value / (1024 * 1024)).toFixed(0)}MB`
                        } else if ((value / (1024)) > 1) {
                            return `${(value / (1024)).toFixed(0)}KB`
                        } else {
                            return `${value}B`
                        }
                    }
                },
                splitLine: {
                    lineStyle: {
                        color: ['#ebf0f5'],
                        type: 'dashed'
                    }
                }
            }
        ],
        series: [
            {
                type: 'line',
                smooth: true,
                showSymbol: false,
                hoverAnimation: false,
                areaStyle: {
                    normal: {
                    }
                },
                itemStyle: {
                    normal: {
                        color: '#ffbe21'
                    }
                },
                data: []
            },
            {
                type: 'line',
                smooth: true,
                showSymbol: false,
                hoverAnimation: false,
                areaStyle: {
                    normal: {
                    }
                },
                itemStyle: {
                    normal: {
                        color: 'red'
                    }
                },
                data: []
            }
        ]
    }
}
