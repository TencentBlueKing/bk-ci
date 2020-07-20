package com.tencent.devops.auth

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.service.impl.DefaultHttpClientServiceImpl
import com.tencent.bk.sdk.iam.service.impl.TokenServiceImpl
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class AuthConfiguration {

    // TODO: 配置化
    val iamBaseUrl = "http://9.136.139.172:8080"

    //    @Value("\${auth.systemId:}")
    val systemId = "bk_ci"

    @Value("\${auth.appCode:}")
    val appCode = "bk_ci"

    @Value("\${auth.appSecret:}")
    val appSecret = "d043bde2-0b49-4024-b102-3bf0552193dd"

    @Bean
    fun iamConfiguration() = IamConfiguration(systemId, appCode, appSecret, iamBaseUrl)

    @Bean
    fun httpClient(iamConfiguration: IamConfiguration) = DefaultHttpClientServiceImpl(iamConfiguration)

    @Bean
    fun tokenService(iamConfiguration: IamConfiguration) = TokenServiceImpl(iamConfiguration, httpClient(iamConfiguration))
}