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

package com.tencent.devops.log.config

import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.log.service.LogPermissionService
import com.tencent.devops.log.service.impl.BluekingLogPermissionService
import com.tencent.devops.log.service.impl.RbacLogPermissionService
import com.tencent.devops.log.service.impl.SimpleLogPermissionService
import com.tencent.devops.log.service.impl.StreamLogPermissionService
import com.tencent.devops.log.service.impl.V3LogPermissionService
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Suppress("UNUSED")
@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class LogPermissionConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "bk_login")
    fun bkLogPermissionService(
        authPermissionApi: AuthPermissionApi,
        pipelineAuthServiceCode: PipelineAuthServiceCode
    ): LogPermissionService = BluekingLogPermissionService(
        authPermissionApi = authPermissionApi,
        pipelineAuthServiceCode = pipelineAuthServiceCode
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "sample")
    fun sampleLogPermissionService(): LogPermissionService = SimpleLogPermissionService()

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "bk_login_v3")
    fun bkV3LogPermissionService(
        authPermissionApi: AuthPermissionApi,
        pipelineAuthServiceCode: PipelineAuthServiceCode,
        client: Client,
        redisOperation: RedisOperation
    ): LogPermissionService = V3LogPermissionService(
        authPermissionApi = authPermissionApi,
        pipelineAuthServiceCode = pipelineAuthServiceCode,
        redisOperation = redisOperation,
        client = client
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "rbac")
    fun rbacLogPermissionService(
        client: Client,
        tokenCheckService: ClientTokenService
    ): LogPermissionService = RbacLogPermissionService(
        client = client,
        tokenCheckService = tokenCheckService
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "github")
    fun githubStreamLogPermissionService(
        client: Client,
        tokenCheckService: ClientTokenService
    ): LogPermissionService = StreamLogPermissionService(
        client = client,
        tokenCheckService = tokenCheckService
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "gitlab")
    fun gitlabStreamLogPermissionService(
        client: Client,
        tokenCheckService: ClientTokenService
    ): LogPermissionService = StreamLogPermissionService(
        client = client,
        tokenCheckService = tokenCheckService
    )
}
