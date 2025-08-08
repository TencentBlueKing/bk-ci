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
    FETCH_ERROR,
    LOG_API_URL_PREFIX,
    MACOS_API_URL_PREFIX,
    PROCESS_API_URL_PREFIX,
    STORE_API_URL_PREFIX,
    UPDATE_PIPELINE_MODE
} from '@/store/constants'
import { UI_MODE, CODE_MODE } from '@/utils/pipelineConst'
import request from '@/utils/request'
import { hashID, randomString } from '@/utils/util'
import { areDeeplyEqual } from '../../../utils/util'
import { PipelineEditActionCreator, actionCreator } from './atomUtil'
import {
    ADD_CONTAINER,
    ADD_STAGE,
    CLEAR_ATOM_DATA,
    DELETE_ATOM,
    DELETE_ATOM_PROP,
    DELETE_STAGE,
    FETCHING_ATOM_LIST,
    FETCHING_ATOM_MORE_LOADING,
    FETCHING_ATOM_VERSION,
    INSERT_ATOM,
    PIPELINE_SETTING_MUTATION,
    PROPERTY_PANEL_VISIBLE,
    RESET_ATOM_MODAL_MAP,
    RESET_PIPELINE_SETTING_MUNTATION,
    SELECT_PIPELINE_VERSION,
    SET_ATOMS,
    SET_ATOMS_CLASSIFY,
    SET_ATOM_EDITING,
    SET_ATOM_MODAL,
    SET_ATOM_MODAL_FETCHING,
    SET_ATOM_PAGE_OVER,
    SET_ATOM_VERSION_LIST,
    SET_ATOMS_OUTPUT_MAP,
    SET_COMMEND_ATOM_COUNT,
    SET_COMMEND_ATOM_PAGE_OVER,
    SET_COMMON_PARAMS,
    SET_COMMON_SETTING,
    SET_CONTAINER_DETAIL,
    SET_DEFAULT_STAGE_TAG,
    SET_EDIT_FROM,
    SET_GLOBAL_ENVS,
    SET_HIDE_SKIP_EXEC_TASK,
    SET_INSERT_STAGE_STATE,
    SET_PIPELINE,
    SET_PIPELINE_EDITING,
    SET_PIPELINE_EXEC_DETAIL,
    SET_PIPELINE_INFO,
    SET_PIPELINE_WITHOUT_TRIGGER,
    SET_PIPELINE_YAML,
    SET_PIPELINE_YAML_HIGHLIGHT_MAP,
    SET_REMOTE_TRIGGER_TOKEN,
    SET_REQUEST_ATOM_DATA,
    SET_SAVE_STATUS,
    SET_SHOW_VARIABLE,
    SET_STAGE_TAG_LIST,
    SET_STORE_SEARCH,
    SET_TEMPLATE,
    SWITCHING_PIPELINE_VERSION,
    TOGGLE_ATOM_SELECTOR_POPUP,
    TOGGLE_STAGE_REVIEW_PANEL,
    UPDATE_ATOM,
    UPDATE_ATOM_INPUT,
    UPDATE_ATOM_OUTPUT_NAMESPACE,
    UPDATE_ATOM_TYPE,
    UPDATE_CONTAINER,
    UPDATE_PIPELINE_SETTING_MUNTATION,
    UPDATE_STAGE,
    UPDATE_WHOLE_ATOM_INPUT
} from './constants'

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
    setSaveStatus ({ commit }, status) {
        commit(SET_SAVE_STATUS, status)
    },
    requestPipelineSummary ({ commit }, { projectId, pipelineId, archiveFlag }) {
        let url = `/${PROCESS_API_URL_PREFIX}/user/version/projects/${projectId}/pipelines/${pipelineId}/detail`

        if (archiveFlag !== undefined && archiveFlag !== null) {
            url += `?archiveFlag=${encodeURIComponent(archiveFlag)}`
        }

        return request.get(url).then(response => {
            commit(SET_PIPELINE_INFO, response.data)
            return response.data
        })
    },
    toggleStageReviewPanel: actionCreator(TOGGLE_STAGE_REVIEW_PANEL),

    setStoreSearch ({ commit }, str) {
        commit(SET_STORE_SEARCH, str)
    },

    setShowVariable ({ commit }, isShow) {
        commit(SET_SHOW_VARIABLE, !!isShow)
    },

    setRequestAtomData ({ commit }, data) {
        commit(SET_REQUEST_ATOM_DATA, data)
    },
    requestTemplate: async ({ commit, dispatch, getters }, { projectId, templateId, version }) => {
        try {
            const versionQuery = version
                ? {
                    version
                }
                : null
            const [templateRes, atomPropRes] = await Promise.all([
                request.get(`/${PROCESS_API_URL_PREFIX}/user/templates/projects/${projectId}/templates/${templateId}`, {
                    params: versionQuery
                }),
                request.get(`/${PROCESS_API_URL_PREFIX}/user/template/atoms/projects/${projectId}/templates/${templateId}/atom/prop/list`, {
                    params: versionQuery
                })
            ])
            const template = templateRes.data.template
            const atomProp = atomPropRes.data
            const elements = getters.getAllElements(template.stages)
            elements.forEach(element => { // 将os属性设置到model内
                Object.assign(element, {
                    ...atomProp[element.atomCode]
                })
            })
            dispatch('setPipeline', template)
            commit(SET_TEMPLATE, {
                template: templateRes.data
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
    requestPipeline: async ({ commit, dispatch, getters, state }, { projectId, pipelineId, version, archiveFlag }) => {
        try {
            let url1 = `${PROCESS_API_URL_PREFIX}/user/version/projects/${projectId}/pipelines/${pipelineId}/versions/${version ?? ''}`
            let url2 = `/${PROCESS_API_URL_PREFIX}/user/pipeline/projects/${projectId}/pipelines/${pipelineId}/atom/prop/list`
            if (archiveFlag !== undefined && archiveFlag !== null) {
                url1 += `?archiveFlag=${encodeURIComponent(archiveFlag)}`
                url2 += `?archiveFlag=${encodeURIComponent(archiveFlag)}`
            }
            const [pipelineRes, atomPropRes] = await Promise.all([
                request.get(url1),
                request.get(url2, {
                    params: version ? { version } : {}
                })
            ])
            const { setting, model } = pipelineRes.data.modelAndSetting
            const atomProp = atomPropRes.data
            const elements = getters.getAllElements(model.stages)
            elements.forEach(element => { // 将os属性设置到model内
                Object.assign(element, {
                    ...atomProp[element.atomCode]
                })
            })
            dispatch('setPipeline', model)
            if (!areDeeplyEqual(state.pipelineWithoutTrigger?.stages, model.stages.slice(1))) {
                commit(SET_PIPELINE_WITHOUT_TRIGGER, {
                    ...model,
                    stages: model.stages.slice(1)
                })
            }

            commit(PIPELINE_SETTING_MUTATION, Object.assign(setting, {
                versionUpdater: pipelineRes.data.updater,
                versionUpdateTime: pipelineRes.data.updateTime
            }))
            if (!pipelineRes.data.yamlSupported) {
                rootCommit(commit, UPDATE_PIPELINE_MODE, UI_MODE)
            }
            if (pipelineRes?.data?.yamlSupported) {
                const { yaml, ...highlightMap } = pipelineRes.data.yamlPreview
                if (pipelineRes?.data?.yamlPreview?.yaml) {
                    commit(SET_PIPELINE_YAML, yaml)
                }
                commit(SET_PIPELINE_YAML_HIGHLIGHT_MAP, highlightMap)
            }
        } catch (e) {
            if (e.code === 403) {
                e.message = ''
            }
            rootCommit(commit, FETCH_ERROR, e)
        }
    },
    fetchPipelineByVersion ({ commit }, { projectId, pipelineId, version, archiveFlag }) {
        let url = `${PROCESS_API_URL_PREFIX}/user/version/projects/${projectId}/pipelines/${pipelineId}/versions/${version ?? ''}`
        if (archiveFlag !== undefined && archiveFlag !== null) {
            url += `?archiveFlag=${encodeURIComponent(archiveFlag)}`
        }
        return request.get(url).then(res => {
            return res.data
        })
    },
    async canSwitchToYaml (_, { projectId, pipelineId, actionType, ...params }) {
        try {
            const { data } = await request.post(`${PROCESS_API_URL_PREFIX}/user/transfer/projects/${projectId}`, params, {
                params: {
                    pipelineId,
                    actionType: 'FULL_MODEL2YAML'
                }
            })
            return {
                newYaml: data.newYaml,
                yamlSupported: data.yamlSupported,
                yamlInvalidMsg: data.yamlInvalidMsg
            }
        } catch (error) {
            return {
                yamlSupported: false,
                yamlInvalidMsg: error.message
            }
        }
    },
    async transfer ({ getters, state }, { projectId, pipelineId, actionType, ...params }) {
        const apis = [
            request.post(`${PROCESS_API_URL_PREFIX}/user/transfer/projects/${projectId}`, params, {
                params: {
                    pipelineId,
                    actionType
                }
            })
        ]
        if (actionType === 'FULL_YAML2MODEL' && !state.editfromImport) {
            apis.push(
                request.get(`/${PROCESS_API_URL_PREFIX}/user/pipeline/projects/${projectId}/pipelines/${pipelineId}/atom/prop/list`, {
                    params: params.version ? { version: params.version } : {}
                })
            )
        }
        const [{ data }, atomPropRes] = await Promise.all(apis)
        if (data.yamlInvalidMsg) {
            throw new Error(data.yamlInvalidMsg)
        }
        if (actionType === 'FULL_YAML2MODEL' && atomPropRes?.data) {
            const atomProp = atomPropRes.data
            const elements = getters.getAllElements(data.modelAndSetting?.model.stages)
            elements.forEach(element => { // 将os属性设置到model内
                Object.assign(element, {
                    ...atomProp[element.atomCode]
                })
            })
        }
        return data
    },
    async transferPipeline ({ commit, dispatch }, { projectId, pipelineId, actionType, ...params }) {
        try {
            const data = await dispatch('transfer', { projectId, pipelineId, actionType, ...params })

            switch (actionType) {
                case 'FULL_YAML2MODEL':
                    if (data?.modelAndSetting?.model) {
                        commit(SET_PIPELINE, data?.modelAndSetting?.model)
                        commit(SET_PIPELINE_WITHOUT_TRIGGER, {
                            ...(data?.modelAndSetting?.model ?? {}),
                            stages: data?.modelAndSetting?.model.stages.slice(1)
                        })
                        commit(PIPELINE_SETTING_MUTATION, data?.modelAndSetting?.setting)
                    }
                    break
                case 'FULL_MODEL2YAML':
                    if (data?.newYaml) {
                        commit(SET_PIPELINE_YAML, data?.newYaml)
                    }
                    break
            }
            return data
        } catch (error) {
            rootCommit(commit, UPDATE_PIPELINE_MODE, UI_MODE)
            throw error
        }
    },
    requestCommonParams: async ({ commit }) => {
        try {
            const { data } = await request.post(`/${PROCESS_API_URL_PREFIX}/user/buildParam/common`)
            commit(SET_COMMON_PARAMS, data)
            return data
        } catch (e) {
            rootCommit(commit, FETCH_ERROR, e)
        }
    },
    requestTriggerParams: async ({ commit }, params) => {
        try {
            const { data } = await request.post(`/${PROCESS_API_URL_PREFIX}/user/buildParam/trigger`, params)
            return data
        } catch (e) {
            rootCommit(commit, FETCH_ERROR, e)
        }
    },
    requestBuildParams: async ({ commit }, { projectId, pipelineId, buildId, archiveFlag }) => {
        try {
            let url = `/${PROCESS_API_URL_PREFIX}/user/builds/${projectId}/${pipelineId}/${buildId}/parameters`
            if (archiveFlag !== undefined && archiveFlag !== null) {
                url += `?archiveFlag=${encodeURIComponent(archiveFlag)}`
            }
            const { data } = await request.get(url)
            return data
        } catch (e) {
            rootCommit(commit, FETCH_ERROR, e)
        }
    },
    setPipeline: ({ commit }, payload = null) => {
        commit(SET_PIPELINE, payload)
    },
    setPipelineWithoutTrigger: actionCreator(SET_PIPELINE_WITHOUT_TRIGGER),
    setPipelineYaml: actionCreator(SET_PIPELINE_YAML),
    updatePipelineSetting: PipelineEditActionCreator(UPDATE_PIPELINE_SETTING_MUNTATION),
    resetPipelineSetting: actionCreator(RESET_PIPELINE_SETTING_MUNTATION),
    setPipelineSetting: actionCreator(PIPELINE_SETTING_MUTATION),
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

    fetchClassify: async ({ commit }) => {
        request.get(`${STORE_API_URL_PREFIX}/user/pipeline/atom/classify`).then(res => {
            const [atomClassifyCodeList, atomClassifyMap] = getMapByKey(res.data, 'classifyCode')

            Object.assign(atomClassifyMap, {
                all: {
                    classifyCode: 'all',
                    classifyName: (window.pipelineVue.$i18n && window.pipelineVue.$i18n.t('All')) || 'All'
                },
                rdStore: {
                    classifyCode: 'rdStore',
                    classifyName: (window.pipelineVue.$i18n && window.pipelineVue.$i18n.t('store')) || 'store'
                }
            })

            commit(SET_ATOMS_CLASSIFY, {
                atomClassifyCodeList,
                atomClassifyMap
            })
        }).catch((e) => {
            console.error(e)
        })
    },

    fetchAtomsOutput: async ({ commit, state, getters }) => {
        const elements = getters.getAllElements(state.pipeline?.stages)
        const arr = elements.map(ele => `${ele.atomCode}@${ele.version}`)
        const data = Array.from(new Set(arr))
        try {
            request.post(`${STORE_API_URL_PREFIX}/user/pipeline/atom/output/info/list`, data).then(res => {
                const map = {}
                for (const item in res.data) {
                    map[item] = JSON.parse(res.data[item])
                }
                console.log(map, 88552)
                commit(SET_ATOMS_OUTPUT_MAP, map)
            })
        } catch (error) {
            commit(SET_ATOMS_OUTPUT_MAP, {})
        }
    },

    fetchAtoms: async ({ commit, state, getters }, { projectCode, category, jobType, classifyId, os, searchKey, queryProjectAtomFlag, fitOsFlag = undefined }) => {
        try {
            const isCommendAtomPageOver = state.isCommendAtomPageOver
            const requestAtomData = state.requestAtomData
            const keyword = searchKey || requestAtomData.keyword || ''
            let recommendFlag = requestAtomData.recommendFlag
            let page = requestAtomData.page || 1
            let pageSize = requestAtomData.pageSize || 50
            let queryFitAgentBuildLessAtomFlag
            const curOs = os

            if (keyword) {
                // 关键字查询 => 搜索研发商店插件数据 (全局搜索 => 无操作系统、无编译环境限制)
                pageSize = 100
                queryProjectAtomFlag = false
                fitOsFlag = false
                os = undefined
                recommendFlag = undefined
                jobType = undefined
                classifyId = undefined
            }

            // 查询不适用插件 category 不传
            if (isCommendAtomPageOver) {
                fitOsFlag = false
                queryFitAgentBuildLessAtomFlag = false
            }

            if (!keyword && isCommendAtomPageOver && os) {
                jobType = undefined
                queryFitAgentBuildLessAtomFlag = false
            } else if (!keyword && isCommendAtomPageOver && !os) {
                fitOsFlag = undefined
                jobType = 'AGENT'
            }
            if (page === 1 && !isCommendAtomPageOver) {
                commit(FETCHING_ATOM_LIST, true)
            } else {
                commit(FETCHING_ATOM_MORE_LOADING, true)
            }

            await request.get(`${STORE_API_URL_PREFIX}/user/pipeline/atom`, {
                params: {
                    page,
                    pageSize,
                    projectCode,
                    jobType,
                    category,
                    classifyId,
                    os,
                    keyword,
                    queryProjectAtomFlag,
                    queryFitAgentBuildLessAtomFlag,
                    fitOsFlag
                }
            }).then(res => {
                const curAtomList = getters.getAtomDisabled(res.data.records, curOs, category)
                const [cruAtomCodeList, curAtomMap] = getMapByKey(curAtomList, 'atomCode')

                const atomCodeList = [...state.atomCodeList, ...cruAtomCodeList]
                const atomMap = Object.assign(state.atomMap, curAtomMap)
                const atomList = [...state.atomList, ...curAtomList]

                const count = res.data.count
                if (recommendFlag) {
                    // 如果长度大于等于 `适用插件` 总条数 => 代表已经拉取完全部适用插件
                    // 下次请求的是 `不适用插件` 数据，页面调整为0页
                    if (category !== 'TRIGGER' && atomCodeList.length === count) {
                        recommendFlag = undefined
                        page = 0
                    }
                    commit(SET_COMMEND_ATOM_COUNT, count)
                    commit(SET_COMMEND_ATOM_PAGE_OVER, atomCodeList.length === count)
                }

                let isAtomPageOver = false
                if (category === 'TRIGGER') {
                    isAtomPageOver = atomList.length === count
                } else if (!recommendFlag && count !== 0) {
                    isAtomPageOver = atomList.length === state.commendAtomCount + count
                }
                commit(SET_ATOM_PAGE_OVER, isAtomPageOver)

                const curRequestAtomData = {
                    page: ++page,
                    pageSize,
                    recommendFlag,
                    keyword
                }
                commit(SET_REQUEST_ATOM_DATA, curRequestAtomData)
                commit(SET_ATOMS, {
                    atomCodeList,
                    atomMap,
                    atomList
                })
            })
        } catch (e) {
            rootCommit(commit, FETCH_ERROR, e)
        } finally {
            commit(FETCHING_ATOM_MORE_LOADING, false)
            commit(FETCHING_ATOM_LIST, false)
        }
    },

    setAtomPageOver: ({ commit }) => {
        commit(SET_ATOM_PAGE_OVER, false)
    },

    clearAtomData: ({ commit }) => {
        commit(CLEAR_ATOM_DATA)
    },
    resetAtomModalMap: actionCreator(RESET_ATOM_MODAL_MAP),
    fetchAtomModal: async ({ commit, dispatch }, { projectCode, atomCode, version, atomIndex, container, queryOfflineFlag = false }) => {
        try {
            commit(SET_ATOM_MODAL_FETCHING, true)
            const { data: atomModal } = await request.get(`${STORE_API_URL_PREFIX}/user/pipeline/atom/${projectCode}/${atomCode}/${version}?queryOfflineFlag=${queryOfflineFlag}`)
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
                    jobControlOption: { // 作业控制选项默认值
                        enable: true,
                        dependOnType: 'ID',
                        dependOnId: [],
                        dependOnName: '',
                        timeoutVar: '900',
                        prepareTimeout: '10',
                        runCondition: 'STAGE_RUNNING',
                        customVariables: [{ key: 'param1', value: '' }],
                        customCondition: ''
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
    updateAtomType: ({ commit }, payload) => {
        commit(UPDATE_ATOM_TYPE, payload)
        commit(SET_PIPELINE_EDITING, true)
    },
    updateAtom: (action, { element: atom, newParam, changeEditStatus = true }) => {
        if (changeEditStatus) {
            PipelineEditActionCreator(UPDATE_ATOM)(action, { atom, newParam })
        } else {
            action.commit(UPDATE_ATOM, { atom, newParam })
        }
    },
    updateAtomInput: PipelineEditActionCreator(UPDATE_ATOM_INPUT),
    updateWholeAtomInput: PipelineEditActionCreator(UPDATE_WHOLE_ATOM_INPUT),
    updateAtomOutputNameSpace: PipelineEditActionCreator(UPDATE_ATOM_OUTPUT_NAMESPACE),
    deleteAtomProps: PipelineEditActionCreator(DELETE_ATOM_PROP),
    togglePropertyPanel: ({ commit }, payload) => {
        // if (payload.isShow && !payload.editingElementPos) {
        //     return
        // }
        commit(PROPERTY_PANEL_VISIBLE, payload)
    },
    requestPipelineExecDetail: async ({ commit, dispatch }, { projectId, buildNo, pipelineId, ...query }) => {
        try {
            const response = await request.get(`${PROCESS_API_URL_PREFIX}/user/builds/projects/${projectId}/pipelines/${pipelineId}/builds/${buildNo}/record`, {
                params: query
            })
            dispatch('setPipelineDetail', response.data)
        } catch (e) {
            if (e.code === 403) {
                e.message = ''
            }
            rootCommit(commit, FETCH_ERROR, e)
        }
    },
    requestPipelineExecDetailByBuildNum: async ({ commit, dispatch }, { projectId, buildNum, pipelineId, version, archiveFlag }) => {
        try {
            let url = `${PROCESS_API_URL_PREFIX}/user/builds/projects/${projectId}/pipelines/${pipelineId}/record/${buildNum}`
            if (archiveFlag !== undefined && archiveFlag !== null) {
                url += `?archiveFlag=${encodeURIComponent(archiveFlag)}`
            }
            return request.get(url, {
                params: {
                    version
                }
            })
        } catch (e) {
            if (e.code === 403) {
                e.message = ''
            }
            rootCommit(commit, FETCH_ERROR, e)
        }
    },
    setPipelineDetail: actionCreator(SET_PIPELINE_EXEC_DETAIL),
    setHideSkipExecTask: actionCreator(SET_HIDE_SKIP_EXEC_TASK),
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
    getInstallAtomList ({ commit }, { projectCode, page, pageSize, classifyCode }) {
        return request.get(`${STORE_API_URL_PREFIX}/user/pipeline/atom/projectCodes/${projectCode}/installedAtoms/list?page=${page}&pageSize=${pageSize}&classifyCode=${classifyCode}`)
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
        return request.get(`${LOG_API_URL_PREFIX}/user/logs/${projectId}/${pipelineId}/${buildId}`, {
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
        return request.get(`${LOG_API_URL_PREFIX}/user/logs/${projectId}/${pipelineId}/${buildId}/after`, {
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
        return request.get(`${LOG_API_URL_PREFIX}/user/logs/${projectId}/${pipelineId}/${buildId}/mode`, { params: { tag, jobId, executeCount } })
    },

    getDownloadLogFromArtifactory ({ commit }, { projectId, pipelineId, buildId, tag, executeCount }) {
        return request.get(`/artifactory/api/user/artifactories/log/plugin/${projectId}/${pipelineId}/${buildId}/${tag}/${executeCount}`).then((res) => {
            const data = res.data || {}
            return data.url || ''
        })
    },

    praiseAi ({ commit }, { projectId, pipelineId, buildId, tag, currentExe, score }) {
        let url = `/misc/api/user/gpt/script_error_analysis_score/${projectId}/${pipelineId}/${buildId}?taskId=${tag}&score=${score}`
        if (currentExe) {
            url += `&executeCount=${currentExe}`
        }
        return request.post(url)
    },

    cancelPraiseAi ({ commit }, { projectId, pipelineId, buildId, tag, currentExe, score }) {
        let url = `/misc/api/user/gpt/script_error_analysis_score/${projectId}/${pipelineId}/${buildId}?taskId=${tag}&score=${score}`
        if (currentExe) {
            url += `&executeCount=${currentExe}`
        }
        return request.delete(url)
    },

    getPraiseAiInfo ({ commit }, { projectId, pipelineId, buildId, tag, currentExe }) {
        let url = `/misc/api/user/gpt/script_error_analysis_score/${projectId}/${pipelineId}/${buildId}?taskId=${tag}&score=true`
        if (currentExe) {
            url += `&executeCount=${currentExe}`
        }
        return request.get(url)
    },

    getLogAIMessage ({ commit }, { projectId, pipelineId, buildId, tag, currentExe, refresh, callBack }) {
        let url = `/misc/api/user/gpt/script_error_analysis/${projectId}/${pipelineId}/${buildId}?taskId=${tag}&refresh=${refresh}`
        if (currentExe) {
            url += `&executeCount=${currentExe}`
        }
        return window.fetch(url, {
            method: 'post'
        }).then((response) => {
            const reader = response.body.getReader()
            const decoder = new TextDecoder()

            const readChunk = () => {
                return reader.read().then(appendChunks)
            }

            const appendChunks = (result) => {
                const chunk = decoder.decode(result.value || new Uint8Array(), {
                    stream: !result.done
                })
                if (chunk) {
                    callBack(chunk)
                }
                if (!result.done) {
                    readChunk()
                }
            }

            readChunk()
        })
    },

    getAIStatus () {
        return request.get('/misc/api/user/gpt_config/is_ok')
    },

    getMacSysVersion () {
        return request.get(`${MACOS_API_URL_PREFIX}/user/systemVersions/v2`)
    },

    getMacXcodeVersion (_, systemVersion = '') {
        return request.get(`${MACOS_API_URL_PREFIX}/user/xcodeVersions/v2?systemVersion=${systemVersion}`)
    },

    getWinVersion () {
        return request.get('/dispatch-windows/api/user/systemVersions').then((res) => {
            return res.data
        })
    },

    pausePlugin ({ commit }, { projectId, pipelineId, buildId, taskId, isContinue, stageId, containerId, element }) {
        return request.post(`${PROCESS_API_URL_PREFIX}/user/builds/projects/${projectId}/pipelines/${pipelineId}/builds/${buildId}/taskIds/${taskId}/execution/pause?isContinue=${isContinue}&stageId=${stageId}&containerId=${containerId}`, element)
    },

    download (_, { url, name, params, type }) {
        const fn = (blob) => {
            const a = document.createElement('a')
            const url = window.URL || window.webkitURL || window.moxURL
            a.href = url.createObjectURL(blob)
            if (name) a.download = name
            document.body.appendChild(a)
            a.click()
            document.body.removeChild(a)
        }

        if (type === CODE_MODE) {
            return fetch(url, {
                credentials: 'include',
                method: 'post',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(params)
            }).then((res) => {
                if (res.status >= 200 && res.status < 300) {
                    return res.json()
                } else {
                    return res.json().then((result) => Promise.reject(result))
                }
            }).then((data) => {
                const result = data.data.newYaml
                const blob = new Blob([result])
                fn(blob)
            })
        } else {
            return fetch(url, { credentials: 'include' }).then((res) => {
                if (res.status >= 200 && res.status < 300) {
                    return res.blob()
                } else {
                    return res.json().then((result) => Promise.reject(result))
                }
            }).then((blob) => {
                fn(blob)
            })
        }
    },
    reviewExcuteAtom: async ({ commit }, { projectId, pipelineId, buildId, elementId, action, ruleIds }) => {
        return request.post(`/${PROCESS_API_URL_PREFIX}/user/quality/builds/${projectId}/${pipelineId}/${buildId}/${elementId}/qualityGateReview/${action}`, {
            ruleIds
        }).then(response => {
            return response.data
        })
    },
    saveDraftPipeline ({ commit }, { projectId, ...draftPipeline }) {
        return request.post(`/${PROCESS_API_URL_PREFIX}/user/version/projects/${projectId}/saveDraft`, draftPipeline)
    },
    releaseDraftPipeline ({ commit }, { projectId, pipelineId, version, params }) {
        return request.post(`/${PROCESS_API_URL_PREFIX}/user/version/projects/${projectId}/pipelines/${pipelineId}/releaseVersion/${version}`, params)
    },
    async prefetchPipelineVersion ({ commit }, { projectId, pipelineId, version, ...params }) {
        const res = await request.get(`/${PROCESS_API_URL_PREFIX}/user/version/projects/${projectId}/pipelines/${pipelineId}/releaseVersion/${version}/prefetch`, {
            params
        })
        return res.data
    },
    yamlNavToPipelineModel ({ commit }, { projectId, body, ...params }) {
        return request.post(`/${PROCESS_API_URL_PREFIX}/user/transfer/projects/${projectId}/position`, {
            yaml: body
        }, {
            params
        })
    },
    previewAtomYAML ({ commit }, { projectId, pipelineId, ...params }) {
        return request.post(`/${PROCESS_API_URL_PREFIX}/user/transfer/projects/${projectId}/pipelines/${pipelineId}/task2yaml`, params)
    },
    insertAtomYAML ({ commit }, { projectId, pipelineId, line, column, ...params }) {
        return request.post(`/${PROCESS_API_URL_PREFIX}/user/transfer/projects/${projectId}/pipelines/${pipelineId}/taskInsert`, params, {
            params: {
                line,
                column
            }
        })
    },
    listPermissionStaticViews ({ commit }, { projectId, pipelineId }) {
        return request.get(`/${PROCESS_API_URL_PREFIX}/user/pipelineViews/projects/${projectId}/pipelines/${pipelineId}/listPermissionStaticViews`).then(response => {
            return response.data
        })
    },
    selectPipelineVersion ({ commit }, version) {
        commit(SELECT_PIPELINE_VERSION, version)
    },
    setSwitchingPipelineVersion ({ commit }, isSwitching) {
        commit(SWITCHING_PIPELINE_VERSION, isSwitching)
    },
    getPipelineVersionInfo ({ commit }, { projectId, pipelineId, version, archiveFlag }) {
        let url = `/${PROCESS_API_URL_PREFIX}/user/version/projects/${projectId}/pipelines/${pipelineId}/versions/${version}/info`
        if (archiveFlag !== undefined && archiveFlag !== null) {
            url += `?archiveFlag=${encodeURIComponent(archiveFlag)}`
        }
        return request.get(url)
    },
    setAtomEditing ({ commit }, isEditing) {
        return commit(SET_ATOM_EDITING, isEditing)
    },
    updateBuildNo ({ commit }, { projectId, pipelineId, currentBuildNo }) {
        return request.post(`/${PROCESS_API_URL_PREFIX}/user/version/projects/${projectId}/pipelines/${pipelineId}/updateBuildNo`, { currentBuildNo })
    },
    requestScmBranchList ({ commit }, { projectId, repositoryHashId, ...searchParams }) {
        return request.get(`/${PROCESS_API_URL_PREFIX}/user/scm/${projectId}/${repositoryHashId}/branches`, {
            params: searchParams
        })
    }

}
