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

const trendMixins = {
    data () {
        return {
            dateRange: [],
            shortcuts: [
                {
                    text: '今天',
                    value () {
                        const end = new Date()
                        const start = new Date()
                        return [start, end]
                    }
                },
                {
                    text: '近7天',
                    value () {
                        const end = new Date()
                        const start = new Date()
                        start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
                        return [start, end]
                    }
                },
                {
                    text: '近15天',
                    value () {
                        const end = new Date()
                        const start = new Date()
                        start.setTime(start.getTime() - 3600 * 1000 * 24 * 15)
                        return [start, end]
                    }
                },
                {
                    text: '近30天',
                    value () {
                        const end = new Date()
                        const start = new Date()
                        start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
                        return [start, end]
                    }
                }
            ],
            dateOptions: {
                disabledDate: (date) => {
                    const max = new Date()
                    const min = new Date(max - 90 * 3600 * 24 * 1000)
                    if (date > max || date < min) {
                        return true
                    } else {
                        return false
                    }
                }
            }
        }
    },
    mounted () {
        this.getDefaultRange()
    },
    methods: {
        getDefaultRange () {
            const now = new Date()
            const before = new Date(now - 7 * 24 * 3600 * 1000)
            this.dateRange = [`${before.getFullYear()}-${before.getMonth() + 1}-${before.getDate()}`, `${now.getFullYear()}-${now.getMonth() + 1}-${now.getDate()}`]
        },
        changeDateRange (date) {
            this.dateRange = date
        }
    }
}

export default trendMixins
