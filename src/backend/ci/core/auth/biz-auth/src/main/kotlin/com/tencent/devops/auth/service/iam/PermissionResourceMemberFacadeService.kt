package com.tencent.devops.auth.service.iam

import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.request.ProjectMembersQueryConditionReq
import com.tencent.devops.common.api.model.SQLPage

interface PermissionResourceMemberFacadeService {
    /**
     * 根据复杂条件进行搜索，用于用户管理界面
     * */
    fun listProjectMembersByComplexConditions(
        conditionReq: ProjectMembersQueryConditionReq
    ): SQLPage<ResourceMemberInfo>
}
