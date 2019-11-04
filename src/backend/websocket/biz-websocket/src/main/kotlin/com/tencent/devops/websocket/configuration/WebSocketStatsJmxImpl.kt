/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.websocket.configuration

import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.web.socket.config.WebSocketMessageBrokerStats
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jmx.export.annotation.ManagedAttribute
import org.springframework.stereotype.Component

@Component
@ManagedResource(objectName = "com.tencent.devops.webSocket:type=index,operation=create", description = "webSocket thread pool")
class WebSocketStatsJmxImpl() {

    fun WebSocketStatsJmxImpl() {
        println("WebSocketStatsJmxImpl::Constructor")
    }

    lateinit var websocketMessageBrokerStats: WebSocketMessageBrokerStats

    @Autowired
    fun setWebSocketMessageBrokerStats(webSocketMessageBrokerStats: WebSocketMessageBrokerStats) {
        this.websocketMessageBrokerStats = webSocketMessageBrokerStats
    }

    // defines an attribute of an MBean
    @ManagedAttribute(description = "Get stats about WebSocket sessions.")
    fun getWebSocketSessionStatsInfo(): String {
        return websocketMessageBrokerStats.getWebSocketSessionStatsInfo()
    }

    @ManagedAttribute(description = "Get stats about STOMP-related WebSocket message processing.")
    fun getStompSubProtocolStatsInfo(): String {
        return websocketMessageBrokerStats.getStompSubProtocolStatsInfo()
    }

    @ManagedAttribute(description = "Get stats about STOMP broker relay (when using a full-featured STOMP broker).")
    fun getStompBrokerRelayStatsInfo(): String {
        return websocketMessageBrokerStats.getStompBrokerRelayStatsInfo()
    }

    @ManagedAttribute(description = "Get stats about the executor processing incoming messages from WebSocket clients.")
    fun getClientInboundExecutorStatsInfo(): String {
        return websocketMessageBrokerStats.getClientInboundExecutorStatsInfo()
    }

    @ManagedAttribute(description = "Get stats about the executor processing outgoing messages to WebSocket clients.")
    fun getClientOutboundExecutorStatsInfo(): String {
        return websocketMessageBrokerStats.getClientOutboundExecutorStatsInfo()
    }

    @ManagedAttribute(description = "Get stats about the SockJS task scheduler.")
    fun getSockJsTaskSchedulerStatsInfo(): String {
        return websocketMessageBrokerStats.getSockJsTaskSchedulerStatsInfo()
    }
}