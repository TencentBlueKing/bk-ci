/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
export default {
    callBack: () => {},
    onReconnect: () => {},

    installWsMessage (callBack) {
        this.callBack = (res) => {
            const type = res?.data?.webSocketType
            console.log('webSocket Receive data', res?.data)
            if (type === 'IFRAME' && res.data?.message) {
                const message = JSON.parse(res.data.message)
                if (message === 'WEBSOCKET_RECONNECT') {
                    console.log('webSocket reconnect', this.onReconnect)
                    this.onReconnect?.()
                } else {
                    callBack(message)
                }
            }
        }
        window.addEventListener('message', this.callBack)
    },

    registeOnReconnect (callBack) {
        this.onReconnect = callBack
    },

    unInstallWsMessage () {
        window.removeEventListener('message', this.callBack)
        this.callBack = () => {}
        this.onReconnect = () => {}
    },

    openDialogWebSocket (callBack, payLoad) {
        const postData = Object.assign({ type: 'openLogWs' }, payLoad)
        window.parent.postMessage(postData, '*')
        this.openDialogWebSocket.dialogCallBack = (res) => {
            const data = res.data
            const type = data.webSocketType
            if (type === 'IFRAMEDIALOG') {
                const message = JSON.parse(data.message)
                callBack(message)
            }
        }
        window.addEventListener('message', this.openDialogWebSocket.dialogCallBack)
    },

    closeDialogWebSocket (payLoad) {
        const postData = Object.assign({ type: 'closeLogWs' }, payLoad)
        window.parent.postMessage(postData, '*')
        window.removeEventListener('message', this.openDialogWebSocket.dialogCallBack)
    }
}
