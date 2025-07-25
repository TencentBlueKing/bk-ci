/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.process.permission.config

import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.permission.template.MockPipelineTemplatePermissionService
import com.tencent.devops.process.permission.template.PipelineTemplatePermissionService
import com.tencent.devops.process.permission.template.RbacPipelineTemplatePermissionService
import com.tencent.devops.process.service.view.PipelineViewGroupService
import org.jooq.DSLContext
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Suppress("ALL")
@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class PipelineTemplatePermConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "rbac")
    fun rbacPipelineTemplatePermissionService(
        authPermissionApi: AuthPermissionApi,
        authProjectApi: AuthProjectApi,
        pipelineAuthServiceCode: PipelineAuthServiceCode,
        dslContext: DSLContext,
        pipelineInfoDao: PipelineInfoDao,
        pipelineViewGroupService: PipelineViewGroupService,
        client: Client,
        authResourceApi: AuthResourceApi,
        pipelinePermissionService: PipelinePermissionService
    ): PipelineTemplatePermissionService = RbacPipelineTemplatePermissionService(
        authPermissionApi = authPermissionApi,
        authProjectApi = authProjectApi,
        pipelineAuthServiceCode = pipelineAuthServiceCode,
        dslContext = dslContext,
        pipelineInfoDao = pipelineInfoDao,
        client = client,
        authResourceApi = authResourceApi,
        pipelinePermissionService = pipelinePermissionService
    )

    @Bean
    @ConditionalOnMissingBean(PipelineTemplatePermissionService::class)
    fun mockPipelineTemplatePermissionService(
        authProjectApi: AuthProjectApi,
        pipelineAuthServiceCode: PipelineAuthServiceCode
    ) = MockPipelineTemplatePermissionService(
        authProjectApi = authProjectApi,
        pipelineAuthServiceCode = pipelineAuthServiceCode
    )
}
