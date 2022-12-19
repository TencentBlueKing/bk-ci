/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
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
import com.tencent.bk.sdk.iam.service.impl.PolicyServiceImpl
import com.tencent.bk.sdk.iam.service.impl.TokenServiceImpl
import com.tencent.devops.common.auth.api.*
import com.tencent.devops.common.auth.api.external.AuthTaskService
import com.tencent.devops.common.auth.service.IamEsbService
import com.tencent.devops.common.auth.utils.CodeCCAuthResourceApi
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.data.redis.core.RedisTemplate

@Configuration
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "bk_login_v3")
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class V3AuthExAutoConfiguration {

    private val logger = LoggerFactory.getLogger(V3AuthExAutoConfiguration::class.java)

    init {
        logger.info("use v3 auth config")
    }

    @Value("\${auth.apigwUrl:}")
    val apigwBaseUrl = ""

    @Value("\${auth.url:}")
    val iamBaseUrl = ""

    @Value("\${auth.appCode:}")
    val systemId = ""

    @Value("\${auth.appCode:}")
    val appCode = ""

    @Value("\${auth.appSecret:}")
    val appSecret = ""

    @Bean
    fun authExPermissionApi(redisTemplate: RedisTemplate<String, String>,
                            client: Client,
                            authTaskService: AuthTaskService,
                            codeccAuthPermissionStrApi: AuthPermissionStrApi) =
            V3AuthExPermissionApi(
                client = client,
                redisTemplate = redisTemplate,
                authTaskService = authTaskService,
                codeccAuthPermissionApi = codeccAuthPermissionStrApi)

    @Bean
    fun authExRegisterApi(authResourceApi: CodeCCAuthResourceApi) =
            V3AuthExRegisterApi(authResourceApi)

    @Bean
    fun codeCCV3AuthPermissionApi(redisOperation: RedisOperation) =
        CodeCCV3AuthPermissionApi(authHelper(), policyService(), redisOperation)

    @Bean
    fun codeCCV3AuthResourceApi(redisOperation: RedisOperation,
                                iamEsbService: IamEsbService,
                                iamConfiguration: IamConfiguration) =
        CodeCCAuthResourceApi(iamEsbService, iamConfiguration)

    @Bean
    fun iamEsbService() = IamEsbService()

    @Bean
    fun policyService() = PolicyServiceImpl(iamConfiguration(), httpService())

    @Bean
    fun authHelper() = AuthHelper(tokenService(), policyService(), iamConfiguration())

    @Bean
    fun iamConfiguration() = IamConfiguration(systemId, appCode, appSecret, iamBaseUrl, apigwBaseUrl)

    @Bean
    fun httpService() = DefaultHttpClientServiceImpl(iamConfiguration())

    @Bean
    fun tokenService() = TokenServiceImpl(iamConfiguration(), httpService())

}