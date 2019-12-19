import Vue from 'vue'
import Router from 'vue-router'
import { updateRecentVisitServiceList, urlJoin, getServiceAliasByPath, importScript, importStyle } from '../utils/util'

import compilePath from '../utils/pathExp'
import request from '../utils/request'

// 404
// const None = () => import('../views/None.vue')
// const App = () => import('../views/App.vue')

// 首页 - index
const Index = () => import('../views/Index.vue')

const Home = () => import('../views/Home.vue')

const IFrame = () => import('../views/IFrame.vue')

const QuickStart = () => import('../views/QuickStart.vue')

const ProjectManage = () => import('../views/ProjectManage.vue')

// const Docs = () => import('../views/Docs.vue')

const Maintaining = () => import('../views/503.vue')

Vue.use(Router)

let mod: Route[] = []
for (const key in window.Pages) {
    mod = mod.concat(window.Pages[key].routes)
}

const iframeRoutes = window.serviceObject.iframeRoutes.map(r => ({
    path: urlJoin('/console', r.path, ':restPath*'),
    name: r.name,
    component: IFrame,
    meta: r.meta,
    pathToRegexpOptions: {
        strict: true,
        end: false
    }
}))

const routes = [
    {
        path: '/console',
        component: Index,
        children: [
            {
                path: '',
                name: 'home',
                component: Home,
                meta: {
                    showProjectList: false,
                    showNav: true
                }
            },
            {
                path: 'quickstart',
                name: 'quickstart',
                component: QuickStart,
                meta: {
                    showProjectList: false,
                    showNav: true
                }
            },
            {
                path: 'pm',
                name: 'pm',
                component: ProjectManage,
                meta: {
                    showProjectList: false,
                    showNav: true
                }
            },
            ...iframeRoutes,
            ...mod,
            {
                path: 'maintaining',
                name: '503',
                component: Maintaining
            }
        ]
    }
]

function isAmdModule (currentPage: subService): boolean {
    return currentPage && currentPage.inject_type === 'amd'
}

const createRouter = (store: any, dynamicLoadModule: any, i18n: any) => {
    const router = new Router({
        mode: 'history',
        routes: routes
    })
    
    let loadedModule = {}

    if (isAmdModule(window.currentPage)) {
        const serviceAlias = getServiceAliasByPath(window.currentPage.link_new)
        loadedModule = {
            [serviceAlias]: true
        }
    }
    
    router.beforeEach((to, from, next) => {
        const serviceAlias = getServiceAliasByPath(to.path)
        const currentPage = window.serviceObject.serviceMap[serviceAlias]

        window.currentPage = currentPage
        store.dispatch('updateCurrentPage', currentPage) // update currentPage
        if (!currentPage) { // console 首页
            next()
            return
        }
        
        const { css_url, js_url } = currentPage
        if (isAmdModule(currentPage) && !loadedModule[serviceAlias]) {
            loadedModule[serviceAlias] = true
            store.dispatch('toggleModuleLoading', true)
            Promise.all([
                importStyle(css_url, document.head),
                importScript(js_url, document.body),
                dynamicLoadModule(serviceAlias, i18n.locale)
            ]).then(() => {
                const module = window.Pages[serviceAlias]
                store.registerModule(serviceAlias, module.store)
                const dynamicRoutes = [{
                    path: '/console/',
                    component: Index,
                    children: module.routes
                }]
                
                router.addRoutes(dynamicRoutes)
                setTimeout(() => {
                    store.dispatch('toggleModuleLoading', false)
                }, 100)
                goNext(to, store, next)
            })
            goNext(to, store, next)
        } else if (isAmdModule(currentPage) && loadedModule[serviceAlias]) {
            dynamicLoadModule(serviceAlias, i18n.locale).then(() => {
                goNext(to, store, next)    
            })
        } else {
            goNext(to, store, next)
        }
            
    })

    router.afterEach(route => {
        updateRecentVisitServiceList(route.path)
        
        store.dispatch('upadteHeaderConfig', updateHeaderConfig(route.meta))
    })
    return router
}

function updateHeaderConfig ({ showProjectList, showNav }) {
    return {
        showProjectList: showProjectList || (window.currentPage && window.currentPage.show_project_list && typeof showProjectList === 'undefined'),
        showNav: showNav || (window.currentPage && window.currentPage.show_nav && typeof showNav === 'undefined')
    }
}

/**
 * 上报用户信息
 */
function counterUser (): void {
    const userId = window.userInfo.username
    const os = parseOS()
    
    request.post('/project/api/user/count/login', {
        os,
        userId
    })
}

function uploadBKCounter (count: number = 1): void {
    try {
        const date: Date = new Date()
        const appMsg = {
            bkdevops: {
                [`${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()}`]: count
            }
        }
        window.JSONP('http://open.oa.com/app_statistics/liveness/save_jsonp?app_msg=' + JSON.stringify(appMsg), function () {
            // jsonp callback with data
        })
    } catch (e) {
        console.warn('upload bk error', e)
    }
}

function parseOS (): string {
    const { userAgent } = window.navigator
    switch (true) {
        case userAgent.indexOf('Linux') > -1:
            return /android/i.test(userAgent) ? 'ANDROID' : 'LINUX'
        case userAgent.indexOf('iPhone') > -1:
            return 'IOS'
        case userAgent.indexOf('iPad') > -1:
            return 'iPad'
        case userAgent.indexOf('Mac') > -1:
            return 'MACOS'
        case userAgent.indexOf('Win') > -1:
            return 'WINDOWS'
    }
    return 'WINDOWS'
}

function getProjectId (store, params): string {
    try {
        const projectId = window.GLOBAL_PID || localStorage.getItem('projectId') || store.getters.enableProjectList[0].projectCode
        return String(params.projectId) !== '0' && params.projectId ? params.projectId : projectId
    } catch (e) {
        return ''
    }
    
}

function initProjectId (to, store): string {
    try {
        const { matched, params } = to
        const projectId: string = getProjectId(store, params)
        const lastMatched = matched[matched.length - 1]
        
        const options = projectId ? {
            ...params,
            projectId
        } : params
        return matched.length ? compilePath(lastMatched.path)(options) : to.path
    } catch (e) {
        console.log(e)
        return to.path
    }
}

function goNext(to, store, next) {
    const newPath = initProjectId(to, store)

    // @ts-ignore
    window.setProjectIdCookie(getProjectId(store, to.params))
    if (to.path !== newPath) {
        next({
            path: newPath,
            query: to.query,
            hash: to.hash
        })
    } else {
        next()
    }
}
export default createRouter
