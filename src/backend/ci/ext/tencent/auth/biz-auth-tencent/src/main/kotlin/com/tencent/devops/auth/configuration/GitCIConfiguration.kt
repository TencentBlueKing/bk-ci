package com.tencent.devops.auth.configuration

import com.tencent.devops.auth.service.gitci.GitCIPermissionProjectServiceImpl
import com.tencent.devops.auth.service.gitci.GitCIPermissionServiceImpl
import com.tencent.devops.common.client.Client
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GitCIConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "gitCI")
    fun gitCIPermissionServiceImpl(
        client: Client
    ) = GitCIPermissionServiceImpl(client)

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "gitCI")
    fun gitCIPermissionProjectServiceImpl() = GitCIPermissionProjectServiceImpl()
}
