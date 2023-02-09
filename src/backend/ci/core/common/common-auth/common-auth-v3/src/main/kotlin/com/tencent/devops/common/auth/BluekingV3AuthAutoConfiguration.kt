/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.auth

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.impl.DefaultHttpClientServiceImpl
import com.tencent.bk.sdk.iam.service.impl.GrantServiceImpl
import com.tencent.bk.sdk.iam.service.impl.PolicyServiceImpl
import com.tencent.bk.sdk.iam.service.impl.TokenServiceImpl
import com.tencent.devops.common.auth.api.BluekingV3AuthPermissionApi
import com.tencent.devops.common.auth.api.BluekingV3AuthProjectApi
import com.tencent.devops.common.auth.api.BluekingV3AuthTokenApi
import com.tencent.devops.common.auth.api.BluekingV3ResourceApi
import com.tencent.devops.common.auth.code.BluekingV3ArtifactoryAuthServiceCode
import com.tencent.devops.common.auth.code.BluekingV3BcsAuthServiceCode
import com.tencent.devops.common.auth.code.BluekingV3CodeAuthServiceCode
import com.tencent.devops.common.auth.code.BluekingV3EnvironmentAuthServiceCode
import com.tencent.devops.common.auth.code.BluekingV3PipelineAuthServiceCode
import com.tencent.devops.common.auth.code.BluekingV3ProjectAuthServiceCode
import com.tencent.devops.common.auth.code.BluekingV3QualityAuthServiceCode
import com.tencent.devops.common.auth.code.BluekingV3RepoAuthServiceCode
import com.tencent.devops.common.auth.code.BluekingV3TicketAuthServiceCode
import com.tencent.devops.common.auth.service.IamEsbService
import com.tencent.devops.common.redis.RedisOperation
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered

@Suppress("ALL")
@Configuration
@ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "bk_login_v3")
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@AutoConfigureBefore(name = ["com.tencent.devops.common.auth.MockAuthAutoConfiguration"])
class BluekingV3AuthAutoConfiguration {

    @Value("\${auth.url:}")
    val iamBaseUrl = ""

    @Value("\${auth.iamSystem:}")
    val systemId = ""

    @Value("\${auth.appCode:}")
    val appCode = ""

    @Value("\${auth.appSecret:}")
    val appSecret = ""

    @Value("\${auth.apigwUrl:#{null}}")
    val iamApigw = ""

    @Bean
    @Primary
    fun authTokenApi(
        redisOperation: RedisOperation,
        tokenServiceImpl: TokenServiceImpl
    ) = BluekingV3AuthTokenApi(redisOperation, tokenServiceImpl)

    @Bean
    @ConditionalOnMissingBean
    fun iamEsbService() = IamEsbService()

    @Bean
    @Primary
    fun authResourceApi(authTokenApi: BluekingV3AuthTokenApi) =
        BluekingV3ResourceApi(
            iamConfiguration = iamConfiguration(),
            iamEsbService = iamEsbService()
        )

    @Bean
    @Primary
    fun authProjectApi() =
        BluekingV3AuthProjectApi(
            policyService = policyService(),
            authHelper = authHelper(),
            iamConfiguration = iamConfiguration(),
            iamEsbService = iamEsbService()
        )

    @Bean
    fun bcsAuthServiceCode() = BluekingV3BcsAuthServiceCode()

    @Bean
    fun pipelineAuthServiceCode() = BluekingV3PipelineAuthServiceCode()

    @Bean
    fun codeAuthServiceCode() = BluekingV3CodeAuthServiceCode()

    @Bean
    fun projectAuthServiceCode() = BluekingV3ProjectAuthServiceCode()

    @Bean
    fun environmentAuthServiceCode() = BluekingV3EnvironmentAuthServiceCode()

    @Bean
    fun repoAuthServiceCode() = BluekingV3RepoAuthServiceCode()

    @Bean
    fun ticketAuthServiceCode() = BluekingV3TicketAuthServiceCode()

    @Bean
    fun qualityAuthServiceCode() = BluekingV3QualityAuthServiceCode()

    @Bean
    fun artifactoryAuthServiceCode() = BluekingV3ArtifactoryAuthServiceCode()

    @Bean
    fun iamConfiguration() = IamConfiguration(systemId, appCode, appSecret, iamBaseUrl, iamApigw)

    // 鉴权类
    @Bean
    fun policyHttpService() = DefaultHttpClientServiceImpl(iamConfiguration())

    // 非鉴权类(与鉴权类请求分开,方式授权类请求响应慢,影响鉴权类接口的响应)
    @Bean
    fun httpService() = DefaultHttpClientServiceImpl(iamConfiguration())

    @Bean
    fun tokenService() = TokenServiceImpl(iamConfiguration(), httpService())

    @Bean
    fun policyService() = PolicyServiceImpl(iamConfiguration(), policyHttpService())

    @Bean
    fun authHelper() = AuthHelper(tokenService(), policyService(), iamConfiguration())

    @Bean
    fun grantService() = GrantServiceImpl(httpService(), iamConfiguration())

    @Bean
    @Primary
    fun authPermissionApi(redisOperation: RedisOperation) =
        BluekingV3AuthPermissionApi(
            authHelper = authHelper(),
            policyService = policyService(),
            redisOperation = redisOperation,
            iamConfiguration = iamConfiguration()
        )
}
