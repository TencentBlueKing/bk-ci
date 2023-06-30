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

package com.tencent.devops.auth.service.permission.iam

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.devops.auth.service.AuthPipelineIdService
import com.tencent.devops.auth.service.AuthVerifyRecordService
import com.tencent.devops.auth.service.ManagerService
import com.tencent.devops.auth.service.iam.IamCacheService
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
@ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "rbac")
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@Suppress("LongParameterList")
class IamAuthRbacConfiguration {
    @Bean
    fun txPermissionService(
        authHelper: AuthHelper,
        policyService: PolicyService,
        iamConfiguration: IamConfiguration,
        managerService: ManagerService,
        iamCacheService: IamCacheService,
        client: Client,
        authPipelineIdService: AuthPipelineIdService,
        authVerifyRecordService: AuthVerifyRecordService
    ) = TxPermissionServiceImpl(
        authHelper = authHelper,
        policyService = policyService,
        iamConfiguration = iamConfiguration,
        managerService = managerService,
        iamCacheService = iamCacheService,
        client = client,
        authPipelineIdService = authPipelineIdService,
        authVerifyRecordService = authVerifyRecordService
    )

    @Bean
    fun managerService(client: Client) = ManagerService(client)

    @Bean
    @Primary
    fun txRbacPermissionSuperManagerService(managerService: ManagerService) = TxRbacPermissionSuperManagerService(
        managerService = managerService
    )
}
