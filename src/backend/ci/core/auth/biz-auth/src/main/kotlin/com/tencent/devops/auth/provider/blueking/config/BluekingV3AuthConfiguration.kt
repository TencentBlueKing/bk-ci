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

package com.tencent.devops.auth.provider.blueking.config

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.ManagerService
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.bk.sdk.iam.service.impl.ApigwHttpClientServiceImpl
import com.tencent.bk.sdk.iam.service.impl.GrantServiceImpl
import com.tencent.bk.sdk.iam.service.impl.ManagerServiceImpl
import com.tencent.devops.auth.dao.AuthGroupDao
import com.tencent.devops.auth.provider.blueking.service.BkAuthGrantPermissionServiceImpl
import com.tencent.devops.auth.provider.blueking.service.BkIamPermissionRoleExtService
import com.tencent.devops.auth.provider.blueking.service.BkSuperManagerServiceImp
import com.tencent.devops.auth.provider.blueking.service.BkOrganizationService
import com.tencent.devops.auth.provider.blueking.service.BkPermissionExtServiceImpl
import com.tencent.devops.auth.provider.blueking.service.BkPermissionGraderServiceImpl
import com.tencent.devops.auth.provider.blueking.service.BkPermissionProjectService
import com.tencent.devops.auth.provider.blueking.service.BkPermissionRoleMemberImpl
import com.tencent.devops.auth.provider.blueking.service.BkPermissionService
import com.tencent.devops.auth.provider.blueking.service.BkPermissionUrlService
import com.tencent.devops.auth.refresh.dispatch.AuthRefreshDispatch
import com.tencent.devops.auth.service.AuthGroupService
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.StrategyService
import com.tencent.devops.auth.service.iam.IamCacheService
import com.tencent.devops.auth.service.iam.PermissionGradeService
import com.tencent.devops.auth.service.iam.PermissionRoleMemberService
import com.tencent.devops.auth.service.iam.PermissionRoleService
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.code.ProjectAuthServiceCode
import com.tencent.devops.common.auth.service.IamEsbService
import com.tencent.devops.common.client.Client
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered

@Suppress("ALL")
@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "bk_login_v3")
class BluekingV3AuthConfiguration {
    @Value("\${auth.url:}")
    val iamBaseUrl = ""

    @Value("\${auth.iamSystem:}")
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
    @ConditionalOnMissingBean
    fun iamConfiguration() = IamConfiguration(systemId, appCode, appSecret, iamBaseUrl, iamApigw)

    @Bean
    fun apigwHttpClientServiceImpl() = ApigwHttpClientServiceImpl(iamConfiguration())

    @Bean
    fun iamManagerService() = ManagerServiceImpl(apigwHttpClientServiceImpl(), iamConfiguration())

    @Bean
    @Primary
    fun v3permissionService(
        authHelper: AuthHelper,
        policyService: PolicyService,
        iamConfiguration: IamConfiguration,
        iamCacheService: IamCacheService
    ) = BkPermissionService(authHelper, policyService, iamConfiguration, iamCacheService)

    @Bean
    @Primary
    fun v3permissionProjectService(
        permissionRoleService: PermissionRoleService,
        permissionRoleMemberService: PermissionRoleMemberService,
        authHelper: AuthHelper,
        policyService: PolicyService,
        client: Client,
        iamConfiguration: IamConfiguration,
        deptService: DeptService,
        groupService: AuthGroupService,
        iamCacheService: IamCacheService,
        authProjectApi: AuthProjectApi,
        projectAuthServiceCode: ProjectAuthServiceCode
    ) = BkPermissionProjectService(
        permissionRoleService = permissionRoleService,
        permissionRoleMemberService = permissionRoleMemberService,
        authHelper = authHelper,
        policyService = policyService,
        client = client,
        iamConfiguration = iamConfiguration,
        deptService = deptService,
        groupService = groupService,
        iamCacheService = iamCacheService,
        authProjectApi = authProjectApi,
        projectAuthServiceCode = projectAuthServiceCode
    )

    @Bean
    @Primary
    fun bkManagerService() = BkSuperManagerServiceImp()

    @Bean
    @Primary
    fun bkAuthGrantPermissionServiceImpl(
        grantServiceImpl: GrantServiceImpl,
        iamConfiguration: IamConfiguration,
        client: Client
    ) = BkAuthGrantPermissionServiceImpl(grantServiceImpl, iamConfiguration, client)

    @Bean
    @Primary
    fun bkIamPermissionRoleExtService(
        iamManagerService: ManagerService,
        permissionGradeService: PermissionGradeService,
        iamConfiguration: IamConfiguration,
        groupService: AuthGroupService,
        authGroupDao: AuthGroupDao,
        dslContext: DSLContext,
        client: Client,
        strategyService: StrategyService
    ) = BkIamPermissionRoleExtService(
        iamManagerService = iamManagerService,
        permissionGradeService = permissionGradeService,
        iamConfiguration = iamConfiguration,
        groupService = groupService,
        authGroupDao = authGroupDao,
        dslContext = dslContext,
        client = client,
        strategyService = strategyService
    )

    @Bean
    @Primary
    fun bkOrganizationService() = BkOrganizationService()

    @Bean
    @Primary
    fun bkPermissionExtServiceImpl(
        iamEsbService: IamEsbService,
        iamConfiguration: IamConfiguration,
        iamCacheService: IamCacheService,
        refreshDispatch: AuthRefreshDispatch
    ) = BkPermissionExtServiceImpl(
        iamEsbService, iamConfiguration, iamCacheService, refreshDispatch
    )

    @Bean
    @Primary
    fun bkPermissionGraderServiceImpl(iamManagerService: ManagerService) =
        BkPermissionGraderServiceImpl(iamManagerService)

    @Bean
    @Primary
    fun bkPermissionRoleMemberImpl(
        iamManagerService: ManagerService,
        permissionGradeService: PermissionGradeService,
        groupService: AuthGroupService
    ) = BkPermissionRoleMemberImpl(iamManagerService, permissionGradeService, groupService)

    @Bean
    @Primary
    fun bkPermissionUrlService(
        iamEsbService: IamEsbService,
        iamConfiguration: IamConfiguration?,
        bkPermissionProjectService: BkPermissionProjectService
    ) = BkPermissionUrlService(iamEsbService, iamConfiguration, bkPermissionProjectService)

    @Bean
    fun iamCacheService(
        policyService: PolicyService,
        groupService: AuthGroupService
    ) = IamCacheService(
        policyService = policyService,
        groupService = groupService
    )
}
