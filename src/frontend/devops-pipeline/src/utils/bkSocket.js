/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 * tapd-iframe 场景下使用的 WebSocket 客户端：
 * 1) 直接在当前页面（iframe 内部）建立 SockJS + STOMP 连接，
 *    监听 BK-CI 的 IFRAME 类型推送；
 * 2) 与 ci-cd 工程内的母站 BKSocket 不同，这里不再通过
 *    iframe.contentWindow.postMessage 转发到子页面，而是直接
 *    通过 window.postMessage 把推送派发到当前 window 自身，
 *    让 utils/webSocketMessage.js 的既有监听逻辑无侵入接收。
 */

import Vue from 'vue'
import SockJS from 'sockjs-client'
const Stomp = require('stompjs/lib/stomp.js').Stomp

function uuid () {
    let id = ''
    for (let i = 0; i < 7; i++) {
        const randomNum = Math.floor((1 + Math.random()) * 0x10000)
            .toString(16)
            .substring(1)
        id += randomNum
    }
    return id
}

/**
 * 获取当前登录用户名。
 *
 * 说明：
 * - devops-pipeline 子工程并未在自身代码里挂 Vue.prototype.$userInfo，
 *   该值由外层框架（devops-frontend）异步注入到 Vue 原型上；
 * - 普通 JS 模块里 window.userInfo 并不可靠，main.js 中通过
 *   global.pipelineVue = new Vue({...}) 暴露了根实例，可以从这里拿到 $userInfo；
 * - 这里做多源兜底 + 每次调用都实时获取，避免 BKSocket 构造期 $userInfo
 *   尚未注入而拿到空字符串、后续永远是空的问题。
 */
function getCurrentUserName () {
    try {
        const fromVue = global.pipelineVue
            && global.pipelineVue.$userInfo
            && global.pipelineVue.$userInfo.username
        if (fromVue) return fromVue

        const fromProto = Vue.prototype
            && Vue.prototype.$userInfo
            && Vue.prototype.$userInfo.username
        if (fromProto) return fromProto

        if (window.userInfo && window.userInfo.username) {
            return window.userInfo.username
        }
        if (window.GLOBAL_PIPELINE_LOGIN_USER) {
            return window.GLOBAL_PIPELINE_LOGIN_USER
        }
    } catch (e) {
        // ignore
    }
    return ''
}

class BKSocket {
    constructor () {
        this.connectErrTime = 1
        this.connectCallBack = []
        this.callbacks = []
        this.isReconnecting = false
        this.isConnecting = false
        this.hasConnect = false
        this.isManualReconnecting = false
        // 注意：userName 不再在构造期一次性缓存，避免 $userInfo 异步注入晚到导致永远取空。
        // 通过 getter 每次读取时都走 getCurrentUserName() 实时获取最新值。
        this.uuid = uuid()
        this.url = `${API_URL_PREFIX}/websocket/ws/user?sessionId=${this.uuid}`
        this.topicUrl = `/topic/bk/notify/${this.uuid}`
        this.stompClient = {}
        this.page = ''
        this.projectId = ''

        // 实时返回最新的登录用户名（兼容 $userInfo 异步注入）
        Object.defineProperty(this, 'userName', {
            configurable: true,
            enumerable: true,
            get () {
                return getCurrentUserName()
            }
        })

        this._onBeforeUnload = () => this.disconnect()
        this._onOffline = () => this.stompClient && this.stompClient.disconnect && this.stompClient.disconnect()
        this._onOnline = () => !this.isConnecting && this.connect()

        this.connect()
        this.closePageDisConnect()
        this.onlineConnect()
        this.offlineDisconnect()
    }

    connect () {
        const socket = new SockJS(this.url)

        this.stompClient = Stomp.over(socket)
        this.stompClient.debug = null

        this.stompClientConnect()

        socket.onclose = (err) => {
            try {
                if (err && err.code === 1006) {
                    // 异常断开
                    this.isReconnecting = true
                    if (!this.isConnecting) {
                        socket.onclose = null
                        this.connect()
                    }
                } else {
                    // eslint-disable-next-line no-console
                    console.log('websocket close event.code: ', err && err.code)
                }
            } catch (error) {
                // eslint-disable-next-line no-console
                console.error(error)
            }
        }
    }

    stompClientConnect () {
        this.isConnecting = true
        this.stompClient.connect(
            {},
            () => {
                this.isConnecting = false
                this.stompClient.subscribe(this.topicUrl, (res) => this.onMessage(res))
                this.connectErrTime = 1
                this.page && this.changeRoute()

                if (this.isReconnecting) {
                    this.isReconnecting = false
                }

                if (this.connectCallBack.length) {
                    this.connectCallBack.forEach((cb) => cb())
                    this.connectCallBack = []
                }
            },
            (err) => {
                if (this.connectErrTime <= 8) {
                    this.connectErrTime += 1
                    const time = Math.random() * 60000
                    setTimeout(() => this.connect(), time)
                } else {
                    this.isConnecting = false
                    // eslint-disable-next-line no-console
                    console.error('websocket connection failed:', err && err.message)
                }
            }
        )
    }

    disconnect () {
        // 注意：不要在这里清空 this.callbacks。
        // 自动重连（onclose code 1006）会复用现有 callbacks，清空会导致重连后业务页面收不到推送。
        // 真正销毁实例时由 destroy() 负责把回调与连接一并清掉。
        this.loginOut()
        try {
            this.stompClient && this.stompClient.disconnect && this.stompClient.disconnect()
        } catch (e) {
            // ignore
        }
        this.hasConnect = false
    }

    reconnect () {
        if (this.isManualReconnecting) {
            return
        }
        this.isManualReconnecting = true
        try {
            this.stompClient && this.stompClient.disconnect && this.stompClient.disconnect(() => {
                this.isManualReconnecting = false
                this.hasConnect = false
                this.isReconnecting = false
                this.isConnecting = false
                this.connect()
            })
        } catch (error) {
            // eslint-disable-next-line no-console
            console.error(error)
            this.isManualReconnecting = false
        }
    }

    on (event, callback) {
        if (!['message'].includes(event) || this.callbacks.includes(callback)) {
            return
        }
        this.callbacks.push(callback)
    }

    off (event, callback) {
        if (!['message'].includes(event)) return
        if (!callback) {
            this.callbacks = []
            return
        }
        const index = this.callbacks.indexOf(callback)
        if (index >= 0) this.callbacks.splice(index, 1)
    }

    onMessage (res) {
        let data
        try {
            data = JSON.parse(res.body) || {}
        } catch (e) {
            return
        }
        const type = data.webSocketType
        const [page] = (data.page || '').split('?')

        if (type !== 'IFRAME') {
            // eslint-disable-next-line no-console
            console.info(type, data)
            return
        }
        // 后端下发的 page 形如 /console/pipeline/{projectId}/{pipelineId}/{detail|history|edit|preview}/...
        // 当前 iframe 承载在 /pipeline/tapd/{projectId}/{pipelineId}/{stddetail|detail|history|edit|preview}/... 下，
        // 其中 tapd 详情页有两个：tapdPipelinesDetail(detail/) 与 tapdPipelinesStdDetail(stddetail/)。
        // 这里只比对到 {pipelineId} 维度，避免因路径段差异（例如 stddetail vs detail）误丢消息。
        if (page && !this._matchCurrentPipeline(page)) {
            return
        }
        if (!this.callbacks || !this.callbacks.length) {
            return
        }
        this.callbacks.forEach((cb) => cb(data))
    }

    /**
     * 比对推送 page 是否属于当前正在浏览的 tapd 流水线。
     * 仅校验 projectId/pipelineId 两段，不校验后续 detail|history|edit 等子路径，
     * 兼容后端 page 与 iframe 路由片段不一致（如 stddetail）的情况。
     * @param {string} backendPage 后端下发的 /console/pipeline/{projectId}/{pipelineId}/...
     * @returns {boolean}
     */
    _matchCurrentPipeline (backendPage) {
        const m = /^\/console\/pipeline\/([^/]+)\/([^/]+)/.exec(backendPage || '')
        if (!m) {
            // 无法解析时不过滤，交给业务侧自行判断
            return true
        }
        const projectId = m[1]
        const pipelineId = m[2]
        const expected = `/pipeline/tapd/${projectId}/${pipelineId}`
        // location.pathname 形如 /pipeline/tapd/{projectId}/{pipelineId}/...
        return location.pathname.indexOf(expected) === 0
    }

    /**
     * 通知服务端当前所在页面，便于服务端按订阅推送对应消息。
     * @param {string} path 当前路由 fullPath（不含 query 部分）
     * @param {string} projectId 项目 ID
     */
    sendMessage (path = '', projectId = '') {
        // 仅对 tapd iframe 路由生效：/pipeline/tapd/:projectId/...
        const reg = /^\/pipeline\/tapd\//

        if (!reg.test(path)) return

        // 1) 路由前缀替换：/pipeline/tapd/ → /console/pipeline/
        let page = path.replace(reg, '/console/pipeline/')

        // 2) tapd 专用子路径 → 后端可识别的标准 page
        //    后端只认 /console/pipeline/{projectId}/{pipelineId}/detail/{buildNo}/{type} 这种形式，
        //    对 tapd 自定义的 stddetail / 缺省 type 的 detail 都不会下发推送。
        //    这里统一规范化：
        //      a) /stddetail/{buildNo}              → /detail/{buildNo}/executeDetail
        //      b) /detail/{buildNo}                 → /detail/{buildNo}/executeDetail（补 type）
        //      c) /detail/{buildNo}/{type}          → 保持不变
        page = page.replace(
            /\/stddetail\/([^/?#]+)(?:\/([^/?#]+))?/,
            (_, buildNo, type) => `/detail/${buildNo}/${type || 'executeDetail'}`
        )
        page = page.replace(
            /\/detail\/([^/?#]+)(?:\/([^/?#]+))?/,
            (_, buildNo, type) => `/detail/${buildNo}/${type || 'executeDetail'}`
        )

        this.page = page
        this.projectId = projectId

        const data = JSON.stringify({
            sessionId: this.uuid,
            userId: this.userName,
            page,
            projectId,
            showProjectList: false
        })

        setTimeout(() => {
            this.ensureSendMessage(() => {
                this.stompClient.send('/app/changePage', {}, data)
                this.hasConnect = true
            })
        }, 5)
    }

    changeRoute () {
        const projectId = this.projectId
        const data = JSON.stringify({
            sessionId: this.uuid,
            userId: this.userName,
            page: this.page,
            showProjectList: false,
            projectId
        })
        if (this.hasConnect) {
            setTimeout(() => {
                this.ensureSendMessage(() => {
                    this.stompClient.send('/app/changePage', {}, data)
                    this.hasConnect = true
                })
            }, 5)
        }
    }

    loginOut () {
        const data = { sessionId: this.uuid, userId: this.userName, page: this.page }
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
        window.addEventListener('beforeunload', this._onBeforeUnload)
    }

    offlineDisconnect () {
        window.addEventListener('offline', this._onOffline)
    }

    onlineConnect () {
        window.addEventListener('online', this._onOnline)
    }

    /**
     * 销毁实例：清除事件监听、回调并断开连接。组件 beforeDestroy 时调用。
     */
    destroy () {
        try {
            window.removeEventListener('beforeunload', this._onBeforeUnload)
            window.removeEventListener('offline', this._onOffline)
            window.removeEventListener('online', this._onOnline)
        } catch (e) {
            // ignore
        }
        this.off('message')
        this.disconnect()
    }
}

export default BKSocket
