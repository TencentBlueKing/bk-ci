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

/**
 * @file main store
 */

import Vue from 'vue'
import Vuex from 'vuex'
import ajax from '../utils/ajax'
import atom from './modules/atom'
import common from './modules/common/'
import pipelines from './modules/pipelines/'

import { CODE_MODE, UI_MODE } from '@/utils/pipelineConst'

import {
    BKUI_LS_PIPELINE_MODE,
    FETCH_ERROR,
    UPDATE_PIPELINE_MODE
} from './constants'
Vue.use(Vuex)

const modeList = [UI_MODE, CODE_MODE]
const initPipelineMode = localStorage.getItem(BKUI_LS_PIPELINE_MODE)
export default new Vuex.Store({
    // 模块
    modules: {
        atom,
        pipelines,
        common
    },
    // 公共 store
    state: {
        // 当前选中的project
        curProject: {},
        // 是否允许路由跳转
        allowRouterChange: true,
        // fetch error
        fetchError: null,

        cancelTokenMap: {},
        modeList: [...modeList],
        pipelineMode: modeList.includes(initPipelineMode) ? initPipelineMode : UI_MODE
    },
    // 公共 mutations
    mutations: {
        /**
         * 更新当前project
         *
         * @param {Object} state store state
         * @param {boolean} val 值
         */
        updateCurProject (state, project) {
            state.curProject = project || {}
        },
        /**
         * 修改 错误信息
         *
         * @param {Object} state store state
         * @param {boolean} val 值
         */
        [FETCH_ERROR]: (state, fetchError) => {
            console.log(fetchError)
            return Object.assign(state, {
                fetchError
            })
        },
        [UPDATE_PIPELINE_MODE]: (state, mode) => {
            return Object.assign(state, {
                pipelineMode: mode
            })
        }
    },
    // 公共 actions
    actions: {
        requestProjectDetail: async ({ commit }, { projectId }) => {
            return ajax.get(API_URL_PREFIX + `/project/api/user/projects/${projectId}/`).then(response => {
                let data = {}
                if (typeof response.data === 'object' && typeof response.data.data === 'object') {
                    data = response.data.data
                    if (data.ccAppId) {
                        Object.assign(data, { ccAppId: data.ccAppId.toString() })
                    }
                }
                commit('updateCurProject', data)
            })
        },
        updatePipelineMode ({ commit }, mode) {
            localStorage.setItem(BKUI_LS_PIPELINE_MODE, mode)
            commit(UPDATE_PIPELINE_MODE, mode)
        }
    },
    // 公共 getters
    getters: {
        isUiMode: state => {
            return state.pipelineMode === UI_MODE
        },
        isCodeMode: state => {
            return state.pipelineMode === CODE_MODE
        }
    }
})
