package com.tencent.devops.auth.provider.sample.service

import com.tencent.devops.auth.pojo.dto.IamGroupIdsQueryConditionDTO
import com.tencent.devops.auth.pojo.vo.GroupDetailsInfoVo
import com.tencent.devops.auth.pojo.vo.MemberGroupCountWithPermissionsVo
import com.tencent.devops.auth.service.iam.PermissionResourceGroupFacadeService
import com.tencent.devops.common.api.model.SQLPage

class SamplePermissionResourceGroupFacadeService : PermissionResourceGroupFacadeService {
    override fun getMemberGroupsDetails(
        projectId: String,
        memberId: String,
        resourceType: String?,
        iamGroupIds: List<Int>?,
        groupName: String?,
        minExpiredAt: Long?,
        maxExpiredAt: Long?,
        relatedResourceType: String?,
        relatedResourceCode: String?,
        action: String?,
        start: Int?,
        limit: Int?
    ): SQLPage<GroupDetailsInfoVo> = SQLPage(0, emptyList())

    override fun getMemberGroupsCount(
        projectCode: String,
        memberId: String,
        groupName: String?,
        minExpiredAt: Long?,
        maxExpiredAt: Long?,
        relatedResourceType: String?,
        relatedResourceCode: String?,
        action: String?
    ): List<MemberGroupCountWithPermissionsVo> = emptyList()

    override fun listIamGroupIdsByConditions(
        condition: IamGroupIdsQueryConditionDTO
    ): List<Int> = emptyList()
}
