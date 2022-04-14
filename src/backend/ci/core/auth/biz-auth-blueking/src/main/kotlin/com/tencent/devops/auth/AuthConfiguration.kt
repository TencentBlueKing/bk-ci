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

package com.tencent.devops.auth

import com.tencent.devops.auth.service.simple.SimpleAuthPermissionService
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.service.impl.ApigwHttpClientServiceImpl
import com.tencent.bk.sdk.iam.service.impl.ManagerServiceImpl
import com.tencent.devops.auth.service.AuthCustomizePermissionService
import com.tencent.devops.auth.service.AuthDeptServiceImpl
import com.tencent.devops.auth.service.AuthGroupMemberService
import com.tencent.devops.auth.service.AuthGroupService
import com.tencent.devops.auth.service.StrategyService
import com.tencent.devops.auth.service.action.ActionService
import com.tencent.devops.auth.service.action.BkResourceService
import com.tencent.devops.auth.service.ci.PermissionProjectService
import com.tencent.devops.auth.service.ci.PermissionRoleMemberService
import com.tencent.devops.auth.service.ci.PermissionRoleService
import com.tencent.devops.auth.service.ci.PermissionService
import com.tencent.devops.auth.service.iam.IamCacheService
import com.tencent.devops.auth.service.iam.PermissionGradeService
import com.tencent.devops.auth.service.simple.SimplePermissionGraderServiceImpl
import com.tencent.devops.auth.service.simple.SimplePermissionProjectServiceImpl
import com.tencent.devops.auth.service.simple.SimplePermissionRoleMemberServiceImpl
import com.tencent.devops.auth.service.simple.SimplePermissionRoleService
import com.tencent.devops.auth.service.stream.GithubStreamPermissionServiceImpl
import com.tencent.devops.auth.service.stream.GitlabStreamPermissionServiceImpl
import com.tencent.devops.auth.service.stream.StreamPermissionProjectServiceImpl
import com.tencent.devops.auth.service.stream.StreamPermissionServiceImpl
import com.tencent.devops.common.auth.service.IamEsbService
import com.tencent.devops.common.redis.RedisOperation
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Value
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
class AuthConfiguration {
    @Value("\${auth.url:}")
    val iamBaseUrl = ""

    @Value("\${auth.appCode:}")
    val systemId = ""

    @Value("\${auth.appCode:}")
    val appCode = ""

    @Value("\${auth.appSecret:}")
    val appSecret = ""

    @Value("\${auth.apigwUrl:#{null}}")
    val iamApigw = ""

    @Bean
    fun iamEsbService() = IamEsbService()

    @Bean
    @ConditionalOnMissingBean(PermissionService::class)
    fun permissionService(
        groupService: AuthGroupService,
        actionService: ActionService,
        groupMemberService: AuthGroupMemberService,
        authCustomizePermissionService: AuthCustomizePermissionService,
        strategyService: StrategyService
    ) = SimpleAuthPermissionService(
        groupService, actionService, groupMemberService, authCustomizePermissionService, strategyService
    )

    @Bean
    @ConditionalOnMissingBean(PermissionProjectService::class)
    fun permissionProjectService(
        groupMemberService: AuthGroupMemberService,
        groupService: AuthGroupService
    ) = SimplePermissionProjectServiceImpl(
        groupMemberService,
        groupService
    )

    @Bean
    @ConditionalOnMissingBean(PermissionRoleService::class)
    fun permissionRoleService(
        dslContext: DSLContext,
        groupService: AuthGroupService,
        resourceService: BkResourceService,
        actionsService: ActionService,
        authCustomizePermissionService: AuthCustomizePermissionService
    ) = SimplePermissionRoleService(
        dslContext = dslContext,
        groupService = groupService,
        resourceService = resourceService,
        actionsService = actionsService,
        authCustomizePermissionService = authCustomizePermissionService
    )

    @Bean
    @ConditionalOnMissingBean(PermissionRoleMemberService::class)
    fun permissionRoleMemberServiceImpl(
        permissionGradeService: PermissionGradeService,
        groupService: AuthGroupService,
        iamCacheService: IamCacheService,
        groupMemberService: AuthGroupMemberService,
    ) = SimplePermissionRoleMemberServiceImpl(
        permissionGradeService, groupService, iamCacheService, groupMemberService
    )

    @Bean
    @ConditionalOnMissingBean(PermissionGradeService::class)
    fun permissionGradeService(
        permissionProjectService: PermissionProjectService
    ) = SimplePermissionGraderServiceImpl(permissionProjectService)

    @Bean
    @ConditionalOnMissingBean
    fun iamConfiguration() = IamConfiguration(systemId, appCode, appSecret, iamBaseUrl, iamApigw)

    @Bean
    fun apigwHttpClientServiceImpl() = ApigwHttpClientServiceImpl(iamConfiguration())

    @Bean
    fun iamManagerService() = ManagerServiceImpl(apigwHttpClientServiceImpl(), iamConfiguration())

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "bk_login")
    fun deptService(
        redisOperation: RedisOperation,
        objectMapper: ObjectMapper
    ) = AuthDeptServiceImpl(redisOperation, objectMapper)

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "github")
    fun githubStreamPermissionService() = GithubStreamPermissionServiceImpl()

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "github")
    fun githubStreamProjectPermissionService(
        streamPermissionService: StreamPermissionServiceImpl
    ) = StreamPermissionProjectServiceImpl(streamPermissionService)

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "gitlab")
    fun gitlabStreamPermissionService() = GitlabStreamPermissionServiceImpl()

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "gitlab")
    fun gitlabStreamProjectPermissionService(
        streamPermissionService: StreamPermissionServiceImpl
    ) = StreamPermissionProjectServiceImpl(streamPermissionService)
}
