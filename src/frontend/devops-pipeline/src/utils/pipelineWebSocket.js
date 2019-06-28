/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import SockJS from 'sockjs-client'
const Stomp = require('stompjs/lib/stomp.js').Stomp

class PipelineWebSocket {
    constructor () {
        this.errTime = 1
        this.stompClient = {}
    }

    connect (...args) {
        this.disconnect().then(this.build(...args))
    }

    disconnect () {
        return new Promise((resolve) => {
            if (this.stompClient.connected) this.stompClient.disconnect(resolve)
            else resolve()
        })
    }

    build (projectId, subscribeUrl, { success, error }) {
        const socket = new SockJS(`${WS_URL_PREFIX}/process/websocket/user/pipelines/pipelineStatus?projectId=${projectId}`)
        this.stompClient = Stomp.over(socket)
        this.stompClient.debug = null
        this.stompClient.connect({}, () => {
            this.stompClient.subscribe(subscribeUrl, (res) => {
                this.errTime = 1
                success(res)
            })
        }, (err) => {
            if (this.errTime <= 8) {
                // 由于部署原因，可能会出现需要重连的情况
                this.errTime++
                setTimeout(() => this.connect(projectId, subscribeUrl, { success, error }), 10000)
            } else {
                error(err.message || 'websocket异常')
            }
        })
    }
}

export default new PipelineWebSocket()
