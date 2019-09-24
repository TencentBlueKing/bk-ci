package com.tencent.devops.common.archive

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.archive.shorturl.ShortUrlApi
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered

@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class ShortUrlAutoConfiguration {

    @Bean
    @Primary
    fun shortUrlApi(objectMapper: ObjectMapper) = ShortUrlApi(objectMapper)
}