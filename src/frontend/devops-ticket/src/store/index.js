
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

import actions from './actions'
import mutations from './mutations'

const store = {
    namespaced: true,
    state: {
        ticket: {
            PASSWORD: {
                v1: {
                    label: 'ticket.credential.password',
                    component: 'vue-input',
                    rules: 'required',
                    rule: { required: true },
                    required: true,
                    default: '',
                    placeholder: 'ticket.credential.passwordPlaceholder',
                    errorMsg: 'ticket.credential.passwordRequired',
                    type: 'password',
                    modelName: 'v1'
                }
            },
            MULTI_LINE_PASSWORD: {
                v1: {
                    label: 'ticket.credential.password',
                    component: 'vue-textarea',
                    rules: 'required',
                    rule: { required: true },
                    required: true,
                    default: '',
                    placeholder: 'ticket.credential.passwordPlaceholder',
                    errorMsg: 'ticket.credential.passwordRequired',
                    type: 'multiLinePassword',
                    modelName: 'v1'
                }
            },
            USERNAME_PASSWORD: {
                v1: {
                    label: 'ticket.credential.username',
                    component: 'vue-input',
                    rules: 'required',
                    rule: { required: true },
                    default: '',
                    placeholder: 'ticket.credential.usernamePlaceholder',
                    errorMsg: 'ticket.credential.usernameRequired',
                    modelName: 'v1'
                },
                v2: {
                    label: 'ticket.credential.password',
                    component: 'vue-input',
                    rules: 'required',
                    rule: { required: true },
                    required: true,
                    default: '',
                    placeholder: 'ticket.credential.passwordPlaceholder',
                    errorMsg: 'ticket.credential.passwordRequired',
                    type: 'password',
                    modelName: 'v2'
                }
            },
            ACCESSTOKEN: {
                v1: {
                    label: 'ticket.credential.accessToken',
                    component: 'vue-input',
                    rules: 'required',
                    rule: { required: true },
                    default: '',
                    placeholder: 'ticket.credential.accessTokenRequired',
                    errorMsg: 'ticket.credential.accessTokenRequired',
                    modelName: 'v1',
                    type: 'password'
                }
            },
            SECRETKEY: {
                v1: {
                    label: 'ticket.credential.secretKey',
                    component: 'vue-input',
                    rules: 'required',
                    rule: { required: true },
                    default: '',
                    placeholder: 'ticket.credential.secretKeyPlaceholder',
                    errorMsg: 'ticket.credential.secretKeyRequired',
                    type: 'password',
                    modelName: 'v1'
                }
            },
            APPID_SECRETKEY: {
                v1: {
                    label: 'ticket.credential.appId',
                    component: 'vue-input',
                    rules: 'required',
                    rule: { required: true },
                    default: '',
                    placeholder: 'ticket.credential.appIdPlaceholder',
                    errorMsg: 'ticket.credential.appIdRequired',
                    modelName: 'v1'
                },
                v2: {
                    label: 'ticket.credential.secretKey',
                    component: 'vue-input',
                    rules: 'required',
                    rule: { required: true },
                    default: '',
                    placeholder: 'ticket.credential.secretKeyPlaceholder',
                    errorMsg: 'ticket.credential.secretKeyRequired',
                    type: 'password',
                    modelName: 'v2'
                }
            },
            SSH_PRIVATEKEY: {
                v1: {
                    label: 'ticket.credential.sshKey',
                    component: 'vue-textarea',
                    rules: 'required',
                    rule: { regex: /^(-----BEGIN (RSA|OPENSSH) PRIVATE KEY-----){1}[\s\S]*(-----END (RSA|OPENSSH) PRIVATE KEY-----)$/, required: true },
                    default: '',
                    placeholder: 'ticket.credential.sshKeyPlaceholder',
                    errorMsg: 'ticket.credential.sshKeyPlaceholder',
                    modelName: 'v1'
                },
                v2: {
                    label: 'ticket.credential.keyPassword',
                    component: 'vue-input',
                    rules: '',
                    rule: {},
                    default: '',
                    placeholder: 'ticket.credential.keyPasswordPlaceholder',
                    errorMsg: '',
                    type: 'password',
                    modelName: 'v2'
                }
            },
            TOKEN_SSH_PRIVATEKEY: {
                v1: {
                    label: 'ticket.credential.privateToken',
                    component: 'vue-input',
                    rules: 'required',
                    rule: { required: true },
                    default: '',
                    placeholder: 'ticket.credential.tokenPlaceholder',
                    errorMsg: 'ticket.credential.tokenRequired',
                    type: 'password',
                    modelName: 'v1'
                },
                v2: {
                    label: 'ticket.credential.sshKey',
                    component: 'vue-textarea',
                    rules: 'required',
                    rule: { regex: /^(-----BEGIN (RSA|OPENSSH) PRIVATE KEY-----){1}[\s\S]*(-----END (RSA|OPENSSH) PRIVATE KEY-----)$/, required: true },
                    default: '',
                    placeholder: 'ticket.credential.sshKeyPlaceholder',
                    errorMsg: 'ticket.credential.sshKeyPlaceholder',
                    modelName: 'v2'
                },
                v3: {
                    label: 'ticket.credential.keyPassword',
                    component: 'vue-input',
                    rules: '',
                    rule: {},
                    default: '',
                    placeholder: 'ticket.credential.keyPasswordPlaceholder',
                    errorMsg: '',
                    type: 'password',
                    modelName: 'v3'
                }
            },
            TOKEN_USERNAME_PASSWORD: {
                v1: {
                    label: 'ticket.credential.privateToken',
                    component: 'vue-input',
                    rules: 'required',
                    rule: { required: true },
                    default: '',
                    placeholder: 'keyPasswordPlaceholder.tokenPlaceholder',
                    errorMsg: 'ticket.credential.tokenRequired',
                    type: 'password',
                    modelName: 'v1'
                },
                v2: {
                    label: 'ticket.credential.username',
                    component: 'vue-input',
                    rules: 'required',
                    rule: { required: true },
                    default: '',
                    placeholder: 'ticket.credential.usernamePlaceholder',
                    errorMsg: 'ticket.credential.usernameRequired',
                    modelName: 'v2'
                },
                v3: {
                    label: 'ticket.credential.password',
                    component: 'vue-input',
                    rules: 'required',
                    rule: { required: true },
                    required: true,
                    default: '',
                    placeholder: 'ticket.credential.passwordPlaceholder',
                    errorMsg: 'ticket.credential.passwordRequired',
                    type: 'password',
                    modelName: 'v3'
                }
            },
            OAUTHTOKEN: {
                v1: {
                    label: 'OauthToken',
                    component: 'vue-input',
                    rules: 'required',
                    rule: { required: true },
                    default: '',
                    placeholder: 'ticket.credential.oauthTokenPlaceholder',
                    errorMsg: 'ticket.credential.oauthTokenRequired',
                    modelName: 'v1',
                    type: 'password'
                }
            }
        },
        ticketType: [
            {
                id: 'PASSWORD',
                name: 'password',
                desc: 'passwordDesc'
            },
            {
                id: 'MULTI_LINE_PASSWORD',
                name: 'multiLinePassword',
                desc: 'passwordDesc'
            },
            {
                id: 'USERNAME_PASSWORD',
                name: 'usernamePassword',
                desc: 'passwordDesc'
            },
            {
                id: 'ACCESSTOKEN',
                name: 'accessToken',
                desc: '一个访问令牌包含了此登陆会话的安全信息，用于关联Gitlab类型代码库，'
            },
            {
                id: 'SECRETKEY',
                name: 'secretKey',
                desc: 'passwordDesc'
            },
            {
                id: 'APPID_SECRETKEY',
                name: 'appIdSecretKey',
                desc: 'appIdDesc'
            },
            {
                id: 'SSH_PRIVATEKEY',
                name: 'SSHKEY',
                desc: 'sshKeyDesc'
            },
            {
                id: 'TOKEN_SSH_PRIVATEKEY',
                name: 'sshKeyToken',
                desc: 'sshKeyTokenDesc'
            },
            {
                id: 'TOKEN_USERNAME_PASSWORD',
                name: 'passwordToken',
                desc: 'passwordTokenDesc'
            },
            {
                id: 'OAUTHTOKEN',
                name: 'OauthToken',
                desc: 'oauthTokenDesc'
            }
        ]
    },
    getters: {
        getTicketByType: state => (type) => {
            return state.ticket[type]
        },
        getTicketType: state => () => {
            const ticketLocale = window.devops.$i18n.t('ticket.credential')
            return state.ticketType.map(type => {
                return {
                    id: type.id,
                    name: ticketLocale[type.name],
                    desc: ticketLocale[type.desc]
                }
            })
        }
    },
    mutations,
    actions
}

export default store
