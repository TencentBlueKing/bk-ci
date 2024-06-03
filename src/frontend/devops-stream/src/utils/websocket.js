import store from '../store'
import SockJS from 'sockjs-client'
import { getCookie } from '@/utils'
import { getWSpath } from './index'
const Stomp = require('stompjs/lib/stomp.js').Stomp

function uuid () {
    let id = ''
    for (let i = 0; i < 7; i++) {
        const randomNum = Math.floor((1 + Math.random()) * 0x10000).toString(16).substring(1)
        id += randomNum
    }
    return id
}

class StreamWebSocket {
    constructor () {
        this.connectErrTime = 1
        this.connectCallBack = []
        this.isConnecting = false
        this.hasConnect = false
        this.uuid = uuid()
        this.stompClient = {}

        this.connect()
        this.closePageDisconnect()
        this.onlineConnect()
        this.offlineDisconnect()
    }

    connect () {
        const socket = new SockJS(`${WEBSOCKET_URL_PREFIX}/websocket/ws/user?sessionId=${this.uuid}`)
        this.stompClient = Stomp.over(socket)
        this.stompClient.debug = null
        this.isConnecting = true
        this.stompClient.connect({}, () => {
            this.isConnecting = false
            this.stompClient.subscribe(`/topic/bk/notify/${this.uuid}`, (res) => {
                this.handleMessage(res)
            })
            this.connectErrTime = 1
            if (this.connectCallBack.length) {
                this.connectCallBack.forEach(callBack => callBack())
                this.connectCallBack = []
            }
        }, (err) => {
            if (this.connectErrTime <= 8) {
                this.connectErrTime++
                const time = Math.random() * 60000
                console.log(`websocket connection error: ${err.message}`)
                setTimeout(() => this.connect(), time)
            } else {
                this.isConnecting = false
                window.mainComponent.$bkMessage({ message: err.message || 'websocket connection failed, please try again later', theme: 'error' })
            }
        })
    }

    handleMessage (res) {
        const data = JSON.parse(res.body) || {}
        window.postMessage(data)
    }

    changeRoute (router) {
        const meta = router.meta || {}
        const hasWebSocket = meta.websocket
        const state = store.state || {}
        const data = {
            sessionId: this.uuid,
            userId: (state.user || {}).username,
            page: getWSpath(router.path),
            showProjectList: false,
            projectId: state.projectId
        }

        if (hasWebSocket && state.projectId) {
            setTimeout(() => {
                this.ensureSendMessage(() => {
                    this.stompClient.send('/app/changePage', {}, JSON.stringify(data))
                    this.hasConnect = true
                })
            }, 5)
        }
    }

    loginOut (from) {
        const data = {
            sessionId: this.uuid,
            userId: (store.state.user || {}).username,
            page: getWSpath(from.path)
        }
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

    closePageDisconnect () {
        window.addEventListener('beforeunload', (event = {}) => {
            const target = event.target || {}
            const activeElement = target.activeElement || {}
            const tagName = activeElement.tagName || ''
            // a标签也会触发这个事件，需要屏蔽
            if (tagName === 'A') return
            navigator.sendBeacon(`/websocket/api/user/websocket/sessions/${this.uuid}/userIds/${getCookie('bk_uid')}/clear`)
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

export default new StreamWebSocket()
