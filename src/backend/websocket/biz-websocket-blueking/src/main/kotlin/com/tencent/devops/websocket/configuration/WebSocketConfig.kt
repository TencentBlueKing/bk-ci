package com.tencent.devops.websocket.configuration

import com.tencent.devops.common.client.Client
import com.tencent.devops.websocket.handler.BKHandshakeInterceptor
import com.tencent.devops.websocket.handler.ConnectChannelInterceptor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig @Autowired constructor(
    private val client: Client,
    private val bkHandshake: BKHandshakeInterceptor,
    private val connectChannelInterceptor: ConnectChannelInterceptor
) : AbstractWebSocketMessageBrokerConfigurer() {

    @Value("\${thread.min}")
    private val min: Int = 8

    companion object {
        private val logger = LoggerFactory.getLogger(WebSocketConfig::class.java)
    }

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker("/topic")
        config.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws/user/pipelines/pipelineStatus").addInterceptors(
        ).addInterceptors(bkHandshake).setAllowedOrigins("*").withSockJS()
        registry.addEndpoint("/ws/user").addInterceptors(bkHandshake).setAllowedOrigins("*").withSockJS()
    }

    @Override
    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        var defaultCorePoolSize = min
        if (defaultCorePoolSize < Runtime.getRuntime().availableProcessors() * 2) {
            defaultCorePoolSize = Runtime.getRuntime().availableProcessors() * 2
        }
        registration.taskExecutor().corePoolSize(defaultCorePoolSize)
            .maxPoolSize(defaultCorePoolSize * 2)
            .keepAliveSeconds(60)
    }

    @Override
    override fun configureClientOutboundChannel(registration: ChannelRegistration) {
        var defaultCorePoolSize = min
        if (defaultCorePoolSize < Runtime.getRuntime().availableProcessors() * 2) {
            defaultCorePoolSize = Runtime.getRuntime().availableProcessors() * 2
        }
        registration.taskExecutor().corePoolSize(defaultCorePoolSize).maxPoolSize(defaultCorePoolSize * 2)
    }
}