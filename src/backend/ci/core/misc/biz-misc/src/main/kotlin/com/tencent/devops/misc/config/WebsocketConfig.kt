package com.tencent.devops.misc.config

import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WebsocketConfig {

    @Bean
    fun webSocketDispatcher(rabbitTemplate: RabbitTemplate) = WebSocketDispatcher(rabbitTemplate)
}