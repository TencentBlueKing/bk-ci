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

// 根据配置文件获取job流程控制选项的默认值
export function getJobOptionDefault (OPTION = JOB_OPTION) {
    return Object.keys(OPTION).reduce((formProps, key) => {
        if (OPTION[key] && typeof OPTION[key].default === 'object') {
            formProps[key] = JSON.parse(JSON.stringify(OPTION[key].default))
        } else {
            formProps[key] = OPTION[key].default
        }
        return formProps
    }, {})
}

export const JOB_OPTION = {
    enable: {
        rule: {},
        type: 'boolean',
        component: 'atom-checkbox',
        text: '启用本Job',
        default: true
    },
    timeout: {
        rule: { 'numeric': true, 'max_value': 2880 },
        component: 'vuex-input',
        required: true,
        label: 'Job执行超时时间(单位分钟)',
        desc: '请填写0-2880之间的整数，0表示系统允许的最大超时时间',
        placeholder: '请填写0-2880之间的整数，0表示系统允许的最大超时时间',
        default: '900'
    },
    runCondition: {
        rule: {},
        component: 'selector',
        label: '何时运行本Job',
        default: 'STAGE_RUNNING',
        list: [
            {
                id: 'STAGE_RUNNING',
                name: 'Stage开始运行时'
            },
            {
                id: 'CUSTOM_VARIABLE_MATCH',
                name: '自定义变量全部满足时运行'
            },
            {
                id: 'CUSTOM_VARIABLE_MATCH_NOT_RUN',
                name: '自定义变量全部满足时不运行'
            }
            // {
            //     id: 'CUSTOM_CONDITION_MATCH ',
            //     name: ' 满足以下自定义条件时运行'
            // }
        ]
    },
    customVariables: {
        rule: {},
        component: 'key-value-normal',
        default: [{ key: 'param1', value: '' }],
        label: '自定义变量',
        allowNull: false,
        isHidden: (jobOptoin) => {
            return !(jobOptoin && (jobOptoin.runCondition === 'CUSTOM_VARIABLE_MATCH' || jobOptoin.runCondition === 'CUSTOM_VARIABLE_MATCH_NOT_RUN'))
        }
    },
    customCondition: {
        isHidden: true,
        default: ''
    }
}

export const JOB_MUTUAL = {
    enable: {
        default: false
    },
    mutexGroupName: {
        rule: {
            mutualGroup: true
        },
        component: 'vuex-input',
        label: '互斥组名称',
        placeholder: '请填写互斥组名称',
        default: '',
        required: true
    },
    queueEnable: {
        rule: {},
        type: 'boolean',
        component: 'atom-checkbox',
        text: '启用互斥组排队',
        default: false
    },
    timeout: {
        rule: { 'numeric': true, 'max_value': 2880, 'min_value': 1 },
        component: 'vuex-input',
        label: '最长等待时间（单位分钟）',
        placeholder: '请填写1-2880之间的整数',
        default: '900',
        required: true,
        isHidden: (mutexGroup) => {
            return !(mutexGroup && mutexGroup.queueEnable)
        }
    },
    queue: {
        rule: { 'numeric': true, 'max_value': 10, 'min_value': 1 },
        component: 'vuex-input',
        label: '队列最大任务数',
        placeholder: '请填写1-10之间的整数',
        default: '5',
        required: true,
        isHidden: (mutexGroup) => {
            return !(mutexGroup && mutexGroup.queueEnable)
        }
    }
}
