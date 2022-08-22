package com.tencent.devops.auth.service.permission.iam

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.devops.auth.refresh.dispatch.AuthRefreshDispatch
import com.tencent.devops.auth.service.AuthGroupService
import com.tencent.devops.auth.service.AuthPipelineIdService
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.ManagerService
import com.tencent.devops.auth.service.iam.IamCacheService
import com.tencent.devops.auth.service.iam.PermissionRoleMemberService
import com.tencent.devops.auth.service.iam.PermissionRoleService
import com.tencent.devops.common.client.Client
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "new_v3")
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class IamAuthConfiguration {

    @Bean
    fun txPermissionProjectService(
        permissionRoleService: PermissionRoleService,
        permissionRoleMemberService: PermissionRoleMemberService,
        authHelper: AuthHelper,
        policyService: PolicyService,
        client: Client,
        iamConfiguration: IamConfiguration,
        deptService: DeptService,
        groupService: AuthGroupService,
        iamCacheService: IamCacheService
    ) = TxPermissionProjectServiceImpl(
        permissionRoleService = permissionRoleService,
        permissionRoleMemberService = permissionRoleMemberService,
        authHelper = authHelper,
        policyService = policyService,
        client = client,
        iamConfiguration = iamConfiguration,
        deptService = deptService,
        groupService = groupService,
        iamCacheService = iamCacheService
    )

    @Bean
    fun txPermissionService(
        authHelper: AuthHelper,
        policyService: PolicyService,
        iamConfiguration: IamConfiguration,
        managerService: ManagerService,
        iamCacheService: IamCacheService,
        client: Client,
        authPipelineIdService: AuthPipelineIdService
    ) = TxPermissionServiceImpl(
        authHelper = authHelper,
        policyService = policyService,
        iamConfiguration = iamConfiguration,
        managerService = managerService,
        iamCacheService = iamCacheService,
        client = client,
        authPipelineIdService = authPipelineIdService
    )

    @Bean
    fun permissionExtService(
        iamConfiguration: IamConfiguration,
        managerService: com.tencent.bk.sdk.iam.service.ManagerService,
        iamCacheService: IamCacheService,
        authRefreshDispatch: AuthRefreshDispatch
    ) = TxPermissionExtServiceImpl(
        iamConfiguration = iamConfiguration,
        managerService = managerService,
        iamCacheService = iamCacheService,
        authRefreshDispatch = authRefreshDispatch
    )

    @Bean
    fun permissionUrlService(
        iamConfiguration: IamConfiguration,
        managerService: com.tencent.bk.sdk.iam.service.ManagerService,
        permissionProjectService: TxPermissionProjectServiceImpl,
        client: Client,
        authGroupService: AuthGroupService
    ) = TxPermissionUrlServiceImpl(
        iamConfiguration = iamConfiguration,
        managerService = managerService,
        permissionProjectService = permissionProjectService,
        client = client,
        authGroupService = authGroupService
    )

    @Bean
    fun managerService(client: Client) = ManagerService(client)
}
