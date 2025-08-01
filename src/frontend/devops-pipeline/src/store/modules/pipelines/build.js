/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import {
    PROCESS_API_URL_PREFIX
} from '@/store/constants'
import ajax from '@/utils/request'
import { flatSearchKey, isEmptyObj } from '@/utils/util'

const prefix = `/${PROCESS_API_URL_PREFIX}/user/builds/`
const pluginPrefix = 'plugin/api'

const state = {
    historyPageStatus: {
        isQuerying: false,
        count: 0,
        page: 1,
        pageSize: 20,
        dateTimeRange: [],
        query: {
        },
        searchKey: []
    }
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
    resetHistoryFilterCondition ({ commit, state }, { retainArchiveFlag = false } = {}) {
        const archiveFlag = state.historyPageStatus.query.archiveFlag
        const newQuery = retainArchiveFlag && archiveFlag ? { archiveFlag } : {}

        commit('updateHistoryPageStatus', {
            count: 0,
            dateTimeRange: [],
            page: 1,
            pageSize: 20,
            query: newQuery,
            searchKey: []
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
    requestStartupInfo ({ commit, state, dispatch }, { projectId, pipelineId, ...params }) {
        return ajax.get(`${prefix}${projectId}/${pipelineId}/manualStartupInfo`, {
            params
        }).then(response => {
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
    requestExecPipeline ({ commit, state, dispatch }, { projectId, pipelineId, version, params }) {
        const url = `${prefix}${projectId}/${pipelineId}`
        const query = {
            version
        }
        if (params.buildNo && typeof params.buildNo.currentBuildNo !== 'undefined') {
            Object.assign(query, {
                buildNo: params.buildNo.currentBuildNo
            })
            delete params.buildNo
        }
        console.log('exec', query)
        return ajax.post(url, params, {
            params: query
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
    requestPipelinesHistory ({ commit, state, dispatch }, { projectId, pipelineId, isDebug, archiveFlag: isArchive }) {
        const { historyPageStatus: { query, searchKey, page, pageSize } } = state
        const { archiveFlag, ...otherQuery } = query
        const conditions = {
            ...otherQuery,
            ...flatSearchKey(searchKey)
        }
        const queryMap = new URLSearchParams()
        Object.entries(conditions).forEach(([k, v]) => {
            if (Array.isArray(v)) {
                v.forEach(vv => queryMap.append(k, vv))
            } else queryMap.append(k, v)
        })
        dispatch('setHistoryPageStatus', {
            isQuerying: !isEmptyObj(conditions)
        })

        if (isDebug) {
            queryMap.append('debug', true)
        }
        queryMap.append('page', page)
        queryMap.append('pageSize', pageSize)
        if (isArchive) {
            queryMap.append('archiveFlag', isArchive)
        }
        console.log(conditions, queryMap, `${queryMap}`)
        return ajax.get(`${prefix}${projectId}/${pipelineId}/history/new?${queryMap}`).then(response => {
            return response.data
        })
    },
    /**
     * 获取CodeCC报告
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     * @param {String} projectId 项目 id
     * @param {String} pipelineId 任务 id
     * @param {String} buildId 构建 id
     *
     * @return {Promise} promise 对象
     */
    requestCodeCCReport ({ commit, state, dispatch }, { projectId, pipelineId }) {
        return ajax.get(`${prefix}${projectId}/${pipelineId}/codeccReport`).then(response => {
            return response.data
        })
    },
    /**
     * 获取CodeCC报告
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     * @param {String} buildId 构建 id
     *
     * @return {Promise} promise 对象
     */
    requestNewCodeCCReport ({ commit, state, dispatch }, { buildId }) {
        return ajax.get(`${pluginPrefix}/user/codecc/report/${buildId}`).then(response => {
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
    requestRetryPipeline ({ commit, state, dispatch }, { projectId, pipelineId, buildId, taskId, failedContainer, skip }) {
        const failedContainerStr = failedContainer === undefined ? '' : `&failedContainer=${failedContainer}`
        const queryStr = taskId ? `?taskId=${taskId}${failedContainerStr}&skip=${skip}` : ''
        return ajax.post(`${prefix}${projectId}/${pipelineId}/${buildId}/retry${queryStr}`).then(response => {
            return response.data
        })
    },
    /**
     * 编译加速项信息
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     * @param {String} bsPipelineId 流水线 id
     * @param {String} bsContainerId 原子 id
     * @param {String} bsElementId 原子任务 id
     *
     * @return {Promise} promise 对象
     */
    requestTurboIofo ({ commit, state, dispatch }, { bsPipelineId, bsContainerId, bsElementId }) {
        return ajax.get(`turbo/api/user/turbo/task/pipeline/${bsPipelineId}/${bsElementId}`).then(response => {
            return response
        })
    },
    /**
     * 编译加速开关
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     * @param {String} taskId 加速任务 id
     * @param {Boolean} banAllBooster 开关
     *
     * @return {Promise} promise 对象
     */
    setTurboSwitch ({ commit, state, dispatch }, { taskId, banAllBooster }) {
        return ajax.put(`turbo/api/user/turbo/task/switch/${taskId}/${banAllBooster}`).then(response => {
            return response.data
        })
    },

    /**
     * 同步流水线信息至编译加速
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     * @param {String} pipelineId 流水线 id
     * @return {params} params 对象
     */
    updateToTurbo ({ commit, state, dispatch }, { pipelineId, params }) {
        return ajax.post(`turbo/api/user/turbo/pipeline/synchronization/${pipelineId}`, params).then(response => {
            return response.data
        })
    },
    requestMetadataInfo ({ commit, state, dispatch }, { projectId, artifactoryType, path }) {
        return ajax.get(`artifactory/api/user/artifactories/${projectId}/${artifactoryType}/show?path=${path}`).then(response => {
            return response.data
        })
    },
    /**
     * 重放流水线
     */
    requestRePlayPipeline ({ commit, state, dispatch }, { projectId, pipelineId, buildId, forceTrigger = false }) {
        return ajax.post(`${prefix}${projectId}/${pipelineId}/${buildId}/replayByBuild?forceTrigger=${forceTrigger}`).then(response => {
            return response.data
        }).catch(e => {
            return e
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
