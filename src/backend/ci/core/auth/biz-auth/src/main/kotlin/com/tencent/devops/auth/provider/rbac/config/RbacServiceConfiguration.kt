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

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.dao.AuthActionDao
import com.tencent.devops.auth.dao.AuthItsmCallbackDao
import com.tencent.devops.auth.dao.AuthMonitorSpaceDao
import com.tencent.devops.auth.dao.AuthResourceDao
import com.tencent.devops.auth.dao.AuthResourceGroupConfigDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.dao.AuthResourceTypeDao
import com.tencent.devops.auth.provider.rbac.service.AuthResourceCodeConverter
import com.tencent.devops.auth.provider.rbac.service.AuthResourceNameConverter
import com.tencent.devops.auth.provider.rbac.service.AuthResourceService
import com.tencent.devops.auth.provider.rbac.service.ItsmService
import com.tencent.devops.auth.provider.rbac.service.PermissionGradeManagerService
import com.tencent.devops.auth.provider.rbac.service.PermissionGroupPoliciesService
import com.tencent.devops.auth.provider.rbac.service.PermissionSubsetManagerService
import com.tencent.devops.auth.provider.rbac.service.RbacCacheService
import com.tencent.devops.auth.service.AuthAuthorizationScopesService
import com.tencent.devops.auth.service.AuthProjectUserMetricsService
import com.tencent.devops.auth.service.BkHttpRequestService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupService
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.trace.TraceEventDispatcher
import org.jooq.DSLContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "rbac")
@Suppress("TooManyFunctions", "LongParameterList")
class RbacServiceConfiguration {
    @Bean
    fun rbacPermissionCacheService(
        dslContext: DSLContext,
        authResourceTypeDao: AuthResourceTypeDao,
        authActionDao: AuthActionDao,
        iamV2PolicyService: PolicyService,
        iamConfiguration: IamConfiguration,
        authResourceGroupConfigDao: AuthResourceGroupConfigDao,
        authProjectUserMetricsService: AuthProjectUserMetricsService
    ) = RbacCacheService(
        dslContext = dslContext,
        authResourceTypeDao = authResourceTypeDao,
        authActionDao = authActionDao,
        policyService = iamV2PolicyService,
        iamConfiguration = iamConfiguration,
        authResourceGroupConfigDao = authResourceGroupConfigDao,
        authUserDailyService = authProjectUserMetricsService
    )

    @Bean
    fun permissionSubsetManagerService(
        permissionGroupPoliciesService: PermissionGroupPoliciesService,
        authAuthorizationScopesService: AuthAuthorizationScopesService,
        iamV2ManagerService: V2ManagerService,
        dslContext: DSLContext,
        authResourceGroupDao: AuthResourceGroupDao,
        authResourceGroupConfigDao: AuthResourceGroupConfigDao,
        authResourceNameConverter: AuthResourceNameConverter
    ) = PermissionSubsetManagerService(
        permissionGroupPoliciesService = permissionGroupPoliciesService,
        authAuthorizationScopesService = authAuthorizationScopesService,
        iamV2ManagerService = iamV2ManagerService,
        dslContext = dslContext,
        authResourceGroupDao = authResourceGroupDao,
        authResourceGroupConfigDao = authResourceGroupConfigDao,
        authResourceNameConverter = authResourceNameConverter
    )

    @Bean
    fun permissionGradeManagerService(
        client: Client,
        iamV2ManagerService: V2ManagerService,
        iamConfiguration: IamConfiguration,
        authMonitorSpaceDao: AuthMonitorSpaceDao,
        authItsmCallbackDao: AuthItsmCallbackDao,
        dslContext: DSLContext,
        authResourceService: AuthResourceService,
        authResourceGroupDao: AuthResourceGroupDao,
        authResourceGroupConfigDao: AuthResourceGroupConfigDao,
        traceEventDispatcher: TraceEventDispatcher,
        itsmService: ItsmService,
        authAuthorizationScopesService: AuthAuthorizationScopesService,
        permissionResourceGroupService: PermissionResourceGroupService
    ) = PermissionGradeManagerService(
        client = client,
        iamV2ManagerService = iamV2ManagerService,
        iamConfiguration = iamConfiguration,
        authMonitorSpaceDao = authMonitorSpaceDao,
        authItsmCallbackDao = authItsmCallbackDao,
        dslContext = dslContext,
        authResourceService = authResourceService,
        authResourceGroupDao = authResourceGroupDao,
        authResourceGroupConfigDao = authResourceGroupConfigDao,
        traceEventDispatcher = traceEventDispatcher,
        itsmService = itsmService,
        authAuthorizationScopesService = authAuthorizationScopesService,
        permissionResourceGroupService = permissionResourceGroupService
    )

    @Bean
    fun permissionGroupPoliciesService(
        iamV2ManagerService: V2ManagerService,
        authActionDao: AuthActionDao,
        dslContext: DSLContext,
        authResourceGroupConfigDao: AuthResourceGroupConfigDao,
        authResourceGroupDao: AuthResourceGroupDao,
        authAuthorizationScopesService: AuthAuthorizationScopesService
    ) = PermissionGroupPoliciesService(
        iamV2ManagerService = iamV2ManagerService,
        authActionDao = authActionDao,
        dslContext = dslContext,
        authResourceGroupConfigDao = authResourceGroupConfigDao,
        authResourceGroupDao = authResourceGroupDao,
        authAuthorizationScopesService = authAuthorizationScopesService
    )

    @Bean
    fun itsmService(bkHttpRequestService: BkHttpRequestService) = ItsmService(
        bkHttpRequestService = bkHttpRequestService
    )

    @Bean
    fun authResourceService(
        dslContext: DSLContext,
        authResourceDao: AuthResourceDao,
        authResourceGroupDao: AuthResourceGroupDao
    ) = AuthResourceService(
        dslContext = dslContext,
        authResourceDao = authResourceDao,
        authResourceGroupDao = authResourceGroupDao
    )

    @Bean
    fun authResourceCodeConverter(
        client: Client,
        dslContext: DSLContext,
        authResourceDao: AuthResourceDao
    ) = AuthResourceCodeConverter(
        client = client,
        dslContext = dslContext,
        authResourceDao = authResourceDao
    )

    @Bean
    fun authResourceNameConverter() = AuthResourceNameConverter()
}
