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

import ajax from '@/utils/request'
import {
    PROCESS_API_URL_PREFIX
} from '@/store/constants'

const pipelinePrefix = `/${PROCESS_API_URL_PREFIX}/user/pipelines/`
const buildPrefix = `/${PROCESS_API_URL_PREFIX}/user/builds/`

const state = {
    pipelineList: [],
    curPipeline: {}
}

const getters = {
    getPipelineList: state => state.pipelineList,
    getCurPipeline: state => state.curPipeline
}

const mutations = {
    /**
     * 更新 store.pipeline 中的 pipelineList
     *
     * @param {Object} state store state
     * @param {Array} list pipelineList 列表
     */
    updatePipelineList (state, list) {
        state.pipelineList.splice(0, state.pipelineList.length, ...list)
    },
    /**
     * 更新 store.pipeline 中的 curPipeline
     *
     * @param {Object} state store state
     * @param {Object} obj curPipeline 对象
     */
    updateCurPipeline (state, obj) {
        state.curPipeline = obj
    }
}

const actions = {
    /**
     * 获取客户端流水线
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     * @param {String} projectId 项目 id
     * @param {String} pipelineId 任务 id
     *
     * @return {Promise} promise 对象
     */
    requestPipelinesList ({ commit, state, dispatch }, { projectId, page, pageSize }) {
        return ajax.get(`/${pipelinePrefix}${projectId}?page=${page}&pageSize=${pageSize}`).then(response => {
            return response.data.records
        })
    },
    /**
     * 获取流水线参数
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     * @param {String} projectId 项目 id
     * @param {String} pipelineId 任务 id
     *
     * @return {Promise} promise 对象
     */
    requestPipelinesParam ({ commit, state, dispatch }, { projectId, pipelineId }) {
        return ajax.get(`/${buildPrefix}${projectId}/${pipelineId}/manualStartupInfo`).then(response => {
            return response.data
        })
    },
    /**
     * 启动流水线
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     * @param {String} projectId 项目 id
     * @param {String} pipelineId 任务 id
     *
     * @return {Promise} promise 对象
     */
    requestExecPipeline ({ commit, state, dispatch }, { projectId, pipelineId, params }) {
        return ajax.post(`/${buildPrefix}${projectId}/${pipelineId}`, {
            ...params
        }).then(response => {
            return response.data
        })
    },
    /**
     * 终止流水线
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     * @param {String} projectId 项目 id
     * @param {String} pipelineId 任务 id
     * @param {Number} buildId 任务 id
     *
     * @return {Promise} promise 对象
     */
    requestTerminatePipeline ({ commit, state, dispatch }, { projectId, pipelineId, buildId }) {
        return ajax.delete(`/${buildPrefix}${projectId}/${pipelineId}/${buildId}`).then(response => {
            return response.data
        })
    },
    /**
     * 获取当前流水线的历史记录
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     * @param {String} projectId 项目 id
     * @param {String} pipelineId 任务 id
     *
     * @return {Promise} promise 对象
     */
    requestPipelinesHistory ({ commit, state, dispatch }, { projectId, pipelineId, page, pageSize }) {
        return ajax.get(`/${buildPrefix}${projectId}/${pipelineId}/history?page=${page}&pageSize=${pageSize}`).then(response => {
            return response.data
        })
    }
}

export default {
    namespaced: true,
    state,
    getters,
    mutations,
    actions
}
