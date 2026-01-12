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

import { RESOURCE_ACTION, RESOURCE_TYPE } from '@/utils/permission'
import Vue from 'vue'

import {
    DIALOG_LOADING_MUTATION,
    FETCH_ERROR,
    PROCESS_API_URL_PREFIX,
    REPOSITORY_API_URL_PREFIX,
    SET_CODELIBS_MUTATION,
    SET_CODELIB_TYPES,
    SET_OAUTH_MUTATION,
    SET_TEMPLATE_CODELIB,
    SET_TICKETS_MUTATION,
    SET_T_GIT_OAUTH_MUTATION,
    STORE_API_URL_PREFIX,
    TICKET_API_URL_PREFIX,
    TOGGLE_CODE_LIB_DIALOG,
    UPDATE_CODE_LIB_MUTATION,
    SET_PROVIDER_CONFIG
} from './constants'
const vue = new Vue()

const actions = {
    async fetchCodeTypeList ({
        commit
    }) {
        try {
            const result = await vue.$ajax.get(`${REPOSITORY_API_URL_PREFIX}/user/repositories/config/`)
            commit(SET_CODELIB_TYPES, result)
            return result.data
        } catch (error) {
            
        }
    },
    /**
     * 获取代码库列表
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     *
     * @return {Promise} promise 对象
     */
    async requestList ({
        commit,
        state,
        dispatch
    }, {
        projectId,
        aliasName = '',
        page = 1,
        pageSize = 12,
        sortType = '',
        sortBy = ''
    }) {
        try {
            const response = await vue.$ajax.get(`${REPOSITORY_API_URL_PREFIX}/user/repositories/${projectId}/search?aliasName=${aliasName}&page=${page}&pageSize=${pageSize}&sortBy=${sortBy}&sortType=${sortType}`)
            commit(SET_CODELIBS_MUTATION, {
                codelibs: response
            })
        } catch (e) {
            commit(FETCH_ERROR, e, {
                root: true
            })
        }
    },

    /**
     * 获取凭据列表
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     *
     * @return {Promise} promise 对象
     */
    async requestTickets ({
        commit,
        state,
        dispatch
    }, {
        projectId,
        credentialTypes,
        permission = 'USE'
    }) {
        try {
            if (!credentialTypes) return
            const response = await vue.$ajax.get(`${TICKET_API_URL_PREFIX}/user/credentials/${projectId}/hasPermissionList?permission=${permission}&credentialTypes=${credentialTypes}&page=1&pageSize=1000`)
            commit(SET_TICKETS_MUTATION, {
                tickets: response.records
            })
        } catch (e) {
            commit(FETCH_ERROR, e, {
                root: true
            })
        }
    },
    /**
     * 新增代码库
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     *
     * @return {Promise} promise 对象
     */
    createRepo ({
        commit,
        state,
        dispatch
    }, {
        projectId,
        params
    }) {
        return vue.$ajax.post(`${REPOSITORY_API_URL_PREFIX}/user/repositories/${projectId}`, {
            ...params
        })
    },
    /**
     * 编辑代码库
     */
    editRepo ({ commit },
        {
            projectId,
            repositoryHashId,
            params
        }) {
        return vue.$ajax.put(`${REPOSITORY_API_URL_PREFIX}/user/repositories/${projectId}/${repositoryHashId}`,
            params
        )
    },
    /**
     * 删除指定代码库
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     *
     * @return {Promise} promise 对象
     */
    deleteRepo ({
        commit,
        state,
        dispatch
    }, {
        projectId,
        repositoryHashId
    }) {
        return vue.$ajax.delete(`${REPOSITORY_API_URL_PREFIX}/user/repositories/${projectId}/${repositoryHashId}`)
    },
    /**
     * 代码库详情
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     *
     * @return {Promise} promise 对象
     */
    async requestDetail ({
        commit,
        state,
        dispatch
    }, {
        projectId,
        repositoryHashId,
        instance
    }) {
        try {
            const codelib = await vue.$ajax.get(`${REPOSITORY_API_URL_PREFIX}/user/repositories/${projectId}/${repositoryHashId}`)
            dispatch('setCodelib', {
                ...codelib,
                repositoryHashId
            })
        } catch (e) {
            instance.handleError(
                e,
                {
                    projectId,
                    resourceType: RESOURCE_TYPE,
                    resourceCode: repositoryHashId,
                    action: RESOURCE_ACTION.VIEW
                }
            )
        }
    },
    async toggleCodelibDialog ({
        commit,
        state,
        dispatch
    }, {
        showCodelibDialog,
        projectId,
        repositoryHashId,
        credentialTypes,
        credentialType,
        permission,
        typeName,
        authType,
        svnType,
        codelib,
        instance,
        scmCode = '',
        userName
    }) {
        try {
            commit(TOGGLE_CODE_LIB_DIALOG, {
                showCodelibDialog
            })
            if (!showCodelibDialog) return
            if (repositoryHashId) {
                commit(DIALOG_LOADING_MUTATION, true)
                await Promise.all([
                    dispatch('requestDetail', {
                        projectId,
                        repositoryHashId,
                        instance
                    }),
                    dispatch('requestTickets', {
                        projectId,
                        credentialTypes,
                        permission
                    })
                ])
                commit(SET_TEMPLATE_CODELIB, codelib)
                commit(DIALOG_LOADING_MUTATION, false)
            } else {
                dispatch('setCodelib', {
                    '@type': typeName,
                    aliasName: '',
                    credentialId: '',
                    projectName: '',
                    url: '',
                    credentialTypes,
                    credentialType,
                    authType,
                    svnType,
                    scmCode,
                    userName
                })
            }
        } catch (e) {
            commit(FETCH_ERROR, e, {
                root: true
            })
        }
    },
    updateCodelib ({
        commit,
        state,
        dispatch
    }, codelib) {
        commit(UPDATE_CODE_LIB_MUTATION, {
            replace: false,
            codelib
        })
    },
    setCodelib ({
        commit,
        state,
        dispatch
    }, codelib) {
        commit(UPDATE_CODE_LIB_MUTATION, {
            replace: true,
            codelib
        })
    },

    async checkScmOAuth ({
        commit,
        state
    }, {
        projectId,
        scmCode,
        type,
        search = '',
        username
    }) {
        try {
            const query = {
                projectId,
                scmCode,
                search,
                oauthUserId: username
            }
            commit(DIALOG_LOADING_MUTATION, true)
            const queryStr = Object.keys(query).filter(key => query[key]).map(key => `${key}=${query[key]}`).join('&')
            const res = await vue.$ajax.get(`/repository/api/user/scm/repository/api/${projectId}/${scmCode}/listRepoBaseInfo?${queryStr}`)
            commit(SET_OAUTH_MUTATION, {
                oAuth: res,
                type
            })
            commit(DIALOG_LOADING_MUTATION, false)
        } catch (e) {
            commit(FETCH_ERROR, e, {
                root: true
            })
        }
    },
    /**
     * git & github OAuth授权
     * @returns {Promise<void>}
     */
    async checkOAuth ({
        commit,
        state
    }, {
        projectId,
        repositoryHashId,
        search = '',
        type = 'git',
        username
    }) {
        try {
            const query = {
                projectId,
                repositoryHashId,
                search,
                oauthUserId: username
            }
            commit(DIALOG_LOADING_MUTATION, true)
            const queryStr = Object.keys(query).filter(key => query[key]).map(key => `${key}=${query[key]}`).join('&')
            const res = await vue.$ajax.get(`/repository/api/user/${type}/getProject?${queryStr}`)
            const projectIndex = res?.project?.findIndex(project => project.httpUrl === state.templateCodeLib?.url)
            if (projectIndex < 0 && state.templateCodeLib?.url) {
                res.project.push({
                    nameWithNameSpace: state.templateCodeLib?.aliasName,
                    httpUrl: state.templateCodeLib?.url
                })
            }
            commit(SET_OAUTH_MUTATION, {
                oAuth: res,
                type
            })
            commit(DIALOG_LOADING_MUTATION, false)
        } catch (e) {
            commit(FETCH_ERROR, e, {
                root: true
            })
        }
    },
    /**
     * git & github OAuth授权
     * @returns {Promise<void>}
     */
    async checkTGitOAuth ({
        commit,
        state
    }, {
        projectId,
        repositoryHashId,
        type = 'tgit'
    }) {
        try {
            const query = {
                projectId,
                repositoryHashId
            }
            const queryStr = Object.keys(query).filter(key => query[key]).map(key => `${key}=${query[key]}`).join('&')
            const res = await vue.$ajax.get(`/repository/api/user/${type}/getProject?${queryStr}`)
            commit(SET_T_GIT_OAUTH_MUTATION, {
                oAuth: res,
                type
            })
        } catch (e) {
            commit(FETCH_ERROR, e, {
                root: true
            })
        }
    },

    setTemplateCodelib ({
        commit
    }, codeLib) {
        commit(SET_TEMPLATE_CODELIB, codeLib)
    },

    changeMrBlock ({ commit }, {
        projectId,
        repositoryHashId,
        enableMrBlock
    }) {
        return vue.$ajax.put(`${REPOSITORY_API_URL_PREFIX}/user/repositories/${projectId}/${repositoryHashId}/updateRepoSetting`, {
            enableMrBlock
        })
    },

    checkPacProject ({ commit }, {
        repoUrl,
        repositoryType
    }) {
        return vue.$ajax.get(`${REPOSITORY_API_URL_PREFIX}/user/repositories/pac/getPacProjectId/?repoUrl=${repoUrl}&repositoryType=${repositoryType}`)
    },

    /**
     * 刷新 scm git/svn 工蜂授权token
     */
    async refreshScmOauth ({ commit }, {
        scmCode,
        redirectUrl,
        resetType = ''
    }) {
        const res = await vue.$ajax.post(`${REPOSITORY_API_URL_PREFIX}/user/repositories/oauth/reset?resetType=${resetType}&scmCode=${scmCode}&redirectUrl=${redirectUrl}&`)
        return res
    },

    /**
     * 刷新 git / tgit 工蜂授权token
     */
    async refreshGitOauth ({ commit }, {
        type,
        resetType = '',
        redirectUrl = '',
        refreshToken = false
    }) {
        const res = await vue.$ajax.get(`${REPOSITORY_API_URL_PREFIX}/user/${type}/isOauth?validationCheck=true&resetType=${resetType}&redirectUrl=${redirectUrl}&refreshToken=${refreshToken}`)
        commit(SET_OAUTH_MUTATION, {
            oAuth: res,
            type
        })
        return res
    },

    /**
     * 刷新github授权token
     */
    async refreshGithubOauth ({ commit }, {
        projectId,
        resetType = '',
        redirectUrl = '',
        refreshToken = false
    }) {
        const res = await vue.$ajax.get(`${REPOSITORY_API_URL_PREFIX}/user/github/isOauth?projectId=${projectId}&validationCheck=true&resetType=${resetType}&redirectUrl=${redirectUrl}&refreshToken=${refreshToken}`)
        commit(SET_OAUTH_MUTATION, {
            oAuth: res,
            type: 'github'
        })
        return res
    },

    /**
     * 重命名-代码库别名
     */
    renameAliasName ({ commit }, {
        projectId,
        repositoryHashId,
        params
    }) {
        return vue.$ajax.put(`${REPOSITORY_API_URL_PREFIX}/user/repositories/${projectId}/${repositoryHashId}/rename`, params)
    },
    /**
     * 关闭PAC校验- 仓库是否存在.ci文件夹
     */
    checkHasCiFolder ({ commit }, {
        projectId,
        repositoryHashId
    }) {
        return vue.$ajax.put(`${REPOSITORY_API_URL_PREFIX}/user/repositories/pac/${projectId}/${repositoryHashId}/checkCiDirExists`)
    },

    /**
     * 关闭PAC
     */
    closePac ({ commit }, {
        projectId,
        repositoryHashId
    }) {
        return vue.$ajax.put(`${REPOSITORY_API_URL_PREFIX}/user/repositories/pac/${projectId}/${repositoryHashId}/disable`)
    },

    /**
     * 开启PAC
     */
    enablePac ({ commit }, {
        projectId,
        repositoryHashId
    }) {
        return vue.$ajax.put(`${REPOSITORY_API_URL_PREFIX}/user/repositories/pac/${projectId}/${repositoryHashId}/enable`)
    },

    /**
     * 获取代码库关联的流水线列表
     */
    fetchUsingPipelinesList ({ commit }, {
        projectId,
        repositoryHashId,
        page,
        pageSize,
        eventType,
        triggerConditionMd5,
        taskRepoType
    }) {
        return vue.$ajax.get(`${REPOSITORY_API_URL_PREFIX}/user/repositories/${projectId}/${repositoryHashId}/listRepoPipelineRef`, {
            params: {
                page,
                pageSize,
                eventType,
                triggerConditionMd5,
                taskRepoType
            }
        })
    },

    /**
     * 获取触发信息列表
     */
    fetchTriggerEventList ({ commit }, {
        projectId,
        repositoryHashId,
        page,
        pageSize,
        triggerType,
        eventId = '',
        eventType = '',
        triggerUser = '',
        pipelineId = '',
        startTime = '',
        endTime = '',
        reason = ''
    }) {
        return vue.$ajax.get(`${PROCESS_API_URL_PREFIX}/user/trigger/event/${projectId}/${repositoryHashId}/listRepoTriggerEvent?page=${page}&pageSize=${pageSize}&triggerType=${triggerType}&eventType=${eventType}&triggerUser=${triggerUser}&pipelineId=${pipelineId}&startTime=${startTime}&endTime=${endTime}&eventId=${eventId}&reason=${reason}`)
    },

    /**
     * 获取触发事件详情
     */
    fetchEventDetail ({ commit }, {
        projectId,
        eventId,
        page,
        pageSize,
        reason,
        pipelineId
    }) {
        let queryUrl = ''
        queryUrl = pipelineId ? `page=${page}&pageSize=${pageSize}&reason=${reason}&pipelineId=${pipelineId}` : `reason=${reason}&page=${page}&pageSize=${pageSize}`
        return vue.$ajax.get(`${PROCESS_API_URL_PREFIX}/user/trigger/event/${projectId}/${eventId}/listEventDetail?${queryUrl}`)
    },

    /**
     * 一键重新触发
     */
    replayAllEvent ({ commit }, {
        projectId,
        eventId
    }) {
        return vue.$ajax.post(`${PROCESS_API_URL_PREFIX}/user/trigger/event/${projectId}/${eventId}/replayAll`)
    },

    /**
     * 重新触发
     */
    replayEvent ({ commit }, {
        projectId,
        detailId
    }) {
        return vue.$ajax.post(`${PROCESS_API_URL_PREFIX}/user/trigger/event/${projectId}/${detailId}/replay`)
    },

    /**
     * 获取事件类型
     */

    fetchEventType ({ commit }, { scmType }) {
        return vue.$ajax.get(`${PROCESS_API_URL_PREFIX}/user/trigger/event/listEventType?scmType=${scmType}`)
    },

    /**
     * 获取事件类型
     */

    fetchTriggerType ({ commit }, { scmType }) {
        return vue.$ajax.get(`${PROCESS_API_URL_PREFIX}/user/trigger/event/listTriggerType?scmType=${scmType}`)
    },

    fetchTriggerData ({ commit }, {
        projectId,
        repositoryHashId,
        page,
        pageSize,
        triggerType,
        eventType
    }) {
        return vue.$ajax.get(`${REPOSITORY_API_URL_PREFIX}/user/repositories/${projectId}/${repositoryHashId}/listTriggerRef?page=${page}&pageSize=${pageSize}&eventType=${eventType}&triggerType=${triggerType}`)
    },
    
    fetchAtomModal ({ commit }, {
        projectCode,
        atomCode,
        queryOfflineFlag = false
    }) {
        const version = atomCode === 'codeGitWebHookTrigger' ? '2.*' : '1.*'
        return vue.$ajax.get(`${STORE_API_URL_PREFIX}/user/pipeline/atom/${projectCode}/${atomCode}/${version}?queryOfflineFlag=${queryOfflineFlag}`)
    },

    /**
     * PAC - 重试同步YAML
     */
    retrySyncRepository ({ commit }, {
        projectId,
        repositoryHashId
    }) {
        return vue.$ajax.put(`${REPOSITORY_API_URL_PREFIX}/user/repositories/pac/${projectId}/${repositoryHashId}/retry`)
    },

    /**
     * PAC - 获取Yaml同步状态
     */
    getYamlSyncStatus ({ commit }, {
        projectId,
        repositoryHashId
    }) {
        return vue.$ajax.get(`${REPOSITORY_API_URL_PREFIX}/user/repositories/pac/${projectId}/${repositoryHashId}/getYamlSyncStatus`)
    },

    /**
     * PAC - 获取开启pac的流水线数量
     */
    getPacPipelineCount ({ commit }, {
        projectId,
        repositoryHashId
    }) {
        return vue.$ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/yaml/${projectId}/${repositoryHashId}/count`)
    },

    /**
     * PAC - 获取同步失败的流水线列表
     */
    getListYamlSync ({ commit }, {
        projectId,
        repositoryHashId
    }) {
        return vue.$ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/yaml/${projectId}/${repositoryHashId}/listSyncFailedYaml`)
    },

    getYamlPipelines ({ commit }, {
        projectId,
        repositoryHashId,
        page,
        pageSize
    }) {
        return vue.$ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/yaml/${projectId}/${repositoryHashId}/listYamlPipeline?page=${page}&pageSize=${pageSize}`)
    },
    
    fetchPipelinesByName ({ commit }, {
        projectId,
        keyword = ''
    }) {
        return vue.$ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipelineInfos/${projectId}/searchByName?pipelineName=${keyword}`).then(data => data.map(_ => ({
            id: _.pipelineId,
            name: _.pipelineName
        })))
    },
    fetchTriggerReasonNum ({ commit }, {
        projectId,
        eventId,
        pipelineId
    }) {
        return vue.$ajax.get(`${PROCESS_API_URL_PREFIX}/user/trigger/event/${projectId}/${eventId}/triggerReasonStatistics?pipelineId=${pipelineId}`)
    },
    setProviderConfig ({
        commit
    }, value) {
        commit(SET_PROVIDER_CONFIG, value)
    },
    getOauthUserList ({ commit }, { scmCode }) {
        return vue.$ajax.get(`${REPOSITORY_API_URL_PREFIX}/user/repositories/oauth/userList?scmCode=${scmCode}`)
    }
}

export default actions
