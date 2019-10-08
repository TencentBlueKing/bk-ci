package com.tencent.devops.store.configuration

import com.tencent.devops.common.websocket.dispatch.WebsocketPushDispatcher
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WebsocketConfiguration {

    @Bean
    fun getWebsocketPushDispatcher(
        rabbitTemplate: RabbitTemplate
    ): WebsocketPushDispatcher {
        return WebsocketPushDispatcher(rabbitTemplate)
    }
}