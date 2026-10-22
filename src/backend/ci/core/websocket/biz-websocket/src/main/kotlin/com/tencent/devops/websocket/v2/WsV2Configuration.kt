package com.tencent.devops.websocket.v2

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORIGINAL_HOST
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.websocket.v2.WsV2Properties
import com.tencent.devops.common.websocket.v2.WsV2RabbitConfiguration
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.beans.factory.annotation.Qualifier
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
    class Endpoint(
        private val handler: WsV2Handler,
        private val properties: WsV2Properties
    ) : WebSocketConfigurer {
        override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
            registry.addHandler(handler, "/ws/v2")
                .addInterceptors(WsV2HandshakeInterceptor(properties))
                // 容器层不按固定域名拦截。经过网关后请求主机是服务自身地址，固定名单无法覆盖多套域名。
                .setAllowedOriginPatterns("*")
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
        val configured = properties.allowedOrigins.map { it.trim() }.filter { it.isNotEmpty() }
        val allowed = try {
            if (origin == null || origin == "null") false
            else if (configured.isNotEmpty()) origin in configured
            else samePublicOrigin(request, origin)
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

    private fun samePublicOrigin(request: ServerHttpRequest, origin: String): Boolean {
        val source = URI(origin)
        if (source.scheme !in setOf("http", "https") || source.host.isNullOrBlank()) return false
        // 网关只监听 HTTP，浏览器页面是 HTTPS。沿用网关已经写入的原始主机，只核对主机。
        val rawHost = headerValue(request, AUTH_HEADER_DEVOPS_ORIGINAL_HOST)
            ?: headerValue(request, "X-Forwarded-Host")
            ?: headerValue(request, "Host")
            ?: return false
        val (host, explicitPort) = splitHost(rawHost) ?: return false
        if (!source.host.equals(host, ignoreCase = true)) return false
        val originPort = if (source.port >= 0) source.port else if (source.scheme == "https") 443 else 80
        if (explicitPort == null) return originPort == 80 || originPort == 443
        return explicitPort == originPort ||
            (explicitPort == 80 && originPort == 443) ||
            (explicitPort == 443 && originPort == 80)
    }

    private fun headerValue(request: ServerHttpRequest, name: String): String? =
        request.headers.getFirst(name)?.substringBefore(",")?.trim()?.takeIf { it.isNotEmpty() }

    private fun splitHost(value: String): Pair<String, Int?>? {
        if (value.startsWith("[")) {
            val end = value.indexOf(']')
            if (end <= 1) return null
            val host = value.substring(1, end)
            if (value.length == end + 1) return host to null
            if (!value.startsWith("]:", end)) return null
            return host to (value.substring(end + 2).toIntOrNull() ?: return null)
        }
        val colon = value.lastIndexOf(':')
        if (colon <= 0 || value.indexOf(':') != colon) return value to null
        val port = value.substring(colon + 1).toIntOrNull() ?: return value to null
        return value.substring(0, colon) to port
    }
}
