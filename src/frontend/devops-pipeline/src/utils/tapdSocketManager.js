/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 * tapd iframe 全局 WebSocket 管理器（单例）
 *
 * 设计目标：
 * 1) 整个 tapd 路由族共用一份 BKSocket 连接，避免每个组件各建一份；
 * 2) 与 Vue 组件解耦：通过 router 全局守卫按需启停，所有 tapd 子路由
 *    （含同级的 tapdPipelinesDetail、tapdCreatePipeline 等）都能直接接收消息；
 * 3) 收到 IFRAME 推送后，转换为 window message 事件，复用
 *    utils/webSocketMessage.js 既有的订阅分发逻辑，业务页面零侵入。
 */

import BKSocket from '@/utils/bkSocket'

const TAPD_PATH_PREFIX = '/pipeline/tapd/'

class TapdSocketManager {
    constructor () {
        this.socket = null
        this._onMessage = this._onMessage.bind(this)
    }

    /**
     * 是否处于 tapd 路由下
     * @param {string} path 形如 /pipeline/tapd/:projectId/...
     */
    isTapdPath (path) {
        return typeof path === 'string' && path.indexOf(TAPD_PATH_PREFIX) === 0
    }

    /**
     * 确保 socket 已建立。幂等。
     */
    ensureStarted () {
        if (this.socket) return
        this.socket = new BKSocket()
        this.socket.on('message', this._onMessage)
    }

    /**
     * 停止 socket：断开连接、清理监听。幂等。
     */
    stop () {
        if (!this.socket) return
        try {
            this.socket.off('message', this._onMessage)
            this.socket.destroy && this.socket.destroy()
        } catch (e) {
            // ignore
        }
        this.socket = null
    }

    /**
     * 通知服务端当前所在的 tapd 页面，便于按页面订阅推送。
     * 路由变化时调用即可。
     * @param {import('vue-router').Route} route
     */
    notifyRoute (route) {
        if (!this.socket || !route) return
        const fullPath = route.fullPath || ''
        const [path] = fullPath.split('?')
        if (!this.isTapdPath(path)) return
        const projectId = (route.params && route.params.projectId) || ''
        this.socket.sendMessage(path, projectId)
    }

    /**
     * BKSocket 收到 IFRAME 类型推送的处理：
     * 直接把数据派发到当前 window，复用 utils/webSocketMessage.js 的既有监听。
     *
     * 注意：self-postMessage 的接收方就是当前页面自身，使用 '*' 不会扩散到外部窗口；
     * 而显式传 location.origin 在 sandbox iframe 下（origin === 'null'）会导致
     * 消息被静默丢弃，因此固定使用 '*' 更稳妥。
     */
    _onMessage (data) {
        try {
            window.postMessage(data, '*')
        } catch (e) {
            // ignore
        }
    }
}

export default new TapdSocketManager()
