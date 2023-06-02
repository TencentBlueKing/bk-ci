import store from '../store'
const moment = require('moment')

/**
 *  将毫秒值转换成xx:xx(分:秒)的形式
 *  @param {Number} time - 时间的毫秒形式
 *  @return {String} str - 转换后的字符串
 */
export function coverTimer (time, type) {
    let res = ''
    function getSeconds (sec, min) {
        const m = min / 60 >= 1 ? '' : '00:'
        if (type) {
            res = sec < 10 ? `${m}0${sec}秒` : `${m}${sec}秒`
        } else {
            res = sec < 10 ? `${m}0${sec}` : `${m}${sec}`
        }
        return res
    }

    function getMinutes (sec) {
        if (sec / 60 >= 1) {
            let res = ''
            let m = Math.floor(sec / 60)
            m = m < 10 ? `0${m}` : m
            if (type) {
                res = `${m}分${getSeconds(sec % 60, sec)}`
            } else {
                res = `${m}:${getSeconds(sec % 60, sec)}`
            }
            return res
        } else {
            return getSeconds(sec)
        }
    }

    function getHours (sec) {
        if (sec / 3600 >= 1) {
            let res = ''
            let h = Math.floor(sec / 3600)
            h = h < 10 ? `0${h}` : h
            if (type) {
                res = `${h}时${getMinutes(sec % 3600)}`
            } else {
                res = `${h}:${getMinutes(sec % 3600)}`
            }
            return res
        } else {
            return getMinutes(sec)
        }
    }

    return time ? getHours(Math.floor(time / 1000)) : '00:00'
}

/**
 *  将毫秒值转换成x时x分x秒的形式
 *  @param {Number} time - 时间的毫秒形式
 *  @return {String} str - 转换后的字符串
 */
export function convertMStoString (time) {
    function getSeconds (sec) {
        return `${sec}秒`
    }

    function getMinutes (sec) {
        if (sec / 60 >= 1) {
            return `${Math.floor(sec / 60)}分${getSeconds(sec % 60)}`
        } else {
            return getSeconds(sec)
        }
    }

    function getHours (sec) {
        if (sec / 3600 >= 1) {
            return `${Math.floor(sec / 3600)}时${getMinutes(sec % 3600)}`
        } else {
            return getMinutes(sec)
        }
    }

    function getDays (sec) {
        if (sec / 86400 >= 1) {
            return `${Math.floor(sec / 86400)}天${getHours(sec % 86400)}`
        } else {
            return getHours(sec)
        }
    }

    return time ? getDays(Math.floor(time / 1000)) : '0秒'
}

export const uuid = () => {
    let id = ''
    for (let i = 0; i < 7; i++) {
        const randomNum = Math.floor((1 + Math.random()) * 0x10000).toString(16).substring(1)
        id += randomNum
    }
    return id
}

export function convertFileSize (size, unit) {
    const arr = ['B', 'KB', 'MB', 'GB', 'TB']
    const calcSize = size / 1024
    let index

    arr.some((item, _index) => {
        if (unit === item) {
            index = _index
            return true
        }
        return false
    })

    const next = arr[index + 1]

    if (calcSize > 1024) {
        if (!next) {
            return `${calcSize.toFixed(2)}${unit}`
        } else {
            return convertFileSize(calcSize, next)
        }
    } else {
        return `${calcSize.toFixed(2)}${next || unit}`
    }
}

export function goYaml (projectUrl, branch, yamlName) {
    if (yamlName) {
        window.open(`${projectUrl}/blob/${encodeURIComponent(branch)}/${encodeURIComponent(yamlName)}`, '_blank')
    }
}

export function preciseDiff (duration) {
    if (!duration) return '--'
    const durationDate = moment.duration(Math.abs(duration))
    const timeMap = {
        y: durationDate.years(),
        mon: durationDate.months(),
        d: durationDate.days(),
        h: durationDate.hours(),
        m: durationDate.minutes(),
        s: durationDate.seconds()
    }
    const diffTime = []
    let hasFirstNum = false
    for (const key in timeMap) {
        const val = timeMap[key]
        if (val <= 0 && !hasFirstNum) continue
        hasFirstNum = true
        diffTime.push(`${val}${key}`)
    }
    return diffTime.join(' ')
}

export function timeFormatter (time, format = 'YYYY-MM-DD HH:mm:ss') {
    return time ? moment(time).format(format) : '--'
}

export function commitIdFormatter (commitId) {
    return commitId ? commitId.slice(0, 9) : '--'
}

export function getbuildTypeIcon (objectKind, operationKind) {
    const operationKindMap = {
        delete: 'close-circle'
    }
    const objectKindIconMap = {
        manual: 'manual',
        push: 'commit',
        tag_push: 'tag',
        merge_request: 'merge',
        schedule: 'clock_fill',
        openApi: 'open-api',
        issue: 'issue',
        review: 'code-review',
        note: 'comment',
        pull_request: 'merge'
    }
    return operationKindMap[operationKind] || objectKindIconMap[objectKind] || 'well'
}

export function modifyHtmlTitle (title) {
    document.title = title
}

export function debounce (callBack, time = 200) {
    window.clearTimeout(debounce.timeId)
    debounce.timeId = window.setTimeout(callBack, time)
}

export function getWSpath (path = '') {
    const state = store.state || {}
    return path + (path.endsWith('/') ? '' : '/') + state.projectId
}

export function setCookie (cname, cvalue, domain) {
    document.cookie = `${cname}=${cvalue};domain=${domain}; path=/;expires=Fri, 31 Dec 2030 23:59:59 GMT`
}

export function deleteCookie (cname) {
    const cookies = document.cookie.split('; ')
    for (let index = 0; index < cookies.length; index++) {
        const cookie = cookies[index]
        const cookieKey = cookie.split(';')[0].split('=')[0]
        if (cookieKey !== cname) {
            continue
        }
        const hostArray = window.location.hostname.split('.')
        while (hostArray.length > 0) {
            const cookieBase = encodeURIComponent(cookieKey) + '=; expires=Thu, 01-Jan-1970 00:00:01 GMT; domain=' + hostArray.join('.') + ' ;path='
            const pathArray = location.pathname.split('/')
            document.cookie = cookieBase + '/'
            while (pathArray.length > 0) {
                document.cookie = cookieBase + pathArray.join('/')
                pathArray.pop()
            }
            hostArray.shift()
        }
    }
}

export function getCookie (key) {
    const cookieStr = document.cookie || ''
    const cookieArr = cookieStr.split(';').filter(v => v)
    const cookieObj = cookieArr.reduce((res, cookieItem) => {
        const [key, value] = cookieItem.split('=')
        const cKey = (key || '').trim()
        const cVal = (value || '').trim()
        res[cKey] = cVal
        return res
    }, {})
    return cookieObj[key] || ''
}

export function getLanguageMap (key = '') {
    if (!key) return ''
    let language = ''
    if (key.toLowerCase() === 'en' || key.toLowerCase() === 'en-us' || key.toLowerCase() === 'en_us') {
        language = 'en-US'
    } else {
        language = 'zh-CN'
    }
    return language
}

export function getDisplayName (displayName = '') {
    return displayName.replace(/^\.ci\//, '')
}

export const getSubdomain = () => {
    try {
        let subdomain = ''
        const key = `mh_${Math.random()}`
        const expiredDate = new Date(0)
        const { domain } = document
        const domainList = domain.split('.')
  
        const reg = new RegExp(`(^|;)\\s*${key}=12345`)
        // 若为 IP 地址、localhost，则直接返回
        if (domain === 'localhost') {
            return domain
        }
  
        const urlItems = []
        urlItems.unshift(domainList.pop())
  
        while (domainList.length) {
            urlItems.unshift(domainList.pop())
            subdomain = urlItems.join('.')
  
            const cookie = `${key}=12345;domain=.${subdomain}`
            document.cookie = cookie
  
            if (reg.test(document.cookie)) {
                document.cookie = `${cookie};expires=${expiredDate}`
                break
            }
        }
  
        return subdomain || document.domain
    } catch (e) {
        return document.domain
    }
}
