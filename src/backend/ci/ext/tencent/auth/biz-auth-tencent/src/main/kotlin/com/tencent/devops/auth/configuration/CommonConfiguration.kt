package com.tencent.devops.auth.configuration

import com.tencent.devops.auth.service.ManagerService
import com.tencent.devops.common.client.Client
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CommonConfiguration {
    @Bean
    fun managerService(
        client: Client
    ) = ManagerService(client)
}
