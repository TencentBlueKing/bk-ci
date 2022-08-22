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

package com.tencent.devops.ticket

import com.tencent.devops.auth.service.ManagerService
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceApiStr
import com.tencent.devops.common.auth.code.TicketAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.ticket.dao.CertDao
import com.tencent.devops.ticket.dao.CredentialDao
import com.tencent.devops.ticket.service.CertPermissionService
import com.tencent.devops.ticket.service.CertPermissionServiceImpl
import com.tencent.devops.ticket.service.CredentialPermissionService
import com.tencent.devops.ticket.service.CredentialPermissionServiceImpl
import com.tencent.devops.ticket.service.StreamCertPermissionServiceImpl
import com.tencent.devops.ticket.service.StreamCredentialPermissionServiceImpl
import com.tencent.devops.ticket.service.TxV3CertPermissionServiceImpl
import com.tencent.devops.ticket.service.TxV3CredentialPermissionServiceImpl
import org.jooq.DSLContext
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class TicketConfiguration {
    @Bean
    fun managerService(client: Client) = ManagerService(client)

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "client")
    fun certPermissionServiceImpl(
        authResourceApi: AuthResourceApi,
        authPermissionApi: AuthPermissionApi,
        ticketAuthServiceCode: TicketAuthServiceCode,
        managerService: ManagerService,
        certDao: CertDao,
        dslContext: DSLContext
    ) = CertPermissionServiceImpl(
        authResourceApi = authResourceApi,
        authPermissionApi = authPermissionApi,
        ticketAuthServiceCode = ticketAuthServiceCode,
        managerService = managerService,
        certDao = certDao,
        dslContext = dslContext
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "client")
    fun credentialPermissionServiceImpl(
        authResourceApi: AuthResourceApi,
        authPermissionApi: AuthPermissionApi,
        ticketAuthServiceCode: TicketAuthServiceCode,
        managerService: ManagerService,
        credentialDao: CredentialDao,
        dslContext: DSLContext
    ) = CredentialPermissionServiceImpl(
        authResourceApi = authResourceApi,
        authPermissionApi = authPermissionApi,
        ticketAuthServiceCode = ticketAuthServiceCode,
        managerService = managerService,
        credentialDao = credentialDao,
        dslContext = dslContext
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "new_v3")
    fun txV3CertPermissionServiceImpl(
        client: Client,
        certDao: CertDao,
        dslContext: DSLContext,
        tokenService: ClientTokenService,
        authResourceApi: AuthResourceApiStr
    ) = TxV3CertPermissionServiceImpl(
        client = client,
        certDao = certDao,
        dslContext = dslContext,
        tokenService = tokenService,
        authResourceApi = authResourceApi
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "new_v3")
    fun txV3CredentialPermissionServiceImpl(
        client: Client,
        credentialDao: CredentialDao,
        dslContext: DSLContext,
        tokenService: ClientTokenService,
        authResourceApi: AuthResourceApiStr
    ) = TxV3CredentialPermissionServiceImpl(
        client = client,
        credentialDao = credentialDao,
        dslContext = dslContext,
        tokenService = tokenService,
        authResourceApi = authResourceApi
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "git")
    fun gitStreamCertPermissionService(
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
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "git")
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
}
