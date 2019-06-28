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

// 根据配置文件获取原子流程控制选项的默认值
export function getAtomOptionDefault () {
    return Object.keys(ATOM_OPTION).reduce((formProps, key) => {
        if (ATOM_OPTION[key] && typeof ATOM_OPTION[key].default === 'object') {
            formProps[key] = JSON.parse(JSON.stringify(ATOM_OPTION[key].default))
        } else {
            formProps[key] = ATOM_OPTION[key].default
        }
        return formProps
    }, {})
}

export const ATOM_OPTION = {
    enable: {
        rule: {},
        type: 'boolean',
        component: 'atom-checkbox',
        text: '启用本插件',
        default: true
    },
    continueWhenFailed: {
        rule: {},
        type: 'boolean',
        component: 'atom-checkbox',
        text: '失败时继续',
        default: false
    },
    timeout: {
        rule: { 'numeric': true, 'max_value': 2880 },
        component: 'vuex-input',
        label: '插件执行超时时间(单位分钟)',
        desc: '请填写0-2880之间的整数，0表示系统允许的最大超时时间',
        placeholder: '请填写0-2880之间的整数，0表示系统允许的最大超时时间',
        default: '900'
    },
    runCondition: {
        rule: {},
        component: 'selector',
        label: '何时运行本插件',
        default: 'PRE_TASK_SUCCESS',
        list: [
            {
                id: 'PRE_TASK_SUCCESS',
                name: '所有前置插件运行成功时运行'
            },
            {
                id: 'PRE_TASK_FAILED_BUT_CANCEL',
                name: '即使前面有插件运行失败也运行，除非被取消才不运行'
            },
            {
                id: 'PRE_TASK_FAILED_ONLY',
                name: '只有前面有插件运行失败时才运行'
            },
            {
                id: 'CUSTOM_VARIABLE_MATCH',
                name: '自定义变量全部满足时运行'
            },
            {
                id: 'CUSTOM_VARIABLE_MATCH_NOT_RUN',
                name: '自定义变量全部满足时不运行'
            }
        ]
    },
    customVariables: {
        rule: {},
        component: 'key-value-normal',
        default: [{ key: 'param1', value: '' }],
        allowNull: false,
        label: '自定义变量',
        isHidden: (element) => {
            return !(element.additionalOptions && (element.additionalOptions.runCondition === 'CUSTOM_VARIABLE_MATCH' || element.additionalOptions.runCondition === 'CUSTOM_VARIABLE_MATCH_NOT_RUN'))
        }
    },
    otherTask: {
        isHidden: true,
        default: ''
    },
    customCondition: {
        isHidden: true,
        default: ''
    }
}
