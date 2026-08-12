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

import {
    UPDATE_CURRENT_TEMPLATE
} from '@/store/constants'

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
     * 模板列表
     */
    requestTemplateList ({ commit }, params) {
        return vue.$ajax.get(`${prefix}/user/market/desk/template/v2/list`, {
            params
        })
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
    requestPipelineTemplate ({ commit }, params) {
        return vue.$ajax.post(`${processPrefix}/user/pipeline/template/v2/${params.projectId}/list/simple`, params)
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
    requestTplLabel () {
        return vue.$ajax.get(`${prefix}/user/market/template/label/labels`)
    },

    /**
     * 新版上架模板
     */
    releaseTemplate (_, params) {
        return vue.$ajax.put(`${prefix}/user/market/desk/template/v2/release`, params)
    },

    requestTemplateVersionList (_, params) {
        return vue.$ajax.post(`${processPrefix}/user/pipeline/template/v2/${params.projectId}/${params.templateId}/versions`, params)
    },

    /**
     * 模板版本上架
     */
    releaseTemplateVersion (_, params) {
        return vue.$ajax.put(`${prefix}/user/market/desk/template/v2/release/versions`, params)
    },

    /**
     * 根据templateCode获取模板详情
     */
    requestTemplateDetail ({ commit }, code) {
        return vue.$ajax.get(`${prefix}/user/market/template/templateCodes/${code}`)
    },

    /**
     * 获取模板发布进度
     */
    requestTplRelease ({ commit }, templateCode) {
        return vue.$ajax.get(`${prefix}/user/market/desk/template/release/process/templateCodes/${templateCode}`)
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
    cancelReleaseTemplate ({ commit }, templateCode) {
        return vue.$ajax.put(`${prefix}/user/market/desk/template/release/cancel/templateCodes/${templateCode}`)
    },
    
    /**
     * 模板安装的项目
     */
    requestRelativeTplProject ({ commit }, templateCode) {
        return vue.$ajax.get(`${prefix}/user/market/template/installedProjects/${templateCode}`)
    },

    /**
     * 安装模板
     */
    installTemplate ({ commit }, params) {
        return vue.$ajax.post(`${prefix}/user/market/template/install/v2`, params)
    },

    /**
     * 下架模板
     */
    offlineTemplate (_, { templateCode, ...params }) {
        return vue.$ajax.put(`${prefix}/user/market/desk/template/v2/offline/templateCodes/${templateCode}/versions`, params)
    },

    /**
     * 流水线模板管理员校验
     */
    templateUserValidate (_, { templateCode }) {
        return vue.$ajax.get(`${prefix}/user/market/desk/store/member/codes/${templateCode}/user/validate?storeType=TEMPLATE`)
    },

    updateCurrentaTemplate ({ commit }, { res }) {
        commit(UPDATE_CURRENT_TEMPLATE, res)
    },
    updatePublishStrategy: (state, { templateCode, publishStrategy }) => {
        return vue.$ajax.put(`${prefix}/user/market/${templateCode}/store/publishStrategy`, {
            publishStrategy
        })
    },
    /**
     * 获取模板版本列表
     */
    requestTemplateReleasedList ({ commit }, { templateCode, page, pageSize }) {
        return vue.$ajax.get(`${prefix}/user/market/${templateCode}/template/published/history?page=${page}&pageSize=${pageSize}`)
    }
}

export const getters = {
    getCurrentTemplate: state => state.currentTemplate
}
