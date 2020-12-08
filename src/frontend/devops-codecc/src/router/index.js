/**
 * @file router 配置
 * @author blueking
 */

import Vue from 'vue'
import VueRouter from 'vue-router'

import store from '@/store'
import http from '@/api'
import preload, { getTaskDetail, getToolMeta, getToolList } from '@/common/preload'

import taskRoutes from './task'
import toolRoutes from './tool'
import defectRoutes from './defect'
import checkerRoutes from './checker'
import checkersetRoutes from './checkerset'

Vue.use(VueRouter)

const NotFound = () => import(/* webpackChunkName: 'none' */'../views/404')
const Auth = () => import(/* webpackChunkName: 'auth' */'../views/403')
const Serve = () => import(/* webpackChunkName: 'serve' */'../views/500')

const rootRoutes = [
    {
        path: '/403',
        name: '403',
        component: Auth,
        meta: {
            layout: 'full'
        }
    },
    {
        path: '/500',
        name: '500',
        component: Serve,
        meta: {
            layout: 'full'
        }
    },
    // 404
    {
        path: '*',
        name: '404',
        component: NotFound
    }
]
const defaultRouters = [
    {
        path: '/',
        redirect: { name: 'task-list' }
    },
    {
        path: '/codecc/:projectId',
        redirect: { name: 'task-list' }
    },
    {
        path: '/codecc/:projectId/*',
        redirect: { name: 'task-list' }
    }
]

const routes = rootRoutes.concat(taskRoutes, toolRoutes, defectRoutes, checkerRoutes, checkersetRoutes, defaultRouters)

const router = new VueRouter({
    mode: 'history',
    routes: routes
})

const cancelRequest = async () => {
    const allRequest = http.queue.get()
    const requestQueue = allRequest.filter(request => request.cancelWhenRouteChange)
    await http.cancel(requestQueue.map(request => request.requestId))
}

let preloading = true
let canceling = true
// let pageMethodExecuting = true

router.beforeEach(async (to, from, next) => {
    canceling = true
    await cancelRequest()
    canceling = false

    try {
        // const projectList = await getProjectList()
        // 获取蓝盾跳转过来时的项目id
        if (to.query.hasOwnProperty('bkci-projectId')) {
            store.commit('updateProjectId', to.query['bkci-projectId'])
            next({
                name: 'task-list',
                params: { projectId: to.query['bkci-projectId'] },
                replace: true
            })
        }
    } catch (e) {
        console.error(e)
    }

    if (to.params.taskId) {
        store.commit('updateTaskId', to.params.taskId)
        if (to.params.projectId) {
            store.commit('updateProjectId', to.params.projectId)
        }
        // await getTaskDetail()
    }
    if (!to.meta.hasOwnProperty('layout')) {
        to.meta.layout = 'inner'
        next()
    } else {
        next()
    }
})

router.afterEach(async (to, from) => {
    // store.commit('setMainContentLoading', true)

    const pageDataMethods = []
    const routerList = to.matched
    const routeParams = to.params

    if (routeParams.projectId) {
        store.commit('updateProjectId', routeParams.projectId)
    }

    try {
        if (to.params.taskId) {
            getTaskDetail()
        }
        // 当store里面基础数据还没有，且页面需要这些元素，先加载
        if (!store.state.toolMeta.LANG.length && to.meta && !to.meta.notNeedMeta) {
            getToolMeta()
        }
        if (!store.state.tool.mapList.CCN && to.meta && !to.meta.notNeedToolList) {
            getToolList()
        }
        preloading = true
        await preload()
        preloading = false
    } catch (e) {
        console.error(e, e.message)
    }

    routerList.forEach(r => {
        const fetchPageData = r.instances.default && r.instances.default.fetchPageData
        if (fetchPageData && typeof fetchPageData === 'function') {
            pageDataMethods.push(r.instances.default.fetchPageData())
        }
    })

    try {
        // pageMethodExecuting = true
        await Promise.all(pageDataMethods)
        // pageMethodExecuting = false
    } catch (e) {
        console.error(e, e.message)
    }

    if (!preloading && !canceling) {
        // store.commit('setMainContentLoading', false)
    }
})

export default router
