
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
                    label: '密码',
                    component: 'vue-input',
                    rules: 'required',
                    rule: { required: true },
                    required: true,
                    default: '',
                    placeholder: '请输入密码',
                    errorMsg: '密码不能为空',
                    type: 'password',
                    modelName: 'v1'
                }
            },
            USERNAME_PASSWORD: {
                v1: {
                    label: '用户名',
                    component: 'vue-input',
                    rules: 'required',
                    rule: { required: true },
                    default: '',
                    placeholder: '请输入用户名',
                    errorMsg: '用户名不能为空',
                    modelName: 'v1'
                },
                v2: {
                    label: '密码',
                    component: 'vue-input',
                    rules: 'required',
                    rule: { required: true },
                    required: true,
                    default: '',
                    placeholder: '请输入密码',
                    errorMsg: '密码不能为空',
                    type: 'password',
                    modelName: 'v2'
                }
            },
            ACCESSTOKEN: {
                v1: {
                    label: 'AccessToken',
                    component: 'vue-input',
                    rules: 'required',
                    rule: { required: true },
                    default: '',
                    placeholder: '请输入AccessToken',
                    errorMsg: 'AccessToken不能为空',
                    modelName: 'v1',
                    type: 'password'
                }
            },
            SECRETKEY: {
                v1: {
                    label: 'secretKey',
                    component: 'vue-input',
                    rules: 'required',
                    rule: { required: true },
                    default: '',
                    placeholder: '请输入secretKey',
                    errorMsg: 'secretKey不能为空',
                    type: 'password',
                    modelName: 'v1'
                }
            },
            APPID_SECRETKEY: {
                v1: {
                    label: 'appId',
                    component: 'vue-input',
                    rules: 'required',
                    rule: { required: true },
                    default: '',
                    placeholder: '请输入appId',
                    errorMsg: 'appId不能为空',
                    modelName: 'v1'
                },
                v2: {
                    label: 'secretKey',
                    component: 'vue-input',
                    rules: 'required',
                    rule: { required: true },
                    default: '',
                    placeholder: '请输入secretKey',
                    errorMsg: 'secretKey不能为空',
                    type: 'password',
                    modelName: 'v2'
                }
            },
            SSH_PRIVATEKEY: {
                v1: {
                    label: 'ssh私钥',
                    component: 'vue-textarea',
                    rules: 'required',
                    rule: { regex: /^(-----BEGIN RSA PRIVATE KEY-----){1}[\s\S]*(-----END RSA PRIVATE KEY-----)$/, required: true },
                    default: '',
                    placeholder: '请输入SSH Key对应的私钥，以-----BEGIN RSA PRIVATE KEY-----开头，-----END RSA PRIVATE KEY-----结束',
                    errorMsg: '请输入SSH Key对应的私钥，以-----BEGIN RSA PRIVATE KEY-----开头，-----END RSA PRIVATE KEY-----结束',
                    modelName: 'v1'
                },
                v2: {
                    label: '私钥密码',
                    component: 'vue-input',
                    rules: '',
                    rule: {},
                    default: '',
                    placeholder: '请输入私钥密码',
                    errorMsg: '',
                    type: 'password',
                    modelName: 'v2'
                }
            },
            TOKEN_SSH_PRIVATEKEY: {
                v1: {
                    label: 'private token',
                    component: 'vue-input',
                    rules: 'required',
                    rule: { required: true },
                    default: '',
                    placeholder: '请输入token',
                    errorMsg: 'token不能为空',
                    type: 'password',
                    modelName: 'v1'
                },
                v2: {
                    label: 'ssh私钥',
                    component: 'vue-textarea',
                    rules: 'required',
                    rule: { regex: /^(-----BEGIN RSA PRIVATE KEY-----){1}[\s\S]*(-----END RSA PRIVATE KEY-----)$/, required: true },
                    default: '',
                    placeholder: '请输入SSH Key对应的私钥，以-----BEGIN RSA PRIVATE KEY-----开头，-----END RSA PRIVATE KEY-----结束',
                    errorMsg: '请输入SSH Key对应的私钥，以-----BEGIN RSA PRIVATE KEY-----开头，-----END RSA PRIVATE KEY-----结束',
                    modelName: 'v2'
                },
                v3: {
                    label: '私钥密码',
                    component: 'vue-input',
                    rules: '',
                    rule: {},
                    default: '',
                    placeholder: '请输入私钥密码',
                    errorMsg: '',
                    type: 'password',
                    modelName: 'v3'
                }
            },
            TOKEN_USERNAME_PASSWORD: {
                v1: {
                    label: 'private token',
                    component: 'vue-input',
                    rules: 'required',
                    rule: { required: true },
                    default: '',
                    placeholder: '请输入token',
                    errorMsg: 'token不能为空',
                    type: 'password',
                    modelName: 'v1'
                },
                v2: {
                    label: '用户名',
                    component: 'vue-input',
                    rules: 'required',
                    rule: { required: true },
                    default: '',
                    placeholder: '请输入用户名',
                    errorMsg: '用户名不能为空',
                    modelName: 'v2'
                },
                v3: {
                    label: '密码',
                    component: 'vue-input',
                    rules: 'required',
                    rule: { required: true },
                    required: true,
                    default: '',
                    placeholder: '请输入密码',
                    errorMsg: '密码不能为空',
                    type: 'password',
                    modelName: 'v3'
                }
            }
        },
        ticketType: [
            {
                id: 'PASSWORD',
                name: '密码',
                desc: '用于蓝盾平台中需要加密保存的信息，如证书密码、脚本中需要加密字段等，'
            },
            {
                id: 'USERNAME_PASSWORD',
                name: '用户名+密码',
                desc: '用于蓝盾平台中需要加密保存的信息，如证书密码、脚本中需要加密字段等，'
            },
            {
                id: 'ACCESSTOKEN',
                name: 'AccessToken',
                desc: '一个访问令牌包含了此登陆会话的安全信息，用于关联Gitlab类型代码库，'
            },
            {
                id: 'SECRETKEY',
                name: 'SecretKey',
                desc: '用于蓝盾平台中需要加密保存的信息，如证书密码、脚本中需要加密字段等，'
            },
            {
                id: 'APPID_SECRETKEY',
                name: 'AppId+SecretKey',
                desc: '用来设置key value的键值对类型，例如bugly原子要填的用户帐号密码、api调用等，'
            },
            {
                id: 'SSH_PRIVATEKEY',
                name: 'SSH私钥',
                desc: 'SSH包含公钥和私钥,用于关联SVN类型代码库，SSH配置说明请参考蓝盾文档中心，'
            },
            {
                id: 'TOKEN_SSH_PRIVATEKEY',
                name: 'SSH私钥+私有Token',
                desc: '用于使用ssh方式关联Git类型代码库，'
            },
            {
                id: 'TOKEN_USERNAME_PASSWORD',
                name: '用户名密码+私有token',
                desc: '用于使用http方式关联Git类型代码库，'
            }
        ]
    },
    getters: {
        getTicketByType: state => (type) => {
            return state.ticket[type]
        }
    },
    mutations,
    actions
}

export default store
