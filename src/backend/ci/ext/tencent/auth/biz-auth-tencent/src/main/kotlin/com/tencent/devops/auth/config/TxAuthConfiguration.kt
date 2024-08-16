package com.tencent.devops.auth.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.auth.service.AuthDeptServiceImpl
import com.tencent.devops.common.redis.RedisOperation
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
@ConditionalOnWebApplication
class TxAuthConfiguration {

    @Bean
    @Primary
    fun deptService(
        redisOperation: RedisOperation,
        objectMapper: ObjectMapper
    ) = AuthDeptServiceImpl(redisOperation, objectMapper)
}
