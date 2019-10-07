package com.tencent.devops.common.websocket.pojo

enum class WebSocketType {
    CODECC, // codecc错误推送
    WEBHOOK, // webhook错误推送
    STATUS, // 状态，首页
    HISTORY, // 历史页
    STORE, // 研发商店
    DETAIL; // 详情页

    companion object {
        fun changWebType(webSocketType: WebSocketType): String
        {
            if (webSocketType == CODECC || webSocketType == WEBHOOK) {
                return "NAV"
            }
            if (webSocketType == STATUS || webSocketType == HISTORY || webSocketType == DETAIL) {
                return "IFRAME"
            }

            if (webSocketType == STORE) {
                return "AMD"
            }

            return "IFRAME"
        }
    }
}