import cookie from 'js-cookie'
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
        this.connectCallBack = []
        this.isConnecting = false
        this.hasConnect = false
        this.userName = window.userInfo && window.userInfo.username ? window.userInfo.username : 'bkDevops'
        this.uuid = uuid()
        this.stompClient = {}
        
        this.connect()
        this.closePageDisConnect()
        this.onlineConnect()
        this.offlineDisconnect()
    }

    connect () {
        const socket = new SockJS(`/websocket/ws/user?sessionId=${this.uuid}`)
        this.stompClient = Stomp.over(socket)
        this.stompClient.debug = null
        this.stompClientConnect()
        socket.onclose = (err) => {
            try {
                console.log(err, socket, this.stompClient)
                if (err.code === 1006) { // 异常断开
                    const vm = window.devops
                    const currentRoute = vm.$router.currentRoute
                    if (this.detectHasWebsocket(currentRoute) && !this.isConnecting) {
                        if (currentRoute.path.indexOf('executeDetail') > -1) {
                            console.log('executeDetail page close reconnect', currentRoute.path)
                            this.connectCallBack.push(() => {
                                this.handleMessage({
                                    body: JSON.stringify({
                                        webSocketType: 'IFRAME',
                                        page: currentRoute.path,
                                        message: JSON.stringify('WEBSOCKET_RECONNECT')
                                    })
                                })
                            })
                        }
                        console.log('other page close reconnect')
                        socket.onclose = null
                        this.connect()
                    }
                } else {
                    console.log('websocket close event.code: ', err.code)
                }
            } catch (error) {
                console.error(error)
            }
        }
    }

    stompClientConnect () {
        this.isConnecting = true
        
        this.stompClient.connect({}, () => {
            console.log('websocket connected', this.connectCallBack)
            this.isConnecting = false
            this.stompClient.subscribe(`/topic/bk/notify/${this.uuid}`, (res) => {
                this.handleMessage(res)
            })
            this.connectErrTime = 1
            this.changeRoute(window.devops.$router.currentRoute)
            if (this.connectCallBack.length) {
                this.connectCallBack.forEach(callBack => callBack())
                this.connectCallBack = []
            }
        }, (err) => {
            if (this.connectErrTime <= 8) {
                console.log('websocket connection retrying')
                this.connectErrTime++
                const time = Math.random() * 60000
                setTimeout(() => this.connect(), time)
            } else {
                this.isConnecting = false
                window.devops.$bkMessage({ message: err.message || 'websocket connection failed, please try again later', theme: 'error' })
            }
        })
    }

    handleMessage (res) {
        const data = JSON.parse(res.body) || {}
        const type = data.webSocketType
        const page = data.page

        switch (type) {
            case 'NAV':
                this.handleNotify(data)
                break
            case 'IFRAME': {
                if (!location.href.includes(page)) return
                const iframe = document.getElementById('iframe-box')
                const iframeWindow = iframe.contentWindow
                iframeWindow.postMessage(data, '*')
                break
            }
            case 'AMD':
                if (location.href.includes(page)) window.postMessage(data)
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

    detectHasWebsocket (router) {
        try {
            const meta = router.meta || {}
            const path = router.path
            const pathRegs = meta.webSocket || []
            return pathRegs.some((reg) => reg && new RegExp(reg).test(path))
        } catch (error) {
            return false
        }
    }

    changeRoute (router) {
        const hasWebSocket = this.detectHasWebsocket(router)
        const currentPage = window.currentPage || {}
        const showProjectList = currentPage.show_project_list || false
        const projectId = cookie.get(X_DEVOPS_PROJECT_ID)
        const data = JSON.stringify({ sessionId: this.uuid, userId: this.userName, page: router.path, showProjectList, projectId })

        if (hasWebSocket) {
            setTimeout(() => {
                this.ensureSendMessage(() => {
                    this.stompClient.send('/app/changePage', {}, data)
                    this.hasConnect = true
                })
            }, 5)
        }
    }

    loginOut (from) {
        const data = { sessionId: this.uuid, userId: this.userName, page: from.path }
        if (this.hasConnect) {
            this.ensureSendMessage(() => {
                this.stompClient.send('/app/loginOut', {}, JSON.stringify(data))
                this.hasConnect = false
            })
        }
    }

    ensureSendMessage (callBack) {
        if ((this.stompClient || {}).connected) {
            callBack()
        } else if (this.isConnecting) {
            this.connectCallBack.push(callBack)
        } else {
            this.connectCallBack.push(callBack)
            this.connect()
        }
    }

    closePageDisConnect () {
        window.addEventListener('beforeunload', (event = {}) => {
            const target = event.target || {}
            const activeElement = target.activeElement || {}
            const tagName = activeElement.tagName || ''
            // a标签也会触发这个事件，需要屏蔽
            if (tagName === 'A') return
            navigator.sendBeacon(`/websocket/api/user/websocket/sessions/${this.uuid}/userIds/${this.userName}/clear`)
            this.stompClient.disconnect()
            this.hasConnect = false
        })
    }

    offlineDisconnect () {
        window.addEventListener('offline', () => this.stompClient.disconnect())
    }

    onlineConnect () {
        window.addEventListener('online', () => {
            if (!this.isConnecting) this.connect()
        })
    }
}

export default new BlueShieldWebSocket()
