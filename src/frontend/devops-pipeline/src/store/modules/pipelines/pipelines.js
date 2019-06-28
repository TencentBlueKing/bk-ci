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
    FETCH_ERROR,
    PROCESS_API_URL_PREFIX,
    BACKEND_API_URL_PREFIX
} from '@/store/constants'

// import axios from 'axios'
// const CancelToken = axios.CancelToken
import { PIPELINE_SETTING_MUTATION, UPDATE_PIPELINE_SETTING_MUNTATION, PROJECT_GROUP_USERS_MUTATION, RESET_PIPELINE_SETTING_MUNTATION, PIPELINE_AUTHORITY_MUTATION } from './constants'

const prefix = `/${PROCESS_API_URL_PREFIX}/user/pipelines/`
const backpre = `${BACKEND_API_URL_PREFIX}/api`

function rootCommit (commit, ACTION_CONST, payload) {
    commit(ACTION_CONST, payload, { root: true })
}

const state = {
    pipelineList: [],
    curPipeline: {},
    curPipelineAtomParams: {},
    allPipelineList: [],
    hasCreatePermission: false,
    pipelineSetting: {},
    projectGroupAndUsers: [],
    pipelineAuthority: {}
}

const getters = {
    getPipelineList: state => state.pipelineList,
    getCurPipeline: state => state.curPipeline,
    getAllPipelineList: state => state.allPipelineList,
    getCurAtomPrams: state => state.curPipelineAtomParams
}

// function cancellableWrap (commit, actionType) {
//     const source = CancelToken.source()
//     commit('setCancelToken', {
//         token: source,
//         actionType
//     }, { root: true })
//     return source.token
// }

const mutations = {
    [PIPELINE_AUTHORITY_MUTATION]: (state, { pipelineAuthority }) => {
        return Object.assign(state, {
            pipelineAuthority: {
                ...state.pipelineAuthority,
                ...pipelineAuthority
            }
        })
    },
    [RESET_PIPELINE_SETTING_MUNTATION]: (state, payload) => {
        return Object.assign(state, {
            pipelineSetting: {}
        })
    },
    [PROJECT_GROUP_USERS_MUTATION]: (state, { projectGroupAndUsers }) => {
        return Object.assign(state, {
            projectGroupAndUsers
        })
    },
    [PIPELINE_SETTING_MUTATION]: (state, { pipelineSetting }) => {
        return Object.assign(state, {
            pipelineSetting
        })
    },
    [UPDATE_PIPELINE_SETTING_MUNTATION]: (state, { container, param }) => {
        Object.assign(container, param)
        return state
    },
    updateCreatePermission (state, hasPermission) {
        state.hasCreatePermission = hasPermission
    },
    /**
     * 操作 store.pipeline 中的 pipelineList
     *
     * @param {Object} state store state
     * @param {String} type 操作类型，insert，replace，update，remove
     * @param {Object/Array} params 操作后的数据
     * @param {Number} index 操作的位置
     */
    addPipeline (state, { item }) {
        state.allPipelineList.unshift(item)
    },
    /**
     * 更新 store.pipeline 中的 pipelineList
     *
     * @param {Object} state store state
     * @param {Array} list pipelineList 列表
     */
    updateAllPipelineList (state, list) {
        state.allPipelineList.splice(0, state.allPipelineList.length, ...list)
    },
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
     * 删除 store.pipeline 中的某一项
     *
     * @param {Object} state store state
     * @param {String} pipelineId
     */
    removePipelineById (state, pipelineId) {
        state.allPipelineList.map((item, index) => {
            if (item.pipelineId === pipelineId) {
                state.allPipelineList.splice(index, 1)
            }
        })
    },
    /**
     * 更新 store.pipeline 中的 curPipeline
     *
     * @param {Object} state store state
     * @param {Object} obj curPipeline 对象
     */
    updateCurPipeline (state, obj) {
        state.curPipeline = obj
    },
    /**
     * 更新 store.pipeline 中的 curPipeline 的某个字段
     *
     * @param {Object} state store state
     * @param {Object} obj key-value
     */
    updateCurPipelineByKeyValue (state, { key, value }) {
        state.curPipeline[key] = value
    },
    /**
     * 更新 store.pipeline 中的 pipelineList 中的某一项的某个key的value
     *
     * @param {Object} state store state
     * @param {Number} index 更新的位置
     * @param {String} key 要更新的key
     * @param {String} value 要更新的value
     */
    updatePipelineValueById (state, { pipelineId, obj }) {
        const targetPipeline = state.allPipelineList.find(item => item.pipelineId === pipelineId)

        if (!targetPipeline) {
            return
        }
        const target = targetPipeline.feConfig

        if (!target) return

        for (const key in obj) {
            const val = target[key]
            const _target = obj[key]

            if (val instanceof Array) {
                if (_target.index === undefined) {
                    val.splice(0, val.length, ..._target)
                } else {
                    _target.index.map((_index, i) => {
                        val[_index][_target.key[i]] = _target.value[i]
                    })
                }
            } else if (val.toString().toLowerCase() === '[object object]') {
                _target.key.map((item, i) => {
                    val[item] = _target.value[i]
                })
            } else {
                target[key] = _target
            }
        }
    },
    /**
     * 更新 store.curPipelineAtomParams
     *
     * @param {Object} state store state
     * @param {Array} obj curPipelineAtomParams 列表Fha
     */
    updateCurAtomPrams (state, res) {
        state.curPipelineAtomParams = res
    }
}

const actions = {
    resetPipelineSetting: ({ commit }, payload) => {
        commit(RESET_PIPELINE_SETTING_MUNTATION, payload)
    },
    requestPipelineSetting: async ({ commit }, { projectId, pipelineId }) => {
        try {
            const response = await ajax.get(`/${PROCESS_API_URL_PREFIX}/user/setting/get?pipelineId=${pipelineId}&projectId=${projectId}`)
            commit(PIPELINE_SETTING_MUTATION, {
                pipelineSetting: response.data
            })
        } catch (e) {
            if (e.code === 403) {
                e.message = ''
            }
            rootCommit(commit, FETCH_ERROR, e)
        }
    },
    updatePipelineSetting: ({ commit }, payload) => {
        commit(UPDATE_PIPELINE_SETTING_MUNTATION, payload)
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
    // requestAllPipelinesListByFilter ({ commit, state, dispatch }, data) {
    //     let projectId = data.projectId
    //     let sortType = data.sortType || ''
    //     let viewId = data.viewId
    //     let url = `${prefix}projects/${projectId}/viewPipelines?viewId=${viewId}`
    //     let str = ''
    //     for (let obj in data) {
    //         if (obj !== 'viewId') {
    //             str += `&${obj}=${data[obj]}`
    //         }
    //     }
    //     url += str
    //     return ajax.get(url).then(response => {
    //         return response.data
    //     })
    // },
    requestAllPipelinesListByFilter ({ commit, state, dispatch }, data) {
        const projectId = data.projectId
        const viewId = data.viewId
        let url = `${prefix}projects/${projectId}/listViewPipelines?viewId=${viewId}`
        let str = ''
        for (const obj in data) {
            if (obj !== 'viewId' && data[obj]) {
                str += `&${obj}=${data[obj]}`
            }
        }
        url += str
        return ajax.get(url).then(response => {
            return response.data
        })
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
            'project_id': projectId,
            'resource_code': pipelineId,
            'resource_type_code': type,
            role
        }).then(response => {
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
