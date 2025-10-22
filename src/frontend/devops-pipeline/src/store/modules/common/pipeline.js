
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
    ARTIFACTORY_API_URL_PREFIX,
    AUTH_URL_PREFIX,
    FETCH_ERROR,
    PROCESS_API_URL_PREFIX,
    QUALITY_API_URL_PREFIX,
    REPOSITORY_API_URL_PREFIX,
    STORE_API_URL_PREFIX
} from '@/store/constants'
import request from '@/utils/request'

import {
    INTERCEPT_ATOM_MUTATION,
    INTERCEPT_TEMPLATE_MUTATION,
    PIPELINE_TEMPLATE_MUTATION,
    QUALITY_ATOM_MUTATION,
    REFRESH_QUALITY_LOADING_MUNTATION,
    REPOSITORY_MUTATION,
    SET_PAC_SUPPORT_SCM_TYPE_LIST,
    SET_PROJECT_PERM,
    STORE_TEMPLATE_MUTATION,
    TEMPLATE_CATEGORY_MUTATION,
    TEMPLATE_MUTATION
} from './constants'

function rootCommit (commit, ACTION_CONST, payload) {
    commit(ACTION_CONST, payload, { root: true })
}

export const state = {
    templateCategory: null,
    refreshLoading: false,
    pipelineTemplateMap: new Map(),
    storeTemplate: null,
    template: null,
    reposList: null,
    appNodes: {},
    ruleList: [],
    templateRuleList: [],
    qualityAtom: [],
    pacSupportScmTypeList: [],
    hasProjectPermission: false
}

export const mutations = {
    [TEMPLATE_CATEGORY_MUTATION]: (state, { categoryList }) => {
        const customCategory = {
            categoryCode: 'custom',
            categoryName: (window.pipelineVue.$i18n && window.pipelineVue.$i18n.t('storeMap.projectCustom')) || 'projectCustom'
        }
        const storeCategory = {
            categoryCode: 'store',
            categoryName: (window.pipelineVue.$i18n && window.pipelineVue.$i18n.t('store')) || 'store'
        }
        return Object.assign(state, {
            templateCategory: [customCategory, ...categoryList, storeCategory]
        })
    },
    [PIPELINE_TEMPLATE_MUTATION]: (state, { pipelineTemplateMap }) => {
        return Object.assign(state, {
            pipelineTemplateMap
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

    [QUALITY_ATOM_MUTATION]: (state, { qualityAtom }) => {
        const atoms = []
        qualityAtom.forEach(item => atoms.push(...item.controlPoints))
        return Object.assign(state, {
            qualityAtom: atoms
        })
    },

    [INTERCEPT_ATOM_MUTATION]: (state, { ruleList }) => {
        const refreshLoading = false
        return Object.assign(state, {
            ruleList,
            refreshLoading
        })
    },

    [INTERCEPT_TEMPLATE_MUTATION]: (state, { templateRuleList }) => {
        const refreshLoading = false
        return Object.assign(state, {
            templateRuleList,
            refreshLoading
        })
    },

    [REPOSITORY_MUTATION]: (state, { records }) => {
        Object.assign(state, {
            reposList: records
        })
        return state
    },
    [REFRESH_QUALITY_LOADING_MUNTATION]: (state, status) => {
        const refreshLoading = status
        Object.assign(state, {
            refreshLoading
        })
        return state
    },
    [SET_PAC_SUPPORT_SCM_TYPE_LIST]: (state, pacSupportScmTypeList) => {
        Object.assign(state, {
            pacSupportScmTypeList
        })
    },
    [SET_PROJECT_PERM]: (state, hasProjectPermission) => {
        Object.assign(state, {
            hasProjectPermission
        })
    }
}

export const actions = {

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
            const pipelineTemplateMap = new Map()
            for (const key in (response?.data?.templates ?? {})) {
                const item = response.data.templates[key]
                pipelineTemplateMap.set(key, {
                    ...item,
                    isStore: item.templateType === 'CONSTRAINT'
                })
            }
            if (pipelineTemplateMap.size) { // 设置第一个模板为空模板
                const firstKey = pipelineTemplateMap.keys().next().value
                pipelineTemplateMap.get(firstKey).isEmptyTemplate = true
            }
            
            commit(PIPELINE_TEMPLATE_MUTATION, {
                pipelineTemplateMap
            })
        } catch (e) {
            rootCommit(commit, FETCH_ERROR, e)
        }
    },
    // 获取RD Store模板
    requestStoreTemplate: async ({ commit }, params) => {
        return request.get(`/${STORE_API_URL_PREFIX}/user/market/template/list`, { params })
    },

    requestQualityAtom: async ({ commit }, { projectId }) => {
        try {
            const response = await request.get(`/${QUALITY_API_URL_PREFIX}/user/controlPoints/v2/list?projectId=${projectId}`)

            commit(QUALITY_ATOM_MUTATION, {
                qualityAtom: response.data
            })
        } catch (e) {
            if (e.code === 403) {
                e.message = ''
            }
            rootCommit(commit, FETCH_ERROR, e)
        }
    },
    requestInterceptAtom: async ({ commit }, { projectId, ...params }) => {
        try {
            const response = await request.get(`/${QUALITY_API_URL_PREFIX}/user/rules/v2/${projectId}/matchRuleList`, { params })

            commit(INTERCEPT_ATOM_MUTATION, {
                ruleList: response.data
            })
        } catch (e) {
            if (e.code === 403) {
                e.message = ''
            }
            rootCommit(commit, FETCH_ERROR, e)
        }
    },
    requestPipelineCheckVersion: async ({ commit }, { projectId, pipelineId, atomCode, version }) => {
        return request.get(`/${QUALITY_API_URL_PREFIX}/user/rules/v2/project/${projectId}/pipeline/${pipelineId}/listAtomRule?atomCode=${atomCode}&atomVersion=${version}`).then(response => {
            return response.data
        })
    },
    requestTemplateCheckVersion: async ({ commit }, { projectId, templateId, atomCode, version }) => {
        return request.get(`/${QUALITY_API_URL_PREFIX}/user/rules/v2/project/${projectId}/template/${templateId}/listTemplateAtomRule?atomCode=${atomCode}&atomVersion=${version}`).then(response => {
            return response.data
        })
    },
    requestMatchTemplateRuleList: async ({ commit }, { projectId, templateId }) => {
        try {
            const response = await request.get(`/${QUALITY_API_URL_PREFIX}/user/rules/v2/${projectId}/matchTemplateRuleList?templateId=${templateId}`)
            console.log('get', response.data)
            commit(INTERCEPT_TEMPLATE_MUTATION, {
                templateRuleList: response.data
            })
        } catch (e) {
            if (e.code === 403) {
                e.message = ''
            }
            rootCommit(commit, FETCH_ERROR, e)
        }
    },
    startDebugDocker: async ({ commit }, data) => {
        return request.post('dispatch-docker/api/user/docker/debug/start/', data).then(response => {
            return response.data
        })
    },
    stopDebugDocker: async ({ commit }, { projectId, pipelineId, vmSeqId, dispatchType }) => {
        return request.post(`dispatch-docker/api/user/docker/debug/stop/projects/${projectId}/pipelines/${pipelineId}/vmseqs/${vmSeqId}?dispatchType=${dispatchType}`).then(response => {
            return response.data
        })
    },
    resizeTerm: async ({ commit }, { resizeUrl, params }) => {
        return request.post(`dispatch-docker/api/user/${resizeUrl}`, params).then(response => {
            return response && response.Id
        })
    },
    requestPartFile: async ({ commit }, { projectId, params }) => {
        return request.post(`${ARTIFACTORY_API_URL_PREFIX}/user/artifactories/${projectId}/search`, params).then(response => {
            return response.data
        })
    },
    requestOutputs: async ({ commit }, { projectId, pipelineId, buildId, ...params }) => {
        const hasBuildId = !!buildId
        const { data } = await request.post(`${ARTIFACTORY_API_URL_PREFIX}/user/pipeline/output/${projectId}/${pipelineId}/${hasBuildId ? `${buildId}/` : ''}search`, params)
        return {
            page: 1,
            pageSize: data.pageSize ?? data.length,
            count: data.count ?? data.length,
            records: data.records ?? data
        }
    },
    requestExternalUrl: async ({ commit }, { projectId, type, path }) => {
        return request.post(`${ARTIFACTORY_API_URL_PREFIX}/user/artifactories/${projectId}/${type}/externalUrl?path=${encodeURIComponent(path)}`).then(response => {
            return response.data
        })
    },

    requestDownloadUrl: async ({ commit }, { projectId, artifactoryType, path }) => {
        return request.post(`${ARTIFACTORY_API_URL_PREFIX}/user/artifactories/${projectId}/${artifactoryType}/downloadUrl?path=${encodeURIComponent(path)}`).then(response => {
            return response.data
        })
    },
    requestCustomFolder: async (_, { projectId, params }) => {
        const res = await request.get(`${ARTIFACTORY_API_URL_PREFIX}/user/custom-repo/${projectId}/dir/tree`, {
            params
        })
        return res.data
    },
    requestCopyArtifactory: async ({ commit }, { projectId, pipelineId, buildNo, params }) => {
        return request.post(`${ARTIFACTORY_API_URL_PREFIX}/user/artifactories/${projectId}/${pipelineId}/${buildNo}/copyToCustom`, params).then(response => {
            return response.data
        })
    },
    requestCopyArtifactories: async ({ commit }, params) => {
        return request.post(`${ARTIFACTORY_API_URL_PREFIX}/user/artifactories/file/copy`, params).then(response => {
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
        return request.get(`/${ARTIFACTORY_API_URL_PREFIX}/user/artifactories/${projectId}/${type}/show?path=${encodeURIComponent(path)}`).then(response => {
            return response.data
        })
    },
    getMetadataLabel: async ({ commit }, { projectId, pipelineId, debug }) => {
        return request.get(`/${ARTIFACTORY_API_URL_PREFIX}/user/artifactories/quality/metadata/${projectId}/pipeline/${pipelineId}?debug=${debug}`).then(response => {
            return response.data
        })
    },
    requestReportList: async ({ commit }, { projectId, pipelineId, buildId, taskId }) => {
        return request.get(`/${PROCESS_API_URL_PREFIX}/user/reports/${projectId}/${pipelineId}/${buildId}`, { params: { taskId } }).then(response => {
            return response.data
        })
    },
    reviewExcuteAtom: async ({ commit }, { projectId, pipelineId, buildId, elementId, action }) => {
        return request.post(`/${PROCESS_API_URL_PREFIX}/user/quality/builds/${projectId}/${pipelineId}/${buildId}/${elementId}/qualityGateReview/${action}`).then(response => {
            return response.data
        })
    },
    requestAuditUserList: async ({ commit }, { projectId, pipelineId, buildId, params }) => {
        return request.get(`${QUALITY_API_URL_PREFIX}/user/intercepts/${projectId}/${pipelineId}/${buildId}/auditUserList`, { params }).then(response => {
            return response.data
        })
    },
    requestTrendData: async ({ commit }, { pipelineId, startTime, endTime }) => {
        return request.get(`${ARTIFACTORY_API_URL_PREFIX}/user/pipeline/artifactory/construct/${pipelineId}/trend?startTime=${startTime}&endTime=${endTime}`).then(response => {
            return response.data
        })
    },

    updateRefreshQualityLoading: ({ commit }, status) => {
        commit(REFRESH_QUALITY_LOADING_MUNTATION, status)
    },
    getSupportPacScmTypeList: async ({ commit, state }) => {
        try {
            if (state.pacSupportScmTypeList.length) {
                return
            }
            const { data } = await request.get(`${REPOSITORY_API_URL_PREFIX}/user/repositories/pac/supportScmType`)
            commit(SET_PAC_SUPPORT_SCM_TYPE_LIST, data)
        } catch (e) {
            console.log(e)
        }
    },
    isPACOAuth: async (_, { projectId, ...query }) => {
        const { data } = await request.get(`${REPOSITORY_API_URL_PREFIX}/user/repositories/${projectId}/isOauth`, {
            params: query
        })
        return data
    },
    getPACRepoList: async (_, { projectId, ...params }) => {
        try {
            const { data } = await request.get(`${REPOSITORY_API_URL_PREFIX}/user/repositories/${projectId}/hasPermissionList`, {
                params
            })
            return data
        } catch (e) {
            console.log(e)
        }
    },
    getPACRepoCiDirList: (_, { projectId, repoHashId }) => {
        return request.get(`${REPOSITORY_API_URL_PREFIX}/user/repositories/pac/${projectId}/${repoHashId}/ciSubDir`)
    },
    validatePermission: async (_, { projectId, ...params }) => {
        return request.post(`${AUTH_URL_PREFIX}/user/auth/permission/batch/validate`, params, {
            headers: {
                'X-DEVOPS-PROJECT-ID': projectId
            }
        })
    }
}
