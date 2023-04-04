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

package com.tencent.devops.environment.permission.config

import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.code.EnvironmentAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.dao.EnvDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.permission.BluekingEnvironmentPermissionService
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.permission.MockEnvironmentPermissionService
import com.tencent.devops.environment.permission.RbacEnvironmentPermissionService
import com.tencent.devops.environment.permission.StreamEnvironmentPermissionServiceImp
import com.tencent.devops.environment.permission.V3EnvironmentPermissionService
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
class EnvironmentPermConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "bk_login")
    fun environmentPermissionService(
        authResourceApi: AuthResourceApi,
        authPermissionApi: AuthPermissionApi,
        environmentAuthServiceCode: EnvironmentAuthServiceCode
    ): EnvironmentPermissionService = BluekingEnvironmentPermissionService(
        authResourceApi = authResourceApi,
        authPermissionApi = authPermissionApi,
        environmentAuthServiceCode = environmentAuthServiceCode
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "sample")
    fun mockEnvironmentPermissionService(
        dslContext: DSLContext,
        envDao: EnvDao,
        nodeDao: NodeDao,
        authResourceApi: AuthResourceApi,
        authPermissionApi: AuthPermissionApi,
        environmentAuthServiceCode: EnvironmentAuthServiceCode
    ): EnvironmentPermissionService = MockEnvironmentPermissionService(
        dslContext = dslContext,
        envDao = envDao,
        nodeDao = nodeDao,
        authResourceApi = authResourceApi,
        authPermissionApi = authPermissionApi,
        environmentAuthServiceCode = environmentAuthServiceCode
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "bk_login_v3")
    fun v3EnvironmentPermissionService(
        dslContext: DSLContext,
        envDao: EnvDao,
        nodeDao: NodeDao,
        client: Client,
        redisOperation: RedisOperation,
        authResourceApi: AuthResourceApi,
        authPermissionApi: AuthPermissionApi,
        environmentAuthServiceCode: EnvironmentAuthServiceCode
    ): EnvironmentPermissionService = V3EnvironmentPermissionService(
        dslContext = dslContext,
        envDao = envDao,
        nodeDao = nodeDao,
        authResourceApi = authResourceApi,
        authPermissionApi = authPermissionApi,
        environmentAuthServiceCode = environmentAuthServiceCode,
        client = client,
        redisOperation = redisOperation
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "github")
    fun githubStreamEnvironmentPermissionService(
        client: Client,
        dslContext: DSLContext,
        nodeDao: NodeDao,
        envDao: EnvDao,
        tokenCheckService: ClientTokenService
    ): EnvironmentPermissionService = StreamEnvironmentPermissionServiceImp(
        client = client,
        dslContext = dslContext,
        nodeDao = nodeDao,
        envDao = envDao,
        tokenCheckService = tokenCheckService
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "gitlab")
    fun gitlabStreamEnvironmentPermissionService(
        client: Client,
        dslContext: DSLContext,
        nodeDao: NodeDao,
        envDao: EnvDao,
        tokenCheckService: ClientTokenService
    ): EnvironmentPermissionService = StreamEnvironmentPermissionServiceImp(
        client = client,
        dslContext = dslContext,
        nodeDao = nodeDao,
        envDao = envDao,
        tokenCheckService = tokenCheckService
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "rbac")
    fun rbacEnvironmentPermissionService(
        client: Client,
        tokenCheckService: ClientTokenService
    ): EnvironmentPermissionService = RbacEnvironmentPermissionService(
        client = client,
        tokenCheckService = tokenCheckService
    )
}
