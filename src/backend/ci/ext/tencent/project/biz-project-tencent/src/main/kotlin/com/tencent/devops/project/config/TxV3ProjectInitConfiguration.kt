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
import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.devops.common.auth.api.BkAuthProperties
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dao.UserDao
import com.tencent.devops.project.dispatch.ProjectDispatcher
import com.tencent.devops.project.service.ProjectPermissionService
import com.tencent.devops.project.service.iam.IamV3Service
import com.tencent.devops.project.service.impl.TxV3ProjectPermissionServiceImpl
import com.tencent.devops.project.service.impl.V3ProjectExtPermissionServiceImpl
import com.tencent.devops.project.service.tof.TOFService
import org.jooq.DSLContext
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "new_v3")
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class TxV3ProjectInitConfiguration {

    @Bean
    fun projectPermissionService(
        objectMapper: ObjectMapper,
        bkAuthProperties: BkAuthProperties,
        projectDispatcher: ProjectDispatcher,
        client: Client,
        tokenService: ClientTokenService
    ): ProjectPermissionService = TxV3ProjectPermissionServiceImpl(
        objectMapper = objectMapper,
        authProperties = bkAuthProperties,
        projectDispatcher = projectDispatcher,
        client = client,
        tokenService = tokenService
    )

    @Bean
    fun iamV3Service(
        iamManagerService: com.tencent.bk.sdk.iam.service.ManagerService,
        iamConfiguration: IamConfiguration,
        projectDao: ProjectDao,
        dslContext: DSLContext,
        projectDispatcher: ProjectDispatcher,
        client: Client,
        userDao: UserDao
    ) = IamV3Service(
        iamManagerService = iamManagerService,
        iamConfiguration = iamConfiguration,
        projectDao = projectDao,
        dslContext = dslContext,
        projectDispatcher = projectDispatcher,
        client = client,
        userDao = userDao
    )

    @Bean
    fun v3ProjectExtPermissionServiceImpl(
        client: Client,
        tokenService: ClientTokenService,
        projectDao: ProjectDao,
        dslContext: DSLContext,
        tofService: TOFService
    ) = V3ProjectExtPermissionServiceImpl(
        client, tokenService, projectDao, dslContext, tofService
    )
}
