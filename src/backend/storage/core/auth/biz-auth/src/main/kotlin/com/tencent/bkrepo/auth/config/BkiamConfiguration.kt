/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.auth.config

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.HttpClientService
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.bk.sdk.iam.service.TokenService
import com.tencent.bk.sdk.iam.service.impl.DefaultHttpClientServiceImpl
import com.tencent.bk.sdk.iam.service.impl.GrantServiceImpl
import com.tencent.bk.sdk.iam.service.impl.PolicyServiceImpl
import com.tencent.bk.sdk.iam.service.impl.TokenServiceImpl
import com.tencent.bkrepo.auth.service.bkiam.IamEsbClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class BkiamConfiguration {

    @Value("\${auth.iam.systemId:}")
    private val iamSystemId = ""

    @Value("\${auth.iam.baseUrl:}")
    private val iamBaseUrl = ""

    @Value("\${auth.iam.appCode:}")
    private val appCode = ""

    @Value("\${auth.iam.appSecret:}")
    private val appSecret = ""

    @Bean
    fun iamConfiguration() = IamConfiguration(iamSystemId, appCode, appSecret, iamBaseUrl)

    @Bean
    fun iamHttpClient(iamConfiguration: IamConfiguration) = DefaultHttpClientServiceImpl(iamConfiguration)

    @Bean
    fun iamPolicyService(
        @Autowired iamConfiguration: IamConfiguration,
        @Autowired httpClientService: HttpClientService
    ) = PolicyServiceImpl(iamConfiguration, httpClientService)

    @Bean
    fun tokenService(
        @Autowired iamConfiguration: IamConfiguration,
        @Autowired httpClientService: HttpClientService
    ) = TokenServiceImpl(iamConfiguration, httpClientService)

    @Bean
    fun authHelper(
        @Autowired tokenService: TokenService,
        @Autowired policyService: PolicyService,
        @Autowired iamConfiguration: IamConfiguration
    ) = AuthHelper(tokenService, policyService, iamConfiguration)

    @Bean
    fun grantService(
        @Autowired defaultHttpClientServiceImpl: DefaultHttpClientServiceImpl,
        @Autowired iamConfiguration: IamConfiguration
    ) = GrantServiceImpl(defaultHttpClientServiceImpl, iamConfiguration)

    @Bean
    @ConditionalOnMissingBean
    fun iamEsbService() = IamEsbClient()
}
