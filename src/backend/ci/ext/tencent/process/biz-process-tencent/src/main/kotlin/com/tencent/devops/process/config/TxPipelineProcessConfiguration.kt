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

package com.tencent.devops.process.config

import com.tencent.devops.auth.service.ManagerService
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceApiStr
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.permission.PipelinePermissionServiceImpl
import com.tencent.devops.process.permission.StreamPipelinePermissionServiceImpl
import com.tencent.devops.process.ws.GitCIDetailPageBuild
import com.tencent.devops.process.ws.GitCIHistoryPageBuild
import com.tencent.devops.process.ws.GitCIStatusPageBuild
import com.tencent.devops.process.permission.V3PipelinePermissionServiceImpl
import org.jooq.DSLContext
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

/**
 * 流水线引擎初始化配置类
 *
 * @version 1.0
 */

@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class TxPipelineProcessConfiguration {

    @Bean
    fun managerService(client: Client) = ManagerService(client)

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "client")
    fun pipelinePermissionServiceImpl(
        authProjectApi: AuthProjectApi,
        authResourceApi: AuthResourceApi,
        authPermissionApi: AuthPermissionApi,
        pipelineAuthServiceCode: PipelineAuthServiceCode,
        managerService: ManagerService,
        pipelineDao: PipelineInfoDao,
        dslContext: DSLContext
    ) = PipelinePermissionServiceImpl(
        authProjectApi = authProjectApi,
        authResourceApi = authResourceApi,
        authPermissionApi = authPermissionApi,
        pipelineAuthServiceCode = pipelineAuthServiceCode,
        managerService = managerService,
        pipelineDao = pipelineDao,
        dslContext = dslContext
    )

    @Bean
    @ConditionalOnProperty(prefix = "cluster", name = ["tag"], havingValue = "stream")
    fun detailPage() = GitCIDetailPageBuild()

    @Bean
    @ConditionalOnProperty(prefix = "cluster", name = ["tag"], havingValue = "stream")
    fun historyPage() = GitCIHistoryPageBuild()

    @Bean
    @ConditionalOnProperty(prefix = "cluster", name = ["tag"], havingValue = "stream")
    fun statusPage() = GitCIStatusPageBuild()

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "new_v3")
    fun txV3PipelinePermissionService(
        txV3AuthPermission: AuthPermissionApi,
        txV3AuthProjectApi: AuthProjectApi,
        bsPipelineAuthServiceCode: BSPipelineAuthServiceCode,
        dslContext: DSLContext,
        pipelineInfoDao: PipelineInfoDao,
        authResourceApi: AuthResourceApiStr
    ): PipelinePermissionService =
        V3PipelinePermissionServiceImpl(
            authPermissionApi = txV3AuthPermission,
            authProjectApi = txV3AuthProjectApi,
            bsPipelineAuthServiceCode = bsPipelineAuthServiceCode,
            dslContext = dslContext,
            pipelineInfoDao = pipelineInfoDao,
            authResourceApi = authResourceApi
        )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "git")
    fun gitStreamPipelinePermissionService(
        client: Client,
        pipelineInfoDao: PipelineInfoDao,
        dslContext: DSLContext,
        checkTokenService: ClientTokenService
    ): PipelinePermissionService = StreamPipelinePermissionServiceImpl(
        client = client,
        pipelineInfoDao = pipelineInfoDao,
        dslContext = dslContext,
        checkTokenService = checkTokenService
    )
}
