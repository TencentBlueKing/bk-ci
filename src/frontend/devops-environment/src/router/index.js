
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

// 环境管理入口
const envHome = () => import(/* webpackChunkName: 'envHome' */ '../views/index.vue')

// 环境列表
const envList = () => import(/* webpackChunkName: 'envList' */ '../views/env_list')

// 新增环境
const createEnv = () => import(/* webpackChunkName: 'createEnv' */ '../views/create_env')

// 环境详情
const envDetail = () => import(/* webpackChunkName: 'envDetail' */ '../views/env_detail')

// 节点列表
const nodeList = () => import(/* webpackChunkName: 'nodeList' */ '../views/node_list')

// 节点详情
const nodeDetail = () => import(/* webpackChunkName: 'nodeDetail' */ '../views/node_detail')

const routes = [
    {
        path: 'environment/:projectId?',
        component: envHome,
        children: [
            {
                path: '',
                name: 'envList',
                component: envList,
                meta: {
                    title: '环境列表',
                    logo: 'environment',
                    header: '环境管理',
                    to: 'envList'
                }
            },
            {
                path: 'createEnv',
                name: 'createEnv',
                component: createEnv,
                meta: {
                    title: '新增环境',
                    logo: 'environment',
                    header: '环境管理',
                    to: 'envList'
                }
            },
            {
                path: 'envDetail/:envId',
                name: 'envDetail',
                component: envDetail,
                meta: {
                    title: '环境详情',
                    logo: 'environment',
                    header: '环境管理',
                    to: 'envList'
                }
            },
            {
                path: 'nodeList',
                name: 'nodeList',
                component: nodeList,
                meta: {
                    title: '节点列表',
                    logo: 'environment',
                    header: '环境管理',
                    to: 'envList'
                }
            },
            {
                path: 'nodeDetail/:nodeHashId',
                name: 'nodeDetail',
                component: nodeDetail,
                meta: {
                    title: '节点详情',
                    logo: 'environment',
                    header: '环境管理',
                    to: 'envList'
                }
            }
        ]
    }
]

export default routes
