/**
 * @file router 配置
 * @author Blueking
 */

import Vue from 'vue'
import VueRouter from 'vue-router'
import websocket from '@/utils/websocket'
Vue.use(VueRouter)

const dashboard = () => import(/* webpackChunkName: 'dashboard' */'@/views/dashboard')
const main = () => import(/* webpackChunkName: 'entry' */'@/views/index')
const projectIndex = () => import(/* webpackChunkName: 'entry' */'@/views/project-index')
const exception = () => import(/* webpackChunkName: 'entry' */'@/views/exception')
const notifications = () => import(/* webpackChunkName: 'notifications' */'@/views/notifications')
const pipeline = () => import(/* webpackChunkName: 'pipelines' */'@/views/pipeline')
const buildList = () => import(/* webpackChunkName: 'pipelines' */'@/views/pipeline/build-list')
const pipelineDetail = () => import(/* webpackChunkName: 'buildDetail' */'@/views/pipeline/build-detail/index')
const buildDetail = () => import(/* webpackChunkName: 'buildDetail' */'@/views/pipeline/build-detail/detail')
const buildReports = () => import(/* webpackChunkName: 'buildDetail' */'@/views/pipeline/build-detail/reports')
const buildConfig = () => import(/* webpackChunkName: 'buildDetail' */'@/views/pipeline/build-detail/config')
const webConsole = () => import(/* webpackChunkName: "webConsole" */'@/views/pipeline/web-console.vue')
const setting = () => import(/* webpackChunkName: 'setting' */'@/views/setting/index')
const basicSetting = () => import(/* webpackChunkName: 'setting' */'@/views/setting/basic')
const credentialList = () => import(/* webpackChunkName: 'credential' */'@/views/setting/credential/credential-list')
const credentialSettings = () => import(/* webpackChunkName: 'credential' */'@/views/setting/credential/credential-settings')
const expGroupsList = () => import(/* webpackChunkName: 'expGroups' */'@/views/setting/exp-groups')
const agentPools = () => import(/* webpackChunkName: 'pool' */'@/views/setting/agent-pools/index')
const poolSettings = () => import(/* webpackChunkName: 'pool' */'@/views/setting/agent-pools/pool-settings')
const addAgent = () => import(/* webpackChunkName: 'agent' */'@/views/setting/agent-pools/add-agent')
const agentList = () => import(/* webpackChunkName: 'agent' */'@/views/setting/agent-pools/agent-list')
const agentDetail = () => import(/* webpackChunkName: 'agent' */'@/views/setting/agent-pools/agent-detail')
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

router.afterEach(route => {
    websocket.changeRoute(route)
})

// 自动携带项目信息
router.beforeEach((to, from, next) => {
    websocket.loginOut(from)
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
