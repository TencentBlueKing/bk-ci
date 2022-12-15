package com.tencent.devops.auth.resources

import com.tencent.bk.sdk.iam.dto.application.ApplicationDTO
import com.tencent.bk.sdk.iam.dto.manager.vo.V2ManagerRoleGroupVO
import com.tencent.devops.auth.api.user.UserAuthApplyResource
import com.tencent.devops.auth.pojo.vo.ActionInfoVo
import com.tencent.devops.auth.pojo.vo.ResourceTypeInfoVo
import com.tencent.devops.auth.service.AuthApplyService
import com.tencent.devops.common.api.pojo.Result
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@RestController
class UserAuthApplyResourceImpl @Autowired constructor(
    authApplyService: AuthApplyService
) : UserAuthApplyResource {
    override fun listResourceTypes(userId: String): Result<List<ResourceTypeInfoVo>> {
        TODO("Not yet implemented")
    }

    override fun listActions(userId: String, resourceType: String?): Result<List<ActionInfoVo>> {
        TODO("Not yet implemented")
    }

    override fun listGroups(userId: String, projectId: String, inherit: Boolean?, actionId: String?, resourceType: String?, resourceCode: String?, bkIamPath: String?, name: String?, description: String?, page: Int, pageSize: Int): Result<V2ManagerRoleGroupVO> {
        TODO("Not yet implemented")
    }

    override fun applyToJoinGroup(userId: String, projectId: String, applicationDTO: ApplicationDTO): Result<Boolean> {
        TODO("Not yet implemented")
    }
}
