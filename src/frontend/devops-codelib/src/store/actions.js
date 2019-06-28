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

import Vue from 'vue'
import {
    REPOSITORY_API_URL_PREFIX,
    SET_CODELIBS_MUTATION,
    TICKET_API_URL_PREFIX,
    SET_TICKETS_MUTATION,
    UPDATE_CODE_LIB_MUTATION,
    TOGGLE_CODE_LIB_DIALOG,
    FETCH_ERROR,
    DIALOG_LOADING_MUTATION,
    SET_OAUTH_MUTATION,
    SET_T_GIT_OAUTH_MUTATION
} from './constants'
const vue = new Vue()

const actions = {
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
        page = 1,
        pageSize = 12
    }) {
        try {
            const response = await vue.$ajax.get(`${REPOSITORY_API_URL_PREFIX}/user/repositories/${projectId}?page=${page}&pageSize=${pageSize}`)
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
            const response = await vue.$ajax.get(`${TICKET_API_URL_PREFIX}/user/credentials/${projectId}/hasPermissionList?permission=${permission}&credentialTypes=${credentialTypes}`)
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
     * 新增or编辑代码库
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     *
     * @return {Promise} promise 对象
     */
    createOrEditRepo ({
        commit,
        state,
        dispatch
    }, {
        projectId,
        hashId,
        params
    }) {
        return vue.$ajax[`${hashId ? 'put' : 'post'}`](`${REPOSITORY_API_URL_PREFIX}/user/repositories/${projectId}/${hashId ? `${hashId}` : ''}`, {
            ...params
        })
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
        repositoryHashId
    }) {
        try {
            const codelib = await vue.$ajax.get(`${REPOSITORY_API_URL_PREFIX}/user/repositories/${projectId}/${repositoryHashId}`)
            dispatch('setCodelib', {
                ...codelib,
                repositoryHashId
            })
        } catch (e) {
            commit(FETCH_ERROR, e, {
                root: true
            })
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
        permission,
        typeName,
        authType,
        svnType
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
                        repositoryHashId
                    }),
                    dispatch('requestTickets', {
                        projectId,
                        credentialTypes,
                        permission
                    })
                ])
                commit(DIALOG_LOADING_MUTATION, false)
            } else {
                dispatch('setCodelib', {
                    '@type': typeName,
                    aliasName: '',
                    credentialId: '',
                    projectName: '',
                    url: '',
                    authType,
                    svnType
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
        type = 'git'
    }) {
        try {
            const query = {
                projectId,
                repositoryHashId
            }
            const queryStr = Object.keys(query).filter(key => query[key]).map(key => `${key}=${query[key]}`).join('&')
            const res = await vue.$ajax.get(`/repository/api/user/${type}/getProject?${queryStr}`)
            commit(SET_OAUTH_MUTATION, {
                oAuth: res,
                type
            })
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
    }
}

export default actions
