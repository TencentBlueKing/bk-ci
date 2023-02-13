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
 *
 */

package com.tencent.devops.ticket.config

import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.code.TicketAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.ticket.dao.CertDao
import com.tencent.devops.ticket.dao.CredentialDao
import com.tencent.devops.ticket.service.CertPermissionService
import com.tencent.devops.ticket.service.CredentialPermissionService
import com.tencent.devops.ticket.service.permission.BluekingCertPermissionService
import com.tencent.devops.ticket.service.permission.BluekingCredentialPermissionService
import com.tencent.devops.ticket.service.permission.MockCertPermissionService
import com.tencent.devops.ticket.service.permission.MockCredentialPermissionService
import com.tencent.devops.ticket.service.permission.RbacCertPermissionService
import com.tencent.devops.ticket.service.permission.RbacCredentialPermissionService
import com.tencent.devops.ticket.service.permission.StreamCertPermissionServiceImpl
import com.tencent.devops.ticket.service.permission.StreamCredentialPermissionServiceImpl
import com.tencent.devops.ticket.service.permission.V3CertPermissionService
import com.tencent.devops.ticket.service.permission.V3CredentialPermissionService
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
class TicketPermConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "bk_login")
    fun certPermissionService(
        authResourceApi: AuthResourceApi,
        authPermissionApi: AuthPermissionApi,
        ticketAuthServiceCode: TicketAuthServiceCode
    ): CertPermissionService = BluekingCertPermissionService(
        authResourceApi = authResourceApi,
        authPermissionApi = authPermissionApi,
        ticketAuthServiceCode = ticketAuthServiceCode
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "bk_login")
    fun credentialPermissionService(
        authResourceApi: AuthResourceApi,
        authPermissionApi: AuthPermissionApi,
        ticketAuthServiceCode: TicketAuthServiceCode
    ): CredentialPermissionService = BluekingCredentialPermissionService(
        authResourceApi = authResourceApi,
        authPermissionApi = authPermissionApi,
        ticketAuthServiceCode = ticketAuthServiceCode
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "bk_login_v3")
    fun v3CertPermissionService(
        dslContext: DSLContext,
        certDao: CertDao,
        client: Client,
        redisOperation: RedisOperation,
        authResourceApi: AuthResourceApi,
        authPermissionApi: AuthPermissionApi,
        ticketAuthServiceCode: TicketAuthServiceCode
    ): CertPermissionService = V3CertPermissionService(
        dslContext = dslContext,
        certDao = certDao,
        authResourceApi = authResourceApi,
        authPermissionApi = authPermissionApi,
        ticketAuthServiceCode = ticketAuthServiceCode,
        client = client,
        redisOperation = redisOperation
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "bk_login_v3")
    fun v3CredentialPermissionService(
        dslContext: DSLContext,
        client: Client,
        redisOperation: RedisOperation,
        credentialDao: CredentialDao,
        authResourceApi: AuthResourceApi,
        authPermissionApi: AuthPermissionApi,
        ticketAuthServiceCode: TicketAuthServiceCode
    ): CredentialPermissionService = V3CredentialPermissionService(
        dslContext = dslContext,
        credentialDao = credentialDao,
        authResourceApi = authResourceApi,
        authPermissionApi = authPermissionApi,
        ticketAuthServiceCode = ticketAuthServiceCode,
        client = client,
        redisOperation = redisOperation
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "sample")
    fun mockStreamCertPermissionService(
        dslContext: DSLContext,
        certDao: CertDao,
        authResourceApi: AuthResourceApi,
        authPermissionApi: AuthPermissionApi,
        ticketAuthServiceCode: TicketAuthServiceCode
    ): CertPermissionService = MockCertPermissionService(
        dslContext = dslContext,
        certDao = certDao,
        authResourceApi = authResourceApi,
        authPermissionApi = authPermissionApi,
        ticketAuthServiceCode = ticketAuthServiceCode
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "sample")
    fun mockCredentialPermissionService(
        dslContext: DSLContext,
        credentialDao: CredentialDao,
        authResourceApi: AuthResourceApi,
        authPermissionApi: AuthPermissionApi,
        ticketAuthServiceCode: TicketAuthServiceCode
    ): CredentialPermissionService = MockCredentialPermissionService(
        dslContext = dslContext,
        credentialDao = credentialDao,
        authResourceApi = authResourceApi,
        authPermissionApi = authPermissionApi,
        ticketAuthServiceCode = ticketAuthServiceCode
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "github")
    fun githubStreamCertPermissionService(
        client: Client,
        certDao: CertDao,
        dslContext: DSLContext,
        tokenService: ClientTokenService
    ): CertPermissionService = StreamCertPermissionServiceImpl(
        dslContext = dslContext,
        certDao = certDao,
        client = client,
        tokenService = tokenService
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "github")
    fun githubStreamCredentialPermissionService(
        client: Client,
        credentialDao: CredentialDao,
        dslContext: DSLContext,
        tokenService: ClientTokenService
    ): CredentialPermissionService = StreamCredentialPermissionServiceImpl(
        dslContext = dslContext,
        credentialDao = credentialDao,
        client = client,
        tokenService = tokenService
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "gitlab")
    fun gitlabStreamCertPermissionService(
        client: Client,
        certDao: CertDao,
        dslContext: DSLContext,
        tokenService: ClientTokenService
    ): CertPermissionService = StreamCertPermissionServiceImpl(
        dslContext = dslContext,
        certDao = certDao,
        client = client,
        tokenService = tokenService
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "gitlab")
    fun gitlabStreamCredentialPermissionService(
        client: Client,
        credentialDao: CredentialDao,
        dslContext: DSLContext,
        tokenService: ClientTokenService
    ): CredentialPermissionService = StreamCredentialPermissionServiceImpl(
        dslContext = dslContext,
        credentialDao = credentialDao,
        client = client,
        tokenService = tokenService
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "rbac")
    fun rbacCertPermissionService(
        client: Client,
        certDao: CertDao,
        dslContext: DSLContext,
        tokenService: ClientTokenService
    ): CertPermissionService = RbacCertPermissionService(
        client = client,
        certDao = certDao,
        dslContext = dslContext,
        tokenService = tokenService
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "rbac")
    fun rbacCredentialPermissionService(
        client: Client,
        credentialDao: CredentialDao,
        dslContext: DSLContext,
        tokenService: ClientTokenService
    ): CredentialPermissionService = RbacCredentialPermissionService(
        client = client,
        credentialDao = credentialDao,
        dslContext = dslContext,
        tokenService = tokenService
    )
}
