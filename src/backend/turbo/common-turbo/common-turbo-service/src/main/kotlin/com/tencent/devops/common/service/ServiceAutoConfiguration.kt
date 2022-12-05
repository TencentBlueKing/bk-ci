package com.tencent.devops.common.service

import com.tencent.devops.common.service.prometheus.BkTimedAspect
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
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

    @Bean
    fun meterRegistry() = SimpleMeterRegistry()

    @Bean
    fun bkTimedAspect(meterRegistry: MeterRegistry) = BkTimedAspect(meterRegistry)
}
