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

import Vue from 'vue'
import { SET_TEMPLATE, SET_ATOMS, SET_ATOM_MODAL_FETCHING, SET_ATOM_MODAL, SET_CONTAINER_FETCHING, UPDATE_ATOM_TYPE, SET_CONTAINER_DETAIL, ADD_CONTAINER, PROPERTY_PANEL_VISIBLE, INSERT_ATOM, DELETE_ATOM, DELETE_CONTAINER, UPDATE_CONTAINER, DELETE_STAGE, ADD_STAGE, CONTAINER_TYPE_SELECTION_VISIBLE, SET_INSERT_STAGE_INDEX, UPDATE_ATOM, SET_PIPELINE_EDITING, SET_PIPELINE, SET_BUILD_PARAM, DELETE_ATOM_PROP, SET_PIPELINE_EXEC_DETAIL, SET_REMOTE_TRIGGER_TOKEN, SET_GLOBAL_ENVS, TOGGLE_ATOM_SELECTOR_POPUP, UPDATE_ATOM_INPUT, UPDATE_ATOM_OUTPUT, UPDATE_ATOM_OUTPUT_NAMESPACE, FETCHING_ATOM_LIST, SET_STORE_DATA, SET_STORE_LOADING, SET_STORE_SEARCH, FETCHING_ATOM_VERSION, SET_ATOM_VERSION_LIST, SET_EXECUTE_STATUS, SET_SAVE_STATUS } from './constants'
import { getAtomModalKey, getAtomDefaultValue, getAtomOutputObj, isNewAtomTemplate } from './atomUtil'
import { hashID } from '@/utils/util'

export default {
    [SET_EXECUTE_STATUS]: (state, status) => {
        return Object.assign(state, {
            executeStatus: status
        })
    },
    [SET_SAVE_STATUS]: (state, status) => {
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
    [SET_PIPELINE]: (state, pipeline = null) => {
        Vue.set(state, 'pipeline', pipeline)
        return state
    },
    [SET_PIPELINE_EDITING]: (state, editing) => {
        Vue.set(state.pipeline, 'editing', editing)
        return state
    },
    [SET_CONTAINER_DETAIL]: (state, { containerTypeList, containerModalMap }) => {
        Object.assign(state, {
            containerTypeList,
            containerModalMap
        })
        return state
    },
    [SET_ATOMS]: (state, { atomCodeList, atomClassifyCodeList, atomMap, atomClassifyMap }) => {
        Object.assign(state, {
            atomCodeList,
            atomClassifyCodeList,
            atomMap,
            atomClassifyMap
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
    [SET_CONTAINER_FETCHING]: (state, { fetchingContainer }) => {
        Object.assign(state, {
            fetchingContainer
        })
        return state
    },
    [CONTAINER_TYPE_SELECTION_VISIBLE]: (state, payload) => {
        Object.assign(state, payload)
        return state
    },
    [SET_INSERT_STAGE_INDEX]: (state, payload) => {
        Object.assign(state, payload)
        return state
    },
    [UPDATE_ATOM_TYPE]: (state, { container, atomCode, version, atomIndex }) => {
        const key = getAtomModalKey(atomCode, version)
        const atomModal = state.atomModalMap[key]
        let atom = null
        if (isNewAtomTemplate(atomModal.htmlTemplateVersion)) {
            atom = {
                '@type': atomModal.classType !== atomCode ? atomModal.classType : atomCode,
                atomCode,
                name: atomModal.name,
                version,
                data: {
                    input: {
                        ...getAtomDefaultValue(atomModal.props.input)
                    },
                    output: {
                        ...getAtomOutputObj(atomModal.props.output)
                    }
                }
            }
        } else {
            atom = {
                '@type': atomModal.classType !== atomCode ? atomModal.classType : atomCode,
                atomCode,
                version,
                name: atomModal.name,
                ...getAtomDefaultValue(atomModal.props)
            }
        }
        container.elements.splice(atomIndex, 1, atom)
    },
    [UPDATE_ATOM]: (state, { atom, newParam }) => {
        for (const key in newParam) {
            if (newParam.hasOwnProperty(key)) {
                Vue.set(atom, key, newParam[key])
            }
        }
    },

    [UPDATE_ATOM_INPUT]: (state, { atom, newParam }) => {
        try {
            for (const key in newParam) {
                if (newParam.hasOwnProperty(key)) {
                    Vue.set(atom.data.input, key, newParam[key])
                }
            }
        } catch (e) {
            console.warn(e, 'update atom input error', atom)
        }
    },

    [UPDATE_ATOM_OUTPUT]: (state, { atom, newParam }) => {
        try {
            for (const key in newParam) {
                if (newParam.hasOwnProperty(key)) {
                    Vue.set(atom.data.output, key, newParam[key])
                }
            }
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
    [DELETE_STAGE]: (state, { stages, stageIndex }) => {
        stages.splice(stageIndex, 1)
        return state
    },
    [ADD_STAGE]: (state, { stages, insertStageIndex }) => {
        stages.splice(insertStageIndex, 0, {
            id: hashID(8),
            containers: []
        })
        return state
    },
    [ADD_CONTAINER]: (state, { containers, newContainer }) => {
        containers.push(newContainer)
    },
    [DELETE_CONTAINER]: (state, { containers, containerIndex }) => {
        containers.splice(containerIndex, 1)
    },
    [UPDATE_CONTAINER]: (state, { container, newParam }) => {
        Object.assign(container, newParam)
    },
    [INSERT_ATOM]: (state, { elements, insertIndex }) => {
        elements.splice(insertIndex, 0, {
            data: {}
        })
    },
    [DELETE_ATOM]: (state, { elements, atomIndex }) => {
        elements.splice(atomIndex, 1)
    },
    [PROPERTY_PANEL_VISIBLE]: (state, { isShow, editingElementPos = null }) => {
        return Object.assign(state, {
            isPropertyPanelVisible: isShow,
            editingElementPos
        })
    },
    [SET_BUILD_PARAM]: (state, { buildParams, buildId }) => {
        return Object.assign(state, {
            buildParamsMap: {
                ...state.buildParamsMap,
                [buildId]: buildParams
            }
        })
    },
    [DELETE_ATOM_PROP]: (state, { element, propKey }) => {
        delete element[propKey]
        return state
    },
    [SET_PIPELINE_EXEC_DETAIL]: (state, execDetail = null) => {
        Object.assign(state, {
            execDetail
        })
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
    [TOGGLE_ATOM_SELECTOR_POPUP]: (state, show) => {
        Vue.set(state, 'showAtomSelectorPopup', show)
        return state
    },
    [SET_STORE_DATA]: (state, data) => {
        Vue.set(state, 'storeAtomData', data)
    },
    [SET_STORE_LOADING]: (state, data) => {
        state.storeAtomData.loading = data
    },
    [SET_STORE_SEARCH]: (state, str) => {
        state.storeAtomData.atomName = str
    }
}
