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

import { UPDATE_CURRENT_ATOM } from './constants'

const prefix = 'store/api'
const projectPrefix = 'project/api'
const supportPrefix = 'support/api'
const Vue = window.Vue
const vue = new Vue()

export const actions = {
    /**
     * 删除敏感数据
     */
    deleteSensitiveConf ({ commit }, { atomCode, id }) {
        return vue.$ajax.delete(`${prefix}/user/market/ATOM/component/${atomCode}/sensitiveConf?ids=${id}`)
    },

    /**
     * 编辑敏感数据
     */
    modifySensitiveConf ({ commit }, { atomCode, id, postData }) {
        return vue.$ajax.put(`${prefix}/user/market/ATOM/component/${atomCode}/sensitiveConf/${id}`, postData)
    },

    /**
     * 新增敏感数据
     */
    addSensitiveConf ({ commit }, { atomCode, postData }) {
        return vue.$ajax.post(`${prefix}/user/market/ATOM/component/${atomCode}/sensitiveConf`, postData)
    },

    /**
     * 获取敏感数据
     */
    getSensitiveConf ({ commit }, atomCode) {
        return vue.$ajax.get(`${prefix}/user/market/ATOM/component/${atomCode}/sensitiveConf/list`)
    },

    /**
     * 根据ID获取评论
     */
    requestAtomModifyComment ({ commit }, data) {
        return vue.$ajax.put(`${prefix}/user/market/atom/comment/comments/${data.id}`, data.postData)
    },

    /**
     * 根据ID获取评论
     */
    requestAtomUserComment ({ commit }, id) {
        return vue.$ajax.get(`${prefix}/user/market/atom/comment/comments/${id}`)
    },

    /**
     * 评分详情
     */
    requestAtomScoreDetail ({ commit }, code) {
        return vue.$ajax.get(`${prefix}/user/market/atom/comment/score/atomCodes/${code}`)
    },

    /**
     * 评论点赞
     */
    requestAtomPraiseComment ({ commit }, commentId) {
        return vue.$ajax.put(`${prefix}/user/market/atom/comment/praise/${commentId}`)
    },

    /**
     * 添加评论回复
     */
    requestAtomReplyComment ({ commit }, { id, postData }) {
        return vue.$ajax.post(`${prefix}/user/market/atom/comment/reply/comments/${id}/reply`, postData)
    },

    /**
     * 获取原子的评论回复列表
     */
    requestAtomReplyList ({ commit }, commentId) {
        return vue.$ajax.get(`${prefix}/user/market/atom/comment/reply/comments/${commentId}/replys`)
    },

    /**
     * 流水线插件新增评论
     */
    requestAddAtomComment ({ commit }, { id, code, postData }) {
        return vue.$ajax.post(`${prefix}/user/market/atom/comment/atomIds/${id}/atomCodes/${code}/comment`, postData)
    },

    /**
     * 流水线插件评论列表
     */
    requestAtomComments ({ commit }, { code, page, pageSize }) {
        return vue.$ajax.get(`${prefix}/user/market/atom/comment/atomCodes/${code}/comments?page=${page}&pageSize=${pageSize}`)
    },

    /**
     * 流水线插件市场首页的数据
     */
    requestAtomHome ({ commit }) {
        return vue.$ajax.get(`${prefix}/user/market/atom/list/main?page=1&pageSize=8`)
    },

    /**
     * 我的流水线插件列表
     */
    requestAtomList ({ commit }, { atomName, page, pageSize }) {
        return vue.$ajax.get(`${prefix}/user/market/desk/atom/list?atomName=${atomName}&page=${page}&pageSize=${pageSize}`)
    },
    
    /**
     * 新增流水线插件
     */
    createNewAtom ({ commit }, { params }) {
        return vue.$ajax.post(`${prefix}/user/market/desk/atom`, params)
    },
    
    /**
     * 流水线插件详情
     */
    requestAtomDetail ({ commit }, { atomId }) {
        return vue.$ajax.get(`${prefix}/user/market/desk/atom/${atomId}`)
    },

    /**
     * 流水线插件详情
     */
    requestAtom ({ commit }, { atomCode }) {
        return vue.$ajax.get(`${prefix}/user/market/atom/${atomCode}`)
    },

    /**
     * 流水线插件分类
     */
    requestAtomClassify ({ commit }) {
        return vue.$ajax.get(`${prefix}/user/pipeline/atom/classify`)
    },

    /**
     * 上架/升级流水线插件
     */
    editAtom ({ commit }, { projectId, params }) {
        return vue.$ajax.put(`${prefix}/user/market/desk/atom?projectCode=${projectId}`, params)
    },

    /**
     * 上架/升级进度
     */
    requestRelease ({ commit }, { atomId }) {
        return vue.$ajax.get(`${prefix}/user/market/desk/atom/release/process/${atomId}`)
    },

    /**
     * 删除流水线插件
     */
    // requestDeleteAtom ({ commit }, { atomId }) {
    //     return vue.$ajax.delete(`${prefix}/user/market/desk/atom/${atomId}`)
    // },

    /**
     * 下架流水线插件
     */
    offlineAtom ({ commit }, { atomCode, params }) {
        return vue.$ajax.put(`${prefix}/user/market/desk/atom/offline/${atomCode}`, params)
    },

    /**
     * 安装流水线插件
     */
    installAtom ({ commit }, { params }) {
        return vue.$ajax.post(`${prefix}/user/market/atom/install`, params)
    },

    /**
     * 取消发布
     */
    cancelRelease ({ commit }, { atomId }) {
        return vue.$ajax.put(`${prefix}/user/market/desk/atom/release/cancel/${atomId}`)
    },

    /**
     * 流水线插件已安装的项目
     */
    requestRelativeProject ({ commit }, { atomCode }) {
        return vue.$ajax.get(`${prefix}/user/market/atom/installedProjects/${atomCode}`)
    },

    /**
     * 测试通过
     */
    passTest ({ commit }, { atomId }) {
        return vue.$ajax.put(`${prefix}/user/market/desk/atom/release/passTest/${atomId}`)
    },

    /**
     * 查看流水线插件可见范围
     */
    requestVisibleList ({ commit }, { atomCode }) {
        return vue.$ajax.get(`${prefix}/user/market/desk/atom/visible/dept/${atomCode}`)
    },

    /**
     * 获取流水线插件用户列表
     */
    requestMemberList ({ commit }, { atomCode }) {
        return vue.$ajax.get(`${prefix}/user/market/desk/atom/member/list?atomCode=${atomCode}`)
    },

    /**
     * 添加流水线插件成员
     */
    addAtomMember ({ commit }, { params }) {
        return vue.$ajax.post(`${prefix}/user/market/desk/atom/member/add`, params)
    },

    /**
     * 删除流水线插件成员
     */
    requestDeleteMember ({ commit }, { atomCode, id }) {
        return vue.$ajax.delete(`${prefix}/user/market/desk/atom/member/delete?atomCode=${atomCode}&id=${id}`)
    },

    /**
     * 根据机构类型和机构ID查看机构列表
     */
    requestOrganizations ({ commit }, { type, id }) {
        return vue.$ajax.get(`${projectPrefix}/user/organizations/types/${type}/ids/${id}`)
    },

    /**
     * 设置流水线插件可见范围
     */
    setVisableDept ({ commit }, { params }) {
        return vue.$ajax.post(`${prefix}/user/market/desk/atom/visible/dept`, params)
    },

    /**
     * 删除可见对象
     */
    requestDeleteVisiable ({ commit }, { atomCode, deptIds }) {
        return vue.$ajax.delete(`${prefix}/user/market/desk/atom/visible/dept/${atomCode}?deptIds=${deptIds}`)
    },
    
    /**
     * 流水线插件按分类
     */
    requestAtomClassifys ({ commit }) {
        return vue.$ajax.get(`${prefix}/user/market/atom/classifys`)
    },

    /**
     * 流水线插件按功能
     */
    requestAtomLables ({ commit }) {
        return vue.$ajax.get(`${prefix}/user/market/atom/label/labels`)
    },

    /**
     * 流水线插件市场流水线插件列表
     */
    requestMarketAtom ({ commit }, params) {
        return vue.$ajax.get(`${prefix}/user/market/atom/list`, { params })
    },

    /**
     * 流水线插件统计数据
     */
    requestAtomStatistic ({ commit }, { atomCode }) {
        return vue.$ajax.get(`${prefix}/user/market/atom/statistic/${atomCode}`)
    },

    /**
     * 上传文件
     */
    uploadFile ({ commit }, { formData, config }) {
        return vue.$ajax.post(`${supportPrefix}/user/file/upload`, formData, config)
    },

    /**
     * 流水线插件版本列表
     */
    requestVersionList ({ commit }, { atomCode }) {
        return vue.$ajax.get(`${prefix}/user/market/atom/version/list?atomCode=${atomCode}`)
    },

    /**
     * 上传流水线插件logo
     */
    uploadLogo ({ commit }, { formData, config }) {
        return vue.$ajax.post(`${prefix}/user/store/logo/upload`, formData, config)
    },
    
    updateCurrentaAtom ({ commit }, { res }) {
        commit(UPDATE_CURRENT_ATOM, res)
    }
}

export const getters = {
    getCurrentAtom: state => state.currentAtom
}

export const state = {
    currentAtom: {}
}

export const mutations = {
    [UPDATE_CURRENT_ATOM]: (state, res) => {
        Vue.set(state, 'currentAtom', res)
    }
}
