/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.common.auth.rbac

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.service.impl.ApigwHttpClientServiceImpl
import com.tencent.bk.sdk.iam.service.impl.TokenServiceImpl
import com.tencent.devops.common.auth.api.AuthTokenApi
import com.tencent.devops.common.auth.rbac.api.RbacAuthPermissionApi
import com.tencent.devops.common.auth.rbac.api.RbacAuthProjectApi
import com.tencent.devops.common.auth.rbac.api.RbacAuthTokenApi
import com.tencent.devops.common.auth.rbac.api.RbacResourceApi
import com.tencent.devops.common.auth.rbac.code.RbacArtifactoryAuthServiceCode
import com.tencent.devops.common.auth.rbac.code.RbacBcsAuthServiceCode
import com.tencent.devops.common.auth.rbac.code.RbacCodeAuthServiceCode
import com.tencent.devops.common.auth.rbac.code.RbacEnvironmentAuthServiceCode
import com.tencent.devops.common.auth.rbac.code.RbacExperienceAuthServiceCode
import com.tencent.devops.common.auth.rbac.code.RbacPipelineAuthServiceCode
import com.tencent.devops.common.auth.rbac.code.RbacPipelineGroupAuthServiceCode
import com.tencent.devops.common.auth.rbac.code.RbacProjectAuthServiceCode
import com.tencent.devops.common.auth.rbac.code.RbacQualityAuthServiceCode
import com.tencent.devops.common.auth.rbac.code.RbacRepoAuthServiceCode
import com.tencent.devops.common.auth.rbac.code.RbacTicketAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.redis.RedisOperation
import org.apache.http.impl.client.CloseableHttpClient
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered

@Suppress("ALL")
@Configuration
@ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "rbac")
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@EnableConfigurationProperties(RbacAuthProperties::class)
class RbacAuthAutoConfiguration {

    @Bean
    @Primary
    fun iamConfiguration(properties: RbacAuthProperties) = with(properties) {
        IamConfiguration(iamSystem, appCode, appSecret, url, apigwUrl)
    }

    @Bean
    @Primary
    fun apigwHttpClientServiceImpl(httpClient: CloseableHttpClient, iamConfiguration: IamConfiguration) =
        ApigwHttpClientServiceImpl(httpClient, iamConfiguration)

    @Bean
    fun iamTokenService(
        iamConfiguration: IamConfiguration,
        apigwHttpClientServiceImpl: ApigwHttpClientServiceImpl
    ) = TokenServiceImpl(iamConfiguration, apigwHttpClientServiceImpl)

    @Bean
    @ConditionalOnMissingBean(AuthTokenApi::class)
    fun authTokenApi(
        redisOperation: RedisOperation,
        iamTokenService: TokenServiceImpl
    ) = RbacAuthTokenApi(redisOperation, iamTokenService)

    @Bean
    fun authResourceApi(
        client: Client,
        tokenService: ClientTokenService
    ) = RbacResourceApi(client = client, tokenService = tokenService)

    @Bean
    fun authProjectApi(
        client: Client,
        tokenService: ClientTokenService
    ) = RbacAuthProjectApi(client = client, tokenService = tokenService)

    @Bean
    fun authPermissionApi(
        client: Client,
        tokenService: ClientTokenService
    ) = RbacAuthPermissionApi(client = client, tokenService = tokenService)

    @Bean
    @Primary
    fun bcsAuthServiceCode() = RbacBcsAuthServiceCode()

    @Bean
    @Primary
    fun pipelineAuthServiceCode() = RbacPipelineAuthServiceCode()

    @Bean
    @Primary
    fun pipelineGroupAuthServiceCode() = RbacPipelineGroupAuthServiceCode()

    @Bean
    @Primary
    fun codeAuthServiceCode() = RbacCodeAuthServiceCode()

    @Bean
    @Primary
    fun projectAuthServiceCode() = RbacProjectAuthServiceCode()

    @Bean
    @Primary
    fun environmentAuthServiceCode() = RbacEnvironmentAuthServiceCode()

    @Bean
    @Primary
    fun repoAuthServiceCode() = RbacRepoAuthServiceCode()

    @Bean
    @Primary
    fun ticketAuthServiceCode() = RbacTicketAuthServiceCode()

    @Bean
    @Primary
    fun qualityAuthServiceCode() = RbacQualityAuthServiceCode()

    @Bean
    @Primary
    fun artifactoryAuthServiceCode() = RbacArtifactoryAuthServiceCode()

    @Bean
    @Primary
    fun experienceAuthServiceCode() = RbacExperienceAuthServiceCode()
}
