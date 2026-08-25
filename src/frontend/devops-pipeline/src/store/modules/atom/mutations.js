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

import { hashID } from '@/utils/util'
import Vue from 'vue'
import {
    diffAtomVersions,
    getAtomDefaultValue,
    getAtomModalKey,
    getAtomOutputObj,
    isNewAtomTemplate
} from './atomUtil'
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
    SET_CONTAINER_FETCHING,
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
    SET_STORE_LOADING,
    SET_STORE_SEARCH,
    SET_TEMPLATE,
    SET_PLUGIN_HEAD_TAB,
    SWITCHING_PIPELINE_VERSION,
    TOGGLE_ATOM_SELECTOR_POPUP,
    TOGGLE_STAGE_REVIEW_PANEL,
    UPDATE_ATOM,
    UPDATE_ATOM_INPUT,
    UPDATE_ATOM_OUTPUT_NAMESPACE,
    UPDATE_ATOM_TYPE,
    UPDATE_CONTAINER,
    UPDATE_PIPELINE_INFO,
    UPDATE_PIPELINE_SETTING_MUNTATION,
    UPDATE_STAGE,
    UPDATE_WHOLE_ATOM_INPUT
} from './constants'

export default {
    [TOGGLE_STAGE_REVIEW_PANEL]: (state, { showStageReviewPanel, editingElementPos = null }) => {
        Object.assign(state, {
            showStageReviewPanel,
            editingElementPos
        })
    },
    [SET_DEFAULT_STAGE_TAG]: (state, defaultStageTags) => {
        Vue.set(state, 'defaultStageTags', defaultStageTags)
    },
    [SET_STAGE_TAG_LIST]: (state, stageTagList) => {
        Vue.set(state, 'stageTagList', stageTagList)
    },
    [SET_COMMON_SETTING]: (state, setting) => {
        state.pipelineCommonSetting = setting || {}
        try {
            state.pipelineLimit = {
                stageLimit: setting.maxStageNum,
                jobLimit: setting.stageCommonSetting.maxJobNum,
                atomLimit: setting.stageCommonSetting.jobCommonSetting.maxTaskNum
            }
        } catch (err) {
            console.error('commom setting error', err)
        }
    },
    [SET_SAVE_STATUS]: (state, status) => {
        console.log('set save status', status, state.saveStatus)
        return Object.assign(state, {
            saveStatus: status
        })
    },
    [SET_TEMPLATE]: (state, { template }) => {
        return Object.assign(state, {
            template
        })
    },
    [FETCHING_ATOM_LIST]: (state, fetching) => {
        Vue.set(state, 'fetchingAtomList', fetching)
        return state
    },
    /**
     * 更新 store.pipeline 中的 pipelineInfo
     *
     * @param {Object} state store state
     * @param {Object} obj pipelineInfo 对象
     */
    [SET_PIPELINE_INFO]: (state, obj) => {
        Vue.set(state, 'pipelineInfo', obj)
        console.log(obj, 'set123')
    },
    [UPDATE_PIPELINE_INFO]: (state, partOfInfo) => {
        const pipelineInfo = {
            ...(state.pipelineInfo ?? {})
        }
        Object.assign(pipelineInfo, partOfInfo)
        console.log(pipelineInfo, 123, partOfInfo)
        Vue.set(state, 'pipelineInfo', pipelineInfo)
    },
    [SET_PIPELINE]: (state, pipeline = null) => {
        if (!state.pipeline || !pipeline) {
            Vue.set(state, 'pipeline', pipeline)
            return state
        }
        Object.assign(state.pipeline, pipeline)
    },
    [SET_PIPELINE_WITHOUT_TRIGGER]: (state, pipeline = null) => {
        Vue.set(state, 'pipelineWithoutTrigger', pipeline)
        return state
    },
    [SET_PIPELINE_YAML]: (state, yaml = '') => {
        Vue.set(state, 'pipelineYaml', yaml)
        return state
    },
    [SET_PIPELINE_YAML_HIGHLIGHT_MAP]: (state, highlightMap = {}) => {
        Vue.set(state, 'yamlHighlightBlockMap', highlightMap)
        return state
    },
    [RESET_PIPELINE_SETTING_MUNTATION]: (state, payload) => {
        return Object.assign(state, {
            pipelineSetting: null
        })
    },
    [PIPELINE_SETTING_MUTATION]: (state, pipelineSetting) => {
        return Object.assign(state, {
            pipelineSetting
        })
    },
    [UPDATE_PIPELINE_SETTING_MUNTATION]: (state, { setting, param }) => {
        Object.assign(setting, param)
        return state
    },
    [SET_EDIT_FROM]: (state, editfromImport = false) => {
        Vue.set(state, 'editfromImport', editfromImport)
        return state
    },
    [SET_PIPELINE_EDITING]: (state, editing) => {
        if (state.isPipelineEditing !== editing) {
            Vue.set(state, 'isPipelineEditing', editing)
        }
        return state
    },
    [SET_CONTAINER_DETAIL]: (state, { containerTypeList, containerModalMap }) => {
        Object.assign(state, {
            containerTypeList,
            containerModalMap
        })
        return state
    },
    [FETCHING_ATOM_VERSION]: (state, fetchingAtmoVersion) => {
        Object.assign(state, {
            fetchingAtmoVersion
        })
        return state
    },
    [SET_ATOM_VERSION_LIST]: (state, atomVersionList) => {
        Object.assign(state, {
            atomVersionList
        })
        return state
    },
    [SET_ATOMS_OUTPUT_MAP]: (state, atomsOutputMap) => {
        Object.assign(state, {
            atomsOutputMap
        })
        return state
    },
    [SET_ATOM_MODAL_FETCHING]: (state, fetchingAtmoModal) => {
        Object.assign(state, {
            fetchingAtmoModal
        })
        return state
    },
    [SET_ATOM_MODAL]: (state, { atomCode, atomModal, version }) => {
        const key = getAtomModalKey(atomCode, version)
        Vue.set(state, 'atomModalMap', {
            ...state.atomModalMap,
            [key]: atomModal
        })
        return state
    },
    [RESET_ATOM_MODAL_MAP]: (state) => {
        Vue.set(state, 'atomModalMap', {})
        return state
    },
    [SET_CONTAINER_FETCHING]: (state, { fetchingContainer }) => {
        Object.assign(state, {
            fetchingContainer
        })
        return state
    },
    [SET_INSERT_STAGE_STATE]: (state, payload) => {
        Object.assign(state, payload)
        return state
    },
    [UPDATE_ATOM_TYPE] (state, { container, atomCode, version, atomIndex }) {
        const key = getAtomModalKey(atomCode, version)
        const atomModal = state.atomModalMap[key]
        const preVerEle = container.elements[atomIndex]
        const preVerkey = getAtomModalKey(preVerEle.atomCode, preVerEle.version)
        const preVerAtomModal = state.atomModalMap[preVerkey] || { props: {} }
        const isChangeAtom = atomModal.atomCode !== preVerAtomModal.atomCode
        let atom = null
        let atomVersionChangedKeys = []
        if (isNewAtomTemplate(atomModal.htmlTemplateVersion)) {
            const preVerData = preVerEle.data || {}
            const preVerModelProps = preVerAtomModal.props || {}
            const diffRes = diffAtomVersions(preVerData.input, preVerModelProps.input, atomModal.props.input, isChangeAtom)
            atomVersionChangedKeys = diffRes.atomVersionChangedKeys
            const canPause = atomModal.props.config?.canPauseBeforeRun === true

            atom = {
                id: `e-${hashID(32)}`,
                '@type': atomModal.classType !== atomCode ? atomModal.classType : atomCode,
                atomCode,
                name: isChangeAtom ? atomModal.name : preVerEle.name,
                version,
                data: {
                    input: {
                        ...getAtomDefaultValue(atomModal.props.input),
                        ...diffRes.atomValue
                    },
                    output: {
                        ...getAtomOutputObj(atomModal.props.output)
                    },
                    namespace: isChangeAtom ? '' : preVerData.namespace || '',
                    config: atomModal.props.config
                },
                additionalOptions: canPause
                    ? {
                        pauseBeforeExec: true
                    }
                    : {
                    }
            }
        } else {
            const diffRes = diffAtomVersions(preVerEle, preVerAtomModal.props, atomModal.props, isChangeAtom)
            atomVersionChangedKeys = diffRes.atomVersionChangedKeys
            console.log(atomModal)
            atom = {
                id: `e-${hashID(32)}`,
                '@type': atomModal.classType !== atomCode ? atomModal.classType : atomCode,
                atomCode,
                version,
                name: isChangeAtom ? atomModal.name : preVerEle.name,
                ...getAtomDefaultValue(atomModal.props),
                ...diffRes.atomValue
            }
        }
        // 对比出的差异key，会在5秒后清空
        state.atomVersionChangedKeys = atomVersionChangedKeys
        clearTimeout(this.atomVersionChangedCleanId)
        this.atomVersionChangedCleanId = setTimeout(() => {
            state.atomVersionChangedKeys = []
        }, 5000)
        container.elements.splice(atomIndex, 1, {
            ...atom,
            os: atomModal.os,
            buildLessRunFlag: atomModal.buildLessRunFlag,
            logoUrl: atomModal.logoUrl,
            additionalOptions: isChangeAtom ? (atom.additionalOptions ?? {}) : { ...preVerEle.additionalOptions }
        })
    },
    [UPDATE_ATOM]: (state, { atom, newParam }) => {
        for (const key in newParam) {
            if (Object.prototype.hasOwnProperty.call(newParam, key)) {
                Vue.set(atom, key, newParam[key])
            }
        }
    },

    [UPDATE_ATOM_INPUT]: (state, { atom, newParam }) => {
        try {
            Vue.set(atom.data, 'input', {
                ...atom.data.input,
                ...newParam
            })
        } catch (e) {
            console.warn(e, 'update atom input error', atom)
        }
    },

    [UPDATE_WHOLE_ATOM_INPUT]: (state, { atom, newInput }) => {
        try {
            Vue.set(atom.data, 'input', newInput)
        } catch (e) {
            console.warn(e, 'update atom input error', atom)
        }
    },
    [UPDATE_ATOM_OUTPUT_NAMESPACE]: (state, { atom, namespace }) => {
        try {
            Vue.set(atom.data, 'namespace', namespace)
        } catch (e) {
            console.warn(e, 'update atom input error', atom)
        }
    },
    [DELETE_STAGE]: (state, { stageIndex }) => {
        state.pipelineWithoutTrigger.stages.splice(stageIndex, 1)
        return state
    },
    [UPDATE_STAGE]: (state, { stage, newParam }) => {
        Object.assign(stage, newParam)
    },
    [ADD_STAGE]: (state, { stages, insertStageIndex, insertStageIsFinally = false }) => {
        stages.splice(insertStageIndex, 0, {
            id: `s-${hashID(32)}`,
            name: insertStageIsFinally === true ? 'Final' : `stage-${insertStageIndex + 1}`,
            tag: [...state.defaultStageTags],
            containers: [],
            checkIn: { timeout: 24 },
            checkOut: { timeout: 24 },
            finally: insertStageIsFinally === true || undefined,
            stageControlOption: {
                enable: true,
                runCondition: 'AFTER_LAST_FINISHED',
                customCondition: '',
                customVariables: [{ key: 'param1', value: '' }],
                manualTrigger: false,
                triggerUsers: [],
                timeout: 24
            }
        })
        return state
    },
    [ADD_CONTAINER]: (state, { containers, newContainer }) => {
        containers.push(newContainer)
    },
    [UPDATE_CONTAINER]: (state, { container, newParam }) => {
        Object.assign(container, newParam)
    },
    [INSERT_ATOM]: (state, { elements, insertIndex }) => {
        elements.splice(insertIndex, 0, {
            data: {},
            isError: true
        })
    },
    [DELETE_ATOM]: (state, { elements, atomIndex }) => {
        elements.splice(atomIndex, 1)
    },
    [PROPERTY_PANEL_VISIBLE]: (state, { showPanelType, isShow, isComplete, editingElementPos = null }) => {
        return Object.assign(state, {
            showPanelType,
            isPropertyPanelVisible: isShow,
            isShowCompleteLog: isComplete,
            editingElementPos
        })
    },
    [DELETE_ATOM_PROP]: (state, { element, propKey }) => {
        delete element[propKey]
        return state
    },
    [SET_PIPELINE_EXEC_DETAIL]: (state, execDetail = null) => {
        if (execDetail?.model?.stages) {
            execDetail.model.stages = execDetail.model.stages.slice(1)
        }
        Object.assign(state, {
            execDetail
        })
    },
    [SET_HIDE_SKIP_EXEC_TASK]: (state, hideSkipExecTask) => {
        Vue.set(state, 'hideSkipExecTask', hideSkipExecTask)
        return state
    },
    [SET_REMOTE_TRIGGER_TOKEN]: (state, { atom, token }) => {
        Vue.set(atom, 'remoteToken', token)
        return state
    },
    [SET_GLOBAL_ENVS]: (state, globalEnvs) => {
        return Object.assign(state, {
            globalEnvs
        })
    },
    [SET_COMMON_PARAMS]: (state, commonParams) => {
        return Object.assign(state, {
            commonParams
        })
    },
    [TOGGLE_ATOM_SELECTOR_POPUP]: (state, show) => {
        Vue.set(state, 'showAtomSelectorPopup', show)
        return state
    },
    [SET_SHOW_VARIABLE]: (state, show) => {
        Vue.set(state, 'showVariable', show)
        return state
    },
    [SET_STORE_LOADING]: (state, data) => {
        state.storeAtomData.loading = data
    },
    [SET_STORE_SEARCH]: (state, str) => {
        state.storeAtomData.keyword = str
    },
    [SET_ATOMS_CLASSIFY]: (state, { atomClassifyMap, atomClassifyCodeList }) => {
        Object.assign(state, {
            atomClassifyCodeList,
            atomClassifyMap
        })
        return state
    },
    [FETCHING_ATOM_MORE_LOADING]: (state, fetching) => {
        Vue.set(state, 'fetchingAtomMoreLoading', fetching)
        return state
    },
    [SET_ATOMS]: (state, { atomCodeList, atomMap, atomList }) => {
        Object.assign(state, {
            atomCodeList,
            atomMap,
            atomList
        })
        return state
    },
    [SET_REQUEST_ATOM_DATA]: (state, requestAtomData) => {
        Object.assign(state, {
            requestAtomData
        })
        return state
    },
    [SET_COMMEND_ATOM_COUNT]: (state, commendAtomCount) => {
        Vue.set(state, 'commendAtomCount', commendAtomCount)
        return state
    },
    [SET_ATOM_PAGE_OVER]: (state, isAtomPageOver) => {
        Vue.set(state, 'isAtomPageOver', isAtomPageOver)
        return isAtomPageOver
    },
    [CLEAR_ATOM_DATA]: (state) => {
        state.atomList = []
        state.atomMap = {}
        state.atomCodeList = []
        state.fetchingAtomList = true
        state.isCommendAtomPageOver = false
        return state
    },
    [SET_COMMEND_ATOM_PAGE_OVER]: (state, payload) => {
        state.isCommendAtomPageOver = payload
        return state
    },
    [SELECT_PIPELINE_VERSION]: (state, version) => {
        Object.assign(state, {
            activePipelineVersion: version
        })
        return state
    },
    [SWITCHING_PIPELINE_VERSION]: (state, payload) => {
        Object.assign(state, {
            switchingVersion: payload
        })
        return state
    },
    [SET_ATOM_EDITING]: (state, payload) => {
        Object.assign(state, {
            isElementModified: payload

        })
        return state
    },
    [SET_PLUGIN_HEAD_TAB]: (state, { isGetPluginHeadTab }) => {
        return Object.assign(state, {
            isGetPluginHeadTab
        })
    }
}
