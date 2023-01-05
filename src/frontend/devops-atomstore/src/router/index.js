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

const atomEntry = () => import(/* webpackChunkName: 'atomEntry' */ '../views/index')

const marketIndex = () => import(/* webpackChunkName: 'atomHome' */ '@/views/market/index.vue') // store首页
const marketHome = () => import(/* webpackChunkName: 'atomHome' */ '@/views/market/home.vue') // store首页
const marketList = () => import(/* webpackChunkName: 'atomStore' */ '@/views/market/list.vue') // 流水线插件市场列表
const marketDetail = () => import(/* webpackChunkName: 'atomDetails' */ '@/views/market/detail.vue') // 流水线插件信息

const workList = () => import(/* webpackChunkName: 'workList' */ '@/views/workList/index.vue') // 工作台
const atomWork = () => import(/* webpackChunkName: 'workList' */ '@/views/workList/atom.vue') // 工作台
const templateWork = () => import(/* webpackChunkName: 'workList' */ '@/views/workList/template.vue') // 工作台
const imageWork = () => import(/* webpackChunkName: 'workList' */ '@/views/workList/image.vue') // 工作台
const serviceWork = () => import(/* webpackChunkName: 'workList' */ '@/views/workList/service.vue') // 工作台

const install = () => import(/* webpackChunkName: 'install' */ '@/views/install.vue') // 研发商店安装页面
const manage = () => import(/* webpackChunkName: 'manage' */ '@/views/manage/index.vue') // 研发商店管理页面
const detail = () => import(/* webpackChunkName: 'detail' */ '@/views/manage/detail/index.vue') // 研发商店管理页面
const show = () => import(/* webpackChunkName: 'detail' */ '@/views/manage/detail/show.vue') // 研发商店管理页面
const edit = () => import(/* webpackChunkName: 'detail' */ '@/views/manage/detail/edit.vue') // 研发商店管理页面
const overView = () => import(/* webpackChunkName: 'overView' */ '@/views/manage/over-view/index.vue') // 研发商店管理页面
const statisticData = () => import(/* webpackChunkName: 'overView' */ '@/views/manage/over-view/statistic-data.vue') // 研发商店管理页面
const statisticPipeline = () => import(/* webpackChunkName: 'overView' */ '@/views/manage/over-view/statistic-pipeline.vue') // 研发商店管理页面
const approval = () => import(/* webpackChunkName: 'approval' */ '@/views/manage/approval.vue') // 研发商店管理页面
const setting = () => import(/* webpackChunkName: 'setting' */ '@/views/manage/setting/index.vue') // 研发商店管理页面
const visibleSetting = () => import(/* webpackChunkName: 'visibleSetting' */ '@/views/manage/setting/visible-setting.vue') // 研发商店管理页面
const memberSetting = () => import(/* webpackChunkName: 'setting' */ '@/views/manage/setting/member-setting.vue') // 研发商店管理页面
const privateSetting = () => import(/* webpackChunkName: 'setting' */ '@/views/manage/setting/private-setting.vue') // 研发商店管理页面
const apiSetting = () => import(/* webpackChunkName: 'setting' */ '@/views/manage/setting/api-setting.vue') // 研发商店管理页面
const releaseManage = () => import(/* webpackChunkName: 'releaseManage' */ '@/views/manage/release-manage/index.vue') // 研发商店管理页面
const version = () => import(/* webpackChunkName: 'releaseManage' */ '@/views/manage/release-manage/version.vue') // 研发商店管理页面
const environment = () => import(/* webpackChunkName: 'releaseManage' */ '@/views/manage/release-manage/environment.vue') // 研发商店管理页面
const codeCheck = () => import(/* webpackChunkName: 'releaseManage' */ '@/views/manage/release-manage/code-check.vue') // 研发商店管理页面

const editAtom = () => import(/* webpackChunkName: 'editAtom' */ '@/views/edit_atom.vue') // 上架/升级流水线插件
const releaseProgress = () => import(/* webpackChunkName: 'releaseProgress' */ '@/views/release_progress.vue') // 发布进度

const editTemplate = () => import(/* webpackChunkName: 'editTemplate' */ '@/views/edit_template.vue') // 上架模板
const upgradeTemplate = () => import(/* webpackChunkName: 'upgradeTemplate' */ '@/views/upgrade_template.vue') // 上架模板进度

const editImage = () => import(/* webpackChunkName: 'editImage' */ '@/views/edit_image.vue') // 上架镜像
const imageProgress = () => import(/* webpackChunkName: 'imageProgress' */ '@/views/imageProgress.vue') // 镜像进度

const serviceProgress = () => import(/* webpackChunkName: 'serviceProgress' */ '@/views/serviceProgress.vue') // 微扩展进度
const editService = () => import(/* webpackChunkName: 'editService' */ '@/views/edit_service.vue') // 微扩展上架页面
const serviceManage = () => import(/* webpackChunkName: 'serviceManage' */ '@/views/serviceManage.vue') // 扩展管理

const routes = [
    {
        path: 'store',
        component: atomEntry,
        meta: {
            title: 'store',
            logo: 'store',
            header: 'store',
            to: 'atomHome'
        },
        redirect: {
            name: 'atomHome'
        },
        children: [
            {
                path: 'market',
                component: marketIndex,
                name: 'marketIndex',
                meta: {
                    title: 'store',
                    logo: 'store',
                    header: 'store',
                    to: 'atomHome'
                },
                children: [
                    {
                        path: 'list',
                        name: 'list',
                        component: marketList,
                        meta: {
                            title: 'list',
                            logo: 'store',
                            header: 'store',
                            to: 'atomHome'
                        }
                    },
                    {
                        path: 'home',
                        name: 'atomHome',
                        component: marketHome,
                        meta: {
                            title: 'atomHome',
                            logo: 'store',
                            header: 'store',
                            to: 'atomHome'
                        }
                    }
                ]
            },
            {
                path: 'serviceManage/:projectCode',
                name: 'serviceManage',
                component: serviceManage,
                meta: {
                    title: 'serviceManage',
                    logo: 'store',
                    header: 'store',
                    to: 'atomHome'
                }
            },
            {
                path: 'atomStore/detail/:type/:code',
                name: 'details',
                component: marketDetail,
                meta: {
                    title: 'details',
                    logo: 'store',
                    header: 'store',
                    to: 'atomHome'
                }
            },
            {
                path: 'workList',
                name: 'workList',
                component: workList,
                children: [
                    {
                        path: 'atom',
                        name: 'atomWork',
                        component: atomWork
                    },
                    {
                        path: 'template',
                        name: 'templateWork',
                        component: templateWork
                    },
                    {
                        path: 'image',
                        name: 'imageWork',
                        component: imageWork
                    },
                    {
                        path: 'service',
                        name: 'serviceWork',
                        component: serviceWork
                    }
                ]
            },
            {
                path: 'manage/:type/:code',
                name: 'manage',
                component: manage,
                children: [
                    {
                        path: 'detail',
                        name: 'detail',
                        component: detail,
                        children: [
                            {
                                path: 'show',
                                name: 'show',
                                component: show
                            },
                            {
                                path: 'edit',
                                name: 'edit',
                                component: edit
                            }
                        ]
                    },
                    {
                        path: 'setting',
                        name: 'setting',
                        component: setting,
                        children: [
                            {
                                path: 'member',
                                name: 'member',
                                component: memberSetting
                            },
                            {
                                path: 'private',
                                name: 'private',
                                component: privateSetting
                            },
                            {
                                path: 'visible',
                                name: 'visible',
                                component: visibleSetting
                            },
                            {
                                path: 'api',
                                name: 'api',
                                component: apiSetting
                            }
                        ]
                    },
                    {
                        path: 'overview',
                        name: 'overView',
                        component: overView,
                        children: [
                            {
                                path: 'statisticData',
                                name: 'statisticData',
                                component: statisticData
                            },
                            {
                                path: 'statisticPipeline',
                                name: 'statisticPipeline',
                                component: statisticPipeline
                            }
                        ]
                    },
                    {
                        path: 'approval',
                        name: 'approval',
                        component: approval
                    },
                    {
                        path: 'releaseManage',
                        name: 'releaseManage',
                        component: releaseManage,
                        children: [
                            {
                                path: 'version',
                                name: 'version',
                                component: version
                            }, {
                                path: 'environment',
                                name: 'environment',
                                component: environment
                            }, {
                                path: 'check',
                                name: 'check',
                                component: codeCheck
                            }
                        ]
                    }
                ],
                meta: {
                    title: 'manage',
                    logo: 'store',
                    header: 'store',
                    to: 'atomHome'
                }
            },
            {
                path: 'upgradeAtom/:atomId',
                name: 'upgradeAtom',
                component: editAtom,
                meta: {
                    title: ' upgradeAtom',
                    logo: 'store',
                    header: 'store',
                    to: 'atomHome'
                }
            },
            {
                path: 'shelfAtom/:atomId',
                name: 'shelfAtom',
                component: editAtom,
                meta: {
                    title: 'shelfAtom',
                    logo: 'store',
                    header: 'store',
                    to: 'atomHome'
                }
            },
            {
                path: 'editTemplate/:templateId',
                name: 'editTemplate',
                component: editTemplate,
                meta: {
                    title: 'editTemplate',
                    logo: 'store',
                    header: 'store',
                    to: 'atomHome'
                }
            },
            {
                path: 'releaseProgress/:releaseType/:atomId',
                name: 'releaseProgress',
                component: releaseProgress,
                meta: {
                    title: 'releaseProgress',
                    logo: 'store',
                    header: 'store',
                    to: 'atomHome',
                    webSocket: ['^\/console\/store\/releaseProgress\/(shelf|upgrade)\/[^\/]+$']
                }
            },
            {
                path: 'upgradeTemplate/:templateId',
                name: 'upgradeTemplate',
                component: upgradeTemplate,
                meta: {
                    title: 'upgradeTemplate',
                    logo: 'store',
                    header: 'store',
                    to: 'atomHome'
                }
            },
            {
                path: 'editImage/:imageId',
                name: 'editImage',
                component: editImage,
                meta: {
                    title: 'editImage',
                    logo: 'store',
                    header: 'store',
                    to: 'atomHome'
                }
            },
            {
                path: 'imageProgress/:imageId',
                name: 'imageProgress',
                component: imageProgress,
                meta: {
                    title: 'imageProgress',
                    logo: 'store',
                    header: 'store',
                    to: 'atomHome'
                }
            },
            {
                path: 'editService/:serviceId',
                name: 'editService',
                component: editService,
                meta: {
                    title: 'editService',
                    logo: 'store',
                    header: 'store',
                    to: 'atomHome'
                }
            },
            {
                path: 'serviceProgress/:serviceId',
                name: 'serviceProgress',
                component: serviceProgress,
                meta: {
                    title: 'serviceProgress',
                    logo: 'store',
                    header: 'store',
                    to: 'atomHome'
                }
            },
            {
                path: 'install',
                name: 'install',
                component: install,
                meta: {
                    title: 'install',
                    logo: 'store',
                    header: 'store',
                    to: 'atomHome'
                }
            }
        ]
    }
]

export default routes
