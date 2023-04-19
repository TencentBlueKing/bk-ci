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

package com.tencent.devops.environment

import com.tencent.devops.auth.service.ManagerService
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceApiStr
import com.tencent.devops.common.auth.code.EnvironmentAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.environment.dao.EnvDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.permission.StreamEnvironmentPermissionServiceImp
import com.tencent.devops.environment.permission.impl.EnvironmentPermissionServiceImpl
import com.tencent.devops.environment.permission.impl.TxV3EnvironmentPermissionService
import com.tencent.devops.environment.service.TencentAgentUrlServiceImpl
import com.tencent.devops.environment.service.TencentGITCIAgentUrlServiceImpl
import org.jooq.DSLContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class TencentServiceConfig {

    /**
     *  下载链接服务
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "cluster", name = ["tag"], havingValue = "stream")
    fun gitciAgentUrlService(commonConfig: CommonConfig) = TencentGITCIAgentUrlServiceImpl(commonConfig)

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "cluster", name = ["tag"], havingValue = "devops")
    fun agentUrlService(commonConfig: CommonConfig) = TencentAgentUrlServiceImpl(commonConfig)

    @Bean
    fun managerService(client: Client) = ManagerService(client)

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "client")
    fun environmentPermissionServiceImpl(
        authResourceApi: AuthResourceApi,
        authPermissionApi: AuthPermissionApi,
        environmentAuthServiceCode: EnvironmentAuthServiceCode,
        managerService: ManagerService,
        envDao: EnvDao,
        nodeDao: NodeDao,
        dslContext: DSLContext
    ) = EnvironmentPermissionServiceImpl(
        authResourceApi = authResourceApi,
        authPermissionApi = authPermissionApi,
        environmentAuthServiceCode = environmentAuthServiceCode,
        managerService = managerService,
        envDao = envDao,
        nodeDao = nodeDao,
        dslContext = dslContext
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "new_v3")
    fun txV3EnvironmentPermissionService(
        client: Client,
        envDao: EnvDao,
        nodeDao: NodeDao,
        dslContext: DSLContext,
        tokenCheckService: ClientTokenService,
        authResourceApiStr: AuthResourceApiStr
    ) = TxV3EnvironmentPermissionService(
        client = client,
        envDao = envDao,
        nodeDao = nodeDao,
        dslContext = dslContext,
        tokenCheckService = tokenCheckService,
        authResourceApiStr = authResourceApiStr
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "git")
    fun gitStreamEnvironmentPermissionService(
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
}
