package com.tencent.devops.environment

import com.tencent.devops.environment.service.BluekingAgentUrlServiceImpl
import com.tencent.devops.environment.service.slave.SlaveGatewayService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SampleServiceConfig {

    @Bean
    fun agentUrlService(slaveGatewayService: SlaveGatewayService) = BluekingAgentUrlServiceImpl(slaveGatewayService)
}