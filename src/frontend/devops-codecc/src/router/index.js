/**
 * @file router 配置
 * @author blueking
 */

import Vue from 'vue'
import VueRouter from 'vue-router'

import store from '@/store'
import http from '@/api'
import preload, { getTaskDetail, getProjectList, getTaskStatus } from '@/common/preload'

import taskRoutes from './task'
import toolRoutes from './tool'
import defectRoutes from './defect'

Vue.use(VueRouter)

const NotFound = () => import(/* webpackChunkName: 'none' */'../views/404')
const Auth = () => import(/* webpackChunkName: 'auth' */'../views/403')
const Serve = () => import(/* webpackChunkName: 'serve' */'../views/500')

const rootRoutes = [
    {
        path: '/',
        redirect: { name: 'task-list' }
    },
    {
        path: '/codecc/:projectId',
        redirect: { name: 'task-list' }
    },
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

const routes = rootRoutes.concat(taskRoutes, toolRoutes, defectRoutes)

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
let pageMethodExecuting = true

router.beforeEach(async (to, from, next) => {
    canceling = true
    await cancelRequest()
    canceling = false

    try {
        const projectList = await getProjectList()
        // 获取蓝盾跳转过来时的项目id
        if (to.query.hasOwnProperty('bkci-projectId')) {
            store.commit('updateProjectId', to.query['bkci-projectId'])
            next({
                name: 'task-list',
                params: { projectId: to.query['bkci-projectId'] },
                replace: true
            })
        } else {
            // 没有项目ID，则取第一个项目
            if (!to.params.hasOwnProperty('projectId') && to.name !== '404' && to.name !== '403') {
                next({
                    name: 'task-list',
                    params: { projectId: projectList.length ? projectList[0].projectCode : '' },
                    replace: true
                })
            }
        }
    } catch (e) {
        console.error(e)
    }

    // 已停用任务统一跳转到任务管理
    if (to.params.taskId) {
        store.commit('updateTaskId', to.params.taskId)
        if (to.params.projectId) {
            store.commit('updateProjectId', to.params.projectId)
        }
        try {
            const taskStatus = await getTaskStatus()
            if (taskStatus.status === 1 && to.name !== 'task-settings-manage') {
                next({
                    path: `/codecc/${to.params.projectId}/task/${to.params.taskId}/settings/manage`,
                    params: to.params,
                    replace: true
                })
            }
            if (taskStatus.status === 1 && from.name === 'task-settings-manage' && to.name !== 'task-settings-blank' && to.name !== 'task-list' && to.params.projectId === from.params.projectId) {
                next({
                    name: 'task-settings-blank',
                    params: to.params,
                    replace: true
                })
            } else if (taskStatus.status === 1 && to.params.projectId !== from.params.projectId && to.name === 'task-list') {
                next({
                    path: `/codecc/${to.params.projectId}/task/list`,
                    replace: true
                })
            } else if (taskStatus.status === 1 && (to.name === 'task-detail' || to.name === 'tool-rules' || to.name.indexOf('task-settings-') === 0 || to.name.indexOf('defect-') === 0) && to.name !== 'task-settings-manage') {
                next({
                    name: 'task-settings-manage',
                    params: to.params,
                    replace: true
                })
            }
            getTaskDetail()
        } catch (e) {
            console.error(e)
        }
    }
    if (!to.meta.hasOwnProperty('layout')) {
        to.meta.layout = 'inner'
        next()
    } else {
        next()
    }
})

router.afterEach(async (to, from) => {
    store.commit('setMainContentLoading', true)

    const pageDataMethods = []
    const routerList = to.matched
    const routeParams = to.params

    if (routeParams.projectId) {
        store.commit('updateProjectId', routeParams.projectId)
    }

    preloading = true
    await preload()
    preloading = false

    routerList.forEach(r => {
        const fetchPageData = r.instances.default && r.instances.default.fetchPageData
        if (fetchPageData && typeof fetchPageData === 'function') {
            pageDataMethods.push(r.instances.default.fetchPageData())
        }
    })

    pageMethodExecuting = true
    await Promise.all(pageDataMethods)
    pageMethodExecuting = false

    if (!preloading && !canceling && !pageMethodExecuting) {
        store.commit('setMainContentLoading', false)
    }
})

export default router
