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
import Vue from 'vue'
import {
    PROCESS_API_URL_PREFIX,
    MY_PIPELINE_VIEW_ID,
    COLLECT_VIEW_ID,
    COLLECT_VIEW_ID_NAME,
    ALL_PIPELINE_VIEW_ID,
    DELETED_VIEW_ID,
    UNCLASSIFIED_PIPELINE_VIEW_ID,
    RECENT_USED_VIEW_ID
} from '@/store/constants'

const prefix = `/${PROCESS_API_URL_PREFIX}/user/pipelineViews/projects`
const groupPrefix = `/${PROCESS_API_URL_PREFIX}/user/pipelineGroups`
const SET_ALL_PIPELINE_GROUP = 'SET_ALL_PIPELINE_GROUP'
const SET_LABEL_LIST = 'SET_LABEL_LIST'
const UPDATE_PIPELINE_GROUP = 'UPDATE_PIPELINE_GROUP'

const state = {
    allPipelineGroup: [],
    tagGroupList: [],
    sumView: {
        id: ALL_PIPELINE_VIEW_ID,
        name: ALL_PIPELINE_VIEW_ID,
        icon: 'group',
        hideMore: true
    },
    hardViews: [
        {
            id: RECENT_USED_VIEW_ID,
            countKey: 'recentUseCount',
            i18nKey: RECENT_USED_VIEW_ID,
            icon: 'time-circle-fill',
            hideMore: true
        },
        {
            id: COLLECT_VIEW_ID,
            countKey: 'myFavoriteCount',
            i18nKey: COLLECT_VIEW_ID_NAME,
            icon: 'star-shape',
            hideMore: true
        },
        {
            id: MY_PIPELINE_VIEW_ID,
            countKey: 'myPipelineCount',
            i18nKey: MY_PIPELINE_VIEW_ID,
            icon: 'user-shape',
            hideMore: true
        }
    ]
}

const getters = {
    pipelineGroupDict: state => {
        return state.allPipelineGroup.reduce((acc, item) => {
            if (item.projected) {
                acc.projectViewList.push(item)
            } else {
                acc.personalViewList.push(item)
            }
            return acc
        }, {
            personalViewList: [
                ...state.hardViews
            ],
            projectViewList: []
        })
    },
    dynamicPipelineGroups: state => state.allPipelineGroup.filter(group => group.viewType === 1),
    staticPipelineGroups: state => state.allPipelineGroup.filter(group => group.viewType === 2),
    hardViewsMap: state => state.hardViews.reduce((acc, item) => {
        acc[item.id] = item
        return acc
    }, {
        allPipeline: {
            i18nKey: ALL_PIPELINE_VIEW_ID
        }
    }),
    fixedGroupIdSet: (state, getters) => new Set([
        UNCLASSIFIED_PIPELINE_VIEW_ID,
        DELETED_VIEW_ID,
        ...state.sumView.id,
        ...Object.keys(getters.hardViewsMap)
    ]),
    groupMap: (state, getters) => state.allPipelineGroup.reduce((acc, item) => {
        acc[item.id] = item
        return acc
    }, getters.hardViewsMap),
    groupNamesMap: (state) => {
        return state.allPipelineGroup.reduce((acc, item) => {
            acc[item.name] = item
            return acc
        }, {})
    },
    getTagList: (state) => { // 标签分组集
        const list = state.tagGroupList || []
        return list.map((item) => {
            item.labelValue = []
            return item
        })
    }
}

const mutations = {

    [SET_ALL_PIPELINE_GROUP]: (state, allPipelineGroup) => {
        state.allPipelineGroup = allPipelineGroup
    },
    [SET_LABEL_LIST]: (state, tagGroupList) => {
        state.tagGroupList = tagGroupList
    },
    // 删除某个标签分组
    removeTagGroupById (state, { groupId }) {
        state.tagGroupList.forEach((item, index) => {
            if (item.id === groupId) {
                state.tagGroupList.splice(index, 1)
            }
        })
    },
    // 修改某个分组名称
    modifyTagGroupById (state, { id, name }) {
        state.tagGroupList.forEach((item, index) => {
            if (item.id === id) {
                state.tagGroupList[index].name = name
            }
        })
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
    },
    addCollectViewPipelineCount (state, count) {
        state.hardViews[1].pipelineCount += count
        state.hardViews = [
            state.hardViews[0],
            state.hardViews[1],
            ...state.hardViews.slice(2)
        ]
    },
    [UPDATE_PIPELINE_GROUP]: (state, { id, body }) => {
        const group = state.allPipelineGroup.find(group => group.id === id)
        if (group) {
            Object.assign(group, body)
        }
    }
}

const actions = {
    async requestGroupPipelineCount ({ commit, getters }, { projectId, viewId }) {
        try {
            const { data } = await ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipelineViews/projects/${projectId}/views/${viewId}/pipelineCount`)
            if (data && getters.groupMap[viewId]) {
                Vue.set(getters.groupMap[viewId], 'pipelineCountDetail', data)
            }
            return data
        } catch (error) {
            console.error(error)
            return false
        }
    },
    requestPipelineCount (_, { projectId }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipelines/projects/${projectId}/getCount`)
    },
    /**
     * 获取所有流水线分组
    */
    async requestGetGroupLists ({ commit, state, dispatch }, { projectId, viewId }) {
        try {
            const [pipelineGroups, groupCounts] = await Promise.all([
                ajax.get(`${prefix}/${projectId}/list`),
                dispatch('requestPipelineCount', { projectId })
            ])
            commit(SET_ALL_PIPELINE_GROUP, pipelineGroups.data)
            if (viewId) {
                await dispatch('requestGroupPipelineCount', { projectId, viewId })
            }
            
            state.sumView.pipelineCount = groupCounts.data.totalCount
            state.hardViews = state.hardViews.map(hardView => ({
                ...hardView,
                pipelineCount: groupCounts.data[hardView.countKey]
            }))
        } catch (error) {
            console.error(error)
        }
    },
    /**
     * 获取所有流水线分组树
    */
    async requestGroupListsDict ({ commit, state, dispatch }, { projectId }) {
        try {
            const { data } = await ajax.get(`${prefix}/${projectId}/dict`)
            const pipelineGroupMap = [
                ...data.personalViewList,
                ...data.projectViewList
            ].reduce((acc, item) => {
                return [
                    ...acc,
                    ...item.pipelineList
                ]
            }, []).reduce((acc, item) => {
                acc[item.pipelineId] = {
                    pipelineName: item.pipelineName,
                    groupIds: [
                        ...(acc[item.pipelineId]?.groupIds ?? []),
                        item.viewId
                    ]
                }
                return acc
            }, {})
            return {
                pipelineGroupMap,
                dict: data
            }
        } catch (error) {
            console.error(error)
            return {
                pipelineGroupMap: {},
                dict: {
                    personalViewList: [],
                    projectViewList: []
                }
            }
        }
    },
    /**
     * 添加流水线分组
    */
    async addPipelineGroup ({ commit, state, dispatch }, { projectId, ...body }) {
        const { data } = await ajax.post(`${prefix}/${projectId}`, body)
        dispatch('requestGetGroupLists', { projectId })
        return data.id
    },
    /**
     * 获取流水线分组详情
    */
    async requestPipelineGroup ({ commit, state, dispatch }, { projectId, id }) {
        const { data } = await ajax.get(`${prefix}/${projectId}/views/${id}`)
        return data
    },
    /**
     * 修改流水线分组
    */
    async updatePipelineGroup ({ commit, getters, dispatch }, { id, projectId, ...body }) {
        await ajax.put(`${prefix}/${projectId}/views/${id}`, body)

        commit(UPDATE_PIPELINE_GROUP, {
            id,
            body
        })
    },
    /**
     * 删除流水线分组
    */
    async deletePipelineGroup ({ commit, getters, state, dispatch }, { projectId, id }) {
        const res = await ajax.delete(`${prefix}/${projectId}/views/${id}`)
        commit(SET_ALL_PIPELINE_GROUP, state.allPipelineGroup.filter(view => view.id !== id))
        return res
    },

    /**
     * 流水线分组置顶
    */
    toggleStickyTop ({ commit, state, dispatch }, { projectId, viewId, ...body }) {
        return ajax.post(`${prefix}/${projectId}/views/${viewId}/top`, body)
    },
    /**
     * 获取自定义属性列表
    */
    async requestTagList ({ commit, state, dispatch }, { projectId }) {
        const { data } = await ajax.get(`${groupPrefix}/groups?projectId=${projectId}`)
        commit(SET_LABEL_LIST, data)
    },
    previewGroupResult: (_ctx, { projectId, ...body }) => ajax.post(`${prefix}/${projectId}/preview`, body),

    /**
     * 添加标签分组
    */
    addGroup ({ commit, state, dispatch }, { projectId, name }) {
        return ajax.post(`${groupPrefix}/groups`, { projectId, name }).then(response => {
            return response.data
        })
    },
    /**
     * 修改标签分组
    */
    modifyGroup ({ commit, state, dispatch }, { id, projectId, name }) {
        return ajax.put(`${groupPrefix}/groups`, { id, projectId, name }).then(response => {
            return response.data
        })
    },
    /**
     * 删除标签分组
    */
    deleteGroup ({ commit, state, dispatch }, { projectId, groupId }) {
        // return {groupId}
        return ajax.delete(`${groupPrefix}/groups?projectId=${projectId}&groupId=${groupId}`).then(response => {
            return response.data
        })
    },
    /**
     * 添加标签
    */
    addTag ({ commit, state, dispatch }, { projectId, groupId, name }) {
        return ajax.post(`${groupPrefix}/labels?projectId=${projectId}`, { groupId, name }).then(response => {
            return response.data
        })
    },
    /**
     * 修改标签名称
    */
    modifyTag ({ commit, state, dispatch }, { projectId, id, groupId, name }) {
        return ajax.put(`${groupPrefix}/labels?projectId=${projectId}`, { id, groupId, name }).then(response => {
            return response.data
        })
    },
    /**
     * 删除标签
    */
    deleteTag ({ commit, state, dispatch }, { projectId, labelId }) {
        return ajax.delete(`${groupPrefix}/labels?projectId=${projectId}&labelId=${labelId}`).then(response => {
            return response.data
        })
    },
    addPipelineToGroup (_ctx, { projectId, ...body }) {
        return ajax.post(`${prefix}/${projectId}/bulkAdd`, body)
    },
    removePipelineFromGroup (_ctx, { projectId, ...body }) {
        return ajax.post(`${prefix}/${projectId}/bulkRemove`, body)
    },
    matchDynamicView (_ctx, { projectId, ...body }) {
        return ajax.post(`${prefix}/${projectId}/matchDynamicView`, body)
    },
    async fetchPipelineGroups (_ctx, { projectId, pipelineId }) {
        try {
            const { data } = await ajax.get(`/${PROCESS_API_URL_PREFIX}/user/pipelineViews/projects/${projectId}/pipelines/${pipelineId}/listViews`)
            return data.map(({ id, name }) => ({
                id,
                name
            }))
        } catch (error) {
            console.error(error)
            return []
        }
    }
}

export default {
    namespaced: true,
    state,
    getters,
    mutations,
    actions
}
