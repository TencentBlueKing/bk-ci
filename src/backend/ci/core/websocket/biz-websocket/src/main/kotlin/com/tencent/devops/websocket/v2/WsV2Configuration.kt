package com.tencent.devops.websocket.v2

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.websocket.v2.WsV2Properties
import com.tencent.devops.common.websocket.v2.WsV2RabbitConfiguration
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.server.HandshakeInterceptor
import java.net.URI

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "websocket.v2", name = ["enabled"], havingValue = "true")
@Import(WsV2RabbitConfiguration::class)
class WsV2Configuration {
    @Bean
    fun websocketV2Subscriber(
        @Qualifier("websocketV2ConnectionFactory") factory: ConnectionFactory,
        properties: WsV2Properties
    ): WsSubscriptionBroker = WsV2RabbitSubscriber(factory, properties)

    @Bean
    fun websocketV2Handler(
        properties: WsV2Properties,
        broker: WsSubscriptionBroker,
        authorizer: WsTopicAuthorizer
    ) = WsV2Handler(properties, broker, authorizer)

    @Configuration(proxyBeanMethods = false)
    @EnableWebSocket
    @ConditionalOnProperty(prefix = "websocket.v2", name = ["enabled"], havingValue = "true")
    class Endpoint(
        private val handler: WsV2Handler,
        private val properties: WsV2Properties
    ) : WebSocketConfigurer {
        override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
            registry.addHandler(handler, "/ws/v2")
                .addInterceptors(WsV2HandshakeInterceptor(properties))
                .setAllowedOrigins(*properties.allowedOrigins.toTypedArray())
        }
    }
}

class WsV2HandshakeInterceptor(private val properties: WsV2Properties) : HandshakeInterceptor {
    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        val userId = request.headers.getFirst(AUTH_HEADER_DEVOPS_USER_ID)
        if (userId.isNullOrBlank() || userId.length > 256) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED)
            return false
        }
        val origin = request.headers.origin
        val allowed = try {
            if (origin == null || origin == "null") false
            else if (properties.allowedOrigins.isNotEmpty()) origin in properties.allowedOrigins
            else {
                val source = URI(origin)
                val scheme = request.headers.getFirst("X-Forwarded-Proto") ?: request.uri.scheme
                val host = request.headers.getFirst("X-Forwarded-Host") ?: request.headers.getFirst("Host")
                val target = URI("$scheme://$host")
                source.scheme in setOf("http", "https") && source.scheme == target.scheme &&
                    source.host.equals(target.host, true) && port(source) == port(target)
            }
        } catch (ignored: Exception) {
            false
        }
        if (!allowed) {
            response.setStatusCode(HttpStatus.FORBIDDEN)
            return false
        }
        attributes[WsV2Handler.USER_ATTRIBUTE] = userId
        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) = Unit

    private fun port(uri: URI) = if (uri.port >= 0) uri.port else if (uri.scheme == "https") 443 else 80
}
