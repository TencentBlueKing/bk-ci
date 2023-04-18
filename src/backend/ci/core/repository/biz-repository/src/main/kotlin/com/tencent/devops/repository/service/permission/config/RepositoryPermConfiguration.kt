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

package com.tencent.devops.repository.service.permission.config

import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.code.CodeAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.repository.dao.RepositoryDao
import com.tencent.devops.repository.service.RepositoryPermissionService
import com.tencent.devops.repository.service.permission.BluekingRepositoryPermissionService
import com.tencent.devops.repository.service.permission.MockRepositoryPermissionService
import com.tencent.devops.repository.service.permission.RbacRepositoryPermissionService
import com.tencent.devops.repository.service.permission.StreamRepositoryPermissionServiceImpl
import com.tencent.devops.repository.service.permission.V3RepositoryPermissionService
import org.jooq.DSLContext
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Suppress("ALL")
@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class RepositoryPermConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "bk_login")
    fun repositoryPermissionService(
        authResourceApi: AuthResourceApi,
        authPermissionApi: AuthPermissionApi,
        codeAuthServiceCode: CodeAuthServiceCode
    ): RepositoryPermissionService = BluekingRepositoryPermissionService(
        authResourceApi = authResourceApi,
        authPermissionApi = authPermissionApi,
        codeAuthServiceCode = codeAuthServiceCode
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "sample")
    fun mockRepositoryPermissionService(
        dslContext: DSLContext,
        repositoryDao: RepositoryDao,
        authResourceApi: AuthResourceApi,
        authPermissionApi: AuthPermissionApi,
        codeAuthServiceCode: CodeAuthServiceCode
    ): RepositoryPermissionService = MockRepositoryPermissionService(
        dslContext = dslContext,
        repositoryDao = repositoryDao,
        authResourceApi = authResourceApi,
        authPermissionApi = authPermissionApi,
        codeAuthServiceCode = codeAuthServiceCode
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "bk_login_v3")
    fun v3RepositoryPermissionService(
        dslContext: DSLContext,
        client: Client,
        redisOperation: RedisOperation,
        repositoryDao: RepositoryDao,
        authResourceApi: AuthResourceApi,
        authPermissionApi: AuthPermissionApi,
        codeAuthServiceCode: CodeAuthServiceCode
    ): RepositoryPermissionService = V3RepositoryPermissionService(
        dslContext = dslContext,
        repositoryDao = repositoryDao,
        authResourceApi = authResourceApi,
        authPermissionApi = authPermissionApi,
        codeAuthServiceCode = codeAuthServiceCode,
        client = client,
        redisOperation = redisOperation
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "github")
    fun githubStreamRepositoryPermissionService(
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

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "gitlab")
    fun gitlabStreamRepositoryPermissionService(
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

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "rbac")
    fun rbacRepositoryPermissionService(
        client: Client,
        tokenService: ClientTokenService
    ): RepositoryPermissionService = RbacRepositoryPermissionService(
        client = client,
        tokenService = tokenService
    )
}
