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
    PROCESS_API_URL_PREFIX
} from '@/store/constants'

const prefix = `/${PROCESS_API_URL_PREFIX}/user`

const state = {
    viewGroup: [],
    tagGroupList: []
}

const getters = {
    getViewGroup: state => state.viewGroup,
    getTagGroupList: state => state.tagGroupList // 标签分组集
}

const mutations = {
    /**
     * 更新 store.getGroupLists
     *
     * @param {Object} state store state
     * @param {Array} list pipelineList 列表
     */
    updateGroupLists (state, list) {
        state.tagGroupList.splice(0, state.tagGroupList.length, ...list)
    },
    // 删除某个标签分组
    removeTagGroupById (state, { groupId }) {
        state.tagGroupList.map((item, index) => {
            if (item.id === groupId) {
                state.tagGroupList.splice(index, 1)
            }
        })
    },
    // 修改某个分组名称
    modifyTagGroupById (state, { id, name }) {
        state.tagGroupList.map((item, index) => {
            if (item.id === id) {
                state.tagGroupList[index].name = name
            }
        })
    },
    updateViewGroup (state, list) { // 更新筛选视图
        state.viewGroup.splice(0, state.viewGroup.length, ...list)
    },

    resetTag (state, { groupIndex, boolean }) { // 更新tag
        if (boolean) {
            state.tagGroupList[groupIndex].labels.push({
                id: ''
            })
        } else {
            state.tagGroupList[groupIndex].labels.splice(-1, 1)
        }
    },

    modifyTag (state, { groupIndex, tagIndex, name }) {
        const tag = state.tagGroupList[groupIndex].labels[tagIndex]
        tag.name = name
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
     * 获取全部标签分组
    */
    requestGetGroupLists ({ commit, state, dispatch }, { projectId }) {
        return ajax.get(`${prefix}/pipelineGroups/groups?projectId=${projectId}`).then(response => {
            return response.data
        })
    },
    /**
     * 添加标签分组
    */
    addGroup ({ commit, state, dispatch }, { projectId, name }) {
        return ajax.post(`${prefix}/pipelineGroups/groups`, { projectId, name }).then(response => {
            return response.data
        })
    },
    /**
     * 修改标签分组
    */
    modifyGroup ({ commit, state, dispatch }, { id, projectId, name }) {
        return ajax.put(`${prefix}/pipelineGroups/groups`, { id, projectId, name }).then(response => {
            return response.data
        })
    },
    /**
     * 删除标签分组
    */
    deleteGroup ({ commit, state, dispatch }, { groupId }) {
        // return {groupId}
        return ajax.delete(`${prefix}/pipelineGroups/groups?groupId=${groupId}`).then(response => {
            return response.data
        })
    },
    /**
     * 添加标签
    */
    addTag ({ commit, state, dispatch }, { groupId, name }) {
        return ajax.post(`${prefix}/pipelineGroups/labels`, { groupId, name }).then(response => {
            return response.data
        })
    },
    /**
     * 修改标签名称
    */
    modifyTag ({ commit, state, dispatch }, { id, groupId, name }) {
        return ajax.put(`${prefix}/pipelineGroups/labels`, { id, groupId, name }).then(response => {
            return response.data
        })
    },
    /**
     * 删除标签
    */
    deleteTag ({ commit, state, dispatch }, { labelId }) {
        return ajax.delete(`${prefix}/pipelineGroups/labels?labelId=${labelId}`).then(response => {
            return response.data
        })
    },

    /**
     * 获取视图分组
    */
    requestFiltersView ({ commit, state, dispatch }, { projectId }) {
        return ajax.get(`${prefix}/pipelineGroups/views?projectId=${projectId}`).then(response => {
            return response.data
        })
    },
    /**
     * 增加视图分组
    */
    addFilterView ({ commit, state, dispatch }, data) {
        return ajax.post(`${prefix}/pipelineGroups/views`, data).then(response => {
            return response.data
        })
    },
    /**
     * 删除视图分组
    */
    deleteFilterView ({ commit, state, dispatch }, { viewId }) {
        return ajax.delete(`${prefix}/pipelineGroups/views?viewId=${viewId}`).then(response => {
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
