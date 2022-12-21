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

import com.tencent.bk.sdk.iam.service.impl.TokenServiceImpl
import com.tencent.devops.common.auth.api.RbacAuthPermissionApi
import com.tencent.devops.common.auth.api.RbacAuthProjectApi
import com.tencent.devops.common.auth.api.RbacAuthTokenApi
import com.tencent.devops.common.auth.api.RbacResourceApi
import com.tencent.devops.common.auth.code.RbacArtifactoryAuthServiceCode
import com.tencent.devops.common.auth.code.RbacBcsAuthServiceCode
import com.tencent.devops.common.auth.code.RbacCodeAuthServiceCode
import com.tencent.devops.common.auth.code.RbacEnvironmentAuthServiceCode
import com.tencent.devops.common.auth.code.RbacPipelineAuthServiceCode
import com.tencent.devops.common.auth.code.RbacProjectAuthServiceCode
import com.tencent.devops.common.auth.code.RbacQualityAuthServiceCode
import com.tencent.devops.common.auth.code.RbacRepoAuthServiceCode
import com.tencent.devops.common.auth.code.RbacTicketAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.redis.RedisOperation
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Suppress("ALL")
@Configuration
@ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "rbac")
@AutoConfigureBefore(name = ["com.tencent.devops.common.auth.MockAuthAutoConfiguration"])
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class RbacAuthAutoConfiguration {

    @Bean
    fun authTokenApi() = RbacAuthTokenApi()

    @Bean
    fun authResourceApi(
        client: Client,
        tokenService: ClientTokenService
    ) = RbacResourceApi(client = client, tokenService = tokenService)

    @Bean
    fun authProjectApi() = RbacAuthProjectApi()

    @Bean
    fun authPermissionApi() = RbacAuthPermissionApi()

    @Bean
    fun bcsAuthServiceCode() = RbacBcsAuthServiceCode()

    @Bean
    fun pipelineAuthServiceCode() = RbacPipelineAuthServiceCode()

    @Bean
    fun codeAuthServiceCode() = RbacCodeAuthServiceCode()

    @Bean
    fun projectAuthServiceCode() = RbacProjectAuthServiceCode()

    @Bean
    fun environmentAuthServiceCode() = RbacEnvironmentAuthServiceCode()

    @Bean
    fun repoAuthServiceCode() = RbacRepoAuthServiceCode()

    @Bean
    fun ticketAuthServiceCode() = RbacTicketAuthServiceCode()

    @Bean
    fun qualityAuthServiceCode() = RbacQualityAuthServiceCode()

    @Bean
    fun artifactoryAuthServiceCode() = RbacArtifactoryAuthServiceCode()
}
