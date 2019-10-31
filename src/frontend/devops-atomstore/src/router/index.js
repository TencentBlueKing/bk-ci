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

const atomList = () => import(/* webpackChunkName: 'atomList' */ '@/views/atom_list.vue') // 工作台

const install = () => import(/* webpackChunkName: 'install' */ '@/views/install.vue') // 研发商店安装页面

const editAtom = () => import(/* webpackChunkName: 'editAtom' */ '@/views/edit_atom.vue') // 上架/升级流水线插件
const releaseProgress = () => import(/* webpackChunkName: 'releaseProgress' */ '@/views/release_progress.vue') // 发布进度
const atomDatail = () => import(/* webpackChunkName: 'atomDatail' */ '@/views/atomDetail/index.vue') // 流水线插件概览
const atomOverview = () => import(/* webpackChunkName: 'atomOverview' */ '@/views/atomDetail/overview.vue') // 流水线插件概览
const settings = () => import(/* webpackChunkName: 'settings' */ '@/views/atomDetail/settings.vue') // 流水线插件设置
const atomInformation = () => import(/* webpackChunkName: 'atomInformation' */ '@/views/atomDetail/detail.vue') // 流水线插件详情
const approval = () => import(/* webpackChunkName: 'approval' */ '@/views/atomDetail/approval.vue') // 流水线插件审批
const memberManage = () => import(/* webpackChunkName: 'memberManage' */ '@/views/atomDetail/member_manage.vue') // 成员管理
const visibleRange = () => import(/* webpackChunkName: 'visibleRange' */ '@/views/atomDetail/visible_range.vue') // 可见范围
const privateSetting = () => import(/* webpackChunkName: 'privateSetting' */ '@/views/atomDetail/private_setting.vue') // 私有配置
const edit = () => import(/* webpackChunkName: 'editAtom' */ '@/views/atomDetail/edit.vue') // 编辑插件详情

const editTemplate = () => import(/* webpackChunkName: 'editTemplate' */ '@/views/edit_template.vue') // 上架模板
const upgradeTemplate = () => import(/* webpackChunkName: 'upgradeTemplate' */ '@/views/upgrade_template.vue') // 上架模板进度
const tplDatail = () => import(/* webpackChunkName: 'tplDatail' */ '@/views/templateDetail/index.vue') // 模板概览
const tplOverview = () => import(/* webpackChunkName: 'tplOverview' */ '@/views/templateDetail/overview.vue') // 模板概览
const tplSettings = () => import(/* webpackChunkName: 'tplSettings' */ '@/views/templateDetail/settings.vue') // 模板设置
const tplVisibleRange = () => import(/* webpackChunkName: 'tplVisibleRange' */ '@/views/templateDetail/visible_range.vue') // 可见范围

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
                path: 'atomList/:type',
                name: 'atomList',
                component: atomList,
                meta: {
                    title: 'atomList',
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
                path: 'atom/:atomCode',
                name: 'atomDetail',
                component: atomDatail,
                children: [
                    {
                        path: 'overview',
                        name: 'overview',
                        component: atomOverview,
                        meta: {
                            title: 'overview',
                            logo: 'store',
                            header: 'store',
                            to: 'atomHome'
                        }
                    },
                    {
                        path: 'detail',
                        name: 'detail',
                        component: atomInformation,
                        meta: {
                            title: 'detail',
                            logo: 'store',
                            header: 'store',
                            to: 'atomHome'
                        }
                    },
                    {
                        path: 'edit',
                        name: 'edit',
                        component: edit,
                        meta: {
                            title: 'edit',
                            logo: 'store',
                            header: 'store',
                            to: 'atomHome'
                        }
                    },
                    {
                        path: 'approval',
                        name: 'approval',
                        component: approval,
                        meta: {
                            title: 'approval',
                            logo: 'store',
                            header: 'store',
                            to: 'atomHome'
                        }
                    },
                    {
                        path: 'settings',
                        name: 'settings',
                        component: settings,
                        meta: {
                            title: 'settings',
                            logo: 'store',
                            header: 'store',
                            to: 'atomHome'
                        },
                        children: [
                            {
                                path: 'member',
                                name: 'member',
                                component: memberManage,
                                meta: {
                                    title: 'member',
                                    logo: 'store',
                                    header: 'store',
                                    to: 'atomHome'
                                }
                            },
                            {
                                path: 'visible',
                                name: 'visible',
                                component: visibleRange,
                                meta: {
                                    title: 'visible',
                                    logo: 'store',
                                    header: 'store',
                                    to: 'atomHome'
                                }
                            },
                            {
                                path: 'private',
                                name: 'private',
                                component: privateSetting,
                                meta: {
                                    title: 'private',
                                    logo: 'store',
                                    header: 'store',
                                    to: 'atomHome'
                                }
                            }
                        ]
                    }
                ]
            },
            {
                path: 'template/:templateCode',
                name: 'tplDatail',
                component: tplDatail,
                children: [
                    {
                        path: 'overview',
                        name: 'tplOverview',
                        component: tplOverview,
                        meta: {
                            title: 'overview',
                            logo: 'store',
                            header: 'store',
                            to: 'atomHome'
                        }
                    },
                    {
                        path: 'settings',
                        name: 'tplSettings',
                        component: tplSettings,
                        meta: {
                            title: 'settings',
                            logo: 'store',
                            header: 'store',
                            to: 'atomHome'
                        },
                        children: [
                            {
                                path: 'visible',
                                name: 'tplVisible',
                                component: tplVisibleRange,
                                meta: {
                                    title: 'visible',
                                    logo: 'store',
                                    header: 'store',
                                    to: 'atomHome'
                                }
                            }
                        ]
                    }
                ]
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
