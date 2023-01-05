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

/**
 * @file main store
 */

import Vue from 'vue'
import Vuex from 'vuex'
import ajax from '../utils/ajax'
import request from '@/utils/request'
import pipelines from './modules/pipelines/'
import common from './modules/common/'
import atom from './modules/atom'

import {
    FETCH_ERROR,
    SET_SERVICE_HOOKS,
    STORE_API_URL_PREFIX
} from './constants'
import { ARTIFACT_HOOK_CONST, PIPELINE_EXECUTE_DETAIL_HOOK_CONST, PIPELINE_HISTORY_TAB_HOOK_CONST } from '../utils/extensionHooks'
Vue.use(Vuex)

function getHookByHTMLPath (htmlPath) {
    return state => {
        const { hooks } = state
        return Array.isArray(hooks) ? hooks.filter(hook => hook.htmlPath === htmlPath) : []
    }
}

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

        hooks: []
    },
    // 公共 mutations
    mutations: {
        [SET_SERVICE_HOOKS]: (state, hooks) => {
            Object.assign(state, {
                hooks
            })
        },
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
        }
    },
    // 公共 actions
    actions: {
        setServiceHooks: ({ commit }, hooks) => {
            commit(SET_SERVICE_HOOKS, hooks)
        },
        fetchExtensionByHookId: ({ commit }, { projectCode, itemIds }) => {
            return request.get(`${STORE_API_URL_PREFIX}/user/ext/services/items/projects/${projectCode}/list?itemIds=${itemIds}`)
        },
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
        }
    },
    // 公共 getters
    getters: {
        hookKeyMap (state) {
            if (Array.isArray(state.hooks)) {
                return state.hooks.reduce((acc, hook) => {
                    acc[hook.itemId] = hook
                    return acc
                }, {})
            }
            return null
        },
        artifactHooks: getHookByHTMLPath(ARTIFACT_HOOK_CONST),
        extensionTabsHooks: getHookByHTMLPath(PIPELINE_HISTORY_TAB_HOOK_CONST),
        extensionExecuteDetailTabsHooks: getHookByHTMLPath(PIPELINE_EXECUTE_DETAIL_HOOK_CONST)
    }
})
