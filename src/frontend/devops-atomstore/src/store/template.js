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

import {
    UPDATE_CURRENT_TEMPLATE
} from '@/store/constants'

import {

} from './constants'

const prefix = 'store/api'
const processPrefix = 'process/api'
const Vue = window.Vue
const vue = new Vue()

export const state = {
    currentTemplate: {}
}

export const mutations = {
    [UPDATE_CURRENT_TEMPLATE]: (state, res) => {
        Vue.set(state, 'currentTemplate', res)
    }
}

export const actions = {
    /**
     * 删除模板
     */
    deleteTemplate ({ commit }, templateCode) {
        return vue.$ajax.delete(`${prefix}/user/market/templates/${templateCode}`).then(response => {
            return response
        })
    },

    /**
     * 根据ID获取评论
     */
    requestTemplateModifyComment ({ commit }, data) {
        return vue.$ajax.put(`${prefix}/user/market/template/comment/comments/${data.id}`, data.postData)
    },
    /**
     * 根据ID获取评论
     */
    requestTemplateUserComment ({ commit }, id) {
        return vue.$ajax.get(`${prefix}/user/market/template/comment/comments/${id}`)
    },

    /**
     * 评分详情
     */
    requestTemplateScoreDetail ({ commit }, code) {
        return vue.$ajax.get(`${prefix}/user/market/template/comment/score/templateCodes/${code}`)
    },

    /**
     * 评论点赞
     */
    requestTemplatePraiseComment ({ commit }, commentId) {
        return vue.$ajax.put(`${prefix}/user/market/template/comment/praise/${commentId}`)
    },

    /**
     * 添加评论回复
     */
    requestTemplateReplyComment ({ commit }, { id, postData }) {
        return vue.$ajax.post(`${prefix}/user/market/template/comment/reply/comments/${id}/reply`, postData)
    },

    /**
     * 获取评论回复列表
     */
    requestTemplateReplyList ({ commit }, commentId) {
        return vue.$ajax.get(`${prefix}/user/market/template/comment/reply/comments/${commentId}/replys`)
    },

    /**
     * 新增评论
     */
    requestAddTemplateComment ({ commit }, { id, code, postData }) {
        return vue.$ajax.post(`${prefix}/user/market/template/comment/templateIds/${id}/templateCodes/${code}/comment`, postData)
    },

    /**
     * 评论列表
     */
    requestTemplateComments ({ commit }, { code, page, pageSize }) {
        return vue.$ajax.get(`${prefix}/user/market/template/comment/templateCodes/${code}/comments?page=${page}&pageSize=${pageSize}`)
    },

    /**
     * 模板列表
     */
    requestTemplateList ({ commit }, { templateName, page, pageSize }) {
        return vue.$ajax.get(`${prefix}/user/market/desk/template/list?templateName=${templateName}&page=${page}&pageSize=${pageSize}`)
    },
    /*
     * 模板市场首页
     */
    requestTemplateHome ({ commit }) {
        return vue.$ajax.get(`${prefix}/user/market/template/list/main?page=1&pageSize=8`)
    },
    
    /**
     * 模板搜索列表接口
     */
    requestMarketTemplate ({ commit }, params) {
        return vue.$ajax.get(`${prefix}/user/market/template/list`, { params })
    },

    /**
     * 获取流水线项目模板
     */
    requestPipelineTemplate ({ commit }, { projectCode }) {
        return vue.$ajax.get(`${processPrefix}/user/templates/projects/${projectCode}/templates?templateType=CUSTOMIZE&storeFlag=false`)
    },

    /**
     * 关联模板到store
     */
    relateTemplate ({ commit }, { templateCode, params }) {
        return vue.$ajax.post(`${prefix}/user/market/templates/${templateCode}/store/rel`, params)
    },

    /**
     * 获取模板分类
     */
    requestTplClassify ({ commit }) {
        return vue.$ajax.get(`${prefix}/user/market/template/classifys`)
    },

    requestTplCategorys ({ commit }) {
        return vue.$ajax.get(`${prefix}/user/market/template/categorys`)
    },

    /**
     * 获取模板标签
     */
    requestTplLabel ({ commit }) {
        return vue.$ajax.get(`${prefix}/user/market/template/label/labels`)
    },

    /**
     * 上架模板
     */
    editTemplate ({ commit }, { params }) {
        return vue.$ajax.put(`${prefix}/user/market/desk/template/release`, params)
    },

    /**
     * 根据templateCode获取模板详情
     */
    requestTemplateDetail ({ commit }, code) {
        return vue.$ajax.get(`${prefix}/user/market/template/templateCodes/${code}`)
    },

    /**
     * 根据templateCode获取模板详情
     */
    requestTemplate ({ commit }, { templateCode }) {
        return vue.$ajax.get(`${prefix}/user/market/template/templateCodes/${templateCode}`)
    },

    /**
     * 根据templateId获取模板详情
     */
    requestTempIdDetail ({ commit }, { templateId }) {
        return vue.$ajax.get(`${prefix}/user/market/template/templateIds/${templateId}`)
    },

    /**
     * 获取模板发布进度
     */
    requestTplRelease ({ commit }, { templateId }) {
        return vue.$ajax.get(`${prefix}/user/market/desk/template/release/process/${templateId}`)
    },

    /**
     * 查看模板可见范围
     */
    requesttplVisibleList ({ commit }, { templateCode }) {
        return vue.$ajax.get(`${prefix}/user/market/desk/templates/${templateCode}/visible/dept`)
    },

    /**
     * 设置模板可见范围
     */
    setTplVisableDept ({ commit }, { params }) {
        return vue.$ajax.post(`${prefix}/user/market/desk/templates/${params.templateCode}/visible/dept`, params)
    },

    /**
     * 删除可见对象
     */
    deleteTplVisiable ({ commit }, { templateCode, deptIds }) {
        return vue.$ajax.delete(`${prefix}/user/market/desk/templates/${templateCode}/visible/dept?deptIds=${deptIds}`)
    },

    /**
     * 取消发布模板
     */
    cancelReleaseTemplate ({ commit }, { templateId }) {
        return vue.$ajax.put(`${prefix}/user/market/desk/template/release/cancel/templateIds/${templateId}`)
    },
    
    /**
     * 模板安装的项目
     */
    requestRelativeTplProject ({ commit }, { templateCode }) {
        return vue.$ajax.get(`${prefix}/user/market/template/installedProjects/${templateCode}`)
    },

    /**
     * 安装模板
     */
    installTemplate ({ commit }, { params }) {
        return vue.$ajax.post(`${prefix}/user/market/template/install`, params)
    },

    /**
     * 下架模板
     */
    offlineTemplate ({ commit }, { templateCode }) {
        return vue.$ajax.put(`${prefix}/user/market/desk/template/offline/templateCodes/${templateCode}/versions`)
    },

    updateCurrentaTemplate ({ commit }, { res }) {
        commit(UPDATE_CURRENT_TEMPLATE, res)
    }
}

export const getters = {
    getCurrentTemplate: state => state.currentTemplate
}
