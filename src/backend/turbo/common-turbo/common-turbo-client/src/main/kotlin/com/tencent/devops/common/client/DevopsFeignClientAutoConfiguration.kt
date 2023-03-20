package com.tencent.devops.common.client

import feign.Request
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.util.concurrent.TimeUnit

@Configuration
@Import(JerseyFeignRequestRegistrar::class)
class DevopsFeignClientAutoConfiguration {

    @Bean
    fun clientErrorDecoder() = ClientErrorDecoder()

    @Bean
    fun options(): Request.Options {
        return Request.Options(60, TimeUnit.SECONDS, 60, TimeUnit.SECONDS, true)
    }
}
