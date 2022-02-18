package com.tencent.devops.common.service

import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.consul.ConsulAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.Ordered

@Configuration
@PropertySource("classpath:/common-service.properties")
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@AutoConfigureBefore(ConsulAutoConfiguration::class)
@EnableDiscoveryClient
class ServiceAutoConfiguration {

    @Bean
    fun gray() = Gray()
}
