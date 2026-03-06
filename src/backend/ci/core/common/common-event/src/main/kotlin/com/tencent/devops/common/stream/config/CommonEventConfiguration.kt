package com.tencent.devops.common.stream.config

import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.stream.config.interceptor.BkChannelInterceptor
import com.tencent.devops.common.stream.config.listener.AnonymousQueueCleanupListener
import com.tencent.devops.common.stream.customizer.BkProducerMessageHandlerCustomizer
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.config.GlobalChannelInterceptor

@Configuration
class CommonEventConfiguration {
    @Bean
    fun sampleEventDispatcher(streamBridge: StreamBridge) = SampleEventDispatcher(streamBridge)

    @Bean
    @GlobalChannelInterceptor
    fun bkChannelInterceptor() = BkChannelInterceptor()

    @Bean
    fun bkProducerMessageHandlerCustomizer() = BkProducerMessageHandlerCustomizer()

    /**
     * 匿名队列清理监听器
     * 仅在 RabbitAdmin Bean 存在时才注册
     */
    @Bean
    @ConditionalOnBean(RabbitAdmin::class)
    fun anonymousQueueCleanupListener(rabbitAdmin: RabbitAdmin) = AnonymousQueueCleanupListener(rabbitAdmin)
}
