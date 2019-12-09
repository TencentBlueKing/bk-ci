import SockJS from 'sockjs-client'
const Stomp = require('stompjs/lib/stomp.js').Stomp

function uuid () {
    let id = ''
    for (let i = 0; i < 7; i++) {
        const randomNum = Math.floor((1 + Math.random()) * 0x10000).toString(16).substring(1)
        id += randomNum
    }
    return id
}

class BlueShieldWebSocket {
    constructor () {
        this.connectErrTime = 1
        this.sendErrTime = 1
        this.hasConnect = false
        this.userName = /bk_uid=([^;]+);/.exec(document.cookie)[1]
        this.uuid = uuid()
        this.diaLogUuid = ''
        this.stompClient = {}
        window.addEventListener('message', (res) => this.handlePostMessage(res))

        this.connect()
        this.readyDisConnect()
    }

    connect (callBack) {
        const socket = new SockJS(`${WS_URL_PREFIX}/websocket/ws/user?sessionId=${this.uuid}`)
        this.stompClient = Stomp.over(socket)
        this.stompClient.debug = null
        this.stompClient.connect({}, () => {
            this.stompClient.subscribe(`/topic/bk/notify/${this.uuid}`, (res) => {
                this.connectErrTime = 1
                this.handleMessage(res)
                if (callBack) {
                    callBack()
                    callBack = null
                }
            })
        }, (err) => {
            if (this.connectErrTime <= 8) {
                this.connectErrTime++
                const time = Math.random() * 10000
                setTimeout(() => this.connect(), time)
            } else {
                window.devops.$bkMessage({ message: err.message || 'websocket异常，请稍后重试', theme: 'error' })
            }
        })
    }

    handleMessage (res) {
        const data = JSON.parse(res.body) || {}
        const type = data.webSocketType
        const page = data.page
        if (!location.href.includes(page)) return

        switch (type) {
            case 'NAV':
                this.handleNotify(data)
                break
            case 'IFRAME':
            case 'IFRAMEDIALOG':
                const iframe = document.getElementById('iframe-box')
                const iframeWindow = iframe.contentWindow
                iframeWindow.postMessage(data, '*')
                break
            case 'AMD':
            case 'AMDDIALOG':
                window.postMessage(data)
                break
        }
    }

    handleNotify (data) {
        const vm = window.devops
        const h = vm.$createElement
        const messageMap = {
            1: {
                message: data.dealUrl ? h('p', { style: { margin: 0 } }, [data.message, h('a', { style: { color: 'blue' }, attrs: { href: data.dealUrl, target: '_Blank' } }, '，去处理')]) : data.message,
                theme: 'error',
                delay: 0
            },
            9: {
                message: data.message,
                theme: 'success'
            }
        }

        const notify = Object.assign({ title: '蓝盾通知', limitLine: 0 }, messageMap[data.level])
        vm.$bkNotify(notify)
    }

    changeRoute (router) {
        const meta = router.meta || {}
        const path = router.path
        const pathRegs = meta.webSocket || []
        const hasWebSocket = pathRegs.some((reg) => reg && new RegExp(reg).test(path))
        const currentPage = window.currentPage || {}
        const showProjectList = currentPage.show_project_list || false
        const projectId = localStorage.getItem('projectId')
        const data = JSON.stringify({ sessionId: this.uuid, userId: this.userName, page: router.path, showProjectList, projectId })

        if (hasWebSocket) setTimeout(() => { this.loopSendChangePage(data) }, 5)
    }

    loginOut (from) {
        const data = { sessionId: this.uuid, userId: this.userName, page: from.path }
        if (this.hasConnect) { this.stompClient.send('/app/loginOut', {}, JSON.stringify(data)); this.hasConnect = false }
    }

    loopSendChangePage (data) {
        if ((this.stompClient || {}).connected) {
            this.sendErrTime = 1
            this.stompClient.send('/app/changePage', {}, data)
            this.hasConnect = true
        } else {
            this.sendErrTime++
            if (this.sendErrTime <= 15) {
                setTimeout(() => this.loopSendChangePage(data), 1000)
            } else {
                this.connect(() => {
                    this.sendErrTime = 1
                    this.stompClient.send('/app/changePage', {}, data)
                    this.hasConnect = true
                })
            }
        }
    }

    handlePostMessage (res) {
        const resData = res.data
        const type = resData.type
        const page = resData.page
        let data
        switch (type) {
            case 'openLogWs':
                if (this.diaLogUuid) return
                this.diaLogUuid = uuid()
                const projectId = localStorage.getItem('projectId')
                data = JSON.stringify({ sessionId: this.diaLogUuid, userId: this.userName, page, showProjectList: true, projectId })
                this.loopSendChangePage(data)
                break;
            case 'closeLogWs':
                data = { sessionId: this.diaLogUuid, userId: this.userName, page }
                this.stompClient.send('/app/loginOut', {}, JSON.stringify(data))
                this.diaLogUuid = ''
        }
    }

    readyDisConnect () {
        window.addEventListener('beforeunload', () => {
            const data = JSON.stringify({ sessionId: this.uuid, userId: this.userName })
            if (this.hasConnect && this.stompClient.connected) { this.stompClient.send('/app/loginOut', {}, data); this.hasConnect = false }

            if (this.stompClient.connected) {
                this.stompClient.send('/app/clearUserSession', {}, data)
                this.stompClient.disconnect()
            }
        })
    }
}

export default new BlueShieldWebSocket()
