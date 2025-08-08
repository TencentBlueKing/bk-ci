/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.auth.resources.user

import com.tencent.devops.auth.api.user.UserAuthResourceGroupResource
import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.dto.GroupMemberRenewalDTO
import com.tencent.devops.auth.pojo.dto.MemberGroupJoinedDTO
import com.tencent.devops.auth.pojo.dto.RenameGroupDTO
import com.tencent.devops.auth.pojo.enum.JoinedType
import com.tencent.devops.auth.pojo.enum.MemberType
import com.tencent.devops.auth.pojo.enum.OperateChannel
import com.tencent.devops.auth.pojo.request.GroupMemberRemoveConditionReq
import com.tencent.devops.auth.pojo.vo.GroupDetailsInfoVo
import com.tencent.devops.auth.pojo.vo.IamGroupPoliciesVo
import com.tencent.devops.auth.service.iam.PermissionManageFacadeService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupPermissionService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupService
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.auth.service.iam.PermissionResourceValidateService
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.BkManagerCheck
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserAuthResourceGroupResourceImpl @Autowired constructor(
    private val permissionResourceGroupService: PermissionResourceGroupService,
    private val permissionResourceMemberService: PermissionResourceMemberService,
    private val permissionManageFacadeService: PermissionManageFacadeService,
    private val permissionResourceGroupPermissionService: PermissionResourceGroupPermissionService,
    private val permissionResourceValidateService: PermissionResourceValidateService
) : UserAuthResourceGroupResource {
    override fun getGroupPolicies(
        userId: String,
        projectId: String,
        resourceType: String,
        groupId: Int
    ): Result<List<IamGroupPoliciesVo>> {
        return Result(
            permissionResourceGroupPermissionService.getGroupPolices(
                userId = userId,
                projectCode = projectId,
                resourceType = resourceType,
                iamGroupId = groupId
            )
        )
    }

    override fun getMemberGroupsDetails(
        userId: String,
        projectId: String,
        resourceType: String,
        memberId: String,
        groupName: String?,
        minExpiredAt: Long?,
        maxExpiredAt: Long?,
        relatedResourceType: String?,
        relatedResourceCode: String?,
        action: String?,
        operateChannel: OperateChannel?,
        uniqueManagerGroupsQueryFlag: Boolean?,
        start: Int,
        limit: Int
    ): Result<SQLPage<GroupDetailsInfoVo>> {
        permissionResourceValidateService.validateUserProjectPermissionByChannel(
            userId = userId,
            projectCode = projectId,
            operateChannel = operateChannel ?: OperateChannel.MANAGER,
            targetMemberId = memberId
        )

        return Result(
            permissionManageFacadeService.getMemberGroupsDetails(
                projectId = projectId,
                resourceType = resourceType,
                memberId = memberId,
                groupName = groupName,
                minExpiredAt = minExpiredAt,
                maxExpiredAt = maxExpiredAt,
                relatedResourceType = relatedResourceType,
                relatedResourceCode = relatedResourceCode,
                operateChannel = operateChannel,
                action = action,
                uniqueManagerGroupsQueryFlag = uniqueManagerGroupsQueryFlag,
                start = start,
                limit = limit
            )
        )
    }

    override fun getMemberGroupDetails(
        userId: String,
        projectId: String,
        resourceType: String,
        groupId: Int,
        memberId: String
    ): Result<GroupDetailsInfoVo> {
        permissionResourceValidateService.validateUserProjectPermissionByChannel(
            userId = userId,
            projectCode = projectId,
            operateChannel = OperateChannel.PERSONAL,
            targetMemberId = memberId
        )
        return Result(
            permissionManageFacadeService.getMemberGroupsDetails(
                projectId = projectId,
                memberId = memberId,
                resourceType = resourceType,
                iamGroupIds = listOf(groupId),
                operateChannel = OperateChannel.PERSONAL
            ).records.first { it.groupId == groupId && it.joinedType == JoinedType.DIRECT }
        )
    }

    override fun renewal(
        userId: String,
        projectId: String,
        resourceType: String,
        groupId: Int,
        memberRenewalDTO: GroupMemberRenewalDTO
    ): Result<Boolean> {
        permissionResourceValidateService.validateUserProjectPermissionByChannel(
            userId = userId,
            projectCode = projectId,
            operateChannel = OperateChannel.PERSONAL,
            targetMemberId = userId
        )
        return Result(
            permissionResourceMemberService.renewalGroupMember(
                userId = userId,
                projectCode = projectId,
                resourceType = resourceType,
                groupId = groupId,
                memberRenewalDTO = memberRenewalDTO
            )
        )
    }

    override fun deleteMember(
        userId: String,
        projectId: String,
        resourceType: String,
        groupId: Int
    ): Result<Boolean> {
        return Result(
            permissionManageFacadeService.batchDeleteResourceGroupMembersFromManager(
                userId = userId,
                projectCode = projectId,
                removeMemberDTO = GroupMemberRemoveConditionReq(
                    groupIds = listOf(
                        MemberGroupJoinedDTO(
                            id = groupId,
                            memberType = MemberType.USER
                        )
                    ),
                    targetMember = ResourceMemberInfo(
                        id = userId,
                        type = MemberType.USER.type
                    )
                )
            )
        )
    }

    @BkManagerCheck
    override fun deleteGroup(
        userId: String,
        projectId: String,
        resourceType: String,
        groupId: Int
    ): Result<Boolean> {
        return Result(
            permissionResourceGroupService.deleteGroup(
                userId = userId,
                projectId = projectId,
                resourceType = resourceType,
                groupId = groupId
            )
        )
    }

    @BkManagerCheck
    override fun rename(
        userId: String,
        projectId: String,
        resourceType: String,
        groupId: Int,
        renameGroupDTO: RenameGroupDTO
    ): Result<Boolean> {
        return Result(
            permissionResourceGroupService.renameGroup(
                userId = userId,
                projectId = projectId,
                resourceType = resourceType,
                groupId = groupId,
                renameGroupDTO = renameGroupDTO
            )
        )
    }
}
