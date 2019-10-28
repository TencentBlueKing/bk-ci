package com.tencent.devops.common.environment.agent.config

import com.tencent.devops.common.auth.api.AuthTokenApi
import com.tencent.devops.common.auth.code.EnvironmentAuthServiceCode
import com.tencent.devops.common.environment.agent.client.BcsClient
import com.tencent.devops.common.redis.RedisAutoConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@AutoConfigureAfter(RedisAutoConfiguration::class)
class BcsConfiguration {

    @Bean
    fun bcsClient(
        @Autowired bkAuthTokenApi: AuthTokenApi,
        @Autowired environmentAuthServiceCode: EnvironmentAuthServiceCode
    ) =
        BcsClient(bkAuthTokenApi, environmentAuthServiceCode)
}