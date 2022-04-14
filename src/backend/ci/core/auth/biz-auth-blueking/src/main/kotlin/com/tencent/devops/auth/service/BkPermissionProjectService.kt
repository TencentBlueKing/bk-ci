package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.devops.auth.service.ci.PermissionRoleService
import com.tencent.devops.auth.service.iam.IamCacheService
import com.tencent.devops.auth.service.ci.PermissionRoleMemberService
import com.tencent.devops.auth.service.ci.impl.AbsPermissionProjectService
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.BluekingV3ProjectAuthServiceCode
import com.tencent.devops.common.client.Client
import org.springframework.beans.factory.annotation.Autowired

@Suppress("LongParameterList")
class BkPermissionProjectService @Autowired constructor(
    override val permissionRoleService: PermissionRoleService,
    override val permissionRoleMemberService: PermissionRoleMemberService,
    override val authHelper: AuthHelper,
    override val policyService: PolicyService,
    override val client: Client,
    override val iamConfiguration: IamConfiguration,
    override val deptService: DeptService,
    override val groupService: AuthGroupService,
    override val iamCacheService: IamCacheService,
    val authProjectApi: AuthProjectApi,
    val projectAuthServiceCode: BluekingV3ProjectAuthServiceCode
) : AbsPermissionProjectService(
    permissionRoleService = permissionRoleService,
    permissionRoleMemberService = permissionRoleMemberService,
    authHelper = authHelper,
    policyService = policyService,
    client = client,
    iamConfiguration = iamConfiguration,
    deptService = deptService,
    groupService = groupService,
    iamCacheService = iamCacheService
) {
    override fun getUserByExt(group: BkAuthGroup, projectCode: String): List<String> {
        return emptyList()
    }

    override fun isProjectUser(userId: String, projectCode: String, group: BkAuthGroup?): Boolean {
        if (userId == "admin") {
            return true
        }
        return authProjectApi.isProjectUser(userId, projectAuthServiceCode, projectCode, group)
    }

    override fun checkProjectManager(userId: String, projectCode: String): Boolean {
        if (userId == "admin") {
            return true
        }
        return super.checkProjectManager(userId, projectCode)
    }
}
