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
    PROCESS_API_URL_PREFIX
    // STORE_API_URL_PREFIX
} from '@/store/constants'
import ajax from '@/utils/request'
const actions = {
    hasPipelineTemplatePermission (_, { projectId, permission }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${projectId}/hasPipelineTemplatePermission?&permission=${permission}`).then(response => {
            return response.data
        })
    },
    templateCopy (_, { projectId, params }) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${projectId}/copy`, params).then(response => {
            return response.data
        })
    },
    deleteTemplate (_, { projectId, templateId }) {
        return ajax.delete(`${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${projectId}/${templateId}/delete`).then(response => {
            return response.data
        })
    },
    getType2Count (_, { projectId }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${projectId}/getType2Count`).then(response => {
            return response.data
        })
    },
    getTemplateList (_, params) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${params.projectId}/list`, params).then(response => {
            return response.data
        })
    },
    createTemplate (_, { projectId, params }) {
        return ajax.post(`/${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${projectId}/create`, params).then(response => {
            return response.data
        })
    },
    importTemplateFromStore (_, { projectId, params }) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${projectId}/create/market`, params)
    },
    getSourceCount (_, params) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${params.projectId}/getSource2Count`, params).then(response => {
            return response.data
        })
    },
    exportYamlTemplate (_, { projectId, templateId, version }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${projectId}/${templateId}/export`, version)
    },
    requestInstanceList (_, { projectId, templateId, page, pageSize, ...params }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/template/Instances/v2/projects/${projectId}/templates/${templateId}?page=${page}&pageSize=${pageSize}`, { params }).then(response => {
            return response.data
        })
    }
}

export default actions
