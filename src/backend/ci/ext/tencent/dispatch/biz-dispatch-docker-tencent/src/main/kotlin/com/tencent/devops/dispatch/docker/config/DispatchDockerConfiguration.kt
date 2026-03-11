package com.tencent.devops.dispatch.docker.config

import com.tencent.devops.common.event.dispatcher.mq.MQEventDispatcher
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DispatchDockerConfiguration {
    @Bean
    fun pipelineEventDispatcher(streamBridge: StreamBridge) = MQEventDispatcher(streamBridge)
}
