/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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
 */

package com.tencent.devops.ai.resources

import com.tencent.devops.ai.agent.auth.AuthAdminMutationTools
import com.tencent.devops.ai.agent.auth.AuthGroupMemberQueryTools
import com.tencent.devops.ai.agent.auth.AuthMemberSelfServiceTools
import com.tencent.devops.ai.agent.auth.AuthPermissionInsightTools
import com.tencent.devops.ai.api.op.OpAiAuthAgentToolsResource
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

/**
 * 运营联调：封装权限 Agent 四类工具，与 Agent 内调用链一致（Client + 操作人 userId）。
 */
@RestResource
class OpAiAuthAgentToolsResourceImpl @Autowired constructor(
    private val client: Client
) : OpAiAuthAgentToolsResource {

    private fun queryTools(userId: String) = AuthGroupMemberQueryTools(client) { userId }

    private fun insightTools(userId: String) = AuthPermissionInsightTools(client) { userId }

    private fun selfTools(userId: String) = AuthMemberSelfServiceTools(client) { userId }

    private fun adminTools(userId: String) = AuthAdminMutationTools(client) { userId }

    private fun dry(dryRun: Boolean?) = dryRun ?: true

    override fun checkRole(userId: String, projectId: String, memberId: String?): Result<String> {
        return Result(queryTools(userId).checkRole(projectId, memberId))
    }

    override fun listGroups(
        userId: String,
        projectId: String,
        resourceType: String?,
        resourceCode: String?,
        groupName: String?,
        page: Int?,
        pageSize: Int?
    ): Result<String> {
        return Result(
            queryTools(userId).listGroups(
                projectId,
                resourceType,
                resourceCode,
                groupName,
                page,
                pageSize
            )
        )
    }

    override fun getGroupPermissionDetail(
        userId: String,
        projectId: String,
        groupId: Int
    ): Result<String> {
        return Result(queryTools(userId).getGroupPermissionDetail(projectId, groupId))
    }

    override fun listGroupMembers(
        userId: String,
        projectId: String,
        resourceType: String?,
        resourceCode: String?,
        iamGroupId: Int?,
        groupCode: String?,
        memberType: String?,
        expiredStatus: String?,
        expireWithinDays: Int?,
        page: Int?,
        pageSize: Int?
    ): Result<String> {
        return Result(
            queryTools(userId).listGroupMembers(
                projectId,
                resourceType,
                resourceCode,
                iamGroupId,
                groupCode,
                memberType,
                expiredStatus,
                expireWithinDays,
                page,
                pageSize
            )
        )
    }

    override fun listProjectMembers(
        userId: String,
        projectId: String,
        userName: String?,
        page: Int?,
        pageSize: Int?
    ): Result<String> {
        return Result(queryTools(userId).listProjectMembers(projectId, userName, page, pageSize))
    }

    override fun getMemberGroupCount(
        userId: String,
        projectId: String,
        memberId: String
    ): Result<String> {
        return Result(queryTools(userId).getMemberGroupCount(projectId, memberId))
    }

    override fun getAllMemberGroups(
        userId: String,
        projectId: String,
        memberId: String,
        resourceType: String?,
        groupName: String?,
        expiredStatus: String?,
        expireWithinDays: Int?,
        relatedResourceType: String?,
        relatedResourceCode: String?,
        action: String?,
        page: Int?,
        pageSize: Int?
    ): Result<String> {
        return Result(
            queryTools(userId).getAllMemberGroups(
                projectId,
                memberId,
                resourceType,
                groupName,
                expiredStatus,
                expireWithinDays,
                relatedResourceType,
                relatedResourceCode,
                action,
                page,
                pageSize
            )
        )
    }

    override fun searchUsers(
        userId: String,
        keyword: String,
        projectId: String?,
        limit: Int?
    ): Result<String> {
        return Result(queryTools(userId).searchUsers(keyword, projectId, limit))
    }

    override fun analyzeUserPermissions(
        userId: String,
        projectId: String,
        memberId: String
    ): Result<String> {
        return Result(insightTools(userId).analyzeUserPermissions(projectId, memberId))
    }

    override fun getResourcePermissionsMatrix(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): Result<String> {
        return Result(
            insightTools(userId).getResourcePermissionsMatrix(projectId, resourceType, resourceCode)
        )
    }

    override fun recommendGroupsForGrant(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String,
        action: String,
        targetUserId: String
    ): Result<String> {
        return Result(
            insightTools(userId).recommendGroupsForGrant(
                projectId,
                resourceType,
                resourceCode,
                action,
                targetUserId
            )
        )
    }

    override fun diagnosePermission(
        userId: String,
        projectId: String,
        memberId: String,
        resourceType: String,
        resourceCode: String,
        action: String
    ): Result<String> {
        return Result(
            insightTools(userId).diagnosePermission(
                projectId,
                memberId,
                resourceType,
                resourceCode,
                action
            )
        )
    }

    override fun comparePermissions(
        userId: String,
        projectId: String,
        userIdA: String,
        userIdB: String,
        resourceType: String?
    ): Result<String> {
        return Result(
            insightTools(userId).comparePermissions(projectId, userIdA, userIdB, resourceType)
        )
    }

    override fun checkAuthorizationHealth(userId: String, projectId: String): Result<String> {
        return Result(insightTools(userId).checkAuthorizationHealth(projectId))
    }

    override fun applyRenewal(
        userId: String,
        projectId: String,
        reason: String,
        groupIds: String?,
        expiredStatus: String?,
        expireWithinDays: Int?,
        renewalDays: Int?,
        dryRun: Boolean?
    ): Result<String> {
        return Result(
            selfTools(userId).applyRenewal(
                projectId,
                reason,
                groupIds,
                expiredStatus,
                expireWithinDays,
                renewalDays,
                dry(dryRun)
            )
        )
    }

    override fun applyPermissions(
        userId: String,
        projectId: String,
        reason: String,
        groupIds: String?,
        resourceType: String?,
        resourceCode: String?,
        action: String?,
        expiredDays: Int?,
        dryRun: Boolean?
    ): Result<String> {
        return Result(
            selfTools(userId).applyPermissions(
                projectId,
                reason,
                groupIds,
                resourceType,
                resourceCode,
                action,
                expiredDays,
                dry(dryRun)
            )
        )
    }

    override fun exitGroups(
        userId: String,
        projectId: String,
        groupIds: String,
        handoverTo: String?,
        dryRun: Boolean?
    ): Result<String> {
        return Result(selfTools(userId).exitGroups(projectId, groupIds, handoverTo, dry(dryRun)))
    }

    override fun exitProject(
        userId: String,
        projectId: String,
        handoverTo: String?,
        dryRun: Boolean?
    ): Result<String> {
        return Result(selfTools(userId).exitProject(projectId, handoverTo, dry(dryRun)))
    }

    override fun checkMemberExitWithRecommendation(
        userId: String,
        projectId: String,
        targetMemberId: String?,
        handoverTo: String?,
        groupIds: String?
    ): Result<String> {
        return Result(
            selfTools(userId).checkMemberExitWithRecommendation(
                projectId,
                targetMemberId,
                handoverTo,
                groupIds
            )
        )
    }

    override fun grantPermissions(
        userId: String,
        projectId: String,
        targetUserIds: String,
        groupId: Int?,
        resourceType: String?,
        resourceCode: String?,
        action: String?,
        expiredDays: Long?,
        dryRun: Boolean?
    ): Result<String> {
        return Result(
            adminTools(userId).grantPermissions(
                projectId,
                targetUserIds,
                groupId,
                resourceType,
                resourceCode,
                action,
                expiredDays,
                dry(dryRun)
            )
        )
    }

    override fun renewPermissions(
        userId: String,
        projectId: String,
        groupIds: String,
        targetMemberId: String,
        renewalDays: Int,
        dryRun: Boolean?
    ): Result<String> {
        return Result(
            adminTools(userId).renewPermissions(
                projectId,
                groupIds,
                targetMemberId,
                renewalDays,
                dry(dryRun)
            )
        )
    }

    override fun revokePermissions(
        userId: String,
        projectId: String,
        groupIds: String,
        targetMemberId: String,
        handoverTo: String?,
        dryRun: Boolean?
    ): Result<String> {
        return Result(
            adminTools(userId).revokePermissions(
                projectId,
                groupIds,
                targetMemberId,
                handoverTo,
                dry(dryRun)
            )
        )
    }

    override fun removeMemberFromProject(
        userId: String,
        projectId: String,
        targetMemberIds: String,
        handoverTo: String?,
        dryRun: Boolean?
    ): Result<String> {
        return Result(
            adminTools(userId).removeMemberFromProject(
                projectId,
                targetMemberIds,
                handoverTo,
                dry(dryRun)
            )
        )
    }

    override fun clonePermissions(
        userId: String,
        projectId: String,
        sourceUserId: String,
        targetUserId: String,
        resourceTypes: String?,
        dryRun: Boolean?
    ): Result<String> {
        return Result(
            adminTools(userId).clonePermissions(
                projectId,
                sourceUserId,
                targetUserId,
                resourceTypes,
                dry(dryRun)
            )
        )
    }
}
