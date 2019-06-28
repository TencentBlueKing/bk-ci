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
import { getQueryParamList } from '../../../utils/util'

const prefix = `/${PROCESS_API_URL_PREFIX}/user/builds/`

const state = {
    historyPageStatus: {
        currentPage: 1,
        scrollTop: 0,
        hasNext: false,
        isQuerying: false,
        queryMap: {
            query: {
                status: [],
                materialAlias: [],
                materialBranch: []
            },
            searchKey: []
        },
        pageSize: 24
    }
}

function flatSearchKey (searchKey) {
    return searchKey.reduce((searchMap, item) => {
        if (Array.isArray(item.values)) {
            if (typeof searchMap[item.id] === 'undefined') {
                searchMap[item.id] = Array.from(new Set(item.values.map(v => v.id)))
            } else {
                searchMap[item.id] = Array.from(new Set([
                    ...searchMap[item.id],
                    ...item.values.map(v => v.id)
                ]))
            }
        }
        return searchMap
    }, {})
}

function generateQueryString (query) {
    return Object.keys(query).map(key => {
        const val = query[key]
        return getQueryParamList(val, key)
    }).filter(item => item).join('&')
}

const getters = {
    getHistoryPageStatus: state => state.historyPageStatus
}

const mutations = {
    updateHistoryPageStatus (state, status) {
        state.historyPageStatus = {
            ...state.historyPageStatus,
            ...status
        }
    }
}

const actions = {
    setHistoryPageStatus ({ commit, state }, newStatus) {
        commit('updateHistoryPageStatus', newStatus)
    },
    resetHistoryFilterCondition ({ commit }) {
        commit('updateHistoryPageStatus', {
            queryMap: {
                query: {
                    status: [],
                    materialAlias: [],
                    materialBranch: []
                },
                searchKey: []
            }
        })
    },
    /**
     * 请求执行参数
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     * @param {Number} projectId 项目 id
     * @param {Number} pipelineId 流水线 id
     *
     * @return {Promise} promise 对象
     */
    requestStartupInfo ({ commit, state, dispatch }, { projectId, pipelineId }) {
        return ajax.get(`${prefix}${projectId}/${pipelineId}/manualStartupInfo`).then(response => {
            return response.data
        })
    },
    /**
     * 请求执行流水线
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     * @param {Number} projectId 项目 id
     * @param {Number} pluginId pipeline id
     *
     * @return {Promise} promise 对象
     */
    requestExecPipeline ({ commit, state, dispatch }, { projectId, pipelineId, params }) {
        return ajax.post(`${prefix}${projectId}/${pipelineId}`, {
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
        return ajax.delete(`${prefix}${projectId}/${pipelineId}/${buildId}`).then(response => {
            return response.data
        })
    },
    /**
     * 获取活动构建Id
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     * @param {String} projectId 项目 id
     * @param {String} pipelineId 任务 id
     *
     * @return {Promise} promise 对象
     */
    requestActiveBuild ({ commit, state, dispatch }, { projectId, pipelineId }) {
        return ajax.get(`${prefix}${projectId}/${pipelineId}/activeBuild`).then(response => {
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
        const { historyPageStatus: { queryMap } } = state
        const filterStr = generateQueryString({
            ...queryMap.query,
            ...flatSearchKey(queryMap.searchKey)
        })
        dispatch('setHistoryPageStatus', {
            isQuerying: !!filterStr
        })
        return ajax.get(`${prefix}${projectId}/${pipelineId}/history/new?page=${page}&pageSize=${pageSize}&${filterStr}`).then(response => {
            return response.data
        })
    },
    requestPipelineHistory ({ commit, state, dispatch }, { projectId, pipelineId, page, pageSize }) {
        return ajax.get(`${prefix}${projectId}/${pipelineId}/history?page=${page}&pageSize=${pageSize}`).then(response => {
            return response.data
        })
    },
    /**
     * 重试流水线
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     * @param {Number} projectId 项目 id
     * @param {Number} pipelineId 任务 id
     * @param {Number} buildId 构建 id
     *
     * @return {Promise} promise 对象
     */
    requestRetryPipeline ({ commit, state, dispatch }, { projectId, pipelineId, buildId, taskId }) {
        return ajax.post(`${prefix}${projectId}/${pipelineId}/${buildId}/retry${taskId ? '?taskId=' + taskId : ''}`).then(response => {
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
