package com.tencent.devops.auth.service.permission.iam

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.devops.auth.service.AuthGroupService
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.ManagerService
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
        groupService: AuthGroupService
    ) = TxPermissionProjectServiceImpl(
        permissionRoleService = permissionRoleService,
        permissionRoleMemberService = permissionRoleMemberService,
        authHelper = authHelper,
        policyService = policyService,
        client = client,
        iamConfiguration = iamConfiguration,
        deptService = deptService,
        groupService = groupService
    )

    @Bean
    fun txPermissionService(
        authHelper: AuthHelper,
        policyService: PolicyService,
        iamConfiguration: IamConfiguration,
        managerService: ManagerService
    ) = TxPermissionServiceImpl(
        authHelper = authHelper,
        policyService = policyService,
        iamConfiguration = iamConfiguration,
        managerService = managerService
    )

    @Bean
    fun managerService(client: Client) = ManagerService(client)
}
