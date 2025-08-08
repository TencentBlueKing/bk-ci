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
    BACKEND_API_URL_PREFIX,
    FETCH_ERROR,
    PROCESS_API_URL_PREFIX,
    STORE_API_URL_PREFIX,
    AUTH_URL_PREFIX
} from '@/store/constants'
import ajax from '@/utils/request'

// import axios from 'axios'
// const CancelToken = axios.CancelToken
import { PIPELINE_AUTHORITY_MUTATION, RESET_TEMPLATE_SETTING_MUNTATION, TEMPLATE_SETTING_MUTATION, UPDATE_TEMPLATE_SETTING_MUNTATION } from './constants'

const prefix = `/${PROCESS_API_URL_PREFIX}/user/pipelines/`
const versionPrefix = `/${PROCESS_API_URL_PREFIX}/user/version`
const triggerPrefix = `/${PROCESS_API_URL_PREFIX}/user/trigger/event`
const backpre = `${BACKEND_API_URL_PREFIX}/api`

function rootCommit (commit, ACTION_CONST, payload) {
    commit(ACTION_CONST, payload, { root: true })
}

const state = {
    hasCreatePermission: false,
    templateSetting: {},
    pipelineAuthority: {},
    pipelineActionState: {
        activePipeline: null,
        isConfirmShow: false,
        confirmType: '',
        activePipelineList: [],
        isSaveAsTemplateShow: false,
        isCopyDialogShow: false,
        addToDialogShow: false,
        isDisableDialogShow: false,
        isArchiveDialogShow: false,
        isShowDeleteArchivedDialog: false
    }
}

const mutations = {
    [PIPELINE_AUTHORITY_MUTATION]: (state, { pipelineAuthority }) => {
        return Object.assign(state, {
            pipelineAuthority: {
                ...state.pipelineAuthority,
                ...pipelineAuthority
            }
        })
    },
    [RESET_TEMPLATE_SETTING_MUNTATION]: (state, payload) => {
        return Object.assign(state, {
            templateSetting: {}
        })
    },
    [TEMPLATE_SETTING_MUTATION]: (state, { templateSetting }) => {
        return Object.assign(state, {
            templateSetting
        })
    },
    [UPDATE_TEMPLATE_SETTING_MUNTATION]: (state, { templateSetting, param }) => {
        Object.assign(templateSetting, param)
        return state
    },
    updateCreatePermission (state, hasPermission) {
        state.hasCreatePermission = hasPermission
    },

    updatePipelineActionState (state, params) {
        Object.assign(state.pipelineActionState, params)
    }
}

const actions = {
    requestImageDetail ({ commit }, { code }) {
        return ajax.get(`/${STORE_API_URL_PREFIX}/user/market/image/imageCodes/${code}`)
    },
    requestImageHistory ({ commit }, { agentType, value }) {
        return ajax.get(`/${STORE_API_URL_PREFIX}/user/market/history/transfer?agentType=${agentType}&value=${value}`)
    },
    requestImageVersionlist ({ commit }, { projectCode, imageCode }) {
        return ajax.get(`/${STORE_API_URL_PREFIX}/user/market/projectCodes/${projectCode}/imageCodes/${imageCode}/version/list`)
    },

    requestInstallImage ({ commit }, params) {
        return ajax.post(`/${STORE_API_URL_PREFIX}/user/market/image/install`, params)
    },
    requestImageVersion ({ commit }, imageCode) {
        return ajax.get(`/${STORE_API_URL_PREFIX}/user/market/image/imageCodes/${imageCode}/version/list`)
    },

    requestMarketImage ({ commit }, { projectCode, agentType, keyword, recommendFlag }) {
        return ajax.post(`/${STORE_API_URL_PREFIX}/user/market/image/jobMarketImages/search?projectCode=${projectCode}&keyword=${keyword}&agentType=${agentType}&recommendFlag=${recommendFlag}&page=1&pageSize=1000`)
    },

    requestImageClassifys ({ commit }) {
        return ajax.get(`/${STORE_API_URL_PREFIX}/user/market/image/classifys`)
    },

    requestInstallImageList ({ commit }, { projectCode, agentType, recommendFlag, id, page, pageSize }) {
        return ajax.get(`/${STORE_API_URL_PREFIX}/user/market/image/availableImages?projectCode=${projectCode}&agentType=${agentType}&recommendFlag=${recommendFlag}&classifyId=${id}&page=${page}&pageSize=${pageSize}`)
    },

    requestStoreImageList ({ commit }, { projectCode, agentType, recommendFlag, page, pageSize }) {
        return ajax.get(`/${STORE_API_URL_PREFIX}/user/market/image/jobMarketImages?projectCode=${projectCode}&agentType=${agentType}&recommendFlag=${recommendFlag}&page=${page}&pageSize=${pageSize}`)
    },
    requestProjectGroupAndUsers: async ({ commit }, { projectId }) => {
        try {
            const response = await ajax.get(`/experience/api/user/groups/${projectId}/projectGroupAndUsers`)
            return response.data
        } catch (e) {
            if (e.code === 403) {
                e.message = ''
            }
        }
    },
    requestTemplateSetting: async ({ commit }, { projectId, templateId }) => {
        try {
            const response = await ajax.get(`/${PROCESS_API_URL_PREFIX}/user/templates/projects/${projectId}/templates/${templateId}/settings`)
            commit(TEMPLATE_SETTING_MUTATION, {
                templateSetting: response.data
            })
        } catch (e) {
            if (e.code === 403) {
                e.message = ''
            }
            rootCommit(commit, FETCH_ERROR, e)
        }
    },
    updateTemplateSetting: ({ commit }, payload) => {
        commit(UPDATE_TEMPLATE_SETTING_MUNTATION, payload)
    },
    updatePipelineAuthority: ({ commit }, payload) => {
        commit(PIPELINE_AUTHORITY_MUTATION, payload)
    },

    requestHasCreatePermission (state, { projectId }) {
        return ajax.get(`${prefix}${projectId}/hasCreatePermission`).then(response => {
            return response.data
        })
    },
    /**
     * 收藏与取消收藏流水线
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     * @param {string} projectId 项目 id
     *
     * @return {Promise} promise 对象
     */
    requestToggleCollect ({ commit, state, dispatch }, { projectId, pipelineId, isCollect }) {
        return ajax.put(`${prefix}${projectId}/${pipelineId}/favor?type=${isCollect}`).then(response => {
            return response.data
        })
    },

    lockPipeline ({ commit, state, dispatch }, { projectId, pipelineId, enable }) {
        return ajax.post(`${prefix}/projects/${projectId}/pipelines/${pipelineId}/lock?enable=${enable}`).then(response => {
            return response.data
        })
    },
    /**
     * 获取流水线下拉列表信息
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     * @param {string} projectId 项目 id
     * @param {string} tag 类型tag
     *
     * @return {Promise} promise 对象
     */
    requestPipelinesList ({ commit, state, dispatch }, { projectId, tag, page, pageSize, sortType }) {
        return ajax.get(`${prefix}${projectId}?page=${page}&pageSize=${pageSize}${sortType ? `&sortType=${sortType}` : ''}`).then(response => {
            return response.data
        })
    },
    /**
     * 获取全部流水线列表信息
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     * @param {string} projectId 项目 id
     * @param {string} tag 类型tag
     *
     * @return {Promise} promise 对象
     */
    requestAllPipelinesList ({ commit, state, dispatch }, { projectId, tag, page, pageSize, sortType }) {
        return ajax.get(`${prefix}projects/${projectId}/viewPipelines?page=${page}&pageSize=${pageSize}${sortType ? `&sortType=${sortType}` : ''}`).then(response => {
            return response.data
        })
    },
    searchPipelineList ({ commit, state, dispatch }, { projectId, searchName = '', archiveFlag }) {
        const params = new URLSearchParams()
        params.append('pipelineName', searchName)

        if (archiveFlag !== undefined && archiveFlag !== null) {
            params.append('archiveFlag', archiveFlag)
        }
        
        const url = `/${PROCESS_API_URL_PREFIX}/user/pipelineInfos/${projectId}/searchByName?${params.toString()}`
        return ajax.get(url).then(response => {
            return response.data
        })
    },
    async requestAllPipelinesListByFilter ({ commit }, body) {
        const { projectId, ...query } = body
        const url = `${prefix}projects/${projectId}/listViewPipelines`
        const { data } = await ajax.get(url, {
            params: query
        })
        return data
    },
    /**
     * 获取流水线最近构建状态
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     * @param {string} projectId 项目 id
     * @param {string} ids 查询的id
     *
     * @return {Promise} promise 对象
     */
    requestPipelineStatus ({ commit, state, dispatch }, { projectId, pipelineId }) {
        return ajax.post(`${prefix}${projectId}/pipelineStatus`, pipelineId).then(response => {
            return response.data
        })
    },
    /**
     * 删除流水线
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     * @param {Number} projectId 项目 id
     * @param {Number} pipelineId 流水线 id
     *
     * @return {Promise} promise 对象
     */
    deletePipeline ({ commit, state, dispatch }, { projectId, pipelineId }) {
        return ajax.delete(`${prefix}${projectId}/${pipelineId}`).then(response => {
            return response.data
        })
    },
    patchDeletePipelines (_ctx, body) {
        return ajax.delete(`${prefix}batchDelete`, {
            data: body
        })
    },
    /**
     * 复制流水线
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     * @param {Number} projectId 项目 id
     * @param {Number} pipelineId 流水线 id
     * @param {Object} args 复制需要的参数
     *
     * @return {Promise} promise 对象
     */
    copyPipeline ({ commit, state, dispatch }, { projectId, pipelineId, args }) {
        return ajax.post(`${prefix}${projectId}/${pipelineId}/copy`, args).then(response => {
            return response.data
        })
    },
    /**
     * 流水线另存为模板
     * @param {Number} projectId 项目 id
     * @param {Object} args 需要的参数
     *
     * @return {Promise} promise 对象
     */
    saveAsTemplate (_, { projectId, postData }) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/templates/projects/${projectId}/templates/saveAsTemplate`, postData).then(response => {
            return response.data
        })
    },
    /**
     * 流水线订阅状态
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     * @param {Number} projectId 项目 id
     * @param {Number} pipelineId 流水线 id
     *
     * @return {Promise} promise 对象
     */
    requestSubscribePipeline ({ commit, state, dispatch }, { projectId, pipelineId }) {
        return ajax.get(`${prefix}${projectId}/${pipelineId}/subscription`).then(response => {
            return response.data
        })
    },
    /**
     * 流水线订阅/取消订阅
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     * @param {Number} projectId 项目 id
     * @param {Number} pipelineId 流水线 id
     *
     * @return {Promise} promise 对象
     */
    subscribePipeline ({ commit, state, dispatch }, { projectId, pipelineId, add, type }) {
        return ajax[add ? 'put' : 'delete'](`${prefix}${projectId}/${pipelineId}/subscription${add ? `?type=${type}` : ''}`).then(response => {
            return response.data
        })
    },
    /**
     * setting-data
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     * @param {String} projectId 项目 id
     * @param {String} pipelineId 流水线 id
     *
     * @return {Promise} promise 对象
     */
    requestRoleList ({ commit, state, dispatch }, { projectId, pipelineId }) {
        return ajax.get(`${backpre}/perm/service/pipeline/mgr_resource/permission/?project_id=${projectId}&resource_type_code=pipeline&resource_code=${pipelineId}`).then(response => {
            return response.data
        })
    },

    async fetchRoleList ({ commit, state, dispatch }, { projectId, pipelineId }) {
        try {
            const { data } = await ajax.get(`${backpre}/perm/service/pipeline/mgr_resource/permission/?project_id=${projectId}&resource_type_code=pipeline&resource_code=${pipelineId}`)

            commit(PIPELINE_AUTHORITY_MUTATION, {
                pipelineAuthority: {
                    role: data.role.map(item => {
                        item.selected = item.group_list.map(group => group.group_id)
                        return item
                    }),
                    policy: data.policy.map(item => {
                        item.selected = item.group_list.map(group => group.group_id)
                        return item
                    })
                }
            })
        } catch (e) {
            rootCommit(commit, FETCH_ERROR, e)
        }
    },

    commitSetting ({ commit, state, dispatch }, { projectId, pipelineId, type, role }) {
        return ajax.put(`${PROCESS_API_URL_PREFIX}/backend/api/perm/service/pipeline/mgr_resource/permission`, {
            project_id: projectId,
            resource_code: pipelineId,
            resource_type_code: type,
            role
        }).then(response => {
            return response.data
        })
    },
    requestPiplineCreators ({ commit, state, dispatch }, { projectId, pipelineId, page = 1, pageSize = 15 }) {
        return ajax.get(`${versionPrefix}/projects/${projectId}/pipelines/${pipelineId}/creatorList`, {
            params: {
                page,
                pageSize
            }
        })
    },
    // 流水线历史版本列表
    requestPipelineVersionList (_, { projectId, pipelineId, archiveFlag, ...params }) {
        let url = `${versionPrefix}/projects/${projectId}/pipelines/${pipelineId}/versions`
        if (archiveFlag !== undefined && archiveFlag !== null) {
            url += `?archiveFlag=${encodeURIComponent(archiveFlag)}`
        }
        return ajax.get(url, {
            params
        }).then(res => res.data)
    },
    // 删除流水线历史版本
    deletePipelineVersion (_, { projectId, pipelineId, version }) {
        return ajax.delete(`${prefix}${projectId}/${pipelineId}/${version}`)
    },
    async rollbackPipelineVersion ({ rootCommit }, { projectId, pipelineId, version }) {
        const res = await ajax.post(`${versionPrefix}/projects/${projectId}/pipelines/${pipelineId}/rollbackDraft?version=${version}`)
        return res.data
    },
    updateBuildRemark (_, { projectId, pipelineId, buildId, remark }) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/builds/${projectId}/${pipelineId}/${buildId}/updateRemark`, {
            remark
        })
    },
    // 流水线操作日志列表
    requestPipelineChangelogs (_, { projectId, pipelineId, archiveFlag, ...params }) {
        let url = `${PROCESS_API_URL_PREFIX}/user/version/projects/${projectId}/pipelines/${pipelineId}/operationLog`
        if (archiveFlag !== undefined && archiveFlag !== null) {
            url += `?archiveFlag=${encodeURIComponent(archiveFlag)}`
        }
        return ajax.get(url, {
            params
        }).then(res => res.data)
    },
    // 流水线操作日志列表
    requestPipelineOperatorList (_, { projectId, pipelineId, archiveFlag, ...params }) {
        let url = `${PROCESS_API_URL_PREFIX}/user/version/projects/${projectId}/pipelines/${pipelineId}/operatorList`
        if (archiveFlag !== undefined && archiveFlag !== null) {
            url += `?archiveFlag=${encodeURIComponent(archiveFlag)}`
        }
        return ajax.get(url, {
            params
        }).then(res => res.data)
    },
    // 获取触发事件列表
    getTriggerEventList (_, { projectId, pipelineId, ...params }) {
        return ajax.get(`${triggerPrefix}/${projectId}/${pipelineId}/listPipelineTriggerEvent`, {
            params
        }).then(res => res.data)
    },
    // 获取触发类型列表
    getTriggerTypeList () {
        return ajax.get(`${triggerPrefix}/listTriggerType`).then(res => res.data)
    },
    // 获取事件类型列表
    getEventTypeList () {
        return ajax.get(`${triggerPrefix}/listEventType`).then(res => res.data)
    },
    // 重新触发事件
    reTriggerEvent (_, { projectId, detailId }) {
        return ajax.post(`${triggerPrefix}/${projectId}/${detailId}/replay`)
    },
    getResourceAuthorization (_, { projectId, resourceType, resourceCode }) {
        return ajax.get(`${AUTH_URL_PREFIX}/user/auth/authorization/${projectId}/${resourceType}/getResourceAuthorization?resourceCode=${resourceCode}`)
            .then(res => res.data)
    },
    resetPipelineAuthorization (_, { projectId, params }) {
        return ajax.post(`${AUTH_URL_PREFIX}/user/auth/authorization/${projectId}/resetResourceAuthorization`, params)
            .then(res => res.data)
    }
}

export default {
    namespaced: true,
    state,
    mutations,
    actions
}
