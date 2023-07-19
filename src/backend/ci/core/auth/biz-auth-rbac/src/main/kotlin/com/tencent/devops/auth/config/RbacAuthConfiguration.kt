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

package com.tencent.devops.auth.config

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.HttpClientService
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.bk.sdk.iam.service.TokenService
import com.tencent.bk.sdk.iam.service.impl.ApigwHttpClientServiceImpl
import com.tencent.bk.sdk.iam.service.impl.TokenServiceImpl
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.bk.sdk.iam.service.v2.impl.V2GrantServiceImpl
import com.tencent.bk.sdk.iam.service.v2.impl.V2ManagerServiceImpl
import com.tencent.bk.sdk.iam.service.v2.impl.V2PolicyServiceImpl
import com.tencent.devops.auth.dao.AuthMigrationDao
import com.tencent.devops.auth.dao.AuthResourceGroupConfigDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.service.AuthResourceCodeConverter
import com.tencent.devops.auth.service.AuthResourceService
import com.tencent.devops.auth.service.AuthVerifyRecordService
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.ItsmService
import com.tencent.devops.auth.service.PermissionGradeManagerService
import com.tencent.devops.auth.service.PermissionGroupPoliciesService
import com.tencent.devops.auth.service.PermissionSubsetManagerService
import com.tencent.devops.auth.service.PermissionSuperManagerService
import com.tencent.devops.auth.service.RbacCacheService
import com.tencent.devops.auth.service.RbacPermissionApplyService
import com.tencent.devops.auth.service.RbacPermissionExtService
import com.tencent.devops.auth.service.RbacPermissionItsmCallbackService
import com.tencent.devops.auth.service.RbacPermissionProjectService
import com.tencent.devops.auth.service.RbacPermissionResourceCallbackService
import com.tencent.devops.auth.service.RbacPermissionResourceGroupService
import com.tencent.devops.auth.service.RbacPermissionResourceService
import com.tencent.devops.auth.service.RbacPermissionResourceValidateService
import com.tencent.devops.auth.service.RbacPermissionService
import com.tencent.devops.auth.service.ResourceService
import com.tencent.devops.auth.service.iam.MigrateCreatorFixService
import com.tencent.devops.auth.service.iam.PermissionProjectService
import com.tencent.devops.auth.service.iam.PermissionResourceService
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.auth.service.migrate.MigrateCreatorFixServiceImpl
import com.tencent.devops.auth.service.migrate.MigrateIamApiService
import com.tencent.devops.auth.service.migrate.MigrateResourceCodeConverter
import com.tencent.devops.auth.service.migrate.MigrateResourceService
import com.tencent.devops.auth.service.migrate.MigrateResultService
import com.tencent.devops.auth.service.migrate.MigrateV0PolicyService
import com.tencent.devops.auth.service.migrate.MigrateV3PolicyService
import com.tencent.devops.auth.service.migrate.RbacPermissionMigrateService
import com.tencent.devops.common.auth.api.AuthTokenApi
import com.tencent.devops.common.auth.code.ProjectAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.event.dispatcher.trace.TraceEventDispatcher
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.service.config.CommonConfig
import org.jooq.DSLContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
@ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "rbac")
@Suppress("TooManyFunctions", "LongParameterList")
class RbacAuthConfiguration {

    @Bean
    fun iamV2ManagerService(
        iamConfiguration: IamConfiguration,
        apigwHttpClientServiceImpl: ApigwHttpClientServiceImpl
    ) = V2ManagerServiceImpl(apigwHttpClientServiceImpl, iamConfiguration)

    @Bean
    fun iamV2PolicyService(
        iamConfiguration: IamConfiguration,
        apigwHttpClientServiceImpl: ApigwHttpClientServiceImpl
    ) = V2PolicyServiceImpl(apigwHttpClientServiceImpl, iamConfiguration)

    @Bean
    fun grantV2Service(
        iamConfiguration: IamConfiguration,
        apigwHttpClientServiceImpl: ApigwHttpClientServiceImpl
    ) = V2GrantServiceImpl(apigwHttpClientServiceImpl, iamConfiguration)

    @Bean
    fun tokenService(
        iamConfiguration: IamConfiguration,
        apigwHttpClientServiceImpl: HttpClientService
    ) = TokenServiceImpl(iamConfiguration, apigwHttpClientServiceImpl)

    @Bean
    fun authHelper(
        tokenService: TokenService,
        iamV2PolicyService: PolicyService,
        iamConfiguration: IamConfiguration
    ) = AuthHelper(tokenService, iamV2PolicyService, iamConfiguration)

    @Bean
    @SuppressWarnings("LongParameterList")
    fun permissionResourceService(
        authResourceService: AuthResourceService,
        permissionGradeManagerService: PermissionGradeManagerService,
        permissionSubsetManagerService: PermissionSubsetManagerService,
        authResourceCodeConverter: AuthResourceCodeConverter,
        permissionService: PermissionService,
        permissionProjectService: PermissionProjectService,
        traceEventDispatcher: TraceEventDispatcher,
        iamV2ManagerService: V2ManagerService,
        client: Client
    ) = RbacPermissionResourceService(
        authResourceService = authResourceService,
        permissionGradeManagerService = permissionGradeManagerService,
        permissionSubsetManagerService = permissionSubsetManagerService,
        authResourceCodeConverter = authResourceCodeConverter,
        permissionService = permissionService,
        permissionProjectService = permissionProjectService,
        traceEventDispatcher = traceEventDispatcher,
        iamV2ManagerService = iamV2ManagerService,
        client = client
    )

    @Bean
    @SuppressWarnings("LongParameterList")
    fun permissionResourceGroupService(
        iamV2ManagerService: V2ManagerService,
        authResourceService: AuthResourceService,
        permissionGradeManagerService: PermissionGradeManagerService,
        permissionSubsetManagerService: PermissionSubsetManagerService,
        permissionResourceService: PermissionResourceService,
        permissionGroupPoliciesService: PermissionGroupPoliciesService,
        authResourceGroupDao: AuthResourceGroupDao,
        dslContext: DSLContext
    ) = RbacPermissionResourceGroupService(
        iamV2ManagerService = iamV2ManagerService,
        authResourceService = authResourceService,
        permissionGradeManagerService = permissionGradeManagerService,
        permissionSubsetManagerService = permissionSubsetManagerService,
        permissionResourceService = permissionResourceService,
        permissionGroupPoliciesService = permissionGroupPoliciesService,
        authResourceGroupDao = authResourceGroupDao,
        dslContext = dslContext
    )

    @Bean
    @Primary
    fun rbacPermissionExtService(
        permissionResourceService: PermissionResourceService
    ) = RbacPermissionExtService(
        permissionResourceService = permissionResourceService
    )

    @Bean
    @Primary
    fun permissionItsmCallbackService(
        traceEventDispatcher: TraceEventDispatcher,
        itsmService: ItsmService
    ) = RbacPermissionItsmCallbackService(
        traceEventDispatcher = traceEventDispatcher,
        itsmService = itsmService
    )

    @Bean
    @Primary
    fun rbacPermissionService(
        authHelper: AuthHelper,
        authResourceService: AuthResourceService,
        iamConfiguration: IamConfiguration,
        iamV2PolicyService: PolicyService,
        authResourceCodeConverter: AuthResourceCodeConverter,
        permissionSuperManagerService: PermissionSuperManagerService,
        rbacCacheService: RbacCacheService,
        client: Client
    ) = RbacPermissionService(
        authHelper = authHelper,
        authResourceService = authResourceService,
        iamConfiguration = iamConfiguration,
        policyService = iamV2PolicyService,
        authResourceCodeConverter = authResourceCodeConverter,
        permissionSuperManagerService = permissionSuperManagerService,
        rbacCacheService = rbacCacheService,
        client = client
    )

    @Bean
    @Primary
    @Suppress("LongParameterList")
    fun rbacPermissionProjectService(
        authHelper: AuthHelper,
        authResourceService: AuthResourceService,
        iamV2ManagerService: V2ManagerService,
        iamConfiguration: IamConfiguration,
        authResourceGroupDao: AuthResourceGroupDao,
        dslContext: DSLContext,
        rbacCacheService: RbacCacheService
    ) = RbacPermissionProjectService(
        authHelper = authHelper,
        authResourceService = authResourceService,
        iamV2ManagerService = iamV2ManagerService,
        iamConfiguration = iamConfiguration,
        authResourceGroupDao = authResourceGroupDao,
        dslContext = dslContext,
        rbacCacheService = rbacCacheService
    )

    @Bean
    @Primary
    fun rbacPermissionResourceCallbackService(
        authResourceService: AuthResourceService,
        resourceService: ResourceService
    ) = RbacPermissionResourceCallbackService(
        authResourceService = authResourceService,
        resourceService = resourceService
    )

    @Bean
    @Primary
    @Suppress("LongParameterList")
    fun rbacPermissionApplyService(
        dslContext: DSLContext,
        v2ManagerService: V2ManagerService,
        authResourceService: AuthResourceService,
        authResourceGroupConfigDao: AuthResourceGroupConfigDao,
        authResourceGroupDao: AuthResourceGroupDao,
        rbacCacheService: RbacCacheService,
        config: CommonConfig,
        client: Client,
        authResourceCodeConverter: AuthResourceCodeConverter,
        permissionService: PermissionService
    ) = RbacPermissionApplyService(
        dslContext = dslContext,
        v2ManagerService = v2ManagerService,
        authResourceService = authResourceService,
        authResourceGroupConfigDao = authResourceGroupConfigDao,
        authResourceGroupDao = authResourceGroupDao,
        rbacCacheService = rbacCacheService,
        config = config,
        client = client,
        authResourceCodeConverter = authResourceCodeConverter,
        permissionService = permissionService
    )

    @Bean
    @Primary
    fun rbacPermissionResourceValidateService(
        permissionService: PermissionService,
        rbacCacheService: RbacCacheService
    ) = RbacPermissionResourceValidateService(
        permissionService = permissionService,
        rbacCacheService = rbacCacheService
    )

    @Bean
    fun migrateResourceCodeConverter(
        client: Client
    ) = MigrateResourceCodeConverter(
        client = client
    )

    @Bean
    fun migrateResourceService(
        resourceService: ResourceService,
        rbacCacheService: RbacCacheService,
        rbacPermissionResourceService: RbacPermissionResourceService,
        migrateCreatorFixService: MigrateCreatorFixService,
        authResourceService: AuthResourceService,
        migrateResourceCodeConverter: MigrateResourceCodeConverter,
        tokenApi: AuthTokenApi,
        projectAuthServiceCode: ProjectAuthServiceCode,
        dslContext: DSLContext,
        authMigrationDao: AuthMigrationDao,
        authResourceGroupConfigDao: AuthResourceGroupConfigDao
    ) = MigrateResourceService(
        resourceService = resourceService,
        rbacCacheService = rbacCacheService,
        rbacPermissionResourceService = rbacPermissionResourceService,
        migrateCreatorFixService = migrateCreatorFixService,
        authResourceService = authResourceService,
        migrateResourceCodeConverter = migrateResourceCodeConverter,
        tokenApi = tokenApi,
        projectAuthServiceCode = projectAuthServiceCode,
        dslContext = dslContext,
        authMigrationDao = authMigrationDao,
        authResourceGroupConfigDao = authResourceGroupConfigDao
    )

    @Bean
    fun migrateIamApiService() = MigrateIamApiService()

    @Bean
    fun migrateResultService(
        permissionService: PermissionService,
        rbacCacheService: RbacCacheService,
        migrateResourceCodeConverter: MigrateResourceCodeConverter,
        authVerifyRecordService: AuthVerifyRecordService,
        migrateResourceService: MigrateResourceService,
        authResourceService: AuthResourceService,
        deptService: DeptService,
        client: Client,
        tokenService: ClientTokenService,
        bkTag: BkTag,
        redisOperation: RedisOperation
    ) = MigrateResultService(
        permissionService = permissionService,
        rbacCacheService = rbacCacheService,
        migrateResourceCodeConverter = migrateResourceCodeConverter,
        authVerifyRecordService = authVerifyRecordService,
        migrateResourceService = migrateResourceService,
        authResourceService = authResourceService,
        deptService = deptService,
        client = client,
        tokenService = tokenService,
        bkTag = bkTag,
        redisOperation = redisOperation
    )

    @Bean
    fun migrateV3PolicyService(
        v2ManagerService: V2ManagerServiceImpl,
        iamConfiguration: IamConfiguration,
        dslContext: DSLContext,
        authResourceGroupDao: AuthResourceGroupDao,
        authResourceGroupConfigDao: AuthResourceGroupConfigDao,
        migrateResourceCodeConverter: MigrateResourceCodeConverter,
        migrateIamApiService: MigrateIamApiService,
        authResourceCodeConverter: AuthResourceCodeConverter,
        permissionService: PermissionService,
        rbacCacheService: RbacCacheService,
        authMigrationDao: AuthMigrationDao,
        deptService: DeptService,
        permissionGroupPoliciesService: PermissionGroupPoliciesService
    ) = MigrateV3PolicyService(
        v2ManagerService = v2ManagerService,
        iamConfiguration = iamConfiguration,
        dslContext = dslContext,
        authResourceGroupDao = authResourceGroupDao,
        authResourceGroupConfigDao = authResourceGroupConfigDao,
        migrateIamApiService = migrateIamApiService,
        migrateResourceCodeConverter = migrateResourceCodeConverter,
        authResourceCodeConverter = authResourceCodeConverter,
        permissionService = permissionService,
        rbacCacheService = rbacCacheService,
        authMigrationDao = authMigrationDao,
        deptService = deptService,
        permissionGroupPoliciesService = permissionGroupPoliciesService
    )

    @Bean
    fun migrateV0PolicyService(
        v2ManagerService: V2ManagerServiceImpl,
        iamConfiguration: IamConfiguration,
        dslContext: DSLContext,
        authResourceGroupDao: AuthResourceGroupDao,
        authResourceGroupConfigDao: AuthResourceGroupConfigDao,
        migrateResourceCodeConverter: MigrateResourceCodeConverter,
        migrateIamApiService: MigrateIamApiService,
        authResourceCodeConverter: AuthResourceCodeConverter,
        permissionService: PermissionService,
        rbacCacheService: RbacCacheService,
        authMigrationDao: AuthMigrationDao,
        deptService: DeptService,
        permissionGroupPoliciesService: PermissionGroupPoliciesService
    ) = MigrateV0PolicyService(
        v2ManagerService = v2ManagerService,
        iamConfiguration = iamConfiguration,
        dslContext = dslContext,
        authResourceGroupDao = authResourceGroupDao,
        authResourceGroupConfigDao = authResourceGroupConfigDao,
        migrateIamApiService = migrateIamApiService,
        migrateResourceCodeConverter = migrateResourceCodeConverter,
        authResourceCodeConverter = authResourceCodeConverter,
        permissionService = permissionService,
        rbacCacheService = rbacCacheService,
        authMigrationDao = authMigrationDao,
        deptService = deptService,
        permissionGroupPoliciesService = permissionGroupPoliciesService
    )

    @Bean
    @Primary
    fun rbacAuthMigrateService(
        client: Client,
        migrateResourceService: MigrateResourceService,
        migrateV3PolicyService: MigrateV3PolicyService,
        migrateV0PolicyService: MigrateV0PolicyService,
        migrateResultService: MigrateResultService,
        permissionResourceService: PermissionResourceService,
        authResourceService: AuthResourceService,
        migrateCreatorFixService: MigrateCreatorFixService,
        dslContext: DSLContext,
        authMigrationDao: AuthMigrationDao
    ) = RbacPermissionMigrateService(
        client = client,
        migrateResourceService = migrateResourceService,
        migrateV3PolicyService = migrateV3PolicyService,
        migrateV0PolicyService = migrateV0PolicyService,
        migrateResultService = migrateResultService,
        permissionResourceService = permissionResourceService,
        authResourceService = authResourceService,
        migrateCreatorFixService = migrateCreatorFixService,
        dslContext = dslContext,
        authMigrationDao = authMigrationDao
    )

    @Bean
    fun migrateCreatorFixService() = MigrateCreatorFixServiceImpl()
}
