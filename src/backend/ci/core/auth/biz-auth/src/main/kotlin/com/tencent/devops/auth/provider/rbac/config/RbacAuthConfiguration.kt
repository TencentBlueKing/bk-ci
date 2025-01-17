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

package com.tencent.devops.auth.provider.rbac.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.HttpClientService
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.bk.sdk.iam.service.SystemService
import com.tencent.bk.sdk.iam.service.TokenService
import com.tencent.bk.sdk.iam.service.impl.ApigwHttpClientServiceImpl
import com.tencent.bk.sdk.iam.service.impl.SystemServiceImpl
import com.tencent.bk.sdk.iam.service.impl.TokenServiceImpl
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.bk.sdk.iam.service.v2.impl.V2GrantServiceImpl
import com.tencent.bk.sdk.iam.service.v2.impl.V2ManagerServiceImpl
import com.tencent.bk.sdk.iam.service.v2.impl.V2PolicyServiceImpl
import com.tencent.devops.auth.dao.AuthActionDao
import com.tencent.devops.auth.dao.AuthAuthorizationDao
import com.tencent.devops.auth.dao.AuthHandoverDetailDao
import com.tencent.devops.auth.dao.AuthHandoverOverviewDao
import com.tencent.devops.auth.dao.AuthMigrationDao
import com.tencent.devops.auth.dao.AuthMonitorSpaceDao
import com.tencent.devops.auth.dao.AuthResourceDao
import com.tencent.devops.auth.dao.AuthResourceGroupApplyDao
import com.tencent.devops.auth.dao.AuthResourceGroupConfigDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.dao.AuthResourceGroupMemberDao
import com.tencent.devops.auth.dao.AuthResourceGroupPermissionDao
import com.tencent.devops.auth.dao.AuthResourceSyncDao
import com.tencent.devops.auth.provider.rbac.service.AuthResourceCodeConverter
import com.tencent.devops.auth.provider.rbac.service.AuthResourceService
import com.tencent.devops.auth.provider.rbac.service.ItsmService
import com.tencent.devops.auth.provider.rbac.service.PermissionGradeManagerService
import com.tencent.devops.auth.provider.rbac.service.PermissionSubsetManagerService
import com.tencent.devops.auth.provider.rbac.service.RbacCommonService
import com.tencent.devops.auth.provider.rbac.service.RbacPermissionApplyService
import com.tencent.devops.auth.provider.rbac.service.RbacPermissionAuthMonitorSpaceService
import com.tencent.devops.auth.provider.rbac.service.RbacPermissionAuthorizationScopesService
import com.tencent.devops.auth.provider.rbac.service.RbacPermissionExtService
import com.tencent.devops.auth.provider.rbac.service.RbacPermissionHandoverApplicationService
import com.tencent.devops.auth.provider.rbac.service.RbacPermissionItsmCallbackService
import com.tencent.devops.auth.provider.rbac.service.RbacPermissionManageFacadeServiceImpl
import com.tencent.devops.auth.provider.rbac.service.RbacPermissionProjectService
import com.tencent.devops.auth.provider.rbac.service.RbacPermissionResourceCallbackService
import com.tencent.devops.auth.provider.rbac.service.RbacPermissionResourceGroupPermissionService
import com.tencent.devops.auth.provider.rbac.service.RbacPermissionResourceGroupService
import com.tencent.devops.auth.provider.rbac.service.RbacPermissionResourceGroupSyncService
import com.tencent.devops.auth.provider.rbac.service.RbacPermissionResourceMemberService
import com.tencent.devops.auth.provider.rbac.service.RbacPermissionResourceService
import com.tencent.devops.auth.provider.rbac.service.RbacPermissionResourceValidateService
import com.tencent.devops.auth.provider.rbac.service.RbacPermissionService
import com.tencent.devops.auth.provider.rbac.service.migrate.MigrateCreatorFixServiceImpl
import com.tencent.devops.auth.provider.rbac.service.migrate.MigrateIamApiService
import com.tencent.devops.auth.provider.rbac.service.migrate.MigratePermissionHandoverService
import com.tencent.devops.auth.provider.rbac.service.migrate.MigrateResourceAuthorizationService
import com.tencent.devops.auth.provider.rbac.service.migrate.MigrateResourceCodeConverter
import com.tencent.devops.auth.provider.rbac.service.migrate.MigrateResourceGroupService
import com.tencent.devops.auth.provider.rbac.service.migrate.MigrateResourceService
import com.tencent.devops.auth.provider.rbac.service.migrate.MigrateResultService
import com.tencent.devops.auth.provider.rbac.service.migrate.MigrateV0PolicyService
import com.tencent.devops.auth.provider.rbac.service.migrate.MigrateV3PolicyService
import com.tencent.devops.auth.provider.rbac.service.migrate.RbacPermissionMigrateService
import com.tencent.devops.auth.service.AuthAuthorizationScopesService
import com.tencent.devops.auth.service.AuthMonitorSpaceService
import com.tencent.devops.auth.service.AuthProjectUserMetricsService
import com.tencent.devops.auth.service.AuthVerifyRecordService
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.PermissionAuthorizationService
import com.tencent.devops.auth.service.ResourceService
import com.tencent.devops.auth.service.SuperManagerService
import com.tencent.devops.auth.service.iam.MigrateCreatorFixService
import com.tencent.devops.auth.service.iam.PermissionHandoverApplicationService
import com.tencent.devops.auth.service.iam.PermissionManageFacadeService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupPermissionService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupSyncService
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.auth.service.iam.PermissionResourceService
import com.tencent.devops.auth.service.iam.PermissionResourceValidateService
import com.tencent.devops.auth.service.iam.PermissionService
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
    fun permissionResourceService(
        authResourceService: AuthResourceService,
        permissionGradeManagerService: PermissionGradeManagerService,
        permissionSubsetManagerService: PermissionSubsetManagerService,
        authResourceCodeConverter: AuthResourceCodeConverter,
        traceEventDispatcher: TraceEventDispatcher,
        iamV2ManagerService: V2ManagerService,
        permissionAuthorizationService: PermissionAuthorizationService,
        permissionResourceValidateService: PermissionResourceValidateService
    ) = RbacPermissionResourceService(
        authResourceService = authResourceService,
        permissionGradeManagerService = permissionGradeManagerService,
        permissionSubsetManagerService = permissionSubsetManagerService,
        authResourceCodeConverter = authResourceCodeConverter,
        traceEventDispatcher = traceEventDispatcher,
        iamV2ManagerService = iamV2ManagerService,
        permissionAuthorizationService = permissionAuthorizationService,
        permissionResourceValidateService = permissionResourceValidateService
    )

    @Bean
    fun permissionResourceGroupService(
        iamV2ManagerService: V2ManagerService,
        authResourceService: AuthResourceService,
        permissionResourceGroupPermissionService: PermissionResourceGroupPermissionService,
        authResourceGroupDao: AuthResourceGroupDao,
        dslContext: DSLContext,
        authResourceGroupConfigDao: AuthResourceGroupConfigDao,
        authResourceGroupMemberDao: AuthResourceGroupMemberDao,
        authResourceDao: AuthResourceDao,
        resourceGroupSyncService: PermissionResourceGroupSyncService,
        redisOperation: RedisOperation
    ) = RbacPermissionResourceGroupService(
        iamV2ManagerService = iamV2ManagerService,
        authResourceService = authResourceService,
        permissionResourceGroupPermissionService = permissionResourceGroupPermissionService,
        authResourceGroupDao = authResourceGroupDao,
        dslContext = dslContext,
        authResourceGroupConfigDao = authResourceGroupConfigDao,
        authResourceGroupMemberDao = authResourceGroupMemberDao,
        authResourceDao = authResourceDao,
        resourceGroupSyncService = resourceGroupSyncService,
        redisOperation = redisOperation
    )

    @Bean
    fun permissionFacadeService(
        permissionResourceGroupService: PermissionResourceGroupService,
        groupPermissionService: PermissionResourceGroupPermissionService,
        permissionResourceMemberService: PermissionResourceMemberService,
        authResourceGroupDao: AuthResourceGroupDao,
        authResourceGroupMemberDao: AuthResourceGroupMemberDao,
        dslContext: DSLContext,
        deptService: DeptService,
        iamV2ManagerService: V2ManagerService,
        permissionAuthorizationService: PermissionAuthorizationService,
        syncIamGroupMemberService: PermissionResourceGroupSyncService,
        authAuthorizationDao: AuthAuthorizationDao,
        permissionHandoverApplicationService: PermissionHandoverApplicationService,
        rbacCommonService: RbacCommonService,
        redisOperation: RedisOperation,
        authorizationDao: AuthAuthorizationDao,
        authResourceService: AuthResourceService,
        client: Client,
        config: CommonConfig
    ) = RbacPermissionManageFacadeServiceImpl(
        permissionResourceGroupService = permissionResourceGroupService,
        groupPermissionService = groupPermissionService,
        permissionResourceMemberService = permissionResourceMemberService,
        authResourceGroupDao = authResourceGroupDao,
        authResourceGroupMemberDao = authResourceGroupMemberDao,
        dslContext = dslContext,
        deptService = deptService,
        iamV2ManagerService = iamV2ManagerService,
        permissionAuthorizationService = permissionAuthorizationService,
        syncIamGroupMemberService = syncIamGroupMemberService,
        authAuthorizationDao = authAuthorizationDao,
        permissionHandoverApplicationService = permissionHandoverApplicationService,
        rbacCommonService = rbacCommonService,
        redisOperation = redisOperation,
        authorizationDao = authorizationDao,
        authResourceService = authResourceService,
        client = client,
        config = config
    )

    @Bean
    fun permissionResourceGroupPermissionService(
        v2ManagerService: V2ManagerService,
        rbacCommonService: RbacCommonService,
        monitorSpaceService: AuthMonitorSpaceService,
        authResourceGroupDao: AuthResourceGroupDao,
        dslContext: DSLContext,
        resourceGroupPermissionDao: AuthResourceGroupPermissionDao,
        converter: AuthResourceCodeConverter,
        client: Client,
        iamV2ManagerService: V2ManagerService,
        authAuthorizationScopesService: AuthAuthorizationScopesService,
        authActionDao: AuthActionDao,
        authResourceGroupConfigDao: AuthResourceGroupConfigDao,
        objectMapper: ObjectMapper
    ) = RbacPermissionResourceGroupPermissionService(
        v2ManagerService = v2ManagerService,
        rbacCommonService = rbacCommonService,
        monitorSpaceService = monitorSpaceService,
        authResourceGroupDao = authResourceGroupDao,
        dslContext = dslContext,
        resourceGroupPermissionDao = resourceGroupPermissionDao,
        converter = converter,
        client = client,
        iamV2ManagerService = iamV2ManagerService,
        authAuthorizationScopesService = authAuthorizationScopesService,
        authActionDao = authActionDao,
        authResourceGroupConfigDao = authResourceGroupConfigDao,
        objectMapper = objectMapper
    )

    @Bean
    fun permissionResourceMemberService(
        authResourceService: AuthResourceService,
        iamV2ManagerService: V2ManagerService,
        authResourceGroupDao: AuthResourceGroupDao,
        authResourceGroupMemberDao: AuthResourceGroupMemberDao,
        dslContext: DSLContext,
        deptService: DeptService,
        rbacCommonService: RbacCommonService
    ) = RbacPermissionResourceMemberService(
        authResourceService = authResourceService,
        iamV2ManagerService = iamV2ManagerService,
        authResourceGroupDao = authResourceGroupDao,
        authResourceGroupMemberDao = authResourceGroupMemberDao,
        dslContext = dslContext,
        deptService = deptService
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
        superManagerService: SuperManagerService,
        rbacCommonService: RbacCommonService,
        client: Client,
        authProjectUserMetricsService: AuthProjectUserMetricsService
    ) = RbacPermissionService(
        authHelper = authHelper,
        authResourceService = authResourceService,
        iamConfiguration = iamConfiguration,
        policyService = iamV2PolicyService,
        authResourceCodeConverter = authResourceCodeConverter,
        superManagerService = superManagerService,
        rbacCommonService = rbacCommonService,
        client = client,
        authProjectUserMetricsService = authProjectUserMetricsService
    )

    @Bean
    @Primary
    fun rbacPermissionProjectService(
        authHelper: AuthHelper,
        authResourceService: AuthResourceService,
        authResourceGroupDao: AuthResourceGroupDao,
        dslContext: DSLContext,
        rbacCommonService: RbacCommonService,
        resourceGroupMemberService: RbacPermissionResourceMemberService,
        client: Client,
        resourceMemberService: PermissionResourceMemberService,
        permissionManageFacadeService: PermissionManageFacadeService
    ) = RbacPermissionProjectService(
        authHelper = authHelper,
        authResourceService = authResourceService,
        authResourceGroupDao = authResourceGroupDao,
        dslContext = dslContext,
        rbacCommonService = rbacCommonService,
        resourceGroupMemberService = resourceGroupMemberService,
        client = client,
        resourceMemberService = resourceMemberService,
        permissionManageFacadeService = permissionManageFacadeService
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
    fun rbacPermissionApplyService(
        dslContext: DSLContext,
        v2ManagerService: V2ManagerService,
        authResourceService: AuthResourceService,
        authResourceGroupConfigDao: AuthResourceGroupConfigDao,
        authResourceGroupDao: AuthResourceGroupDao,
        rbacCommonService: RbacCommonService,
        config: CommonConfig,
        client: Client,
        authResourceCodeConverter: AuthResourceCodeConverter,
        permissionService: PermissionService,
        itsmService: ItsmService,
        deptService: DeptService,
        authResourceGroupApplyDao: AuthResourceGroupApplyDao
    ) = RbacPermissionApplyService(
        dslContext = dslContext,
        v2ManagerService = v2ManagerService,
        authResourceService = authResourceService,
        authResourceGroupConfigDao = authResourceGroupConfigDao,
        authResourceGroupDao = authResourceGroupDao,
        rbacCommonService = rbacCommonService,
        config = config,
        client = client,
        authResourceCodeConverter = authResourceCodeConverter,
        permissionService = permissionService,
        itsmService = itsmService,
        deptService = deptService,
        authResourceGroupApplyDao = authResourceGroupApplyDao
    )

    @Bean
    @Primary
    fun rbacPermissionResourceValidateService(
        permissionService: PermissionService,
        rbacCommonService: RbacCommonService,
        client: Client,
        authAuthorizationDao: AuthAuthorizationDao,
        dslContext: DSLContext
    ) = RbacPermissionResourceValidateService(
        permissionService = permissionService,
        rbacCommonService = rbacCommonService,
        client = client,
        authAuthorizationDao = authAuthorizationDao,
        dslContext = dslContext
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
        rbacCommonService: RbacCommonService,
        rbacPermissionResourceService: RbacPermissionResourceService,
        migrateCreatorFixService: MigrateCreatorFixService,
        authResourceService: AuthResourceService,
        permissionGradeManagerService: PermissionGradeManagerService,
        permissionResourceGroupPermissionService: PermissionResourceGroupPermissionService,
        migrateResourceCodeConverter: MigrateResourceCodeConverter,
        tokenApi: AuthTokenApi,
        projectAuthServiceCode: ProjectAuthServiceCode,
        dslContext: DSLContext,
        authMigrationDao: AuthMigrationDao,
        authResourceGroupConfigDao: AuthResourceGroupConfigDao,
        authResourceGroupDao: AuthResourceGroupDao
    ) = MigrateResourceService(
        resourceService = resourceService,
        rbacCommonService = rbacCommonService,
        rbacPermissionResourceService = rbacPermissionResourceService,
        migrateCreatorFixService = migrateCreatorFixService,
        authResourceService = authResourceService,
        permissionGradeManagerService = permissionGradeManagerService,
        permissionResourceGroupPermissionService = permissionResourceGroupPermissionService,
        migrateResourceCodeConverter = migrateResourceCodeConverter,
        tokenApi = tokenApi,
        projectAuthServiceCode = projectAuthServiceCode,
        dslContext = dslContext,
        authMigrationDao = authMigrationDao,
        authResourceGroupConfigDao = authResourceGroupConfigDao,
        authResourceGroupDao = authResourceGroupDao
    )

    @Bean
    fun migrateResourceGroupService(
        authResourceService: AuthResourceService,
        dslContext: DSLContext,
        authResourceGroupDao: AuthResourceGroupDao,
        iamV2ManagerService: V2ManagerService
    ) = MigrateResourceGroupService(
        authResourceService = authResourceService,
        dslContext = dslContext,
        authResourceGroupDao = authResourceGroupDao,
        iamV2ManagerService = iamV2ManagerService
    )

    @Bean
    fun migrateIamApiService() = MigrateIamApiService()

    @Bean
    fun migrateResultService(
        permissionService: PermissionService,
        rbacCommonService: RbacCommonService,
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
        rbacCommonService = rbacCommonService,
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
        rbacCommonService: RbacCommonService,
        authMigrationDao: AuthMigrationDao,
        deptService: DeptService,
        permissionResourceGroupPermissionService: PermissionResourceGroupPermissionService,
        permissionResourceMemberService: PermissionResourceMemberService
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
        rbacCommonService = rbacCommonService,
        authMigrationDao = authMigrationDao,
        deptService = deptService,
        permissionResourceGroupPermissionService = permissionResourceGroupPermissionService,
        permissionResourceMemberService = permissionResourceMemberService
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
        rbacCommonService: RbacCommonService,
        authMigrationDao: AuthMigrationDao,
        deptService: DeptService,
        permissionResourceGroupPermissionService: PermissionResourceGroupPermissionService,
        permissionResourceMemberService: PermissionResourceMemberService
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
        rbacCommonService = rbacCommonService,
        authMigrationDao = authMigrationDao,
        deptService = deptService,
        permissionResourceGroupPermissionService = permissionResourceGroupPermissionService,
        permissionResourceMemberService = permissionResourceMemberService
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
        migratePermissionHandoverService: MigratePermissionHandoverService,
        permissionGradeManagerService: PermissionGradeManagerService,
        dslContext: DSLContext,
        authMigrationDao: AuthMigrationDao,
        authMonitorSpaceDao: AuthMonitorSpaceDao,
        permissionResourceMemberService: RbacPermissionResourceMemberService,
        migrateResourceAuthorizationService: MigrateResourceAuthorizationService,
        migrateResourceGroupService: MigrateResourceGroupService
    ) = RbacPermissionMigrateService(
        client = client,
        migrateResourceService = migrateResourceService,
        migrateV3PolicyService = migrateV3PolicyService,
        migrateV0PolicyService = migrateV0PolicyService,
        migrateResultService = migrateResultService,
        permissionResourceService = permissionResourceService,
        authResourceService = authResourceService,
        migrateCreatorFixService = migrateCreatorFixService,
        migratePermissionHandoverService = migratePermissionHandoverService,
        permissionGradeManagerService = permissionGradeManagerService,
        dslContext = dslContext,
        authMigrationDao = authMigrationDao,
        authMonitorSpaceDao = authMonitorSpaceDao,
        permissionResourceMemberService = permissionResourceMemberService,
        migrateResourceAuthorizationService = migrateResourceAuthorizationService,
        migrateResourceGroupService = migrateResourceGroupService
    )

    @Bean
    fun migrateCreatorFixService() = MigrateCreatorFixServiceImpl()

    @Bean
    fun migratePermissionHandoverService(
        permissionResourceMemberService: PermissionResourceMemberService,
        authResourceGroupDao: AuthResourceGroupDao,
        authResourceService: AuthResourceService,
        authResourceGroupMemberService: PermissionResourceMemberService,
        dslContext: DSLContext,
        permissionManageFacadeService: PermissionManageFacadeService
    ) = MigratePermissionHandoverService(
        permissionResourceMemberService = permissionResourceMemberService,
        authResourceGroupDao = authResourceGroupDao,
        authResourceService = authResourceService,
        dslContext = dslContext,
        permissionManageFacadeService = permissionManageFacadeService
    )

    @Bean
    fun permissionHandoverService(
        dslContext: DSLContext,
        handoverOverviewDao: AuthHandoverOverviewDao,
        handoverDetailDao: AuthHandoverDetailDao,
        authorizationDao: AuthAuthorizationDao,
        authResourceGroupDao: AuthResourceGroupDao,
        rbacCommonService: RbacCommonService,
        redisOperation: RedisOperation,
        client: Client,
        config: CommonConfig,
        deptService: DeptService
    ) = RbacPermissionHandoverApplicationService(
        dslContext = dslContext,
        handoverOverviewDao = handoverOverviewDao,
        handoverDetailDao = handoverDetailDao,
        authorizationDao = authorizationDao,
        authResourceGroupDao = authResourceGroupDao,
        rbacCommonService = rbacCommonService,
        redisOperation = redisOperation,
        client = client,
        config = config,
        deptService = deptService
    )

    @Bean
    fun systemService(
        iamConfiguration: IamConfiguration,
        apigwHttpClientServiceImpl: ApigwHttpClientServiceImpl
    ) = SystemServiceImpl(apigwHttpClientServiceImpl, iamConfiguration)

    @Bean
    fun rbacPermissionAuthorizationScopesService(
        authMonitorSpaceService: AuthMonitorSpaceService,
        iamConfiguration: IamConfiguration
    ) = RbacPermissionAuthorizationScopesService(
        authMonitorSpaceService = authMonitorSpaceService,
        iamConfiguration = iamConfiguration
    )

    @Bean
    fun rbacPermissionAuthMonitorSpaceService(
        authMonitorSpaceDao: AuthMonitorSpaceDao,
        dslContext: DSLContext,
        objectMapper: ObjectMapper,
        systemService: SystemService
    ) = RbacPermissionAuthMonitorSpaceService(
        authMonitorSpaceDao = authMonitorSpaceDao,
        dslContext = dslContext,
        objectMapper = objectMapper,
        systemService = systemService
    )

    @Bean
    fun permissionResourceGroupSyncService(
        client: Client,
        dslContext: DSLContext,
        authResourceService: AuthResourceService,
        authResourceGroupDao: AuthResourceGroupDao,
        iamV2ManagerService: V2ManagerService,
        authResourceGroupMemberDao: AuthResourceGroupMemberDao,
        rbacCommonService: RbacCommonService,
        redisOperation: RedisOperation,
        authResourceSyncDao: AuthResourceSyncDao,
        authResourceGroupApplyDao: AuthResourceGroupApplyDao,
        resourceGroupPermissionService: PermissionResourceGroupPermissionService,
        deptService: DeptService
    ) = RbacPermissionResourceGroupSyncService(
        client = client,
        dslContext = dslContext,
        authResourceService = authResourceService,
        authResourceGroupDao = authResourceGroupDao,
        iamV2ManagerService = iamV2ManagerService,
        authResourceGroupMemberDao = authResourceGroupMemberDao,
        rbacCommonService = rbacCommonService,
        redisOperation = redisOperation,
        authResourceSyncDao = authResourceSyncDao,
        authResourceGroupApplyDao = authResourceGroupApplyDao,
        resourceGroupPermissionService = resourceGroupPermissionService,
        deptService = deptService
    )
}
