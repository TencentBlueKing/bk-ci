package com.tencent.devops.common.client

import feign.Request
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
class DevopsFeignClientAutoConfiguration {

    @Bean
    fun options(): Request.Options {
        return Request.Options(60, TimeUnit.SECONDS, 60, TimeUnit.SECONDS, true)
    }
}
