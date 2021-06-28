package com.tencent.devops.auth.configuration

import com.tencent.devops.auth.service.v0.V0AuthPermissionProjectServiceImpl
import com.tencent.devops.auth.service.v0.V0AuthPermissionServiceImpl
import com.tencent.devops.auth.service.v0.ServiceCodeService
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.code.BSCommonAuthServiceCode
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DefaultConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "client")
    fun defaultPermissionServiceImpl(
        authPermissionApi: AuthPermissionApi,
        serviceCodeService: ServiceCodeService
    ) = V0AuthPermissionServiceImpl(authPermissionApi, serviceCodeService)

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "client")
    fun defaultPermissionProjectServiceImpl(
        authProjectApi: AuthProjectApi,
        commonServiceCode: BSCommonAuthServiceCode
    ) = V0AuthPermissionProjectServiceImpl(authProjectApi, commonServiceCode)
}
