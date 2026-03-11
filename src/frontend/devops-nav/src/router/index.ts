import Vue from 'vue'
import Router from 'vue-router'
import { getServiceAliasByPath, ifShowNotice, importScript, importStyle, updateRecentVisitServiceList, urlJoin } from '../utils/util'

import request from '@/utils/request'
import cookie from 'js-cookie'
import compilePath from '../utils/pathExp'

// 首页 - index
const Index = () => import('../views/Index.vue')

const Home = () => import('../views/Home.vue')

const IFrame = () => import('../views/IFrame.vue')

const ProjectManage = () => import('../views/ProjectManage.vue')

const Maintaining = () => import('../views/503.vue')

const UnDeploy = () => import('../views/UnDeploy.vue')

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
            },
            {
                path: 'undeploy/:id',
                name: 'undeploy',
                component: UnDeploy
            }
        ]
    }
]

function isAmdModule (currentPage: subService): boolean {
    return currentPage && currentPage.inject_type === 'amd'
}

const createRouter = (store: any, dynamicLoadModule: any, i18n: any) => {
    counterUser()
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
    
    router.beforeEach(async (to, from, next) => {
        if (window.diclosurePrjoectList?.includes(to.params.projectId)) {
            for await (const signed of showNonDisclosureAgreement(store, to.params.projectId)) {
                if (signed) {
                    if (store.state.cancelDisclosureHandler === 'function') {
                        // 已开始签署
                        setTimeout(() => {
                            resolveRoute(to, from, next)
                        }, 3000)
                    } else {
                        resolveRoute(to, from, next)
                    }
                    break
                }
            }
            next(false)
        } else {
            resolveRoute(to, from, next)
        }
    })

    router.afterEach(route => {
        updateRecentVisitServiceList(route.path)
        
        store.dispatch('upadteHeaderConfig', updateHeaderConfig(route.meta))

        const isShowNotice = ifShowNotice(store.state.currentNotice || {})
        isShowNotice && store.dispatch('toggleNoticeDialog', isShowNotice)
    })

    async function resolveRoute (to, from, next) {
        try {
            const serviceAlias = getServiceAliasByPath(to.path)
            const currentPage = window.serviceObject.serviceMap[serviceAlias]
            const { platformInfo } = (store.state as any).platFormConfig
        
            if (to.name !== from.name && platformInfo) {
                let platformTitle = `${platformInfo.i18n.name || platformInfo.name} | ${platformInfo.i18n.brandName || platformInfo.brandName}`
                if (currentPage) {
                    platformTitle = `${currentPage.name} | ${platformTitle}`
                }
                document.title = platformTitle
            }
            window.currentPage = currentPage
        
            store.dispatch('updateCurrentPage', currentPage) // update currentPage
            if (!currentPage) { // console 首页
                next()
                return
            }
            if (isAmdModule(currentPage) && !loadedModule[serviceAlias]) {
                const { css_url, js_url } = currentPage
                loadedModule[serviceAlias] = true
                store.dispatch('toggleModuleLoading', true)
                
                await Promise.all([
                    goNext(from, to, next),
                    importStyle(css_url, document.head),
                    importScript(js_url, document.body),
                    dynamicLoadModule(serviceAlias, i18n.locale)
                ])
                const module = window.Pages[serviceAlias]
                store.registerModule(serviceAlias, module.store)
                
                router.addRoute({
                    path: '/console/',
                    component: Index,
                    children: module.routes
                })
                setTimeout(() => {
                    store.dispatch('toggleModuleLoading', false)
                }, 100)
            } else if (isAmdModule(currentPage) && loadedModule[serviceAlias]) {
                await dynamicLoadModule(serviceAlias, i18n.locale)
                goNext(from, to, next)
            } else {
                goNext(from, to, next)
            }
        } catch (e) {
            next({
                name: '503'
            })
        }
    }
    
    return router
}

function updateHeaderConfig (routeMeta: any) {
    return {
        // eslint-disable-next-line camelcase
        showProjectList: routeMeta.showProjectList || (window.currentPage?.show_project_list && typeof routeMeta.showProjectList === 'undefined'),
        // eslint-disable-next-line camelcase
        showNav: routeMeta.showNav || (window.currentPage?.show_nav && typeof routeMeta.showNav === 'undefined')
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

export function getProjectId (params): string {
    try {
        const cookiePid = cookie.get(X_DEVOPS_PROJECT_ID)
        const projectId = window.GLOBAL_PID || cookiePid
        return String(params.projectId) !== '0' && params.projectId ? params.projectId : projectId
    } catch (e) {
        return ''
    }
}

function initProjectId (to): string {
    try {
        const { matched, params } = to
        const projectId: string = getProjectId(params)
        const lastMatched = matched[matched.length - 1]
        
        const options = projectId
            ? {
                ...params,
                projectId
            }
            : params

        return matched.length ? compilePath(lastMatched.path)(options) : to.path
    } catch (e) {
        console.log(e)
        return to.path
    }
}

function goNext (from, to, next) {
    const newPath = initProjectId(to)
    // @ts-ignore
    window.setProjectIdCookie(getProjectId(to.params))
    if (to.path !== newPath) {
        next({
            path: newPath,
            query: to.query,
            hash: to.hash,
            replace: true
        })
    } else {
        next()
    }
}

async function* showNonDisclosureAgreement (store, projectId) {
    let timeoutId
    let cancelled = false
    
    // eslint-disable-next-line no-unmodified-loop-condition
    while (!cancelled) {
        try {
            const signed = await store.dispatch('fetchSignatureStatus', { projectId })
            if (signed) {
                yield signed
                break
            }

            if (!store.state.isShowNonDisclosureAgreement) {
                store.dispatch('toggleSignatureDialog', true)
                // Add cancel handler in store
                store.dispatch('setCancelHandler', () => {
                    cancelled = true
                    store.dispatch('setCancelHandler', null)
                    if (timeoutId) clearTimeout(timeoutId)
                })
            }

            yield false
            
            // Make timeout cancellable
            await new Promise(resolve => {
                timeoutId = setTimeout(resolve, 3000)
            })
        } catch (e) {
            console.error('Error checking non-disclosure agreement:', e)
            yield true
            break
        } finally {
            timeoutId = null
        }
    }
}

export default createRouter
