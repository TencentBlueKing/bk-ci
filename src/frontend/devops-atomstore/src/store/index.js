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

import * as atom from './atom'
import * as template from './template'
import * as IDE from './IDE'
import * as Image from './image'
import * as Service from './service'
import { mergeModules } from '@/utils/index'
import { UPDATE_CURRENT_LIST, UPDATE_MARKET_QUERY, UPDATE_MARKET_DETAIL, CLEAR_MARKET_DETAIL, UPDATE_USER_INFO } from './constants'
const repositoryPrefix = 'repository/api'
const Vue = window.Vue
const vue = new Vue()
const prefix = 'store/api'

const commonModules = {
    namespaced: true,
    state: {
        commentList: [],
        marketQuery: {},
        marketDetail: {},
        userInfo: {}
    },
    mutations: {
        [UPDATE_CURRENT_LIST]: (state, res) => {
            Vue.set(state, 'commentList', res)
        },
        [UPDATE_MARKET_QUERY]: (state, res) => {
            Vue.set(state, 'marketQuery', res)
        },
        [UPDATE_MARKET_DETAIL]: (state, res) => {
            const detail = Object.assign(JSON.parse(JSON.stringify(state.marketDetail)), res)
            Vue.set(state, 'marketDetail', detail)
        },
        [CLEAR_MARKET_DETAIL]: (state, res) => {
            Vue.set(state, 'marketDetail', {})
        },
        [UPDATE_USER_INFO]: (state, res) => {
            Vue.set(state, 'userInfo', res)
        }
    },
    actions: {
        updateUserInfo ({ commit }, res) {
            commit(UPDATE_USER_INFO, res)
        },

        clearDetail ({ commit }) {
            commit(CLEAR_MARKET_DETAIL)
        },

        setDetail ({ commit }, res) {
            commit(UPDATE_MARKET_DETAIL, res)
        },

        setMarketQuery ({ commit }, res) {
            commit(UPDATE_MARKET_QUERY, res)
        },

        setCommentList ({ commit }, res) {
            commit(UPDATE_CURRENT_LIST, res)
        },

        setCommentReplay ({ commit, state }, { id, newList, isAdd }) {
            const commentList = state.commentList || []
            const currentComment = commentList.find(comment => comment.data.commentId === id) || {}
            const data = currentComment.data || {}
            if (isAdd) data.replyCount++

            const children = currentComment.children || []
            const length = children.length || 0
            children.splice(length, 0, ...newList)
            commit(UPDATE_CURRENT_LIST, commentList)
        },

        clearCommentReply ({ commit, state }, id) {
            const commentList = state.commentList || []
            const currentComment = commentList.find(comment => comment.data.commentId === id) || {}
            currentComment.children = []
            commit(UPDATE_CURRENT_LIST, commentList)
        },

        setCommentPraise ({ commit, state }, { id, count }) {
            const commentList = state.commentList || []
            commentList.forEach((comment) => {
                const data = comment.data || {}
                if (data.commentId === id) {
                    data.praiseCount = count
                    data.praiseFlag = !data.praiseFlag
                } else {
                    const children = comment.children || []
                    const modifyChild = children.find((item) => item.commentId === id) || {}
                    modifyChild.praiseCount = count
                    modifyChild.praiseFlag = !modifyChild.praiseFlag
                }
            })
            commit(UPDATE_CURRENT_LIST, commentList)
        },

        getLogoUrl ({ commit }, { type }) {
            return vue.$ajax.get(`${prefix}/user/store/logo/type/${type}`)
        },

        requestProjectList ({ commit }, params = {}) {
            return vue.$ajax.get('/project/api/user/projects/', { params })
        },

        requestProgressLog ({ commit }, { type, projectCode, pipelineId, buildId, start, executeCount }) {
            return vue.$ajax.get(`${prefix}/user/store/logs/types/${type}/projects/${projectCode}/pipelines/${pipelineId}/builds/${buildId}/after?start=${start}&executeCount=${executeCount}`)
        },

        /**
         * git OAuth授权
         */
        checkIsOAuth ({ commit }, redirectUrl = location.href) {
            return vue.$ajax.get(`${repositoryPrefix}/user/git/isOauth?redirectUrlType=SPEC&redirectUrl=${redirectUrl}`)
        }
    },
    getters: {
        getCommentList: state => state.commentList,
        getMarketQuery: state => state.marketQuery,
        getDetail: state => state.marketDetail,
        getUserInfo: state => state.userInfo
    }
}

export default mergeModules(commonModules, atom, template, IDE, Image, Service)
