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

const installAtom = () => import(/* webpackChunkName: 'installAtom' */ '@/views/install_atom.vue') // 安装流水线插件

const editAtom = () => import(/* webpackChunkName: 'editAtom' */ '@/views/edit_atom.vue') // 上架/升级流水线插件
const releaseProgress = () => import(/* webpackChunkName: 'releaseProgress' */ '@/views/release_progress.vue') // 发布进度
const atomDatail = () => import(/* webpackChunkName: 'atomDatail' */ '@/views/atomDetail/index.vue') // 流水线插件概览
const atomOverview = () => import(/* webpackChunkName: 'atomOverview' */ '@/views/atomDetail/overview.vue') // 流水线插件概览
const settings = () => import(/* webpackChunkName: 'settings' */ '@/views/atomDetail/settings.vue') // 流水线插件设置
const atomInformation = () => import(/* webpackChunkName: 'atomInformation' */ '@/views/atomDetail/detail.vue') // 流水线插件详情
const memberManage = () => import(/* webpackChunkName: 'memberManage' */ '@/views/atomDetail/member_manage.vue') // 成员管理
const privateSetting = () => import(/* webpackChunkName: 'privateSetting' */ '@/views/atomDetail/private_setting.vue') // 可见范围

const editTemplate = () => import(/* webpackChunkName: 'editTemplate' */ '@/views/edit_template.vue') // 上架模板
const upgradeTemplate = () => import(/* webpackChunkName: 'upgradeTemplate' */ '@/views/upgrade_template.vue') // 上架模板进度
const tplDatail = () => import(/* webpackChunkName: 'tplDatail' */ '@/views/templateDetail/index.vue') // 模板概览
const tplOverview = () => import(/* webpackChunkName: 'tplOverview' */ '@/views/templateDetail/overview.vue') // 模板概览
const tplSettings = () => import(/* webpackChunkName: 'tplSettings' */ '@/views/templateDetail/settings.vue') // 模板设置

const routes = [
    {
        path: 'store',
        component: atomEntry,
        meta: {
            title: '研发商店',
            logo: 'store',
            header: '研发商店',
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
                    title: '研发商店',
                    logo: 'store',
                    header: '研发商店',
                    to: 'atomHome'
                },
                children: [
                    {
                        path: 'list',
                        name: 'list',
                        component: marketList,
                        meta: {
                            title: '商店列表',
                            logo: 'store',
                            header: '研发商店',
                            to: 'atomHome'
                        }
                    },
                    {
                        path: 'home',
                        name: 'atomHome',
                        component: marketHome,
                        meta: {
                            title: '商店首页',
                            logo: 'store',
                            header: '研发商店',
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
                    title: '详情',
                    logo: 'store',
                    header: '研发商店',
                    to: 'atomHome'
                }
            },
            {
                path: 'atomList/:type',
                name: 'atomList',
                component: atomList,
                meta: {
                    title: '工作台',
                    logo: 'store',
                    header: '研发商店',
                    to: 'atomHome'
                }
            },
            {
                path: 'upgradeAtom/:atomId',
                name: 'upgradeAtom',
                component: editAtom,
                meta: {
                    title: ' 升级流水线插件',
                    logo: 'store',
                    header: '研发商店',
                    to: 'atomHome'
                }
            },
            {
                path: 'shelfAtom/:atomId',
                name: 'shelfAtom',
                component: editAtom,
                meta: {
                    title: '上架流水线插件',
                    logo: 'store',
                    header: '研发商店',
                    to: 'atomHome'
                }
            },
            {
                path: 'editTemplate/:templateId',
                name: 'editTemplate',
                component: editTemplate,
                meta: {
                    title: '上架模板',
                    logo: 'store',
                    header: '研发商店',
                    to: 'atomHome'
                }
            },
            {
                path: 'releaseProgress/:releaseType/:atomId',
                name: 'releaseProgress',
                component: releaseProgress,
                meta: {
                    title: '发布进度',
                    logo: 'store',
                    header: '研发商店',
                    to: 'atomHome'
                }
            },
            {
                path: 'upgradeTemplate/:templateId',
                name: 'upgradeTemplate',
                component: upgradeTemplate,
                meta: {
                    title: '上架模板进度',
                    logo: 'store',
                    header: '研发商店',
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
                            title: '概览',
                            logo: 'store',
                            header: '研发商店',
                            to: 'atomHome'
                        }
                    },
                    {
                        path: 'detail',
                        name: 'detail',
                        component: atomInformation,
                        meta: {
                            title: '详情',
                            logo: 'store',
                            header: '研发商店',
                            to: 'atomHome'
                        }
                    },
                    {
                        path: 'settings',
                        name: 'settings',
                        component: settings,
                        meta: {
                            title: '设置',
                            logo: 'store',
                            header: '研发商店',
                            to: 'atomHome'
                        },
                        children: [
                            {
                                path: 'member',
                                name: 'member',
                                component: memberManage,
                                meta: {
                                    title: '成员管理',
                                    logo: 'store',
                                    header: '研发商店',
                                    to: 'atomHome'
                                }
                            },
                            {
                                path: 'private',
                                name: 'private',
                                component: privateSetting,
                                meta: {
                                    title: '私有设置',
                                    logo: 'store',
                                    header: '研发商店',
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
                            title: '概览',
                            logo: 'store',
                            header: '研发商店',
                            to: 'atomHome'
                        }
                    },
                    {
                        path: 'settings',
                        name: 'tplSettings',
                        component: tplSettings,
                        meta: {
                            title: '设置',
                            logo: 'store',
                            header: '研发商店',
                            to: 'atomHome'
                        }
                    }
                ]
            },
            {
                path: ':atomCode/install/atom',
                name: 'installAtom',
                component: installAtom,
                meta: {
                    title: '安装流水线插件',
                    logo: 'store',
                    header: '研发商店',
                    to: 'atomHome'
                }
            },
            {
                path: ':templateCode/install/template',
                name: 'installTemplate',
                component: installAtom,
                meta: {
                    title: '安装模板',
                    logo: 'store',
                    header: '研发商店',
                    to: 'atomHome'
                }
            }
        ]
    }
]

export default routes
