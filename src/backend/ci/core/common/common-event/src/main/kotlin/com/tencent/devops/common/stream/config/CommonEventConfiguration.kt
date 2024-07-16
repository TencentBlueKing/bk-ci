package com.tencent.devops.common.stream.config

import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CommonEventConfiguration {
    @Bean
    fun sampleEventDispatcher(streamBridge: StreamBridge) = SampleEventDispatcher(streamBridge)
}
