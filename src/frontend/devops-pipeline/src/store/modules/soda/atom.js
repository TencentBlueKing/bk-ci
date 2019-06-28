
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

import request from '@/utils/request'

const state = {
}

const getters = {
}

const mutations = {
}

const actions = {
    /**
     * 根据projectcode获取项目详情
     */
    requestProjectDetail: async ({ commit }, { projectId }) => {
        return request.get(`backend/api/projects/${projectId}/`).then(response => {
            Object.assign(response.data, { cc_app_name: response.cc_app_name })
            return response.data
        })
    },
    getBcsProjectInstance: async ({ commit }, { projectId, ccId, category, namespace = '' }) => {
        let url = ''
        if (namespace) {
            url = `backend/api/ci/paas-cd/apps/cc_app_ids/${ccId}/projects/${projectId}/instances?category=${category}&namespace=${namespace}`
        } else {
            url = `backend/api/ci/paas-cd/apps/cc_app_ids/${ccId}/projects/${projectId}/instances?category=${category}`
        }
        return request.get(url).then(response => {
            return response.data
        })
    },
    getBcsInstVersion: async ({ commit }, { projectId, ccId, instId }) => {
        return request.get(`backend/api/ci/paas-cd/apps/cc_app_ids/${ccId}/projects/${projectId}/instances/${instId}/versions`).then(response => {
            return response.data
        })
    },
    getBcsInstVar: async ({ commit }, { projectId, ccId, instId, versionId }) => {
        return request.get(`backend/api/ci/paas-cd/apps/cc_app_ids/${ccId}/projects/${projectId}/instances/${instId}/version/configs/?show_version_id=${versionId}`).then(response => {
            return response.data
        })
    },
    getBcsCluster: async ({ commit }, { projectCode }) => {
        return request.get(`backend/api/projects/${projectCode}/clusters/`).then(response => {
            return response.data
        })
    },
    getBcsMuster: async ({ commit }, { projectId, ccId }) => {
        return request.get(`backend/api/ci/paas-cd/apps/cc_app_ids/${ccId}/projects/${projectId}/musters/`).then(response => {
            return response.data
        })
    },
    getBcsMusterVersion: async ({ commit }, { projectId, ccId, musterId }) => {
        return request.get(`backend/api/ci/paas-cd/apps/cc_app_ids/${ccId}/projects/${projectId}/musters/${musterId}/versions`).then(response => {
            return response.data
        })
    },
    getBcsInstanceEntity: async ({ commit }, { projectId, ccId, versionId }) => {
        return request.get(`backend/api/ci/paas-cd/apps/cc_app_ids/${ccId}/projects/${projectId}/versions/${versionId}/templates`).then(response => {
            return response.data
        })
    },
    getTcmTemplate: async ({ commit }, { tcmId, appId }) => {
        return request.get(`plugin/api/user/tcm/templates?ccid=${appId}&tcmAppId=${tcmId}`).then(response => {
            return response.data
        })
    },
    getTcmTemplateParam: async ({ commit }, { tcmId, appId, templateId }) => {
        return request.get(`plugin/api/user/tcm/templateInfo?ccid=${appId}&tcmAppId=${tcmId}&templateId=${templateId}`).then(response => {
            return response.data
        })
    },
    // getGithubConfig: async ({ commit }, { projectId, pipelineId }) => {
    //     return request.get(`process/api/user/containers/${projectId}/${pipelineId}/webhookConfig`).then(response => {
    //         return response.data
    //     })
    // },
    getGithubAppUrl: async ({ commit }) => {
        return request.get(`process/api/user/github/githubAppUrl`).then(response => {
            return response.data
        })
    },
    getBcsNamespaces: async ({ commit }, { projectId, ccId, used = 1 }) => {
        return request.get(`backend/api/ci/paas-cd/ns/cc_app_ids/${ccId}/projects/${projectId}/namespaces/?used=${used}`).then(response => {
            return response.data
        })
    },
    getImageList: async ({ commit }, { projectId }) => {
        return request.get(`/image/api/user/image/${projectId}/listImages/`).then(response => {
            return response.data
        })
    },
    getImageTagList: async ({ commit }, { repo }) => {
        return request.get(`/image/api/user/image/getImageInfo?imageRepo=${repo}`).then(response => {
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
