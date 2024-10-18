package com.tencent.devops.common.stream.config

import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.stream.config.interceptor.BkChannelInterceptor
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
}
