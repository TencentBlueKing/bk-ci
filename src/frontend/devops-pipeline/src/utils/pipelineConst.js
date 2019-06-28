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

export const jobConst = {
    LINUX: 'Linux',
    MACOS: 'macOS',
    WINDOWS: 'Windows',
    NONE: '无编译环境'
}

export const CONFIRM_MSG = '离开后，新编辑的数据将丢失'

export const CONFIRM_TITLE = '确认要离开'

export const RD_STORE_CODE = 'rdStore'

export const buildEnvMap = {
    thirdPartyAgentId: 'THIRD_PARTY_AGENT_ID',
    thirdPartyAgentEnvId: 'THIRD_PARTY_AGENT_ENV',
    dockerBuildVersion: 'DOCKER',
    tstackAgentId: 'TSTACK'
}

export const BUILD_HISTORY_TABLE_COLUMNS_MAP = {
    buildNum: {
        index: 0,
        prop: 'buildNum',
        label: '构建号',
        width: 120
    },
    material: {
        index: 1,
        prop: 'material',
        label: '源材料',
        width: 360
    },
    startType: {
        index: 2,
        prop: 'startType',
        label: '触发方式',
        width: 120
    },
    queueTime: {
        index: 3,
        prop: 'queueTime',
        label: '排队于',
        width: 120
    },
    startTime: {
        index: 4,
        prop: 'startTime',
        label: '开始于',
        width: 120
    },
    endTime: {
        index: 5,
        prop: 'endTime',
        label: '完成于',
        width: 120
    },
    totalTime: {
        index: 6,
        prop: 'totalTime',
        label: '耗时'
    },
    artifactList: {
        index: 7,
        prop: 'artifactList',
        label: '构件列表',
        width: 180
    },
    appVersions: {
        index: 8,
        prop: 'appVersions',
        label: 'APP版本'
    },
    remark: {
        index: 9,
        prop: 'remark',
        label: '备注'
    },
    recommendVersion: {
        index: 10,
        prop: 'recommendVersion',
        label: '推荐版本号'
    },
    pipelineVersion: {
        index: 11,
        prop: 'pipelineVersion',
        label: '编排版本号'
    },
    entry: {
        index: 12,
        prop: 'entry',
        label: '快捷入口',
        width: 120,
        entries: [{
            type: '',
            label: '详情'

        }, {
            type: 'partView',
            label: '构件列表'

        }, {
            type: 'codeRecords',
            label: '代码变更记录'
        }, {
            type: 'output',
            label: '产出物报告'
        }]
    }
}

export const BUILD_HISTORY_TABLE_DEFAULT_COLUMNS = [
    'buildNum',
    'material',
    'startType',
    'startTime',
    'endTime',
    'totalTime',
    'artifactList',
    'remark',
    'entry'
]

export const statusMap = {
    RUNNING: '执行中',
    PREPARE_ENV: '准备构建环境中',
    CANCELED: '用户取消',
    FAILED: '执行失败',
    SUCCEED: '执行成功',
    REVIEW_ABORT: '已驳回',
    HEARTBEAT_TIMEOUT: '心跳超时',
    QUEUE: '排队',
    QUEUE_TIMEOUT: '排队超时',
    EXEC_TIMEOUT: '执行超时'
}
