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

package com.tencent.devops.auth.provider.sample.service

import com.tencent.devops.auth.pojo.AuthResourceGroup
import com.tencent.devops.auth.pojo.AuthResourceGroupMember
import com.tencent.devops.auth.pojo.AuthResourceInfo
import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.dto.IamGroupIdsQueryConditionDTO
import com.tencent.devops.auth.pojo.enum.BatchOperateType
import com.tencent.devops.auth.pojo.request.ai.AiApplyJoinGroupReq
import com.tencent.devops.auth.pojo.request.ai.AiRemoveMemberFromProjectReq
import com.tencent.devops.auth.pojo.request.ai.BatchHandoverMembersReq
import com.tencent.devops.auth.pojo.request.ai.BatchOperateCheckReq
import com.tencent.devops.auth.pojo.request.ai.BatchRemoveMembersReq
import com.tencent.devops.auth.pojo.request.ai.BatchRenewalMembersReq
import com.tencent.devops.auth.pojo.request.ai.GroupRecommendReq
import com.tencent.devops.auth.pojo.vo.AuthorizationHealthVO
import com.tencent.devops.auth.pojo.vo.AuthorizationStatsVO
import com.tencent.devops.auth.pojo.vo.BatchOperateGroupMemberCheckVo
import com.tencent.devops.auth.pojo.vo.GroupDetailsInfoVo
import com.tencent.devops.auth.pojo.dto.ResourceGroupPermissionDTO
import com.tencent.devops.auth.pojo.vo.GroupRecommendationVO
import com.tencent.devops.auth.pojo.vo.MemberExitCheckVO
import com.tencent.devops.auth.pojo.vo.PermissionCloneResultVO
import com.tencent.devops.auth.pojo.vo.PermissionCompareSummaryVO
import com.tencent.devops.auth.pojo.vo.PermissionCompareVO
import com.tencent.devops.auth.pojo.vo.PermissionDiagnoseVO
import com.tencent.devops.auth.pojo.vo.ResolvedUserByNameVO
import com.tencent.devops.auth.pojo.vo.ResourcePermissionsMatrixVO
import com.tencent.devops.auth.pojo.vo.ResourceType2CountVo
import com.tencent.devops.auth.pojo.vo.UserPermissionAnalysisVO
import com.tencent.devops.auth.pojo.vo.UserSearchResultVO
import com.tencent.devops.auth.service.AuthAiService
import com.tencent.devops.common.api.model.SQLPage

/**
 * 开源 / 非 RBAC 模式下的 AI 权限能力占位实现，避免缺少 [AuthAiService] Bean 导致启动失败。
 */
@Suppress("TooManyFunctions")
class SampleAuthAiServiceImpl : AuthAiService {

    override fun diagnosePermission(
        userId: String,
        projectId: String,
        memberId: String,
        resourceType: String,
        resourceCode: String,
        action: String
    ) = PermissionDiagnoseVO(
        hasPermission = false,
        resourceType = resourceType,
        resourceCode = resourceCode,
        resourceName = resourceCode,
        action = action,
        actionName = action,
        missingReason = DISABLED_HINT,
        applicableGroups = emptyList(),
        groupManagers = emptyList(),
        suggestion = DISABLED_HINT
    )

    override fun clonePermissions(
        userId: String,
        projectId: String,
        sourceUserId: String,
        targetUserId: String,
        resourceTypes: List<String>?,
        dryRun: Boolean
    ) = PermissionCloneResultVO(
        sourceUserId = sourceUserId,
        targetUserId = targetUserId,
        dryRun = dryRun,
        groupsToClone = emptyList(),
        skippedGroups = emptyList(),
        successCount = 0,
        failedCount = 0,
        failedDetails = emptyList(),
        summary = DISABLED_HINT
    )

    override fun comparePermissions(
        userId: String,
        projectId: String,
        userIdA: String,
        userIdB: String,
        resourceType: String?
    ) = PermissionCompareVO(
        userIdA = userIdA,
        userNameA = userIdA,
        userIdB = userIdB,
        userNameB = userIdB,
        commonGroups = emptyList(),
        onlyInA = emptyList(),
        onlyInB = emptyList(),
        summary = PermissionCompareSummaryVO(
            totalGroupsA = 0,
            totalGroupsB = 0,
            commonCount = 0,
            onlyInACount = 0,
            onlyInBCount = 0,
            differenceDescription = DISABLED_HINT
        )
    )

    override fun checkAuthorizationHealth(userId: String, projectId: String) = AuthorizationHealthVO(
        projectId = projectId,
        checkTime = System.currentTimeMillis(),
        healthStatus = "healthy",
        riskCount = 0,
        warningCount = 0,
        authorizationStats = AuthorizationStatsVO(),
        risks = emptyList(),
        suggestions = listOf(DISABLED_HINT)
    )

    override fun searchUsers(
        userId: String,
        keyword: String,
        projectId: String?,
        limit: Int
    ) = UserSearchResultVO(
        users = emptyList(),
        totalCount = 0,
        exactMatch = false,
        keyword = keyword
    )

    override fun resolveUsersByName(
        userName: String
    ): List<ResolvedUserByNameVO> = emptyList()

    override fun checkMemberExitWithRecommendation(
        userId: String,
        projectId: String,
        targetMemberId: String,
        handoverTo: String?,
        groupIds: String?,
        recommendLimit: Int
    ) = MemberExitCheckVO(
        canExitDirectly = false,
        needHandover = false,
        hasDepartmentJoined = false,
        suggestion = DISABLED_HINT
    )

    override fun searchResource(
        userId: String,
        projectId: String,
        resourceType: String,
        keyword: String
    ): List<AuthResourceInfo> = emptyList()

    override fun listAuthResourceGroups(
        userId: String,
        projectId: String,
        condition: IamGroupIdsQueryConditionDTO
    ): SQLPage<AuthResourceGroup> = SQLPage(count = 0, records = emptyList())

    override fun getGroupPermissionDetail(
        userId: String,
        projectId: String,
        groupId: Int
    ): List<ResourceGroupPermissionDTO> = emptyList()

    override fun listGroupMembers(
        userId: String,
        projectId: String,
        resourceType: String?,
        resourceCode: String?,
        iamGroupId: Int?,
        groupCode: String?,
        memberId: String?,
        memberType: String?,
        minExpiredAt: Long?,
        maxExpiredAt: Long?,
        page: Int,
        pageSize: Int
    ): SQLPage<AuthResourceGroupMember> = SQLPage(count = 0, records = emptyList())

    override fun getMemberGroupCount(
        userId: String,
        projectId: String,
        memberId: String,
        relatedResourceType: String?,
        relatedResourceCode: String?
    ): List<ResourceType2CountVo> = emptyList()

    override fun getMemberGroupsDetails(
        userId: String,
        projectId: String,
        resourceType: String,
        memberId: String,
        relatedResourceType: String?,
        relatedResourceCode: String?,
        page: Int,
        pageSize: Int
    ): SQLPage<GroupDetailsInfoVo> = SQLPage(count = 0, records = emptyList())

    override fun getAllMemberGroupsDetails(
        userId: String,
        projectId: String,
        memberId: String,
        resourceType: String?,
        iamGroupIds: String?,
        groupName: String?,
        minExpiredAt: Long?,
        maxExpiredAt: Long?,
        relatedResourceType: String?,
        relatedResourceCode: String?,
        action: String?,
        page: Int,
        pageSize: Int
    ): SQLPage<GroupDetailsInfoVo> = SQLPage(count = 0, records = emptyList())

    override fun batchRenewalMembers(
        userId: String,
        projectId: String,
        request: BatchRenewalMembersReq
    ): Boolean = false

    override fun applyRenewalGroupMember(
        userId: String,
        projectId: String,
        groupIds: String,
        renewalDays: Int,
        reason: String
    ): Boolean = false

    override fun batchRemoveMembers(
        userId: String,
        projectId: String,
        request: BatchRemoveMembersReq
    ): Boolean = false

    override fun batchHandoverMembers(
        userId: String,
        projectId: String,
        request: BatchHandoverMembersReq
    ): Boolean = false

    override fun exitGroupsFromPersonal(
        userId: String,
        projectId: String,
        request: BatchRemoveMembersReq
    ): String = "Sample 模式下不支持此操作"

    override fun applyHandoverFromPersonal(
        userId: String,
        projectId: String,
        request: BatchHandoverMembersReq
    ): String = "Sample 模式下不支持此操作"

    override fun batchOperateCheck(
        userId: String,
        projectId: String,
        batchOperateType: BatchOperateType,
        request: BatchOperateCheckReq
    ) = BatchOperateGroupMemberCheckVo(
        totalCount = 0,
        operableCount = 0,
        inoperableCount = 0,
        uniqueManagerCount = 0,
        invalidGroupCount = 0,
        invalidPipelineAuthorizationCount = 0,
        invalidRepositoryAuthorizationCount = 0,
        invalidEnvNodeAuthorizationCount = 0,
        canHandoverCount = 0,
        needToHandover = false
    )

    override fun removeMemberFromProject(
        userId: String,
        projectId: String,
        request: AiRemoveMemberFromProjectReq
    ): List<ResourceMemberInfo> = emptyList()

    override fun analyzeUserPermissions(
        userId: String,
        projectId: String,
        memberId: String
    ) = UserPermissionAnalysisVO(
        role = "unknown",
        roleDisplayName = "—",
        totalGroupCount = 0,
        expiredGroupCount = 0,
        resourceSummary = emptyList(),
        authorizationSummary = emptyList(),
        totalAuthorizationCount = 0,
        hasAllPermissions = false,
        warnings = listOf(DISABLED_HINT)
    )

    override fun getResourcePermissionsMatrix(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ) = ResourcePermissionsMatrixVO(
        resourceName = resourceCode,
        resourceType = resourceType,
        projectId = projectId,
        groups = emptyList(),
        totalGroupCount = 0
    )

    override fun recommendGroupsForGrant(
        userId: String,
        projectId: String,
        request: GroupRecommendReq
    ) = GroupRecommendationVO(
        recommendation = DISABLED_HINT,
        candidateGroups = emptyList()
    )

    override fun applyToJoinGroup(
        userId: String,
        projectId: String,
        request: AiApplyJoinGroupReq
    ): Boolean = false

    private companion object {
        private const val DISABLED_HINT =
            "当前为开源默认权限模式（非 RBAC），AI 权限增强能力未启用。"
    }
}
