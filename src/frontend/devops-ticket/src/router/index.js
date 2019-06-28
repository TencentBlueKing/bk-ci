
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

// 凭据证书入口
const ticketHome = () => import(/* webpackChunkName: 'ticketHome' */ '../views/index.vue')

// 我的凭据
const credentialList = () => import(/* webpackChunkName: 'credentialList' */ '../views/credential_list')

// 新增凭据
const createCredential = () => import(/* webpackChunkName: 'createCredential' */ '../views/create_credential')

// 我的证书
// const certList = () => import(/* webpackChunkName: 'certificateList' */'../views/certificate_list')

// 我的证书
// const createCert = () => import(/* webpackChunkName: 'createCertificate' */'../views/create_certificate')

const routes = [
    {
        path: 'ticket/:projectId?',
        component: ticketHome,
        children: [
            {
                path: '',
                name: 'credentialList',
                component: credentialList,
                meta: {
                    title: '凭据列表',
                    logo: 'ticket',
                    header: '凭证管理',
                    to: 'credentialList'
                }
            },
            {
                path: 'createCredential',
                name: 'createCredential',
                component: createCredential,
                meta: {
                    title: '新增凭据',
                    logo: 'ticket',
                    header: '凭证管理',
                    to: 'credentialList'
                }
            },
            {
                path: 'createCredential/:type/:locked?',
                name: 'createCredentialWithType',
                component: createCredential,
                meta: {
                    title: '新增凭据',
                    logo: 'ticket',
                    header: '凭证管理',
                    to: 'credentialList'
                }
            },
            {
                path: 'editCredential/:credentialId',
                name: 'editCredential',
                component: createCredential,
                meta: {
                    title: '编辑凭据',
                    logo: 'ticket',
                    header: '凭证管理',
                    to: 'credentialList'
                }
            }
            // {
            //     path: 'certList',
            //     name: 'certList',
            //     component: certList,
            //     meta: {
            //         title: '证书列表',
            //         logo: 'ticket',
            //         header: '凭证管理',
            //         to: 'credentialList'
            //     }
            // },
            // {
            //     path: 'createCert/:certType?',
            //     name: 'createCert',
            //     component: createCert,
            //     meta: {
            //         title: '新增证书',
            //         logo: 'ticket',
            //         header: '凭证管理',
            //         to: 'credentialList'
            //     }
            // },
            // {
            //     path: 'editCert/:certType/:certId',
            //     name: 'editCert',
            //     component: createCert,
            //     meta: {
            //         title: '编辑证书',
            //         logo: 'ticket',
            //         header: '凭证管理',
            //         to: 'credentialList'
            //     }
            // }
        ]
    }
]

export default routes
