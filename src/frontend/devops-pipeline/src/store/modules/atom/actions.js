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
    STORE_API_URL_PREFIX,
    LOG_API_URL_PREFIX,
    MACOS_API_URL_PREFIX
} from '@/store/constants'
import {
    SET_STAGE_TAG_LIST,
    SET_PIPELINE_STAGE,
    SET_COMMON_SETTING,
    SET_PIPELINE_CONTAINER,
    SET_TEMPLATE,
    SET_CONTAINER_DETAIL,
    SET_ATOMS,
    SET_ATOM_MODAL,
    SET_ATOM_MODAL_FETCHING,
    UPDATE_ATOM_TYPE,
    UPDATE_ATOM,
    INSERT_ATOM,
    PROPERTY_PANEL_VISIBLE,
    SET_PIPELINE_EDITING,
    DELETE_CONTAINER,
    DELETE_STAGE,
    ADD_CONTAINER,
    DELETE_ATOM,
    UPDATE_CONTAINER,
    ADD_STAGE,
    UPDATE_STAGE,
    SET_INSERT_STAGE_STATE,
    SET_PIPELINE,
    SET_BUILD_PARAM,
    DELETE_ATOM_PROP,
    SET_PIPELINE_EXEC_DETAIL,
    SET_REMOTE_TRIGGER_TOKEN,
    SET_GLOBAL_ENVS,
    TOGGLE_ATOM_SELECTOR_POPUP,
    UPDATE_ATOM_INPUT,
    UPDATE_WHOLE_ATOM_INPUT,
    UPDATE_ATOM_OUTPUT,
    UPDATE_ATOM_OUTPUT_NAMESPACE,
    FETCHING_ATOM_LIST,
    SET_STORE_DATA,
    SET_STORE_LOADING,
    SET_STORE_SEARCH,
    FETCHING_ATOM_VERSION,
    SET_ATOM_VERSION_LIST,
    SET_EXECUTE_STATUS,
    SET_SAVE_STATUS,
    SET_DEFAULT_STAGE_TAG,
    TOGGLE_STAGE_REVIEW_PANEL,
    SET_IMPORTED_JSON,
    SET_EDIT_FROM
} from './constants'
import { PipelineEditActionCreator, actionCreator } from './atomUtil'
import { hashID, randomString } from '@/utils/util'

function rootCommit (commit,
    ACTION_CONST, payload) {
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
    triggerStage ({ commit }, { projectId, pipelineId, buildNo, stageId, cancel, reviewParams, id, suggest }) {
        return request.post(`/${PROCESS_API_URL_PREFIX}/user/builds/projects/${projectId}/pipelines/${pipelineId}/builds/${buildNo}/stages/${stageId}/manualStart?cancel=${cancel}`, { reviewParams, id, suggest })
    },
    async fetchStageTagList ({ commit }) {
        try {
            const res = await request.get(`/${PROCESS_API_URL_PREFIX}/user/pipelines/stageTag`)
            const defaultStageTag = res.data.filter(item => item.defaultFlag).map(item => item.id)
            commit(SET_STAGE_TAG_LIST, res.data)
            commit(SET_DEFAULT_STAGE_TAG, defaultStageTag)
        } catch (error) {
            console.log(error)
        }
    },
    async fetchCommonSetting ({ commit }) {
        try {
            const res = await request.get(`/${PROCESS_API_URL_PREFIX}/user/setting/common/get`)
            commit(SET_COMMON_SETTING, res.data)
        } catch (error) {
            console.log(error)
        }
    },
    setExecuteStatus ({ commit }, status) {
        commit(SET_EXECUTE_STATUS, status)
    },
    setSaveStatus ({ commit }, status) {
        commit(SET_SAVE_STATUS, status)
    },
    toggleStageReviewPanel: actionCreator(TOGGLE_STAGE_REVIEW_PANEL),
    addStoreAtom ({ commit, state }) {
        const store = state.storeAtomData || {}
        let page = store.page || 1
        const pageSize = store.pageSize || 1500
        const keyword = store.keyword || undefined
        const loadEnd = store.loadEnd || false
        const loading = store.loading || false
        if (loadEnd || loading) return

        commit(SET_STORE_LOADING, true)
        return request.get(`${STORE_API_URL_PREFIX}/user/market/atom/list`, { params: { page, pageSize, keyword } }).then((res) => {
            const data = res.data || {}
            const records = data.records || []
            const atomList = store.data || []
            const storeData = {
                data: [...atomList, ...records],
                page: ++page,
                pageSize: 2000,
                loadEnd: records.length < pageSize,
                loading: false,
                keyword
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

    setPipelineStage ({ commit }, stages) {
        commit(SET_PIPELINE_STAGE, stages)
    },

    setPipelineContainer ({ commit }, { oldContainers, containers }) {
        commit(SET_PIPELINE_CONTAINER, { oldContainers, containers })
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
    getCheckAtomInfo: ({ commit }, { projectId, pipelineId, buildId, elementId }) => {
        return request.get(`/${PROCESS_API_URL_PREFIX}/user/builds/${projectId}/${pipelineId}/${buildId}/${elementId}/toReview`).then(response => {
            return response.data
        })
    },
    handleCheckAtom: ({ commit }, { projectId, pipelineId, buildId, elementId, postData }) => {
        return request.post(`/${PROCESS_API_URL_PREFIX}/user/builds/${projectId}/${pipelineId}/${buildId}/${elementId}/review/`, postData).then(response => {
            return response.data
        })
    },
    requestPipeline: async ({ commit, dispatch, getters }, { projectId, pipelineId }) => {
        try {
            const [pipelineRes, atomPropRes] = await Promise.all([
                request.get(`/${PROCESS_API_URL_PREFIX}/user/pipelines/${projectId}/${pipelineId}`),
                request.get(`/${PROCESS_API_URL_PREFIX}/user/pipeline/projects/${projectId}/pipelines/${pipelineId}/atom/prop/list`)
            ])
            const pipeline = pipelineRes.data
            const atomProp = atomPropRes.data
            const elements = getters.getAllElements(pipeline.stages)
            elements.forEach(element => { // 将os属性设置到model内
                Object.assign(element, {
                    ...atomProp[element.atomCode]
                })
            })
            dispatch('setPipeline', pipeline)
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
    setEditFrom: actionCreator(SET_EDIT_FROM),
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
                        projectCode,
                        page: 1,
                        pageSize: 100
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
    setInsertStageState: actionCreator(SET_INSERT_STAGE_STATE),
    addStage: PipelineEditActionCreator(ADD_STAGE),
    deleteStage: ({ commit }, payload) => {
        commit(DELETE_STAGE, payload)
        commit(SET_PIPELINE_EDITING, true)
    },
    addContainer: ({ commit, getters }, { type, ...restPayload }) => {
        const newContainer = getters.getContainerModalByType(type)
        if (newContainer) {
            const { name, required, typeList, type, baseOS, defaultBuildType, defaultPublicBuildResource = '', ...restProps } = newContainer
            const defaultType = (typeList || []).find(type => type.type === defaultBuildType) || {}
            const defaultBuildResource = defaultType.defaultBuildResource || {}
            const baseOSObject = baseOS !== 'NONE' ? { baseOS } : {}
            const isError = ['WINDOWS', 'MACOS'].includes(baseOS)
            commit(ADD_CONTAINER, {
                ...restPayload,
                newContainer: {
                    '@type': type,
                    name,
                    ...restProps,
                    ...baseOSObject,
                    dispatchType: {
                        buildType: defaultBuildType,
                        imageVersion: defaultBuildResource.version || '',
                        value: defaultBuildResource.code || defaultPublicBuildResource || '',
                        imageCode: defaultBuildResource.code || '',
                        imageName: defaultBuildResource.name || '',
                        recommendFlag: defaultBuildResource.recommendFlag,
                        imageType: 'BKSTORE'
                    },
                    elements: [],
                    containerId: `c-${hashID(32)}`,
                    jobId: `job_${randomString(3)}`,
                    nfsSwitch: false,
                    isError
                }
            })
            commit(SET_PIPELINE_EDITING, true)
        }
    },
    deleteContainer: ({ commit }, payload) => {
        commit(DELETE_CONTAINER, payload)
        commit(SET_PIPELINE_EDITING, true)
    },
    updateContainer: PipelineEditActionCreator(UPDATE_CONTAINER),
    updateStage: PipelineEditActionCreator(UPDATE_STAGE),
    addAtom: ({ commit }, { stageIndex, containerIndex, atomIndex, container }) => {
        const insertIndex = atomIndex + 1
        commit(INSERT_ATOM, {
            elements: container.elements,
            insertIndex
        })
        commit(PROPERTY_PANEL_VISIBLE, {
            isShow: true,
            isComplete: false,
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
    updateAtom: (action, { element: atom, newParam }) => {
        PipelineEditActionCreator(UPDATE_ATOM)(action, { atom, newParam })
    },
    updateAtomInput: PipelineEditActionCreator(UPDATE_ATOM_INPUT),
    updateWholeAtomInput: PipelineEditActionCreator(UPDATE_WHOLE_ATOM_INPUT),
    updateAtomOutput: PipelineEditActionCreator(UPDATE_ATOM_OUTPUT),
    updateAtomOutputNameSpace: PipelineEditActionCreator(UPDATE_ATOM_OUTPUT_NAMESPACE),
    deleteAtomProps: PipelineEditActionCreator(DELETE_ATOM_PROP),
    togglePropertyPanel: ({ commit }, payload) => {
        // if (payload.isShow && !payload.editingElementPos) {
        //     return
        // }
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
    toggleAtomSelectorPopup: actionCreator(TOGGLE_ATOM_SELECTOR_POPUP),

    // 安装插件
    installAtom ({ dispatch }, param) {
        return request.post(`${STORE_API_URL_PREFIX}/user/market/atom/install`, param).then(() => dispatch('fetchAtoms', { projectCode: param.projectCode[0] }))
    },

    getAtomEnvConfig ({ commit }, atomCode) {
        return request.get(`${STORE_API_URL_PREFIX}/user/market/ATOM/component/${atomCode}/sensitiveConf/list/?types=FRONTEND,ALL`).then((res) => {
            return res.data || []
        })
    },

    // 获取项目下已安装的插件列表
    getInstallAtomList ({ commit }, projectCode) {
        return request.get(`${STORE_API_URL_PREFIX}/user/pipeline/atom/projectCodes/${projectCode}/list?page=1&pageSize=2000`)
    },

    // 获取已安装的插件详情
    getInstallAtomDetail ({ commit }, { projectCode, atomCode }) {
        return request.get(`${STORE_API_URL_PREFIX}/user/market/atom/statistic/projectCodes/${projectCode}/atomCodes/${atomCode}/pipelines`)
    },

    // 卸载插件
    unInstallAtom ({ commit }, { projectCode, atomCode, reasonList }) {
        return request.delete(`${STORE_API_URL_PREFIX}/user/pipeline/atom/projectCodes/${projectCode}/atoms/${atomCode}`, { data: { reasonList } })
    },

    // 获取卸载原因
    getDeleteReasons () {
        return request.get(`${STORE_API_URL_PREFIX}/user/store/reason/types/UNINSTALLATOM`)
    },

    // 获取分类
    getAtomClassify () {
        return request.get(`${STORE_API_URL_PREFIX}/user/pipeline/atom/classify`)
    },

    // 第一次拉取日志

    getInitLog ({ commit }, { projectId, pipelineId, buildId, tag, jobId, currentExe, subTag, debug }) {
        return request.get(`${API_URL_PREFIX}/${LOG_API_URL_PREFIX}/user/logs/${projectId}/${pipelineId}/${buildId}`, {
            params: {
                tag,
                jobId,
                executeCount: currentExe,
                subTag,
                debug
            }
        })
    },

    // 后续拉取日志
    getAfterLog ({ commit }, { projectId, pipelineId, buildId, tag, jobId, currentExe, lineNo, subTag, debug }) {
        return request.get(`${API_URL_PREFIX}/${LOG_API_URL_PREFIX}/user/logs/${projectId}/${pipelineId}/${buildId}/after`, {
            params: {
                start: lineNo,
                executeCount: currentExe,
                tag,
                jobId,
                subTag,
                debug
            }
        })
    },

    fetchDevcloudSettings ({ commit }, { projectId, buildType }) {
        return request.get(`/dispatch-docker/api/user/dispatch-docker/resource-config/projects/${projectId}/list?buildType=${buildType}`)
    },

    getLogStatus ({ commit }, { projectId, pipelineId, buildId, tag, jobId, executeCount }) {
        return request.get(`${API_URL_PREFIX}/${LOG_API_URL_PREFIX}/user/logs/${projectId}/${pipelineId}/${buildId}/mode`, { params: { tag, jobId, executeCount } })
    },

    getDownloadLogFromArtifactory ({ commit }, { projectId, pipelineId, buildId, tag, executeCount }) {
        return request.get(`${API_URL_PREFIX}/artifactory/api/user/artifactories/log/plugin/${projectId}/${pipelineId}/${buildId}/${tag}/${executeCount}`).then((res) => {
            const data = res.data || {}
            return data.url || ''
        })
    },

    getMacSysVersion () {
        return request.get(`${MACOS_API_URL_PREFIX}/user/systemVersions`)
    },

    getMacXcodeVersion () {
        return request.get(`${MACOS_API_URL_PREFIX}/user/xcodeVersions`)
    },
    setImportedPipelineJson ({ commit }, importedJson) {
        commit(SET_IMPORTED_JSON, importedJson)
    },

    pausePlugin ({ commit }, { projectId, pipelineId, buildId, taskId, isContinue, stageId, containerId, element }) {
        return request.post(`${PROCESS_API_URL_PREFIX}/user/builds/projects/${projectId}/pipelines/${pipelineId}/builds/${buildId}/taskIds/${taskId}/execution/pause?isContinue=${isContinue}&stageId=${stageId}&containerId=${containerId}`, element)
    },

    download (_, { url, name }) {
        return fetch(url, { credentials: 'include' }).then((res) => {
            if (res.status >= 200 && res.status < 300) {
                return res.blob()
            } else {
                return res.json().then((result) => Promise.reject(result))
            }
        }).then((blob) => {
            const a = document.createElement('a')
            const url = window.URL || window.webkitURL || window.moxURL
            a.href = url.createObjectURL(blob)
            if (name) a.download = name
            document.body.appendChild(a)
            a.click()
            document.body.removeChild(a)
        })
    },
    reviewExcuteAtom: async ({ commit }, { projectId, pipelineId, buildId, elementId, action }) => {
        return request.post(`/${PROCESS_API_URL_PREFIX}/user/quality/builds/${projectId}/${pipelineId}/${buildId}/${elementId}/qualityGateReview/${action}`).then(response => {
            return response.data
        })
    }
}
