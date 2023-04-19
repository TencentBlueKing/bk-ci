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

package com.tencent.devops.project.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.auth.service.ManagerService
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthTokenApi
import com.tencent.devops.common.auth.api.BSAuthProjectApi
import com.tencent.devops.common.auth.api.BkAuthProperties
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.auth.code.BSProjectServiceCodec
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.project.service.ProjectPermissionService
import com.tencent.devops.project.service.iam.ProjectIamV0Service
import com.tencent.devops.project.service.impl.StreamProjectPermissionServiceImpl
import com.tencent.devops.project.service.impl.V0ProjectExtPermissionServiceImpl
import com.tencent.devops.project.service.impl.TxV0ProjectPermissionServiceImpl
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class TxProjectInitConfiguration {

    @Bean
    fun managerService(client: Client) = ManagerService(client)

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "client")
    fun projectPermissionService(
        objectMapper: ObjectMapper,
        authProperties: BkAuthProperties,
        authProjectApi: BSAuthProjectApi,
        authTokenApi: AuthTokenApi,
        bsProjectAuthServiceCode: BSProjectServiceCodec,
        authResourceApi: AuthResourceApi,
        authPermissionApi: AuthPermissionApi,
        managerService: ManagerService
    ): ProjectPermissionService = TxV0ProjectPermissionServiceImpl(
        authProjectApi = authProjectApi,
        authResourceApi = authResourceApi,
        objectMapper = objectMapper,
        authProperties = authProperties,
        authTokenApi = authTokenApi,
        bsProjectAuthServiceCode = bsProjectAuthServiceCode,
        managerService = managerService,
        authPermissionApi = authPermissionApi
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "client")
    fun txProjectPermissionServiceImpl(
        objectMapper: ObjectMapper,
        projectIamV0Service: ProjectIamV0Service,
        bsPipelineAuthServiceCode: BSPipelineAuthServiceCode
    ) = V0ProjectExtPermissionServiceImpl(objectMapper, projectIamV0Service, bsPipelineAuthServiceCode)

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "git")
    fun gitCIProjectPermissionServiceImpl(
        objectMapper: ObjectMapper,
        projectIamV0Service: ProjectIamV0Service,
        bsPipelineAuthServiceCode: BSPipelineAuthServiceCode
    ) = V0ProjectExtPermissionServiceImpl(objectMapper, projectIamV0Service, bsPipelineAuthServiceCode)

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "git")
    fun gitStreamProjectPermissionService(
        client: Client,
        tokenService: ClientTokenService
    ): ProjectPermissionService = StreamProjectPermissionServiceImpl(
        client = client,
        tokenService = tokenService
    )
}
