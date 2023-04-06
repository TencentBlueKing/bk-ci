
export function firstUpperCase (str: string): string {
    try {
        return str[0].toUpperCase() + str.slice(1)
    } catch (e) {
        console.warn(e)
        return str
    }
}

export function camelCase (str: string, separator: string = '_'): string {
    try {
        const [firstWord, ...restWord] = str.split(separator)
        const camelString = restWord.reduce((camelString, word) => {
            camelString += firstUpperCase(word)
            return camelString
        }, '')

        return firstWord + camelString
    } catch (e) {
        console.warn(e)
        return str
    }
}

/**
 * 将对象属性转为 camelCase格式
 * { hello_world: '' } => { helloWorld: '' }
 *
 * @param {Object} obj 待转换的对象
 *
 * @return {Object} 结果
 */
export function transformObj (obj: ObjectMap): ObjectMap {
    if (!isObject(obj)) {
        console.warn('transformObj need obj params', obj)
        return obj
    }
    return Object.keys(obj).reduce((user: any, key: string) => {
        user[camelCase(key)] = obj[key]
        return user
    }, {})
}

export function getServiceLogoByPath (link: string): string {
    return link.replace(/\/?(devops\/)?(\w+)\S*$/, '$2')
}

export function urlJoin (...args): string {
    return args.filter(arg => arg).join('/').replace(/([^:]\/)\/+/g, '$1')
}

export function queryStringify (query: ObjectMap): string {
    return Object.keys(query).map((key: string) => query[key] ? `${key}=${query[key]}` : key).join('&')
}

/**
 * 根据访问路径更新最近访问服务列表
 * @param path  当前访问路径
 */
export function updateRecentVisitServiceList (path: string): void {
    try {
        const recentVisitService: string | null = localStorage.getItem('recentVisitService')
        const recentVisitServiceList = recentVisitService ? JSON.parse(recentVisitService) : []
        const serviceReg: RegExp = /^\/(console\/)?(\w+)\/?/
        const serviceMatch: object | null = path.match(serviceReg)
        const serviceKey: string = serviceMatch ? serviceMatch[2] : ''

        if (serviceKey) {
            const visitedService = recentVisitServiceList.find(service => service.key === serviceKey)
            const service = window.serviceObject.serviceMap[serviceKey]
            
            if (visitedService) {
                Object.assign(visitedService, {
                    visitTimestamp: +new Date()
                })
                
                // 按照访问时间排序
                recentVisitServiceList.sort((s1, s2) => s2.visitTimestamp - s1.visitTimestamp)
            } else if (service && service.status !== 'planning' && service.status !== 'developing') {
                recentVisitServiceList.unshift({
                    key: serviceKey,
                    visitTimestamp: +new Date()
                })
            }
            if (recentVisitServiceList.length > 4) { // 最多保存4个最近访问服务列表
                recentVisitServiceList.pop()
            }
            // 更新LocalStorage
            localStorage.setItem('recentVisitService', JSON.stringify(recentVisitServiceList))
        }
    } catch (e) {
        console.warn(e)
    }
}

export function isObject (param) {
    const type = typeof param
    return param !== null && type === 'object' && !Array.isArray(param)
}

export function isShallowEqual (obj1: object, obj2: object): boolean {
    if (!isObject(obj1) || !isObject(obj2)) {
        return false
    }
    const obj1Keys = Object.keys(obj1)
    const obj2Keys = Object.keys(obj2)
    if (obj1Keys.length !== obj2Keys.length) {
        return false
    }

    return obj1Keys.every((key: string) => obj1[key] === obj2[key])
}

export function judgementLsVersion () {
    const curLsVersion = window.localStorage.getItem('lsVersion')
    if (!curLsVersion || curLsVersion !== DEVOPS_LS_VERSION) {
        window.localStorage.clear()
        localStorage.setItem('lsVersion', DEVOPS_LS_VERSION)
    }
}

// 动态加载js
export function importScript (src, oHead) {
    return new Promise(resolve => {
        const oScript = document.createElement('script')
        oScript.type = 'text\/javascript'
        oScript.setAttribute('src', window.PUBLIC_URL_PREFIX + src)
        oHead.appendChild(oScript)

        oScript.onload = resolve
    })
}

// 动态加载css
export function importStyle (href, oHead) {
    return new Promise(resolve => {
        const oStyle = document.createElement('link')
        oStyle.setAttribute('rel', 'stylesheet')
        oStyle.setAttribute('type', 'text/css')
        oStyle.setAttribute('href', window.PUBLIC_URL_PREFIX + href)
        oHead.appendChild(oStyle)

        oStyle.onload = resolve
    })
}

export function getServiceAliasByPath (path: string): string {
    const serviceAliasREG = /^\/(console\/)?([^\/]+)\/?/
    const execRes = serviceAliasREG.exec(path) || []
    return execRes[2] || path
}

export function isAbsoluteUrl (url) {
    return /^(http(s)?:)?\/\//.test(url)
}

export class HttpError extends Error {
    code = 500
    constructor (code, message = 'http request error message') {
        super(message)
        this.code = code
    }
}

// 判断是否显示公告
export function ifShowNotice (currentNotice) {
    const announcementHistory = localStorage.getItem('announcementHistory') ? JSON.parse(localStorage.getItem('announcementHistory')) : []
    // 判断当前公告是否生效中，并且未展示过
    if (currentNotice && currentNotice.id && currentNotice.noticeType === 0 && announcementHistory.indexOf(currentNotice.id) === -1) {
        // 判断当前公共是否只在特定服务展示
        const noticeService = currentNotice.noticeService || []
        if (!(noticeService.length > 0 && noticeService.indexOf(window.currentPage && window.currentPage.link_new) === -1)) {
            announcementHistory.push(currentNotice.id)
            localStorage.setItem('announcementHistory', JSON.stringify(announcementHistory))
            return true
        }
    }
    return false
}
