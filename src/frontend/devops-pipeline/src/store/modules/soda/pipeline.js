
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
import {
    FETCH_ERROR,
    PROCESS_API_URL_PREFIX,
    ARTIFACTORY_API_URL_PREFIX,
    REPOSITORY_API_URL_PREFIX,
    STORE_API_URL_PREFIX
} from '@/store/constants'

import {
    REPOSITORY_MUTATION,
    TEMPLATE_CATEGORY_MUTATION,
    PIPELINE_TEMPLATE_MUTATION,
    STORE_TEMPLATE_MUTATION,
    TEMPLATE_MUTATION,
    PIPELINE_SETTING_MUTATION,
    UPDATE_PIPELINE_SETTING_MUNTATION,
    RESET_PIPELINE_SETTING_MUNTATION
} from './constants'

function rootCommit (commit, ACTION_CONST, payload) {
    commit(ACTION_CONST, payload, { root: true })
}

export const state = {
    templateCategory: null,
    pipelineTemplate: null,
    storeTemplate: null,
    template: null,
    reposList: null,
    appNodes: {},
    pipelineSetting: {}
}

export const mutations = {
    [TEMPLATE_CATEGORY_MUTATION]: (state, { categoryList }) => {
        const customCategory = {
            categoryCode: 'custom',
            categoryName: '项目自定义'
        }
        const storeCategory = {
            categoryCode: 'store',
            categoryName: '研发商店'
        }
        return Object.assign(state, {
            templateCategory: [customCategory, ...categoryList, storeCategory]
        })
    },
    [PIPELINE_TEMPLATE_MUTATION]: (state, { pipelineTemplate }) => {
        return Object.assign(state, {
            pipelineTemplate
        })
    },
    [STORE_TEMPLATE_MUTATION]: (state, { storeTemplate }) => {
        return Object.assign(state, {
            storeTemplate
        })
    },
    [TEMPLATE_MUTATION]: (state, { template }) => {
        return Object.assign(state, {
            template
        })
    },

    [PIPELINE_SETTING_MUTATION]: (state, { pipelineSetting }) => {
        return Object.assign(state, {
            pipelineSetting
        })
    },

    [REPOSITORY_MUTATION]: (state, { records }) => {
        Object.assign(state, {
            reposList: records
        })
        return state
    },
    [UPDATE_PIPELINE_SETTING_MUNTATION]: (state, { container, param }) => {
        Object.assign(container, param)
        return state
    },
    [RESET_PIPELINE_SETTING_MUNTATION]: (state, payload) => {
        return Object.assign(state, {
            pipelineSetting: {}
        })
    }
}

export const actions = {
    requestTemplate: async ({ commit }, { projectId, templateId, version }) => {
        try {
            const url = version ? `/${PROCESS_API_URL_PREFIX}/user/templates/projects/${projectId}/templates/${templateId}?version=${version}` : `/${PROCESS_API_URL_PREFIX}/user/templates/projects/${projectId}/templates/${templateId}`
            const response = await request.get(url)
            commit(TEMPLATE_MUTATION, {
                template: response.data
            })
        } catch (e) {
            if (e.code === 403) {
                e.message = ''
            }
            rootCommit(commit, FETCH_ERROR, e)
        }
    },
    // 获取模板的所有范畴
    requestCategory: async ({ commit }) => {
        try {
            const response = await request.get(`/${STORE_API_URL_PREFIX}/user/market/template/categorys`)
            commit(TEMPLATE_CATEGORY_MUTATION, {
                categoryList: response.data
            })
        } catch (e) {
            rootCommit(commit, FETCH_ERROR, e)
        }
    },
    // 新增流水线时拉取模板
    requestPipelineTemplate: async ({ commit }, { projectId }) => {
        try {
            const response = await request.get(`/${PROCESS_API_URL_PREFIX}/user/templates/projects/${projectId}/allTemplates`)
            commit(PIPELINE_TEMPLATE_MUTATION, {
                pipelineTemplate: (response.data || {}).templates
            })
        } catch (e) {
            rootCommit(commit, FETCH_ERROR, e)
        }
    },
    // 获取RD Store模板
    requestStoreTemplate: async ({ commit }, { templateName, category }) => {
        const params = Object.assign({ page: 1, pageSize: 1000 }, { templateName: templateName, categoryCode: category })
        try {
            const response = await request.get(`/${STORE_API_URL_PREFIX}/user/market/template/list`, { params })
            commit(STORE_TEMPLATE_MUTATION, {
                storeTemplate: response.data.records
            })
        } catch (e) {
            rootCommit(commit, FETCH_ERROR, e)
        }
    },
    requestPipelineSetting: async ({ commit }, { projectId, pipelineId }) => {
        try {
            const response = await request.get(`/${PROCESS_API_URL_PREFIX}/user/setting/get?pipelineId=${pipelineId}&projectId=${projectId}`)
            commit(PIPELINE_SETTING_MUTATION, {
                pipelineSetting: response.data
            })
        } catch (e) {
            if (e.code === 403) {
                e.message = ''
            }
            rootCommit(commit, FETCH_ERROR, e)
        }
    },
    requestTemplateSetting: async ({ commit }, { projectId, templateId }) => {
        try {
            const response = await request.get(`/${PROCESS_API_URL_PREFIX}/user/templates/projects/${projectId}/templates/${templateId}/settings`)
            commit(PIPELINE_SETTING_MUTATION, {
                pipelineSetting: response.data
            })
        } catch (e) {
            if (e.code === 403) {
                e.message = ''
            }
            rootCommit(commit, FETCH_ERROR, e)
        }
    },
    requestPartFile: async ({ commit }, { projectId, params }) => {
        return request.post(`${ARTIFACTORY_API_URL_PREFIX}/user/artifactories/${projectId}/search`, params).then(response => {
            return response.data
        })
    },
    requestExternalUrl: async ({ commit }, { projectId, artifactoryType, path }) => {
        return request.post(`${ARTIFACTORY_API_URL_PREFIX}/user/artifactories/${projectId}/${artifactoryType}/externalUrl?path=${path}`).then(response => {
            return response.data
        })
    },
    requestDownloadUrl: async ({ commit }, { projectId, artifactoryType, path }) => {
        return request.post(`${ARTIFACTORY_API_URL_PREFIX}/user/artifactories/${projectId}/${artifactoryType}/downloadUrl?path=${path}`).then(response => {
            return response.data
        })
    },
    requestExecPipPermission: async ({ commit }, { projectId, pipelineId, permission }) => {
        return request.get(`${PROCESS_API_URL_PREFIX}/user/pipelines/${projectId}/${pipelineId}/hasPermission?permission=${permission}`).then(response => {
            return response.data
        })
    },
    requestCommitList: async ({ commit }, { buildId }) => {
        return request.get(`${REPOSITORY_API_URL_PREFIX}/user/repositories/${buildId}/commit/get/record`).then(response => {
            return response.data
        })
    },
    requestFileInfo: async ({ commit }, { projectId, path, type }) => {
        return request.get(`/${ARTIFACTORY_API_URL_PREFIX}/user/artifactories/${projectId}/${type}/show?path=${path}`).then(response => {
            return response.data
        })
    },
    requestReportList: async ({ commit }, { projectId, pipelineId, buildId }) => {
        return request.get(`/${PROCESS_API_URL_PREFIX}/user/reports/${projectId}/${pipelineId}/${buildId}`).then(response => {
            return response.data
        })
    },
    /**
     * wetest测试报告
     */
    requestWetestReport: async ({ commit }, { projectId, pipelineId, buildId }) => {
        return request.get(`wetest/api/user/wetest/taskInst/${projectId}/listByBuildId?pipelineId=${pipelineId}&buildId=${buildId}`).then(response => {
            return response.data
        })
    },
    requestRepository: async ({ commit }, payload) => {
        try {
            const { data } = await request.get(`/${REPOSITORY_API_URL_PREFIX}/user/repositories/${payload.projectId}?repositoryType=${payload.repoType}`)
            commit(REPOSITORY_MUTATION, data)
        } catch (e) {
            rootCommit(commit, FETCH_ERROR, e)
        }
    },

    updatePipelineSetting: ({ commit }, payload) => {
        commit(UPDATE_PIPELINE_SETTING_MUNTATION, payload)
    },
    resetPipelineSetting: ({ commit }, payload) => {
        commit(RESET_PIPELINE_SETTING_MUNTATION, payload)
    }
}

export const getters = {
    getAppNodes: state => (os) => state.appNodes[os] || {},
    getHasAtomCheck: state => (stages, atom) => {
        return stages.some((stage, index) => {
            if (index) {
                return stage.containers.some(container => {
                    return container.elements.find(el => {
                        return el['@type'] === atom
                    })
                })
            }
        })
    }
}
