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

package com.tencent.devops.repository

import com.tencent.devops.auth.service.ManagerService
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.repository.dao.RepositoryDao
import com.tencent.devops.repository.service.impl.RepositoryPermissionServiceImpl
import org.jooq.DSLContext
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceApiStr
import com.tencent.devops.common.auth.code.CodeAuthServiceCode
import com.tencent.devops.repository.service.RepositoryPermissionService
import com.tencent.devops.repository.service.impl.StreamRepositoryPermissionServiceImpl
import com.tencent.devops.repository.service.impl.TxV3RepositoryPermissionServiceImpl
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class RepositoryConfiguration {
    @Bean
    fun managerService(client: Client) = ManagerService(client)

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "client")
    fun repositoryPermissionService(
        authResourceApi: AuthResourceApi,
        authPermissionApi: AuthPermissionApi,
        codeAuthServiceCode: CodeAuthServiceCode,
        managerService: ManagerService,
        repositoryDao: RepositoryDao,
        dslContext: DSLContext
    ) = RepositoryPermissionServiceImpl(
        authResourceApi = authResourceApi,
        authPermissionApi = authPermissionApi,
        codeAuthServiceCode = codeAuthServiceCode,
        managerService = managerService,
        repositoryDao = repositoryDao,
        dslContext = dslContext
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "new_v3")
    fun txRepositoryPermissionService(
        repositoryDao: RepositoryDao,
        dslContext: DSLContext,
        client: Client,
        tokenService: ClientTokenService,
        authResourceApiStr: AuthResourceApiStr
    ) = TxV3RepositoryPermissionServiceImpl(
        repositoryDao = repositoryDao,
        dslContext = dslContext,
        client = client,
        tokenService = tokenService,
        authResourceApiStr = authResourceApiStr
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "git")
    fun gitStreamRepositoryPermissionService(
        dslContext: DSLContext,
        tokenService: ClientTokenService,
        repositoryDao: RepositoryDao,
        client: Client
    ): RepositoryPermissionService = StreamRepositoryPermissionServiceImpl(
        dslContext = dslContext,
        repositoryDao = repositoryDao,
        tokenService = tokenService,
        client = client
    )
}
