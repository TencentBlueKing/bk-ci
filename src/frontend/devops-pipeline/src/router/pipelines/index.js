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

import zyPipelineRoute from './zhiyan'

const pipelines = () => import(/* webpackChunkName: "pipelines" */'../../views')

const pipelinesNewList = () => import(/* webpackChunkName: "pipelinesNewList" */'../../views/list/newlist')
const pipelinesListEntry = () => import(/* webpackChunkName: "pipelinesListEntry" */'../../views/list/index')
const pipelinesGroup = () => import(/* webpackChunkName: "pipelinesGroup" */'../../views/list/group')
const pipelinesView = () => import(/* webpackChunkName: "pipelinesView" */'../../views/list/view')
const pipelinesTemplate = () => import(/* webpackChunkName: "pipelinesTemplate" */'../../views/list/template')
const pipelinesRestore = () => import(/* webpackChunkName: "pipelinesRestore" */'../../views/list/restore')
const pipelinesAudit = () => import(/* webpackChunkName: "pipelinesAudit" */'../../views/list/audit')

const templateEntry = () => import(/* webpackChunkName: "pipelinesTemplate" */'../../views/template/')
const templateEdit = () => import(/* webpackChunkName: "pipelinesTemplate" */'../../views/template/edit.vue')
const templateSetting = () => import(/* webpackChunkName: "pipelinesTemplate" */'../../views/template/setting.vue')
const templateInstance = () => import(/* webpackChunkName: "pipelinesTemplate" */'../../views/template/instance.vue')
const templateInstanceCreate = () => import(/* webpackChunkName: "pipelinesTemplate" */'../../views/template/instance_create.vue')

const atomManage = () => import(/* webpackChunkName: "atomManage" */'../../views/list/atomManage.vue')

// 客户端流水线任务子页 - subpages
const pipelinesEntry = () => import(/* webpackChunkName: "pipelinesEntry" */'../../views/subpages')
// 客户端流水线任务历史 - history
const pipelinesHistory = () => import(/* webpackChunkName: "pipelinesHistory" */'../../views/subpages/history.vue')
// 客户端流水线任务详情 - detail
const pipelinesDetail = () => import(/* webpackChunkName: "pipelinesDetail" */'../../views/subpages/exec_detail.vue')
// 客户端流水线编辑 - edit
const pipelinesEdit = () => import(/* webpackChunkName: "pipelinesEdit" */'../../views/subpages/edit.vue')
// 客户端流水线执行预览 - edit
const pipelinesPreview = () => import(/* webpackChunkName: "pipelinesPreview" */'../../views/subpages/preview.vue')
// docker console
const pipelinesDocker = () => import(/* webpackChunkName: "pipelinesDocker" */'../../views/subpages/docker_console.vue')
// 插件前端task.json在线调试
const atomDebug = () => import(/* webpackChunkName: "atomDebug" */'../../views/atomDebug.vue')
const ImportPipelineEdit = () => import(/* webpackChunkName: "atomDebug" */'../../views/list/ImportPipelineEdit.vue')

const moocPipelinePage = () => import(/* webpackChunkName: "moocPipelinePage" */'../../views/list/mooc.vue')

const routes = [
    {
        path: '/pipeline/:projectId',
        component: pipelines,
        children: [
            {
                path: '',
                redirect: {
                    name: 'pipelinesList'
                }
            },
            {
                path: 'mooc',
                name: 'mooc',
                meta: {
                    title: 'pipeline',
                    header: 'pipeline',
                    icon: 'pipeline',
                    to: 'pipelinesList'
                },
                component: moocPipelinePage
            },
            {
                path: 'list',
                name: 'pipelinesListEntry',
                meta: {
                    title: 'pipeline',
                    header: 'pipeline',
                    icon: 'pipeline',
                    to: 'pipelinesList'
                },
                component: pipelinesListEntry,
                children: [
                    {
                        path: 'group',
                        name: 'pipelinesGroup',
                        component: pipelinesGroup
                    },
                    {
                        path: 'view',
                        name: 'pipelinesView',
                        component: pipelinesView
                    },
                    {
                        path: 'template',
                        name: 'pipelinesTemplate',
                        component: pipelinesTemplate
                    },
                    {
                        path: 'atomManage',
                        name: 'atomManage',
                        component: atomManage
                    },
                    {
                        path: 'restore',
                        name: 'pipelinesRestore',
                        component: pipelinesRestore

                    },
                    {
                        path: 'audit',
                        name: 'pipelinesAudit',
                        component: pipelinesAudit
                    },
                    {
                        path: ':type?',
                        name: 'pipelinesList',
                        component: pipelinesNewList,
                        meta: {
                            webSocket: true
                        }
                    }
                ]
            },
            {
                path: 'template/:templateId',
                component: templateEntry,
                children: [
                    {
                        path: 'edit',
                        name: 'templateEdit',
                        component: templateEdit
                    },
                    {
                        path: 'setting',
                        name: 'templateSetting',
                        component: templateSetting
                    },
                    {
                        path: 'instance',
                        name: 'templateInstance',
                        component: templateInstance
                    },
                    {
                        path: 'createInstance/:curVersionId/:pipelineName?',
                        name: 'createInstance',
                        component: templateInstanceCreate
                    }
                ]
            },
            {
                // docker console
                path: 'dockerConsole',
                name: 'pipelinesDocker',
                component: pipelinesDocker
            },
            {
                path: 'atomDebug',
                name: 'atomDebug',
                component: atomDebug
            },

            {
                path: 'import',
                component: ImportPipelineEdit,
                children: [
                    {
                        path: '',
                        redirect: {
                            name: 'pipelineImportEdit'
                        }
                    },
                    {
                        // 流水线编辑
                        path: 'edit/:tab?',
                        name: 'pipelineImportEdit',
                        meta: {
                            icon: 'pipeline',
                            title: 'pipeline',
                            header: 'pipeline',
                            to: 'pipelinesList'
                        },
                        component: pipelinesEdit
                    }
                ]
            },
            {
                path: ':pipelineId',
                component: pipelinesEntry,
                children: [
                    {
                        path: '',
                        redirect: {
                            name: 'pipelinesHistory'
                        }
                    },
                    {
                        // 详情
                        path: 'detail/:buildNo/:type?',
                        name: 'pipelinesDetail',
                        component: pipelinesDetail,
                        meta: {
                            title: 'pipeline',
                            header: 'pipeline',
                            icon: 'pipeline',
                            to: 'pipelinesList'
                        }
                    },
                    {
                        path: 'detail',
                        redirect: {
                            name: 'pipelinesHistory'
                        }
                    },
                    {
                        // 执行历史
                        path: 'history/:type?',
                        name: 'pipelinesHistory',
                        component: pipelinesHistory,
                        meta: {
                            title: 'pipeline',
                            header: 'pipeline',
                            icon: 'pipeline',
                            to: 'pipelinesList'
                        }
                    },
                    {
                        // 流水线编辑
                        path: 'edit/:tab?',
                        name: 'pipelinesEdit',
                        meta: {
                            icon: 'pipeline',
                            title: 'pipeline',
                            header: 'pipeline',
                            to: 'pipelinesList'
                        },
                        component: pipelinesEdit
                    },
                    {
                        // 流水线执行可选插件
                        path: 'preview',
                        name: 'pipelinesPreview',
                        meta: {
                            icon: 'pipeline',
                            title: 'pipeline',
                            header: 'pipeline',
                            to: 'pipelinesList'
                        },
                        component: pipelinesPreview
                    }
                ]
            }
        ]
    },
    {
        path: '/pipeline/zhiyan/:projectId',
        component: pipelines,
        children: zyPipelineRoute
    }
]

export default routes
