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
    PROCESS_API_URL_PREFIX,
    STORE_API_URL_PREFIX
} from '@/store/constants'
import ajax from '@/utils/request'

const prefix = `/${PROCESS_API_URL_PREFIX}/user`

const state = {

}

const getters = {

}

const mutations = {
   
}

const actions = {
    installPipelineTemplate (_, params) {
        return ajax.post(`${STORE_API_URL_PREFIX}/user/market/template/install`, params).then(response => {
        })
    },
    requestInstallTemplate (_, params) {
        return ajax.post(`${STORE_API_URL_PREFIX}/user/market/template/install/new`, params).then(response => {
            return response.data
        })
    },
    requestInstanceList (_, { projectId, templateId, params }) {
        return ajax.get(`${prefix}/templateInstances/projects/${projectId}/templates/${templateId}`, { params }).then(response => {
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
    updateTemplateInstance (_, { projectId, templateId, versionId, useTemplateSettings, params }) {
        return ajax.put(`${prefix}/templateInstances/projects/${projectId}/templates/${templateId}/async/update?version=${versionId}&useTemplateSettings=${useTemplateSettings}`, params).then(response => {
            return response.data
        })
    },
    requestVersionCompare (_, { projectId, templateId, versionId, pipelineId }) {
        return ajax.post(`${prefix}/templateInstances/projects/${projectId}/templates/${templateId}/pipelines/${pipelineId}/compare?version=${versionId}`).then(response => {
            return response.data
        })
    },
    requestTemplateList (_, { projectId, pageIndex, pageSize, params }) {
        return ajax.get(`${prefix}/templates/projects/${projectId}/templates?page=${pageIndex}&pageSize=${pageSize}`, { params }).then(response => {
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
    },
    deleteTemplateVersionByName (_, { projectId, templateId, versionName }) {
        return ajax.delete(`${prefix}/templates/projects/${projectId}/templates/${templateId}/deletetemplate?versionName=${versionName}`).then(response => {
            return response.data
        })
    },
    createPipelineWithTemplate (_, { projectId, ...params }) {
        return ajax.post(`${prefix}/version/projects/${projectId}/createPipelineWithTemplate`, params).then(response => {
            return response.data
        })
    },
    requestTemplatePreview (_, { projectId, templateId, ...params }) {
        return ajax.get(`${prefix}/templates/projects/${projectId}/templates/${templateId}/preview`, {
            params
        }).then(response => {
            return response.data
        })
    },
    enableTemplatePermissionManage (_, projectId) {
        return ajax.get(`/${PROCESS_API_URL_PREFIX}/user/templates/projects/${projectId}/templates/enableTemplatePermissionManage`)
    },
    getTemplateHasViewPermission (_, { projectId, templateId }) {
        return ajax.get(`/${PROCESS_API_URL_PREFIX}/user/templates/projects/${projectId}/templates/${templateId}/hasPipelineTemplatePermission?permission=VIEW`)
    },
    getTemplateHasCreatePermission (_, { projectId, templateId }) {
        return ajax.get(`/${PROCESS_API_URL_PREFIX}/user/templates/projects/${projectId}/templates/${templateId}/hasPipelineTemplatePermission?permission=CREATE`)
    }
}

export default {
    namespaced: true,
    state,
    getters,
    mutations,
    actions
}
