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

package com.tencent.devops.project.service.permission.config

import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.code.ProjectAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.service.ProjectApprovalService
import com.tencent.devops.project.service.ProjectExtService
import com.tencent.devops.project.service.ProjectPermissionService
import com.tencent.devops.project.service.impl.StreamProjectPermissionServiceImpl
import com.tencent.devops.project.service.permission.BluekingProjectPermissionServiceImpl
import com.tencent.devops.project.service.permission.ProjectPermissionServiceImpl
import com.tencent.devops.project.service.permission.RbacProjectPermissionService
import com.tencent.devops.project.service.permission.V3ProjectPermissionServiceImpl
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
class ProjectPermissionConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "bk_login")
    fun projectPermissionService(
        authProjectApi: AuthProjectApi,
        authResourceApi: AuthResourceApi,
        projectAuthServiceCode: ProjectAuthServiceCode
    ): ProjectPermissionService = BluekingProjectPermissionServiceImpl(
        authProjectApi = authProjectApi,
        authResourceApi = authResourceApi,
        projectAuthServiceCode = projectAuthServiceCode
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "sample")
    fun sampleProjectPermissionService(
        dslContext: DSLContext,
        projectDao: ProjectDao,
        authProjectApi: AuthProjectApi,
        authResourceApi: AuthResourceApi,
        projectAuthServiceCode: ProjectAuthServiceCode
    ): ProjectPermissionService = ProjectPermissionServiceImpl(
        dslContext = dslContext,
        projectDao = projectDao,
        authProjectApi = authProjectApi,
        authResourceApi = authResourceApi,
        projectAuthServiceCode = projectAuthServiceCode
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "bk_login_v3")
    fun v3ProjectPermissionService(
        client: Client,
        authProjectApi: AuthProjectApi,
        authResourceApi: AuthResourceApi,
        authPermissionApi: AuthPermissionApi,
        projectAuthServiceCode: ProjectAuthServiceCode,
        projectDao: ProjectDao,
        dslContext: DSLContext
    ): ProjectPermissionService = V3ProjectPermissionServiceImpl(
        authProjectApi = authProjectApi,
        authPermissionApi = authPermissionApi,
        projectAuthServiceCode = projectAuthServiceCode,
        projectDao = projectDao,
        dslContext = dslContext,
        authResourceApi = authResourceApi
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "github")
    fun githubStreamProjectPermissionService(
        client: Client,
        tokenService: ClientTokenService
    ): ProjectPermissionService = StreamProjectPermissionServiceImpl(
        client = client,
        tokenService = tokenService
    )

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "rbac")
    fun rbacProjectPermissionService(
        authProjectApi: AuthProjectApi,
        authResourceApi: AuthResourceApi,
        authPermissionApi: AuthPermissionApi,
        projectAuthServiceCode: ProjectAuthServiceCode,
        projectApprovalService: ProjectApprovalService,
        dslContext: DSLContext,
        projectDao: ProjectDao,
        projectExtService: ProjectExtService
    ): ProjectPermissionService = RbacProjectPermissionService(
        authResourceApi = authResourceApi,
        authProjectApi = authProjectApi,
        authPermissionApi = authPermissionApi,
        projectAuthServiceCode = projectAuthServiceCode,
        projectApprovalService = projectApprovalService,
        dslContext = dslContext,
        projectDao = projectDao,
        projectExtService = projectExtService
    )
}
