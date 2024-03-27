package com.tencent.devops.auth.provider.stream.config

import com.tencent.devops.auth.provider.stream.service.GitlabStreamPermissionServiceImpl
import com.tencent.devops.auth.provider.stream.service.StreamPermissionProjectServiceImpl
import com.tencent.devops.auth.provider.stream.service.StreamPermissionServiceImpl
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
@ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "gitlab")
class GitlabAuthConfiguration {

    @Bean
    @Primary
    fun gitlabStreamPermissionService() = GitlabStreamPermissionServiceImpl()

    @Bean
    @Primary
    fun gitlabStreamProjectPermissionService(
        streamPermissionService: StreamPermissionServiceImpl
    ) = StreamPermissionProjectServiceImpl(streamPermissionService)
}
