package com.tencent.devops.common.environment.agent.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.environment.agent.AgentGrayUtils
import com.tencent.devops.common.environment.agent.ThirdPartyAgentHeartbeatUtils
import com.tencent.devops.common.redis.RedisAutoConfiguration
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@AutoConfigureAfter(RedisAutoConfiguration::class)
class AgentConfiguration {

    @Bean
    fun agentGrayUtils(
        @Autowired redisOperation: RedisOperation,
        @Autowired gray: Gray
    ) = AgentGrayUtils(redisOperation, gray)

    @Bean
    fun thirdPartyAgentHeartbeatUtils(
        @Autowired redisOperation: RedisOperation,
        @Autowired objectMapper: ObjectMapper
    ) = ThirdPartyAgentHeartbeatUtils(redisOperation, objectMapper)
}