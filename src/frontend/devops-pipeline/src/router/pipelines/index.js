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

const pipelines = () => import(/* webpackChunkName: "pipelines" */'../../views')

const pipelinesNewList = () => import(/* webpackChunkName: "pipelinesNewList" */'../../views/list/newlist')
const pipelinesListEntry = () => import(/* webpackChunkName: "pipelinesListEntry" */'../../views/list/index')
const pipelinesGroup = () => import(/* webpackChunkName: "pipelinesGroup" */'../../views/list/group')
const pipelinesView = () => import(/* webpackChunkName: "pipelinesView" */'../../views/list/view')
const pipelinesTemplate = () => import(/* webpackChunkName: "pipelinesTemplate" */'../../views/list/template')

const templateEntry = () => import(/* webpackChunkName: "pipelinesTemplate" */'../../views/template/')
const templateEdit = () => import(/* webpackChunkName: "pipelinesTemplate" */'../../views/template/edit.vue')
const templateSetting = () => import(/* webpackChunkName: "pipelinesTemplate" */'../../views/template/setting.vue')
const templateInstance = () => import(/* webpackChunkName: "pipelinesTemplate" */'../../views/template/instance.vue')
const templateInstanceCreate = () => import(/* webpackChunkName: "pipelinesTemplate" */'../../views/template/instance_create.vue')

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
                path: 'list',
                name: 'pipelinesListEntry',
                meta: {
                    title: '流水线',
                    header: '流水线',
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
                        path: ':type?',
                        name: 'pipelinesList',
                        component: pipelinesNewList
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
                            title: '流水线执行详情',
                            header: '流水线',
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
                        path: 'history',
                        name: 'pipelinesHistory',
                        component: pipelinesHistory,
                        meta: {
                            title: '执行历史',
                            header: '流水线',
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
                            title: '编辑',
                            header: '流水线',
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
                            title: '执行预览流水线',
                            header: '流水线',
                            to: 'pipelinesList'
                        },
                        component: pipelinesPreview
                    }
                ]
            }
        ]
    }
]

export default routes
