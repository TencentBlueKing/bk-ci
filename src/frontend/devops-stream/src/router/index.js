/**
 * @file router 配置
 * @author Blueking
 */

import Vue from 'vue'
import VueRouter from 'vue-router'
import websocket from '@/utils/websocket'
Vue.use(VueRouter)

const dashboard = () => import(/* webpackChunkName: 'dashboard' */'@/views/dashboard.vue')
const main = () => import(/* webpackChunkName: 'entry' */'@/views/index.vue')
const projectIndex = () => import(/* webpackChunkName: 'entry' */'@/views/project-index.vue')
const exception = () => import(/* webpackChunkName: 'entry' */'@/views/exception.vue')
const notifications = () => import(/* webpackChunkName: 'notifications' */'@/views/notifications.vue')
const pipeline = () => import(/* webpackChunkName: 'pipelines' */'@/views/pipeline/index.vue')
const buildList = () => import(/* webpackChunkName: 'pipelines' */'@/views/pipeline/build-list.vue')
const pipelineDetail = () => import(/* webpackChunkName: 'buildDetail' */'@/views/pipeline/build-detail/index.vue')
const buildArtifacts = () => import(/* webpackChunkName: 'buildDetail' */'@/views/pipeline/build-detail/artifacts.vue')
const buildDetail = () => import(/* webpackChunkName: 'buildDetail' */'@/views/pipeline/build-detail/detail.vue')
const buildReports = () => import(/* webpackChunkName: 'buildDetail' */'@/views/pipeline/build-detail/reports.vue')
const buildConfig = () => import(/* webpackChunkName: 'buildDetail' */'@/views/pipeline/build-detail/config.vue')
const webConsole = () => import(/* webpackChunkName: "webConsole" */'@/views/pipeline/web-console.vue')
const setting = () => import(/* webpackChunkName: 'setting' */'@/views/setting/index.vue')
const basicSetting = () => import(/* webpackChunkName: 'setting' */'@/views/setting/basic.vue')
const credentialList = () => import(/* webpackChunkName: 'credential' */'@/views/setting/credential/credential-list.vue')
const credentialSettings = () => import(/* webpackChunkName: 'credential' */'@/views/setting/credential/credential-settings.vue')
const expGroupsList = () => import(/* webpackChunkName: 'expGroups' */'@/views/setting/exp-groups/index.vue')
const agentPools = () => import(/* webpackChunkName: 'pool' */'@/views/setting/agent-pools/index.vue')
const poolSettings = () => import(/* webpackChunkName: 'pool' */'@/views/setting/agent-pools/pool-settings.vue')
const addAgent = () => import(/* webpackChunkName: 'agent' */'@/views/setting/agent-pools/add-agent.vue')
const agentList = () => import(/* webpackChunkName: 'agent' */'@/views/setting/agent-pools/agent-list.vue')
const agentDetail = () => import(/* webpackChunkName: 'agent' */'@/views/setting/agent-pools/agent-detail.vue')
const metric = () => import(/* webpackChunkName: 'metric' */'@/views/metric.vue')

const routes = [
    {
        path: '',
        components: {
            default: main,
            exception: exception
        },
        children: [
            {
                path: 'dashboard',
                name: 'dashboard',
                component: dashboard
            },
            {
                path: '',
                component: projectIndex,
                children: [
                    {
                        path: 'webConsole',
                        name: 'webConsole',
                        component: webConsole
                    },
                    {
                        path: 'pipeline',
                        component: pipeline,
                        name: 'pipeline',
                        children: [
                            {
                                path: ':pipelineId?',
                                name: 'buildList',
                                component: buildList,
                                meta: {
                                    websocket: true
                                }
                            },
                            {
                                path: ':pipelineId/detail/:buildId',
                                name: 'pipelineDetail',
                                component: pipelineDetail,
                                children: [
                                    {
                                        path: '',
                                        name: 'buildDetail',
                                        component: buildDetail,
                                        meta: {
                                            websocket: true
                                        }
                                    },
                                    {
                                        path: 'artifacts',
                                        name: 'buildArtifacts',
                                        component: buildArtifacts
                                    },
                                    {
                                        path: 'reports',
                                        name: 'buildReports',
                                        component: buildReports
                                    },
                                    {
                                        path: 'config',
                                        name: 'buildConfig',
                                        component: buildConfig
                                    }
                                ]
                            }
                        ]
                    },
                    {
                        path: 'setting',
                        name: 'setting',
                        component: setting,
                        children: [
                            {
                                path: '',
                                name: 'basicSetting',
                                component: basicSetting
                            },
                            {
                                path: 'credential',
                                name: 'credentialList',
                                component: credentialList
                            },
                            {
                                path: 'credential/:credentialId',
                                name: 'credentialSettings',
                                component: credentialSettings
                            },
                            {
                                path: 'expGroups',
                                name: 'expGroups',
                                component: expGroupsList
                            },
                            {
                                path: 'agent-pools',
                                name: 'agentPools',
                                component: agentPools
                            },
                            {
                                path: 'pool-settings/:poolId/:poolName',
                                name: 'poolSettings',
                                component: poolSettings
                            },
                            {
                                path: 'add-agent/:poolId/:poolName',
                                name: 'addAgent',
                                component: addAgent
                            },
                            {
                                path: 'agent-list/:poolId/:poolName',
                                name: 'agentList',
                                component: agentList
                            },
                            {
                                path: 'agent-detail/:poolId/:poolName/:agentId',
                                name: 'agentDetail',
                                component: agentDetail
                            }
                        ]
                    },
                    {
                        path: 'notifications',
                        name: 'notifications',
                        component: notifications
                    },
                    {
                        path: 'metric',
                        name: 'metric',
                        component: metric
                    }
                ]
            },
            {
                path: '*',
                name: '404',
                component: exception
            }
        ]
    }
]

const router = new VueRouter({
    mode: 'history',
    routes: routes
})

// 自动携带项目信息
router.beforeEach((to, from, next) => {
    // 清除
    websocket.loginOut(from)
    // 写入
    websocket.changeRoute(to)
    const params = {
        ...to,
        hash: to.hash || from.hash
    }
    if (to.hash || (!to.hash && !from.hash) || (to.name === 'home' || to.name === 'dashboard')) {
        next()
    } else {
        next(params)
    }
})

export default router
