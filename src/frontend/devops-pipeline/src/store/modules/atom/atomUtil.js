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

import { VM_CONTAINER_TYPE, TRIGGER_CONTAINER_TYPE, NORMAL_CONTAINER_TYPE, SET_PIPELINE_EDITING, ADD_STAGE, UPDATE_ATOM_OUTPUT_NAMESPACE } from './constants'

/**
 * 获取原子模型unique Key
 * @param {*} atomCode 原子标识
 * @param {*} atomVersion 原子版本
 */
export function getAtomModalKey (atomCode, atomVersion) {
    return `${atomCode}-${atomVersion}`
}

/**
 * 根据原子模型获取原子默认值
 * @param {*} atomProps 原子模型表单对象
 */
export function getAtomDefaultValue (atomProps = {}) {
    return Object.keys(atomProps).reduce((formProps, key) => {
        formProps[key] = atomProps[key].default
        return formProps
    }, {})
}

/**
 * 判断是否为构建环境
 * @param {*} containerType contatiner类型
 */
export function isVmContainer (containerType) {
    return containerType === VM_CONTAINER_TYPE
}
/**
 * 判断是否为触发器
 * @param {*} containerType contatiner类型
 */
export function isTriggerContainer (containerType) {
    return containerType === TRIGGER_CONTAINER_TYPE
}

/**
 * 判断是否为无构建环境
 * @param {*} containerType contatiner类型
 */
export function isNormalContainer (containerType) {
    return containerType === NORMAL_CONTAINER_TYPE
}

/**
 * 判断原子是否是拉代码原子
 * @param {*} atomCode  原子类型
 */
export function isCodePullAtom (atomCode) {
    return ['CODE_SVN', 'CODE_GIT', 'CODE_GITLAB'].includes(atomCode)
}

/**
 * 更新动作模板
 * @param {STRING} mutation 更新动作常量
 */
export function actionCreator (mutation) {
    return ({ commit }, payload = {}) => {
        commit(mutation, payload)
    }
}

/**
 *  更新动作模板 【同时触发流水线更新操作】
 * @param {STRING} mutation 更新动作常量
 */
export function PipelineEditActionCreator (mutation) {
    return ({ commit }, payload = {}) => {
        if (payload.container && payload.newParam) {
            if (compareParam(payload.newParam, payload.container)) {
                commit(SET_PIPELINE_EDITING, true)
            }
        } else if (payload.element && payload.newParam) {
            if (compareParam(payload.newParam, payload.element)) {
                commit(SET_PIPELINE_EDITING, true)
            }
        } else if (payload.atom && payload.newParam) {
            if (compareParam(payload.newParam, payload.atom)) {
                commit(SET_PIPELINE_EDITING, true)
            }
        } else if ([UPDATE_ATOM_OUTPUT_NAMESPACE, ADD_STAGE].includes(mutation)) {
            commit(SET_PIPELINE_EDITING, true)
        }
        commit(mutation, payload)
    }
}

/**
 * 对比是否更新
 * @param {Object} param 更新的参数
 * @param {Object} originElement 目标对象
 */
export function compareParam (param, originElement) {
    return Object.keys(param).some(key => {
        return param[key] !== originElement[key] && key !== 'isError'
    })
}

/**
 * 转化原子对象
 * @param {*} props 新的原子JSON串
 */
export function transformAtomModalProps (props = {}) {
    return Object.keys(props).reduce((newProps, key) => {
        const prop = props[key]
        newProps[key] = {
            ...prop,
            ...prop.options_conf,
            component: prop.type
        }
        return newProps
    }, {})
}

/**
 * 获取原子输出对象
 * @param {*} output 原子输出
 */
export function getAtomOutputObj (output = {}) {
    try {
        const outputObj = {}
        for (const key in output) {
            if (output.hasOwnProperty(key)) {
                outputObj[key] = output[key].type
            }
        }
        return outputObj
    } catch (e) {
        console.warn('获取原子输出对象出错', output)
        return {}
    }
}

export function isNewAtomTemplate (htmlTemplateVersion) {
    return htmlTemplateVersion !== '1.0'
}
