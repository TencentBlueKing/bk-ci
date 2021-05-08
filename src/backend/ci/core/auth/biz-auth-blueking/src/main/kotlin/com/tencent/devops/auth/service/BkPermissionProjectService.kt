package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.devops.auth.service.iam.PermissionRoleMemberService
import com.tencent.devops.auth.service.iam.PermissionRoleService
import com.tencent.devops.auth.service.iam.impl.AbsPermissionProjectService
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.client.Client
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BkPermissionProjectService @Autowired constructor(
    val permissionRoleService: PermissionRoleService,
    val permissionRoleMemberService: PermissionRoleMemberService,
    val authHelper: AuthHelper,
    val policyService: PolicyService,
    val client: Client,
    val iamConfiguration: IamConfiguration,
    val deptService: DeptService
) : AbsPermissionProjectService(
    permissionRoleService = permissionRoleService,
    permissionRoleMemberService = permissionRoleMemberService,
    authHelper = authHelper,
    policyService = policyService,
    client = client,
    iamConfiguration = iamConfiguration,
    deptService = deptService
) {
    override fun getProjectUsers(serviceCode: String, projectCode: String, group: BkAuthGroup?): List<String> {
        return super.getProjectUsers(serviceCode, projectCode, group)
    }

    override fun getProjectGroupAndUserList(serviceCode: String, projectCode: String): List<BkAuthGroupAndUserList> {
        return super.getProjectGroupAndUserList(serviceCode, projectCode)
    }

    override fun getUserProjects(userId: String): List<String> {
        return super.getUserProjects(userId)
    }

    override fun isProjectUser(userId: String, projectCode: String, group: BkAuthGroup?): Boolean {
        return super.isProjectUser(userId, projectCode, group)
    }

    override fun createProjectUser(userId: String, projectCode: String, role: String): Boolean {
        return super.createProjectUser(userId, projectCode, role)
    }

    override fun getProjectRoles(projectCode: String, projectId: String): List<BKAuthProjectRolesResources> {
        return super.getProjectRoles(projectCode, projectId)
    }
}
