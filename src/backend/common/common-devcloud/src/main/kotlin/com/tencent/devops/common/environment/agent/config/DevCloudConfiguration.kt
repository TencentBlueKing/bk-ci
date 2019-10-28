package com.tencent.devops.common.environment.agent.config

import com.tencent.devops.common.environment.agent.client.DevCloudClient
import com.tencent.devops.common.environment.agent.client.EsbAgentClient
import com.tencent.devops.common.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@AutoConfigureAfter(RedisAutoConfiguration::class)
class DevCloudConfiguration {

    @Bean
    fun devCloudClient() = DevCloudClient()

    @Bean
    fun esbAgentClient() = EsbAgentClient()
}