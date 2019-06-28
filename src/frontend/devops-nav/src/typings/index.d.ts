interface User {
    [index: string]: any
    id?: string
    isAuthenticated?: boolean
    username?: string
    avatarUrl?: string
    chineseName?: string
    phone?: string
    email?: string
}

interface ObjectMap {
    [index: string]: any
}

interface Route {
    path: string,
    name: string,
    component: any,
    children: any[],
    meta: any
}
interface Window {
    Pages: any
    eventBus: object
    devops: object
    iframeUtil: ObjectMap
    allServices: ObjectMap[]
    projectList: ObjectMap[]
    serviceObject: ObjectMap
    currentPage: subService
    userInfo: User
    JSONP: Function
    attachEvent(event: string, listener: EventListener): boolean
    detachEvent(event: string, listener: EventListener): void
}
interface subService {
    collected: boolean
    css_url: string
    id: string
    iframe_url: string
    inject_type: string
    show_project_list: boolean
    show_nav: boolean
    js_url: string
    link: string
    name: string
    status: string
    link_new: string,
    project_id_type: string
}

interface Permission {
    resource: string
    option: string
}

declare var LOGIN_SERVICE_URL: string
declare var LOGOUT_SERVICE_URL: string
declare var GW_URL_PREFIX: string
declare var DOCS_URL_PREFIX: string
declare module '*.png'
declare const require: any
declare const X_DEVOPS_PROJECT_ID: string

