package com.tencent.devops.auth.provider.sample.service

import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.request.ProjectMembersQueryConditionReq
import com.tencent.devops.auth.service.iam.PermissionResourceMemberFacadeService
import com.tencent.devops.common.api.model.SQLPage

class SamplePermissionResourceMemberFacadeService : PermissionResourceMemberFacadeService {
    override fun listProjectMembersByComplexConditions(
        conditionReq: ProjectMembersQueryConditionReq
    ): SQLPage<ResourceMemberInfo> {
        return SQLPage(0, emptyList())
    }
}
