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

const pipelines = () => import(/* webpackChunkName: "pipelines" */'../views')

const CreatePipeline = () => import(/* webpackChunkName: "pipelineCreate" */'../views/CreatePipeline.vue')

const pipelineListEntry = () => import(/* webpackChunkName: "pipelinesNewList" */'../views/PipelineList')
const PipelineManageList = () => import(/* webpackChunkName: "pipelinesNewList" */'../views/PipelineList/list')
const PipelineListAuth = () => import(/* webpackChunkName: "pipelinesNewList" */'../views/PipelineList/Auth')

const PipelinesGroup = () => import(/* webpackChunkName: "pipelinesGroup" */'../views/Group')
const PipelinesTemplate = () => import(/* webpackChunkName: "pipelinesTemplate" */'../views/Template')
const PipelinesAudit = () => import(/* webpackChunkName: "pipelinesAudit" */'../views/Audit')
const AtomDebug = () => import(/* webpackChunkName: "atomDebug" */'../views/AtomDebug.vue')
const AtomManage = () => import(/* webpackChunkName: "atomManage" */'../views/AtomManage.vue')

const templateEntry = () => import(/* webpackChunkName: "pipelinesTemplate" */'../views/template/')
const templateEdit = () => import(/* webpackChunkName: "pipelinesTemplate" */'../views/template/edit.vue')
const templateSetting = () => import(/* webpackChunkName: "pipelinesTemplate" */'../views/template/setting.vue')
const templateInstance = () => import(/* webpackChunkName: "pipelinesTemplate" */'../views/template/instance.vue')
const templateInstanceCreate = () => import(/* webpackChunkName: "pipelinesTemplate" */'../views/template/instance_create.vue')
const templatePermission = () => import(/* webpackChunkName: "pipelinesTemplate" */'../views/template/permission.vue')

// 客户端流水线任务子页 - subpages
const pipelinesEntry = () => import(/* webpackChunkName: "pipelinesEntry" */'../views/subpages')

// 客户端流水线任务历史 - history
const HistoryHeader = () => import(/* webpackChunkName: "pipelinesHistory" */'../components/PipelineHeader/HistoryHeader.vue')
const pipelinesHistory = () => import(/* webpackChunkName: "pipelinesHistory" */'../views/subpages/History.vue')
// 客户端流水线任务详情 - detail
const pipelinesDetail = () => import(/* webpackChunkName: "pipelinesDetail" */'../views/subpages/ExecDetail.vue')
const DetailHeader = () => import(/* webpackChunkName: "pipelinesDetail" */'../components/PipelineHeader/DetailHeader.vue')

// 客户端流水线编辑 - edit

const EditHeader = () => import(/* webpackChunkName: "pipelinesEdit" */'../components/PipelineHeader/EditHeader.vue')
const pipelinesEdit = () => import(/* webpackChunkName: "pipelinesEdit" */'../views/subpages/edit.vue')
const DraftDebugRecord = () => import(/* webpackChunkName: "draftDebug" */'../views/subpages/DraftDebugRecord.vue')
const DraftDebugHeader = () => import(/* webpackChunkName: "draftDebug" */'../components/PipelineHeader/DraftDebugHeader.vue')

// 客户端流水线执行预览 - edit
const pipelinesPreview = () => import(/* webpackChunkName: "pipelinesPreview" */'../views/subpages/preview.vue')
const PreviewHeader = () => import(/* webpackChunkName: "pipelinesPreview" */'../components/PipelineHeader/PreviewHeader.vue')

// 插件前端task.json在线调试
// docker console
const pipelinesDocker = () => import(/* webpackChunkName: "pipelinesDocker" */'../views/subpages/docker_console.vue')

// 流水线导入
const ImportPipelineEdit = () => import(/* webpackChunkName: "importPipeline" */'../views/ImportEdit.vue')

const routes = [
    {
        path: '/pipeline/:projectId',
        component: pipelines,
        name: 'pipelineRoot',
        redirect: {
            name: 'PipelineManageList'
        },
        children: [
            {
                path: 'create',
                component: CreatePipeline,
                name: 'createPipeline'
            },
            {
                path: 'import',
                component: ImportPipelineEdit,
                children: [{
                    path: ':tab',
                    name: 'pipelineImportEdit',
                    component: pipelinesEdit
                }]
            },
            {
                path: 'list',
                component: pipelineListEntry,
                children: [
                    {
                        path: 'listAuth/:id/:groupName',
                        name: 'PipelineListAuth',
                        component: PipelineListAuth
                    },
                    {
                        path: ':viewId?/:type?',
                        name: 'PipelineManageList',
                        component: PipelineManageList,
                        meta: {
                            webSocket: true
                        }
                    }
                ]
            },
            {
                path: 'group',
                name: 'pipelinesGroup',
                component: PipelinesGroup
            },
            {
                path: 'atomManage',
                name: 'atomManage',
                component: AtomManage
            },
            {
                path: 'audit',
                name: 'pipelinesAudit',
                component: PipelinesAudit
            },
            {
                path: 'template',
                name: 'pipelinesTemplate',
                component: PipelinesTemplate
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
                    },
                    {
                        path: 'permission',
                        name: 'templatePermission',
                        component: templatePermission
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
                component: AtomDebug
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
                        path: 'detail/:buildNo/:type?/:executeCount?',
                        name: 'pipelinesDetail',
                        components: {
                            header: DetailHeader,
                            default: pipelinesDetail
                        },
                        meta: {
                            title: 'pipeline',
                            header: 'pipeline',
                            icon: 'pipeline',
                            to: 'PipelineManageList'
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
                        path: 'history/:type?/:version?',
                        name: 'pipelinesHistory',
                        components: {
                            header: HistoryHeader,
                            default: pipelinesHistory
                        },
                        meta: {
                            title: 'pipeline',
                            header: 'pipeline',
                            icon: 'pipeline',
                            to: 'PipelineManageList'
                        },
                        beforeEnter (to, from, next) {
                            if (!to.params.type) {
                                next({
                                    name: to.name,
                                    params: Object.assign(to.params, {
                                        type: 'history'
                                    }),
                                    query: to.query
                                })
                            } else {
                                next(true)
                            }
                        }
                    },
                    {
                        // 流水线编辑
                        path: 'edit/:version?',
                        name: 'pipelinesEdit',
                        components: {
                            header: EditHeader,
                            default: pipelinesEdit
                        },
                        meta: {
                            icon: 'pipeline',
                            title: 'pipeline',
                            header: 'pipeline',
                            to: 'PipelineManageList'
                        }
                    },
                    {
                        // 流水线执行可选插件
                        path: 'preview/:version?',
                        name: 'executePreview',
                        components: {
                            header: PreviewHeader,
                            default: pipelinesPreview
                        },
                        meta: {
                            icon: 'pipeline',
                            title: 'pipeline',
                            header: 'pipeline',
                            to: 'PipelineManageList'
                        }
                    },
                    {
                        path: 'draftDebug',
                        name: 'draftDebugRecord',
                        meta: {
                            icon: 'pipeline',
                            title: 'pipeline',
                            header: 'pipeline',
                            to: 'PipelineManageList'
                        },
                        components: {
                            header: DraftDebugHeader,
                            default: DraftDebugRecord
                        }
                    }
                ]
            }
        ]
    }
]

export default routes
