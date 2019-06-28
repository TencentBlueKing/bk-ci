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

import request from '@/utils/request'
import {
    FETCH_ERROR,
    PROCESS_API_URL_PREFIX,
    STORE_API_URL_PREFIX
} from '@/store/constants'
import { SET_TEMPLATE, SET_CONTAINER_DETAIL, SET_ATOMS, SET_ATOM_MODAL, SET_ATOM_MODAL_FETCHING, UPDATE_ATOM_TYPE, UPDATE_ATOM, INSERT_ATOM, PROPERTY_PANEL_VISIBLE, SET_PIPELINE_EDITING, DELETE_CONTAINER, DELETE_STAGE, ADD_CONTAINER, DELETE_ATOM, UPDATE_CONTAINER, ADD_STAGE, CONTAINER_TYPE_SELECTION_VISIBLE, SET_INSERT_STAGE_INDEX, SET_PIPELINE, SET_BUILD_PARAM, DELETE_ATOM_PROP, SET_PIPELINE_EXEC_DETAIL, SET_REMOTE_TRIGGER_TOKEN, SET_GLOBAL_ENVS, TOGGLE_ATOM_SELECTOR_POPUP, UPDATE_ATOM_INPUT, UPDATE_ATOM_OUTPUT, UPDATE_ATOM_OUTPUT_NAMESPACE, FETCHING_ATOM_LIST, SET_STORE_DATA, SET_STORE_LOADING, SET_STORE_SEARCH, FETCHING_ATOM_VERSION, SET_ATOM_VERSION_LIST, SET_EXECUTE_STATUS, SET_SAVE_STATUS } from './constants'
import { PipelineEditActionCreator, actionCreator } from './atomUtil'

function rootCommit (commit, ACTION_CONST, payload) {
    commit(ACTION_CONST, payload, { root: true })
}

function getMapByKey (list, key) {
    const keyList = []
    const objMap = list.reduce((objMap, item) => {
        objMap[item[key]] = item
        keyList.push(item[key])
        return objMap
    }, {})
    return [keyList, objMap]
}

export default {
    setExecuteStatus ({ commit }, status) {
        commit(SET_EXECUTE_STATUS, status)
    },
    setSaveStatus ({ commit }, status) {
        commit(SET_SAVE_STATUS, status)
    },
    addStoreAtom ({ commit, state }) {
        const store = state.storeAtomData || {}
        let page = store.page || 1
        const pageSize = store.pageSize || 40
        const atomName = store.atomName || undefined
        const loadEnd = store.loadEnd || false
        const loading = store.loading || false
        if (loadEnd || loading) return

        commit(SET_STORE_LOADING, true)
        return request.get(`${STORE_API_URL_PREFIX}/user/market/atom/list`, { params: { page, pageSize, atomName } }).then((res) => {
            const data = res.data || {}
            const records = data.records || []
            const atomList = store.data || []
            const storeData = {
                data: [...atomList, ...records],
                page: ++page,
                pageSize: 40,
                loadEnd: records.length < pageSize,
                loading: false,
                atomName
            }
            commit(SET_STORE_DATA, storeData)
        }).catch((e) => {
            if (e.code === 403) e.message = ''
            rootCommit(commit, FETCH_ERROR, e)
        }).finally(() => {
            commit(SET_STORE_LOADING, false)
        })
    },

    setStoreSearch ({ commit }, str) {
        commit(SET_STORE_SEARCH, str)
    },

    clearStoreAtom ({ commit }) {
        commit(SET_STORE_DATA, {})
    },

    requestTemplate: async ({ commit, dispatch }, { projectId, templateId, version }) => {
        try {
            const url = version ? `/${PROCESS_API_URL_PREFIX}/user/templates/projects/${projectId}/templates/${templateId}?version=${version}` : `/${PROCESS_API_URL_PREFIX}/user/templates/projects/${projectId}/templates/${templateId}`
            const response = await request.get(url)
            dispatch('setPipeline', response.data.template)
            commit(SET_TEMPLATE, {
                template: response.data
            })
        } catch (e) {
            if (e.code === 403) {
                e.message = ''
            }
            rootCommit(commit, FETCH_ERROR, e)
        }
    },
    handleCheckAtom: ({ commit }, { projectId, pipelineId, buildId, elementId, action }) => {
        return request.post(`/${PROCESS_API_URL_PREFIX}/user/builds/${projectId}/${pipelineId}/${buildId}/${elementId}/review/${action}`).then(response => {
            return response.data
        })
    },
    requestPipeline: async ({ commit, dispatch }, { projectId, pipelineId }) => {
        try {
            const response = await request.get(`/${PROCESS_API_URL_PREFIX}/user/pipelines/${projectId}/${pipelineId}`)
            dispatch('setPipeline', response.data)
        } catch (e) {
            if (e.code === 403) {
                e.message = ''
            }
            rootCommit(commit, FETCH_ERROR, e)
        }
    },
    requestBuildParams: async ({ commit }, { projectId, pipelineId, buildId }) => {
        try {
            const response = await request.get(`/${PROCESS_API_URL_PREFIX}/user/builds/${projectId}/${pipelineId}/${buildId}/parameters`)
            commit(SET_BUILD_PARAM, {
                buildParams: response.data,
                buildId
            })
        } catch (e) {
            rootCommit(commit, FETCH_ERROR, e)
        }
    },
    setPipeline: actionCreator(SET_PIPELINE),
    setPipelineEditing: actionCreator(SET_PIPELINE_EDITING),
    fetchContainers: async ({ commit }, { projectCode }) => {
        try {
            const { data: containers } = await request.get(`${STORE_API_URL_PREFIX}/user/pipeline/container/${projectCode}`)
            const containerList = containers.filter(container => container.type !== 'trigger')
            const triggerContainer = containers.find(container => container.type === 'trigger')
            const [containerTypeList, containerModalMap] = getMapByKey(containerList, 'baseOS')
            commit(SET_CONTAINER_DETAIL, {
                containerTypeList: ['TRIGGER', ...containerTypeList],
                containerModalMap: {
                    ...containerModalMap,
                    TRIGGER: triggerContainer
                }
            })
        } catch (e) {
            rootCommit(commit, FETCH_ERROR, e)
        }
    },
    fetchBuildResourceByType: ({ commit }, { projectCode, containerId, os, buildType }) => {
        return request.get(`${STORE_API_URL_PREFIX}/user/pipeline/container/projects/${projectCode}/containers/${containerId}/oss/${os}?buildType=${buildType}`)
    },
    fetchAtoms: async ({ commit }, { projectCode }) => {
        try {
            commit(FETCHING_ATOM_LIST, true)
            const [{ data: atomClassifyList }, { data: atomList }] = await Promise.all([
                request.get(`${STORE_API_URL_PREFIX}/user/pipeline/atom/classify`),
                request.get(`${STORE_API_URL_PREFIX}/user/pipeline/atom`, {
                    params: {
                        projectCode
                    }
                })
            ])

            const [atomCodeList, atomMap] = getMapByKey(atomList.records, 'atomCode')
            const [atomClassifyCodeList, atomClassifyMap] = getMapByKey(atomClassifyList, 'classifyCode')
            commit(SET_ATOMS, {
                atomCodeList,
                atomClassifyCodeList,
                atomMap,
                atomClassifyMap
            })
        } catch (e) {
            rootCommit(commit, FETCH_ERROR, e)
        } finally {
            commit(FETCHING_ATOM_LIST, false)
        }
    },
    fetchAtomModal: async ({ commit, dispatch }, { projectCode, atomCode, version, atomIndex, container }) => {
        try {
            commit(SET_ATOM_MODAL_FETCHING, true)
            const { data: atomModal } = await request.get(`${STORE_API_URL_PREFIX}/user/pipeline/atom/${projectCode}/${atomCode}/${version}`)
            commit(SET_ATOM_MODAL, {
                atomCode,
                version,
                atomModal
            })
            if (container && typeof atomIndex !== 'undefined') { // 获取并更新原子模型
                dispatch('updateAtomType', { container, atomCode, version, atomIndex })
            }
        } catch (error) {
            rootCommit(commit, FETCH_ERROR, error)
        } finally {
            commit(SET_ATOM_MODAL_FETCHING, false)
        }
    },
    fetchAtomVersionList: async ({ commit }, { projectCode, atomCode }) => {
        try {
            commit(FETCHING_ATOM_VERSION, true)
            const { data: versionList } = await request.get(`${STORE_API_URL_PREFIX}/user/pipeline/atom/projectCodes/${projectCode}/atomCodes/${atomCode}/version/list`)

            commit(SET_ATOM_VERSION_LIST, versionList)
        } catch (error) {
            rootCommit(commit, FETCH_ERROR, error)
        } finally {
            commit(FETCHING_ATOM_VERSION, false)
        }
    },
    setInertStageIndex: actionCreator(SET_INSERT_STAGE_INDEX),
    toggleStageSelectPopup: actionCreator(CONTAINER_TYPE_SELECTION_VISIBLE),
    addStage: PipelineEditActionCreator(ADD_STAGE),
    deleteStage: ({ commit }, payload) => {
        commit(DELETE_STAGE, payload)
        commit(PROPERTY_PANEL_VISIBLE, { isShow: false })
        commit(SET_PIPELINE_EDITING, true)
    },
    addContainer: ({ commit, getters }, { type, ...restPayload }) => {
        const newContainer = getters.getContainerModalByType(type)
        if (newContainer) {
            const { name, required, typeList, type, baseOS, defaultBuildType, defaultPublicBuildResource = '', ...restProps } = newContainer
            const baseOSObject = baseOS !== 'NONE' ? { baseOS } : {}
            commit(ADD_CONTAINER, {
                ...restPayload,
                newContainer: {
                    '@type': type,
                    name,
                    ...restProps,
                    ...baseOSObject,
                    dispatchType: {
                        buildType: defaultBuildType,
                        value: defaultPublicBuildResource
                    },
                    elements: [],
                    isError: true
                }
            })
            commit(SET_PIPELINE_EDITING, true)
        }
    },
    deleteContainer: ({ commit }, payload) => {
        commit(DELETE_CONTAINER, payload)
        commit(PROPERTY_PANEL_VISIBLE, { isShow: false })
        commit(SET_PIPELINE_EDITING, true)
    },
    updateContainer: PipelineEditActionCreator(UPDATE_CONTAINER),
    addAtom: ({ commit }, { stageIndex, containerIndex, atomIndex, container }) => {
        const insertIndex = atomIndex + 1
        commit(INSERT_ATOM, {
            elements: container.elements,
            insertIndex
        })
        commit(PROPERTY_PANEL_VISIBLE, {
            isShow: true,
            editingElementPos: {
                stageIndex: stageIndex,
                containerIndex: containerIndex,
                elementIndex: insertIndex
            }
        })
        commit(SET_PIPELINE_EDITING, true)
    },
    deleteAtom: ({ commit }, { container, atomIndex }) => {
        commit(DELETE_ATOM, { elements: container.elements, atomIndex })
        commit(SET_PIPELINE_EDITING, true)
    },
    updateAtomType: PipelineEditActionCreator(UPDATE_ATOM_TYPE),
    updateAtom: ({ commit }, { element: atom, newParam }) => {
        PipelineEditActionCreator(UPDATE_ATOM)({ commit }, { atom, newParam })
    },
    updateAtomInput: PipelineEditActionCreator(UPDATE_ATOM_INPUT),
    updateAtomOutput: PipelineEditActionCreator(UPDATE_ATOM_OUTPUT),
    updateAtomOutputNameSpace: PipelineEditActionCreator(UPDATE_ATOM_OUTPUT_NAMESPACE),
    deleteAtomProps: PipelineEditActionCreator(DELETE_ATOM_PROP),
    togglePropertyPanel: ({ commit }, payload) => {
        if (payload.isShow && !payload.editingElementPos) {
            return
        }
        commit(PROPERTY_PANEL_VISIBLE, payload)
    },
    requestPipelineExecDetail: async ({ commit, dispatch }, { projectId, buildNo, pipelineId }) => {
        try {
            const response = await request.get(`${PROCESS_API_URL_PREFIX}/user/builds/${projectId}/${pipelineId}/${buildNo}/detail`)
            dispatch('setPipelineDetail', response.data)
        } catch (e) {
            if (e.code === 403) {
                e.message = ''
            }
            rootCommit(commit, FETCH_ERROR, e)
        }
    },
    requestPipelineExecDetailByBuildNum: async ({ commit, dispatch }, { projectId, buildNum, pipelineId }) => {
        try {
            return request.get(`${PROCESS_API_URL_PREFIX}/user/builds/${projectId}/${pipelineId}/detail/${buildNum}`)
        } catch (e) {
            if (e.code === 403) {
                e.message = ''
            }
            rootCommit(commit, FETCH_ERROR, e)
        }
    },
    setPipelineDetail: actionCreator(SET_PIPELINE_EXEC_DETAIL),
    getRemoteTriggerToken: async ({ commit }, { projectId, pipelineId, element, preToken }) => {
        try {
            const { data: { token } } = await request.put(`${PROCESS_API_URL_PREFIX}/user/pipelines/${projectId}/${pipelineId}/remoteToken`)
            if (preToken !== token) {
                commit(SET_REMOTE_TRIGGER_TOKEN, { atom: element, token })
                commit(SET_PIPELINE_EDITING, true)
            }
        } catch (e) {
            rootCommit(commit, FETCH_ERROR, e)
        }
    },
    requestGlobalEnvs: async ({ commit }) => {
        try {
            const response = await request.get(`/${PROCESS_API_URL_PREFIX}/user/buildParam`)
            commit(SET_GLOBAL_ENVS, response.data)
        } catch (e) {
            rootCommit(commit, FETCH_ERROR, e)
        }
    },
    toggleAtomSelectorPopup: actionCreator(TOGGLE_ATOM_SELECTOR_POPUP)
}
