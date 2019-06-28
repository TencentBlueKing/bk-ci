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

const state = {
    statusMap: { // 流水线状态Map
        // 'SUCCESS': 'success',
        // 'NOT_BUILT': 'not_built',
        // 'UNKNOWN': 'not_built',
        // 'FAILURE': 'error',
        // 'BUILDING': 'running',
        // 'PREPARING': 'running',
        // 'PAUSED': 'warning',
        // 'ABORTED': 'error',
        // 'SKIPPED': 'skipped',
        // 'KNOWNED_FAILURE': 'known_error'
        'SUCCEED': 'success',
        'FAILED': 'error',
        'CANCELED': 'error',
        'RUNNING': 'running',
        'REVIEWING': 'reviewing',
        'HEARTBEAT_TIMEOUT': 'timeout',
        'REVIEWING_ABORT': 'error',
        'QUEUE': 'queue',
        'QUEUE_TIMEOUT': 'queue_timeout',
        'EXEC_TIMEOUT': 'exec_timeout'
    },
    statusMapCN: {
        'SUCCEED': '成功',
        'success': '成功',
        'FAILED': '失败',
        'HEARTBEAT_TIMEOUT': '超时',
        'error': '失败',
        'known_error': '失败',
        'running': '执行中',
        'QUEUE': '排队',
        'QUEUE_TIMEOUT': '排队超时',
        'EXEC_TIMEOUT': '执行超时'
    },
    statusToIconMap: { // 执行状态和icon之间的转换，主要用在stage中
        'success': 'check-1',
        'paused': 'pause',
        'error': 'exclamation'
    },
    argsTypes: [
        {
            id: 1,
            name: '字符串',
            alias: 'string'
        },
        {
            id: 2,
            name: '布尔值',
            alias: 'boolean'
        },
        {
            id: 3,
            name: '下拉框',
            alias: 'select'
        }
    ],
    timeMap: {
        'years': '年',
        'months': '月',
        'days': '天',
        'hours': '小时',
        'minutes': '分钟',
        'seconds': '秒'
    }
}

const getters = {
    getStatusMap: state => state.statusMap,
    getStatusToIconMap: state => state.statusToIconMap,
    getArgsTypes: state => state.argsTypes,
    getTimeMap: state => state.timeMap,
    getStatusMapCN: state => state.statusMapCN
}

export default {
    namespaced: true,
    state,
    getters
}
