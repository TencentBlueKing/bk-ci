package com.tencent.devops.auth.provider.stream.config

import com.tencent.devops.auth.provider.stream.service.GithubStreamPermissionServiceImpl
import com.tencent.devops.auth.provider.stream.service.StreamPermissionProjectServiceImpl
import com.tencent.devops.auth.provider.stream.service.StreamPermissionServiceImpl
import com.tencent.devops.common.client.Client
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered

@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "github")
class GithubAuthConfiguration {
    @Bean
    @Primary
    fun githubStreamPermissionService(client: Client) = GithubStreamPermissionServiceImpl(client)

    @Bean
    @Primary
    fun githubStreamProjectPermissionService(
        streamPermissionService: StreamPermissionServiceImpl
    ) = StreamPermissionProjectServiceImpl(streamPermissionService)
}
