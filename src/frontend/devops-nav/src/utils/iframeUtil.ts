import { showLoginModal } from '@blueking/login-modal'
import eventBus from './eventBus'

interface UrlParam {
    url: string
    refresh: boolean
}

function iframeUtil (router: any) {
    const utilMap: ObjectMap = {}
    function init () {
        if (window.addEventListener) {
            window.addEventListener('message', onMessage)
        } else if (window.attachEvent) {
            window.attachEvent('onmessage', onMessage)
        }
    }

    function onMessage (e) {
        parseMessage(e.data)
    }

    function send (target, action, params) {
        target.postMessage({
            action,
            params
        }, '*')
    }
    
    utilMap.updateTabTitle = function (title: string): void {
        if (title) {
            document.title = title
        }
    }

    utilMap.syncUrl = function ({ url, refresh = false }: UrlParam): void {
        const pathname = `${location.pathname.replace(/^\/(\w+)\/(\w+)\/(\S+)$/, '/$1/$2')}${url}`
        if (refresh) {
            location.pathname = pathname
        } else {
            router.replace(pathname)
        }
    }

    utilMap.toggleLoginDialog = function () {
        const successUrl = `${window.location.origin}/console/static/login_success.html`

        // 系统的登录页地址
        let siteLoginUrl = window.getLoginUrl()
        if (!siteLoginUrl) {
            console.error('Login URL not configured!')
            return
        }

        // 处理登录地址为登录小窗需要的格式，主要是设置c_url参数
        !siteLoginUrl.startsWith('https:') && (siteLoginUrl = `${location.protocol}${siteLoginUrl}`)
        const loginURL = new URL(siteLoginUrl)
        loginURL.searchParams.set('c_url', successUrl)
        const pathname = loginURL.pathname.endsWith('/') ? loginURL.pathname : `${loginURL.pathname}/`
        const loginUrl = `${loginURL.origin}${pathname}plain/${loginURL.search}`

        // 传入最终的登录地址，弹出登录窗口，更多选项参考 Options
        showLoginModal({ loginUrl })
    }

    utilMap.popProjectDialog = function (project: Project): void {
        eventBus.$emit('show-project-dialog', project)
    }
 
    utilMap.toggleProjectMenu = function (show): void {
        show ? eventBus.$emit('show-project-menu') : eventBus.$emit('hide-project-menu')
    }

    utilMap.syncTopProjectId = function ({ projectId }): void {
        eventBus.$emit('update-project-id', projectId)
    }

    utilMap.showTips = function (tips): void {
        if (tips.message === 'Network Error') {
            tips.message = '网络出现问题，请检查你的网络是否正常'
        }
        tips.message = tips.message || tips.msg || ''
        eventBus.$bkMessage({
            offsetY: 20,
            limit: 1,
            ...tips
        })
    }
 
    utilMap.syncLocale = function (target: object, locale: string) {
        send(target, 'syncLocale', locale)
    }
 
    utilMap.syncProjectList = function (target, projectList: object[]): void {
        send(target, 'syncProjectList', projectList)
    }

    utilMap.syncProjectId = function (target, projectId: string): void {
        send(target, 'receiveProjectId', projectId)
    }
    
    utilMap.syncUserInfo = function (target, userInfo: object): void {
        send(target, 'syncUserInfo', userInfo)
    }

    utilMap.goHome = function (target: object): void {
        send(target, 'backHome', '')
    }

    utilMap.leaveConfirmOrder = function (target): void {
        send(target, 'leaveConfirmOrder', '')
    }

    utilMap.leaveCancelOrder = function (target): void {
        send(target, 'leaveCancelOrder', '')
    }

    utilMap.leaveConfirm = function ({ content = '离开后，新编辑的数据将丢失', type, subHeader, theme, ...restConf }):void {
        const iframeBox: any = document.getElementById('iframe-box')
        eventBus.$bkInfo({
            type: type || theme,
            theme: theme || type,
            subTitle: content,
            subHeader: subHeader ? eventBus.$createElement('p', {}, subHeader) : null,
            ...restConf,
            confirmFn: () => {
                utilMap.leaveConfirmOrder(iframeBox.contentWindow)
            },
            cancelFn: () => {
                utilMap.leaveCancelOrder(iframeBox.contentWindow)
            }
        })
    }
 
    function parseMessage (data) {
        try {
            const cb = utilMap[data.action]
            if (typeof cb === 'function') {
                return cb(data.params)
            }
        } catch (e) {
            console.warn(e)
        }
    }

    init()

    return utilMap
}

export default iframeUtil
