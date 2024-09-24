package com.tencent.devops.auth.service.iam

import com.tencent.devops.auth.pojo.dto.IamGroupIdsQueryConditionDTO
import com.tencent.devops.auth.pojo.vo.GroupDetailsInfoVo
import com.tencent.devops.auth.pojo.vo.MemberGroupCountWithPermissionsVo
import com.tencent.devops.common.api.model.SQLPage

interface PermissionFacadeService {
    /**
     * 查询成员所在资源用户组详情，直接加入+通过用户组（模板）加入
     * */
    fun getMemberGroupsDetails(
        projectId: String,
        memberId: String,
        resourceType: String? = null,
        iamGroupIds: List<Int>? = null,
        groupName: String? = null,
        minExpiredAt: Long? = null,
        maxExpiredAt: Long? = null,
        relatedResourceType: String? = null,
        relatedResourceCode: String? = null,
        action: String? = null,
        start: Int? = null,
        limit: Int? = null
    ): SQLPage<GroupDetailsInfoVo>

    /**
     * 获取用户有权限的用户组数量
     * */
    fun getMemberGroupsCount(
        projectCode: String,
        memberId: String,
        groupName: String?,
        minExpiredAt: Long?,
        maxExpiredAt: Long?,
        relatedResourceType: String?,
        relatedResourceCode: String?,
        action: String?
    ): List<MemberGroupCountWithPermissionsVo>

    /**
     * 根据条件查询组ID
     * */
    fun listIamGroupIdsByConditions(
        condition: IamGroupIdsQueryConditionDTO
    ): List<Int>
}
