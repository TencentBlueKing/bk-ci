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

const getRangeArray = max => [...Array(max).keys()].map(i => '' + i)
const getOptions = type => array => {
    return array.map((label, index) => ({
        index,
        label,
        id: `${type}_${index}`,
        value: index
    }))
}

const weekCN = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
const peroidCN = ['小时', '天', '周', '月']

export const PERIOD_ARRAY = getOptions('peroid')(peroidCN)
export const HOUR_ARRAY = getOptions('hour')(getRangeArray(24))
export const MINUTES_ARRAY = getOptions('minute')(getRangeArray(60))
export const DAY_ARRAY = getRangeArray(31).map((label, index) => ({
    index,
    id: `day_${index}`,
    label: `${index + 1}号`,
    value: index + 1
}))
export const MONTH_ARRAY = getRangeArray(12).map((label, index) => ({
    index,
    id: `month_${index}`,
    label: `${(index + 1).toLocaleString('zh-Hans-CN-u-nu-hanidec')}月`,
    value: index + 1
}))
export const WEEK_ARRAY = getOptions('week')(weekCN)
