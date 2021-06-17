package com.tencent.devops.common.auth

import com.tencent.devops.auth.service.ManagerService
import com.tencent.devops.common.client.Client
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CommonConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun managerService(client: Client) = ManagerService(client)
}
