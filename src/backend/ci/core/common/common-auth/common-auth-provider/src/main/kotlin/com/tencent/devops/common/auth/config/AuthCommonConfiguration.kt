package com.tencent.devops.common.auth.config

import com.tencent.devops.common.auth.authorization.AuthAuthorizationService
import com.tencent.devops.common.auth.dept.AuthUserAndDeptService
import com.tencent.devops.common.client.Client
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class AuthCommonConfiguration {
    @Bean
    fun authAuthorizationApi(
        client: Client
    ) = AuthAuthorizationService(
        client = client
    )

    @Bean
    fun authUserAndDeptApi(
        client: Client
    ) = AuthUserAndDeptService(
        client = client
    )
}
