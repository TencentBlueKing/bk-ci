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
    
} from './constant'

const prefix = '/ticket/api'
const vue = new Vue()

const actions = {
    /**
     * 凭据列表
     */
    requestCredentialList ({ commit }, { projectId, page, pageSize }) {
        return vue.$ajax.get(`${prefix}/user/credentials/${projectId}?page=${page}&pageSize=${pageSize}`).then(response => {
            return response
        })
    },
    /**
     * 删除凭据
     */
    toDeleteCredential ({ commit }, { projectId, id }) {
        return vue.$ajax.delete(`${prefix}/user/credentials/${projectId}/${id}`).then(response => {
            return response
        })
    },
    /**
     * 新增凭据权限
     */
    requestCredentialPermission ({ commit }, { projectId }) {
        return vue.$ajax.get(`${prefix}/user/credentials/${projectId}/hasCreatePermission`).then(response => {
            return response
        })
    },
    /**
     * 获取单条凭据详情
     */
    requestCredentialDetail ({ commit }, { projectId, creId }) {
        return vue.$ajax.get(`${prefix}/user/credentials/${projectId}/${creId}`).then(response => {
            return response
        })
    },
    /**
     * 新增凭据
     */
    createCredential ({ commit }, { projectId, credential }) {
        return vue.$ajax.post(`${prefix}/user/credentials/${projectId}`, credential).then(response => {
            return response
        })
    },
    /**
     * 编辑凭据
     */
    editCredential ({ commit }, { projectId, creId, credential }) {
        return vue.$ajax.put(`${prefix}/user/credentials/${projectId}/${creId}`, credential).then(response => {
            return response
        })
    },
    /**
     * 证书列表
     */
    requestCertificateList ({ commit }, { projectId, page, pageSize }) {
        return vue.$ajax.get(`${prefix}/user/certs/${projectId}?page=${page}&pageSize=${pageSize}`).then(response => {
            return response
        })
    },
    /**
     * 删除证书
     */
    toDeleteCerts ({ commit }, { projectId, id }) {
        return vue.$ajax.delete(`${prefix}/user/certs/${projectId}/${id}`).then(response => {
            return response
        })
    },
    /**
     * 新增凭据权限
     */
    requestCertsPermission ({ commit }, { projectId }) {
        return vue.$ajax.get(`${prefix}/user/certs/${projectId}/hasCreatePermission`).then(response => {
            return response
        })
    },
    /**
     * use权限凭据获取
     */
    requestCreditByPermission ({ commit, state, dispatch }, { projectId, permission, creTypes }) {
        return vue.$ajax.get(`${prefix}/user/credentials/${projectId}/hasPermissionList?permission=${permission}&page=1&pageSize=100${creTypes ? `&credentialTypes=${creTypes}` : ''}`).then(response => {
            return response
        })
    },
    /**
     * u新增证书
     */
    createCert ({ commit }, { url, formData, config }) {
        return vue.$ajax.post(`${prefix}/user/${url}`, formData, config).then(response => {
            return response
        })
    },
    /**
     * 证书获取
     */
    requestCert ({ commit, state, dispatch }, projectId) {
        return vue.$ajax.get(`/certs/${projectId}?page=1&pageSize=100`).then(response => {
            return response.data
        })
    },
    /**
     * 证书详情获取
     */
    requestCertDetail ({ commit, state, dispatch }, { projectId, certType, certId }) {
        return vue.$ajax.get(`${prefix}/user/certs/${projectId}/${certType}?certId=${certId}`).then(response => {
            return response
        })
    },
    /**
     * 编辑证书
     */
    editCert ({ commit }, { url, formData, config }) {
        return vue.$ajax.put(`${prefix}/user/${url}`, formData, config).then(response => {
            return response
        })
    }
}

export default actions
