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

}

const getters = {

}

const mutations = {

}

const actions = {
    requestTemplatePermission: async (_, projectId) => {
        return ajax.get(`${prefix}/templates/projects/${projectId}/templates/hasManagerPermission`).then(response => {
            return response.data
        })
    },
    requestInstanceList (_, { projectId, templateId }) {
        return ajax.get(`${prefix}/templateInstances/projects/${projectId}/templates/${templateId}`).then(response => {
            return response.data
        })
    },
    requestTemplateDatail (_, { projectId, templateId, versionId }) {
        return ajax.get(`${prefix}/templates/projects/${projectId}/templates/${templateId}?version=${versionId}`).then(response => {
            return response.data
        })
    },
    checkPipelineName (_, { projectId, pipelineName }) {
        return ajax.get(`${prefix}/pipelines/${projectId}/pipelineExist?pipelineName=${pipelineName}`).then(response => {
            return response.data
        })
    },
    createTemplateInstance (_, { projectId, templateId, versionId, useTemplateSettings, params }) {
        return ajax.post(`${prefix}/templateInstances/projects/${projectId}/templates/${templateId}?version=${versionId}&useTemplateSettings=${useTemplateSettings}`, params).then(response => {
            return response.data
        })
    },
    requestPipelineParams (_, { projectId, templateId, versionId, params }) {
        return ajax.post(`${prefix}/templateInstances/projects/${projectId}/templates/${templateId}/pipelines?version=${versionId}`, params).then(response => {
            return response.data
        })
    },
    updateTemplateInstance (_, { projectId, templateId, versionId, params }) {
        return ajax.put(`${prefix}/templateInstances/projects/${projectId}/templates/${templateId}?version=${versionId}`, params).then(response => {
            return response.data
        })
    },
    requestVersionCompare (_, { projectId, templateId, versionId, pipelineId }) {
        return ajax.post(`${prefix}/templateInstances/projects/${projectId}/templates/${templateId}/pipelines/${pipelineId}/compare?version=${versionId}`).then(response => {
            return response.data
        })
    },
    requestTemplateList (_, { projectId, pageIndex, pageSize }) {
        return ajax.get(`${prefix}/templates/projects/${projectId}/templates?page=${pageIndex}&pageSize=${pageSize}`).then(response => {
            return response.data
        })
    },
    createTemplate (_, { projectId, params }) {
        return ajax.post(`${prefix}/templates/projects/${projectId}/templates`, params).then(response => {
            return response.data
        })
    },
    updateTemplate (_, { projectId, templateId, versionName, params }) {
        return ajax.put(`${prefix}/templates/projects/${projectId}/templates/${templateId}?versionName=${versionName}`, params).then(response => {
            return response.data
        })
    },
    deleteTemplate (_, { projectId, templateId }) {
        return ajax.delete(`${prefix}/templates/projects/${projectId}/templates/${templateId}`).then(response => {
            return response.data
        })
    },
    copyTemplate (_, { projectId, templateId, params }) {
        return ajax.post(`${prefix}/templates/projects/${projectId}/templates/${templateId}/copy`, params).then(response => {
            return response.data
        })
    },
    deleteTemplateVersion (_, { projectId, templateId, versionId }) {
        return ajax.delete(`${prefix}/templates/projects/${projectId}/templates/${templateId}/versions/${versionId}`).then(response => {
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
