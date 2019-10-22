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