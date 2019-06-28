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
    PROCESS_API_URL_PREFIX,
    BACKEND_API_URL_PREFIX
} from '@/store/constants'

const prefix = `/${PROCESS_API_URL_PREFIX}/user`
const backendPrefix = `/${BACKEND_API_URL_PREFIX}/api`

const state = {
    pageLoading: false,
    showViewManage: false,
    showViewCreate: false,
    currentViewId: '',
    userInfo: [],
    currentViewList: [],
    viewSettingList: [],
    createViewForm: {
        projected: false,
        name: '',
        logic: 'AND',
        filters: [
            { id: 'filterByName', name: '流水线名称', '@type': 'filterByName', pipelineName: '' }
        ]
    }
}

const getters = {
    getViewGroup: state => state.viewGroup,
    getTagGroupList: state => state.tagGroupList, // 标签分组集
    getShowViewCreate: state => state.showViewCreate,
    getCreateViewForm: state => state.createViewForm,
    getUserInfo: state => state.userInfo
}

const mutations = {
    showPageLoading (state, loading) {
        state.pageLoading = loading
    },
    // 是否展示视图选择框
    toggleShowViewManage (state, res) {
        state.showViewManage = res
    },
    // 更新viewSetting
    updateViewSettingInfo (state, viewSetting) {
        state.currentViewId = viewSetting.currentViewId
        state.currentViewList = viewSetting.currentViews
        state.viewSettingList = viewSetting.viewClassifies
    },
    // 更新currentViewId
    updateCurrentViewId (state, viewId) {
        state.currentViewId = viewId
    },
    // 更新currentViewId
    updateCurrentViewList (state, currentViewList) {
        state.currentViewList = currentViewList
    },
    toggleViewCreateDialog (state, isShow) {
        state.showViewCreate = isShow
    },
    updateViewForm (state, obj) {
        state.createViewForm = obj
    },
    setUserInfo (state, res) {
        state.userInfo = res
    }
}

const actions = {
    /**
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     * @param {string} projectId 项目 id
     *
     * @return {Promise} promise 对象
     */
    /**
     * 获取视图设置信息
    */
    requestViewSettingInfo ({ commit, state, dispatch }, { projectId }) {
        return ajax.get(`${prefix}/pipelineViews/${projectId}/settings`).then(response => {
            return response.data
        })
    },
    updateCurrentViewList ({ state }, { projectId, ids }) {
        return ajax.post(`${prefix}/pipelineViews/${projectId}/settings`, ids).then(response => {
            return response.data
        })
    },
    requestPipelineViewList ({ commit }, { projectId }) {
        return ajax.get(`${prefix}/pipelineViews/${projectId}`).then(response => {
            return response.data
        })
    },
    createPipelineView ({ commit }, { projectId, params }) {
        return ajax.post(`${prefix}/pipelineViews/${projectId}`, params).then(response => {
            return response.data
        })
    },
    requestPipelineViewDetail ({ commit }, { projectId, viewId }) {
        return ajax.get(`${prefix}/pipelineViews/${projectId}/${viewId}`).then(response => {
            return response.data
        })
    },
    editPipelineView ({ commit }, { projectId, viewId, params }) {
        return ajax.put(`${prefix}/pipelineViews/${projectId}/${viewId}`, params).then(response => {
            return response.data
        })
    },
    deletePipelineView ({ commit }, { projectId, viewId }) {
        return ajax.delete(`${prefix}/pipelineViews/${projectId}/${viewId}`).then(response => {
            return response.data
        })
    },
    requestUserInfo ({ commit }, { projectId }) {
        return ajax.get(`${backendPrefix}/ci/projects/user/roles/${projectId}/`).then(response => {
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
