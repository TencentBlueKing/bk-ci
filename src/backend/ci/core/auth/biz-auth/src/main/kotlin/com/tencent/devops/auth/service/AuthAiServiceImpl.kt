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

package com.tencent.devops.auth.service

import com.tencent.devops.auth.dao.AuthAuthorizationDao
import com.tencent.devops.auth.dao.AuthResourceDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.dao.AuthResourceGroupMemberDao
import com.tencent.devops.auth.dao.AuthResourceGroupPermissionDao
import com.tencent.devops.auth.dao.UserInfoDao
import com.tencent.devops.auth.pojo.ApplyJoinGroupInfo
import com.tencent.devops.auth.pojo.AuthResourceGroup
import com.tencent.devops.auth.pojo.AuthResourceGroupMember
import com.tencent.devops.auth.pojo.AuthResourceInfo
import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.dto.IamGroupIdsQueryConditionDTO
import com.tencent.devops.auth.pojo.dto.MemberGroupJoinedDTO
import com.tencent.devops.auth.pojo.dto.ResourceGroupPermissionDTO
import com.tencent.devops.auth.pojo.enum.BatchOperateType
import com.tencent.devops.auth.pojo.enum.MemberType
import com.tencent.devops.auth.pojo.enum.OperateChannel
import com.tencent.devops.auth.pojo.enum.PermissionTagType
import com.tencent.devops.auth.pojo.request.GroupMemberCommonConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberHandoverConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberRemoveConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberRenewalConditionReq
import com.tencent.devops.auth.pojo.request.RemoveMemberFromProjectReq
import com.tencent.devops.auth.pojo.request.ai.AiApplyJoinGroupReq
import com.tencent.devops.auth.pojo.request.ai.AiRemoveMemberFromProjectReq
import com.tencent.devops.auth.pojo.request.ai.BatchHandoverMembersReq
import com.tencent.devops.auth.pojo.request.ai.BatchOperateCheckReq
import com.tencent.devops.auth.pojo.request.ai.BatchRemoveMembersReq
import com.tencent.devops.auth.pojo.request.ai.BatchRenewalMembersReq
import com.tencent.devops.auth.pojo.request.ai.GroupRecommendReq
import com.tencent.devops.auth.pojo.vo.ApplicableGroupVO
import com.tencent.devops.auth.pojo.vo.AuthorizationHealthVO
import com.tencent.devops.auth.pojo.vo.AuthorizationRiskVO
import com.tencent.devops.auth.pojo.vo.AuthorizationStatsVO
import com.tencent.devops.auth.pojo.vo.AuthorizationSummaryVO
import com.tencent.devops.auth.pojo.vo.AuthorizationsToHandoverVO
import com.tencent.devops.auth.pojo.vo.BatchOperateGroupMemberCheckVo
import com.tencent.devops.auth.pojo.vo.CandidateGroupVO
import com.tencent.devops.auth.pojo.vo.CloneFailedDetailVO
import com.tencent.devops.auth.pojo.vo.CompareGroupVO
import com.tencent.devops.auth.pojo.vo.GroupCloneInfoVO
import com.tencent.devops.auth.pojo.vo.GroupDetailsInfoVo
import com.tencent.devops.auth.pojo.vo.GroupRecommendationVO
import com.tencent.devops.auth.pojo.vo.HandoverCandidateVO
import com.tencent.devops.auth.pojo.vo.MemberExitCheckVO
import com.tencent.devops.auth.pojo.vo.PermissionCloneResultVO
import com.tencent.devops.auth.pojo.vo.PermissionCompareSummaryVO
import com.tencent.devops.auth.pojo.vo.PermissionCompareVO
import com.tencent.devops.auth.pojo.vo.PermissionDiagnoseVO
import com.tencent.devops.auth.pojo.vo.ResolvedUserByNameVO
import com.tencent.devops.auth.pojo.vo.PermissionTagVO
import com.tencent.devops.auth.pojo.vo.ResourceGroupMatrixVO
import com.tencent.devops.auth.pojo.vo.ResourcePermissionsMatrixVO
import com.tencent.devops.auth.pojo.vo.ResourceSummaryVO
import com.tencent.devops.auth.pojo.vo.ResourceType2CountVo
import com.tencent.devops.auth.pojo.vo.UserInfoVO
import com.tencent.devops.auth.pojo.vo.UserPermissionAnalysisVO
import com.tencent.devops.auth.pojo.vo.UserSearchResultVO
import com.tencent.devops.auth.provider.rbac.service.RbacCommonService
import com.tencent.devops.auth.service.iam.PermissionApplyService
import com.tencent.devops.auth.service.iam.PermissionManageFacadeService
import com.tencent.devops.auth.service.iam.PermissionProjectService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupPermissionService
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.auth.service.iam.PermissionResourceService
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.ResetAllResourceAuthorizationReq
import com.tencent.devops.model.auth.tables.records.TAuthResourceGroupRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

@Suppress("TooManyFunctions", "LongParameterList")
class AuthAiServiceImpl(
    private val dslContext: DSLContext,
    private val rbacCommonService: RbacCommonService,
    private val permissionResourceService: PermissionResourceService,
    private val permissionResourceMemberService: PermissionResourceMemberService,
    private val permissionResourceGroupPermissionService: PermissionResourceGroupPermissionService,
    private val permissionManageFacadeService: PermissionManageFacadeService,
    private val permissionProjectService: PermissionProjectService,
    private val permissionAuthorizationService: PermissionAuthorizationService,
    private val permissionApplyService: PermissionApplyService,
    private val authResourceDao: AuthResourceDao,
    private val authResourceGroupDao: AuthResourceGroupDao,
    private val authAuthorizationDao: AuthAuthorizationDao,
    private val authResourceGroupMemberDao: AuthResourceGroupMemberDao,
    private val authResourceGroupPermissionDao: AuthResourceGroupPermissionDao,
    private val userInfoDao: UserInfoDao
) : AuthAiService {

    override fun diagnosePermission(
        userId: String,
        projectId: String,
        memberId: String,
        resourceType: String,
        resourceCode: String,
        action: String
    ): PermissionDiagnoseVO {
        requireSelfOrManager(userId, projectId, memberId)

        val resource = permissionResourceService.getResourceByCode(
            projectCode = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
        val resourceName = resource?.resourceName ?: resourceCode

        val actionInfo = rbacCommonService.listResourceType2Action(resourceType)
            .firstOrNull { it.action == action }
        val actionName = actionInfo?.actionName ?: action

        val memberGroups = permissionResourceMemberService.getMemberGroupsInProject(projectId, memberId).toSet()

        val matchingGroupIds = permissionResourceGroupPermissionService.listGroupsByPermissionConditions(
            projectCode = projectId,
            relatedResourceType = resourceType,
            relatedResourceCode = resourceCode,
            action = action
        )

        val hasPermission = matchingGroupIds.any { it in memberGroups }

        if (hasPermission) {
            return PermissionDiagnoseVO(
                hasPermission = true,
                resourceType = resourceType,
                resourceCode = resourceCode,
                resourceName = resourceName,
                action = action,
                actionName = actionName,
                suggestion = "用户已拥有该权限"
            )
        }

        val applicableGroups = if (matchingGroupIds.isNotEmpty()) {
            val groups = authResourceGroupDao.listByRelationId(
                dslContext = dslContext,
                projectCode = projectId,
                iamGroupIds = matchingGroupIds.map { it.toString() }
            )
            val permissionsByGroup = authResourceGroupPermissionDao.listByGroupIds(
                dslContext = dslContext,
                projectCode = projectId,
                iamGroupIds = matchingGroupIds
            ).groupBy { it.iamGroupId }
            groups.map { group ->
                val groupPermissions = permissionsByGroup[group.relationId.toInt()] ?: emptyList()
                val permissions = groupPermissions.map { perm ->
                    runCatching { rbacCommonService.getActionInfo(perm.action).actionName }
                        .getOrElse { perm.action }
                }.distinct()
                val tags = buildRecommendationTags(group.resourceType, resourceType)
                ApplicableGroupVO(
                    groupId = group.relationId.toInt(),
                    groupName = group.groupName,
                    managementLevel = resolveManagementLevel(group.resourceType),
                    managementScope = group.resourceName,
                    permissions = permissions,
                    tags = tags
                )
            }.sortedBy { levelSortOrder(it.managementLevel) }
        } else {
            emptyList()
        }

        return PermissionDiagnoseVO(
            hasPermission = false,
            resourceType = resourceType,
            resourceCode = resourceCode,
            resourceName = resourceName,
            action = action,
            actionName = actionName,
            missingReason = "用户未加入包含该权限的用户组",
            applicableGroups = applicableGroups,
            groupManagers = emptyList(),
            suggestion = if (applicableGroups.isNotEmpty()) {
                "建议申请加入「${applicableGroups.first().groupName}」用户组"
            } else {
                "暂无可申请的用户组，请联系项目管理员"
            }
        )
    }

    override fun clonePermissions(
        userId: String,
        projectId: String,
        sourceUserId: String,
        targetUserId: String,
        resourceTypes: List<String>?,
        dryRun: Boolean
    ): PermissionCloneResultVO {
        val sourceGroups = permissionManageFacadeService.getMemberGroupsDetails(
            projectId = projectId,
            resourceType = null,
            memberId = sourceUserId,
            operateChannel = OperateChannel.MANAGER,
            start = 0,
            limit = MAX_CLONE_GROUPS
        ).records

        val filteredGroups = if (resourceTypes.isNullOrEmpty()) {
            sourceGroups
        } else {
            sourceGroups.filter { it.resourceType in resourceTypes }
        }

        val targetGroupIds = permissionResourceMemberService
            .getMemberGroupsInProject(projectId, targetUserId)
            .toSet()

        val groupsToClone = filteredGroups.filter { it.groupId !in targetGroupIds }
        val skippedGroups = filteredGroups.filter { it.groupId in targetGroupIds }

        val allGroupIds = filteredGroups.map { it.groupId }
        val permissionsByGroup = authResourceGroupPermissionDao.listByGroupIds(
            dslContext = dslContext,
            projectCode = projectId,
            iamGroupIds = allGroupIds
        ).groupBy { it.iamGroupId }

        val groupsToCloneVO = groupsToClone.map { group ->
            val permissions = (permissionsByGroup[group.groupId] ?: emptyList()).map { it.action }
            GroupCloneInfoVO(
                groupId = group.groupId,
                groupName = group.groupName,
                resourceType = group.resourceType,
                resourceName = group.resourceName,
                permissions = permissions
            )
        }

        val skippedGroupsVO = skippedGroups.map { group ->
            val permissions = (permissionsByGroup[group.groupId] ?: emptyList()).map { it.action }
            GroupCloneInfoVO(
                groupId = group.groupId,
                groupName = group.groupName,
                resourceType = group.resourceType,
                resourceName = group.resourceName,
                permissions = permissions
            )
        }

        if (dryRun) {
            return PermissionCloneResultVO(
                sourceUserId = sourceUserId,
                targetUserId = targetUserId,
                dryRun = true,
                groupsToClone = groupsToCloneVO,
                skippedGroups = skippedGroupsVO,
                summary = "预检查完成：将克隆 ${groupsToClone.size} 个用户组，" +
                        "跳过 ${skippedGroups.size} 个（目标用户已有）"
            )
        }

        var successCount = 0
        val failedDetails = mutableListOf<CloneFailedDetailVO>()

        groupsToClone.forEach { group ->
            try {
                val expiredTime = System.currentTimeMillis() / 1000 + TimeUnit.DAYS.toSeconds(DEFAULT_EXPIRED_DAYS)
                permissionResourceMemberService.batchAddResourceGroupMembers(
                    projectCode = projectId,
                    iamGroupId = group.groupId,
                    expiredTime = expiredTime,
                    members = listOf(targetUserId),
                    departments = emptyList()
                )
                successCount++
            } catch (e: Exception) {
                logger.warn("Failed to clone group ${group.groupId} to $targetUserId", e)
                failedDetails.add(
                    CloneFailedDetailVO(
                        groupId = group.groupId,
                        groupName = group.groupName,
                        reason = e.message ?: "未知错误"
                    )
                )
            }
        }

        return PermissionCloneResultVO(
            sourceUserId = sourceUserId,
            targetUserId = targetUserId,
            dryRun = false,
            groupsToClone = groupsToCloneVO,
            skippedGroups = skippedGroupsVO,
            successCount = successCount,
            failedCount = failedDetails.size,
            failedDetails = failedDetails,
            summary = "克隆完成：成功 $successCount 个，失败 ${failedDetails.size} 个，" +
                    "跳过 ${skippedGroups.size} 个"
        )
    }

    override fun comparePermissions(
        userId: String,
        projectId: String,
        userIdA: String,
        userIdB: String,
        resourceType: String?
    ): PermissionCompareVO {
        val groupsA = permissionManageFacadeService.getMemberGroupsDetails(
            projectId = projectId,
            resourceType = resourceType,
            memberId = userIdA,
            operateChannel = OperateChannel.MANAGER,
            start = 0,
            limit = MAX_COMPARE_GROUPS
        ).records

        val groupsB = permissionManageFacadeService.getMemberGroupsDetails(
            projectId = projectId,
            resourceType = resourceType,
            memberId = userIdB,
            operateChannel = OperateChannel.MANAGER,
            start = 0,
            limit = MAX_COMPARE_GROUPS
        ).records

        val groupIdsA = groupsA.map { it.groupId }.toSet()
        val groupIdsB = groupsB.map { it.groupId }.toSet()

        val commonIds = groupIdsA.intersect(groupIdsB)
        val onlyInAIds = groupIdsA - groupIdsB
        val onlyInBIds = groupIdsB - groupIdsA

        val resourceTypeNames = rbacCommonService.listResourceTypes()
            .associateBy({ it.resourceType }, { it.name })

        val allGroupIds = (groupIdsA + groupIdsB).toList()
        val permissionsByGroup = authResourceGroupPermissionDao.listByGroupIds(
            dslContext = dslContext,
            projectCode = projectId,
            iamGroupIds = allGroupIds
        ).groupBy { it.iamGroupId }

        fun toCompareGroupVO(group: GroupDetailsInfoVo): CompareGroupVO {
            val permissions = (permissionsByGroup[group.groupId] ?: emptyList()).map { it.action }
            return CompareGroupVO(
                groupId = group.groupId,
                groupName = group.groupName,
                resourceType = group.resourceType,
                resourceTypeName = resourceTypeNames[group.resourceType] ?: group.resourceType,
                resourceName = group.resourceName,
                permissions = permissions
            )
        }

        val commonGroups = groupsA.filter { it.groupId in commonIds }.map { toCompareGroupVO(it) }
        val onlyInA = groupsA.filter { it.groupId in onlyInAIds }.map { toCompareGroupVO(it) }
        val onlyInB = groupsB.filter { it.groupId in onlyInBIds }.map { toCompareGroupVO(it) }

        val differenceDescription = when {
            onlyInA.isEmpty() && onlyInB.isEmpty() -> "两个用户的权限完全一致"
            onlyInA.isEmpty() -> "$userIdB 比 $userIdA 多 ${onlyInB.size} 个用户组"
            onlyInB.isEmpty() -> "$userIdA 比 $userIdB 多 ${onlyInA.size} 个用户组"
            else -> "$userIdA 独有 ${onlyInA.size} 个用户组，$userIdB 独有 ${onlyInB.size} 个用户组"
        }

        return PermissionCompareVO(
            userIdA = userIdA,
            userNameA = userIdA,
            userIdB = userIdB,
            userNameB = userIdB,
            commonGroups = commonGroups,
            onlyInA = onlyInA,
            onlyInB = onlyInB,
            summary = PermissionCompareSummaryVO(
                totalGroupsA = groupsA.size,
                totalGroupsB = groupsB.size,
                commonCount = commonGroups.size,
                onlyInACount = onlyInA.size,
                onlyInBCount = onlyInB.size,
                differenceDescription = differenceDescription
            )
        )
    }

    override fun checkAuthorizationHealth(
        userId: String,
        projectId: String
    ): AuthorizationHealthVO {
        val authCountByType = authAuthorizationDao.countByResourceTypeInProject(
            dslContext = dslContext,
            projectCode = projectId
        )

        val pipelineCount = authCountByType["pipeline"]?.toInt() ?: 0
        val repertoryCount = authCountByType["repertory"]?.toInt() ?: 0
        val envNodeCount = authCountByType["env_node"]?.toInt() ?: 0

        val uniqueManagerGroups = authResourceGroupMemberDao.listProjectUniqueManagerGroups(
            dslContext = dslContext,
            projectCode = projectId,
            iamGroupIds = emptyList()
        )
        val uniqueManagerCount = uniqueManagerGroups.size

        val authorizerCount = authAuthorizationDao.countDistinctAuthorizersInProject(
            dslContext = dslContext,
            projectCode = projectId
        )

        val risks = mutableListOf<AuthorizationRiskVO>()
        val suggestions = mutableListOf<String>()

        if (uniqueManagerCount > 0) {
            risks.add(
                AuthorizationRiskVO(
                    level = "warning",
                    riskType = "unique_manager",
                    description = "项目中有 $uniqueManagerCount 个用户组仅有一个管理员，若该管理员离开将导致无人管理",
                    suggestion = "建议为这些用户组添加备用管理员"
                )
            )
            suggestions.add("为唯一管理员组添加备用管理员")
        }

        val totalAuthorizations = pipelineCount + repertoryCount + envNodeCount
        if (authorizerCount == 1 && totalAuthorizations > 0) {
            risks.add(
                AuthorizationRiskVO(
                    level = "warning",
                    riskType = "single_authorizer",
                    description = "项目中所有 $totalAuthorizations 条授权都集中在一个人身上，存在单点风险",
                    suggestion = "建议分散授权，避免单点依赖"
                )
            )
            suggestions.add("分散授权人，避免单点风险")
        }

        if (totalAuthorizations == 0) {
            suggestions.add("项目暂无授权记录，状态健康")
        }

        val healthStatus = when {
            risks.any { it.level == "critical" } -> "critical"
            risks.any { it.level == "warning" } -> "warning"
            else -> "healthy"
        }

        return AuthorizationHealthVO(
            projectId = projectId,
            checkTime = System.currentTimeMillis(),
            healthStatus = healthStatus,
            riskCount = risks.count { it.level == "critical" },
            warningCount = risks.count { it.level == "warning" },
            authorizationStats = AuthorizationStatsVO(
                pipelineAuthorizationCount = pipelineCount,
                repertoryAuthorizationCount = repertoryCount,
                envNodeAuthorizationCount = envNodeCount,
                uniqueManagerGroupCount = uniqueManagerCount,
                totalAuthorizerCount = authorizerCount
            ),
            risks = risks,
            suggestions = suggestions
        )
    }

    override fun searchUsers(
        userId: String,
        keyword: String,
        projectId: String?,
        limit: Int
    ): UserSearchResultVO {
        val users = if (projectId != null) {
            val members = permissionResourceMemberService.listProjectMembers(
                projectCode = projectId,
                memberType = null,
                userName = keyword,
                deptName = null,
                departedFlag = false,
                page = 1,
                pageSize = limit
            )
            members.records.map { member ->
                UserInfoVO(
                    userId = member.id,
                    displayName = member.name ?: member.id,
                    isProjectMember = true
                )
            }
        } else {
            emptyList()
        }

        val exactMatch = users.any { it.userId.equals(keyword, ignoreCase = true) }

        return UserSearchResultVO(
            users = users,
            totalCount = users.size,
            exactMatch = exactMatch,
            keyword = keyword
        )
    }

    override fun resolveUsersByName(
        userName: String
    ): List<ResolvedUserByNameVO> {
        return userInfoDao.listByUserName(dslContext, userName).map {
            ResolvedUserByNameVO(
                userId = it.userId,
                userName = it.userName,
                departmentName = it.departmentName,
                enabled = it.enabled,
                departed = it.departed
            )
        }
    }

    override fun checkMemberExitWithRecommendation(
        userId: String,
        projectId: String,
        targetMemberId: String,
        handoverTo: String?,
        groupIds: String?,
        recommendLimit: Int
    ): MemberExitCheckVO {
        val groupIdList = groupIds?.split(",")?.mapNotNull {
            it.trim().toIntOrNull()
        }?.takeIf { it.isNotEmpty() }
        requireSelfOrManager(userId, projectId, targetMemberId)

        val authorizationsToHandover: AuthorizationsToHandoverVO
        val hasDepartmentJoined: Boolean
        val departments: String?
        val canExitDirectly: Boolean

        if (groupIdList.isNullOrEmpty()) {
            val checkResult = permissionManageFacadeService.checkMemberExitsProject(
                projectCode = projectId,
                userId = targetMemberId
            )
            hasDepartmentJoined = (checkResult.departmentJoinedCount ?: 0) > 0
            departments = checkResult.departments
            canExitDirectly = (checkResult.uniqueManagerCount ?: 0) == 0 &&
                    (checkResult.pipelineAuthorizationCount ?: 0) == 0 &&
                    (checkResult.repositoryAuthorizationCount ?: 0) == 0 &&
                    (checkResult.envNodeAuthorizationCount ?: 0) == 0
            authorizationsToHandover = AuthorizationsToHandoverVO(
                pipeline = checkResult.pipelineAuthorizationCount ?: 0,
                repertory = checkResult.repositoryAuthorizationCount ?: 0,
                envNode = checkResult.envNodeAuthorizationCount ?: 0,
                uniqueManagerGroups = checkResult.uniqueManagerCount ?: 0
            )
        } else {
            val groupCheckResult = permissionManageFacadeService.batchOperateGroupMembersCheck(
                userId = userId,
                projectCode = projectId,
                batchOperateType = BatchOperateType.HANDOVER,
                conditionReq = GroupMemberCommonConditionReq(
                    groupIds = groupIdList.map { MemberGroupJoinedDTO(id = it, memberType = MemberType.USER) },
                    allSelection = false,
                    targetMember = ResourceMemberInfo(id = targetMemberId, type = MemberType.USER.type)
                )
            )
            hasDepartmentJoined = false
            departments = null
            canExitDirectly = groupCheckResult.needToHandover != true
            authorizationsToHandover = AuthorizationsToHandoverVO(
                pipeline = groupCheckResult.invalidPipelineAuthorizationCount ?: 0,
                repertory = groupCheckResult.invalidRepositoryAuthorizationCount ?: 0,
                envNode = groupCheckResult.invalidEnvNodeAuthorizationCount ?: 0,
                uniqueManagerGroups = groupCheckResult.uniqueManagerCount ?: 0
            )
        }

        if (canExitDirectly) {
            val suggestion = buildString {
                if (groupIdList.isNullOrEmpty()) {
                    append("该成员无需交接，可以直接退出项目")
                } else {
                    append("这些用户组无需交接，可以直接退出")
                }
                if (hasDepartmentJoined) {
                    append("。注意：该成员通过组织（$departments）加入项目，退出后仍可能保留组织带来的权限")
                }
            }
            return MemberExitCheckVO(
                canExitDirectly = true,
                needHandover = false,
                hasDepartmentJoined = hasDepartmentJoined,
                departments = departments,
                suggestion = suggestion
            )
        }

        val hasPendingAuthorizations = authorizationsToHandover.pipeline > 0 ||
                authorizationsToHandover.repertory > 0 ||
                authorizationsToHandover.envNode > 0

        val recommendedCandidates = buildRecommendedCandidates(
            userId = userId,
            projectId = projectId,
            targetMemberId = targetMemberId,
            authorizationsToHandover = authorizationsToHandover,
            hasPendingAuthorizations = hasPendingAuthorizations,
            limit = recommendLimit
        )

        if (handoverTo == null) {
            val baseSuggestion = buildRecommendationSuggestion(recommendedCandidates)
            val suggestion = buildString {
                if (groupIdList.isNullOrEmpty()) {
                    append("该成员需要交接授权才能退出。")
                } else {
                    append("这些用户组需要交接才能退出。")
                }
                append(baseSuggestion)
                if (hasDepartmentJoined) {
                    append("\n注意：该成员通过组织（$departments）加入项目，退出后仍可能保留组织带来的权限。")
                }
            }

            return MemberExitCheckVO(
                canExitDirectly = false,
                needHandover = true,
                hasDepartmentJoined = hasDepartmentJoined,
                departments = departments,
                authorizationsToHandover = authorizationsToHandover,
                recommendedCandidates = recommendedCandidates,
                suggestion = suggestion
            )
        }

        val specifiedCandidate = recommendedCandidates.find { it.userId == handoverTo }

        val handoverToCanReceiveAll: Boolean
        val handoverToCanReceive: List<String>
        val handoverToCannotReceive: List<String>
        val handoverToCannotReceiveReasons: Map<String, String>

        if (specifiedCandidate != null) {
            handoverToCanReceiveAll = specifiedCandidate.canReceiveAll
            handoverToCanReceive = specifiedCandidate.canReceive
            handoverToCannotReceive = specifiedCandidate.cannotReceive
            handoverToCannotReceiveReasons = specifiedCandidate.cannotReceiveReasons
        } else {
            val validationResult = validateHandoverCandidate(
                userId = userId,
                projectId = projectId,
                targetMemberId = targetMemberId,
                candidateId = handoverTo,
                authorizationsToHandover = authorizationsToHandover
            )
            handoverToCanReceiveAll = validationResult.canReceiveAll
            handoverToCanReceive = validationResult.canReceive
            handoverToCannotReceive = validationResult.cannotReceive
            handoverToCannotReceiveReasons = validationResult.cannotReceiveReasons
        }

        val filteredCandidates = if (!handoverToCanReceiveAll) {
            recommendedCandidates.filter { it.userId != handoverTo }
        } else {
            emptyList()
        }

        val suggestion = buildString {
            if (handoverToCanReceiveAll) {
                append("交接人 $handoverTo 可以接收全部授权，可以继续执行")
                if (groupIdList.isNullOrEmpty()) append("退出操作") else append("交接操作")
            } else {
                val cannotReceiveNames = handoverToCannotReceive.map { type ->
                    when (type) {
                        "pipeline" -> "流水线"
                        "repertory" -> "代码库"
                        "envNode" -> "环境节点"
                        else -> type
                    }
                }
                append("交接人 $handoverTo 无法接收部分授权（${cannotReceiveNames.joinToString("、")}）")
                if (filteredCandidates.isNotEmpty()) {
                    val bestCandidate = filteredCandidates.firstOrNull { it.canReceiveAll }
                    if (bestCandidate != null) {
                        append("。推荐更换为 ${bestCandidate.displayName}，可接收全部授权")
                    } else {
                        append("。建议先在对应平台处理无法接收的授权，或选择其他交接人")
                    }
                }
            }
            if (hasDepartmentJoined) {
                append("\n注意：该成员通过组织（$departments）加入项目，退出后仍可能保留组织带来的权限。")
            }
        }

        return MemberExitCheckVO(
            canExitDirectly = false,
            needHandover = true,
            hasDepartmentJoined = hasDepartmentJoined,
            departments = departments,
            authorizationsToHandover = authorizationsToHandover,
            specifiedHandoverTo = handoverTo,
            handoverToCanReceiveAll = handoverToCanReceiveAll,
            handoverToCanReceive = handoverToCanReceive,
            handoverToCannotReceive = handoverToCannotReceive,
            handoverToCannotReceiveReasons = handoverToCannotReceiveReasons,
            recommendedCandidates = filteredCandidates,
            suggestion = suggestion
        )
    }

    override fun searchResource(
        userId: String,
        projectId: String,
        resourceType: String,
        keyword: String
    ): List<AuthResourceInfo> {
        val records = authResourceDao.searchResource(
            dslContext = dslContext,
            projectCode = projectId,
            resourceType = resourceType,
            keyword = keyword
        )
        return records.map { authResourceDao.convert(it) }
    }

    override fun listAuthResourceGroups(
        userId: String,
        projectId: String,
        condition: IamGroupIdsQueryConditionDTO
    ): SQLPage<AuthResourceGroup> {
        // 1. 根据条件查出用户组id
        val iamGroupIds = permissionManageFacadeService.listIamGroupIdsByConditions(condition)
        if (iamGroupIds.isEmpty()) {
            return SQLPage(count = 0, records = emptyList())
        }
        val total = iamGroupIds.size.toLong()
        // 2. 排序
        val sortedIds = iamGroupIds.sorted()
        // 3. 内存分页
        val page = condition.page ?: 1
        val pageSize = (condition.pageSize ?: 50).coerceIn(1, 200)
        val pagedIds = sortedIds.drop((page - 1) * pageSize).take(pageSize)
        if (pagedIds.isEmpty()) {
            return SQLPage(count = total, records = emptyList())
        }
        // 4. 根据组id列表查询组详情
        val records = authResourceGroupDao.listByRelationId(
            dslContext = dslContext,
            projectCode = projectId,
            iamGroupIds = pagedIds.map { it.toString() }
        ).mapNotNull { authResourceGroupDao.convert(it) }
            .sortedBy { it.relationId }
        // 5. 返回
        return SQLPage(count = total, records = records)
    }

    override fun getGroupPermissionDetail(
        userId: String,
        projectId: String,
        groupId: Int
    ): List<ResourceGroupPermissionDTO> {
        authResourceGroupDao.get(
            dslContext = dslContext,
            projectCode = projectId,
            relationId = groupId.toString()
        ) ?: return emptyList()
        val rows = authResourceGroupPermissionDao.listByGroupId(
            dslContext = dslContext,
            projectCode = projectId,
            iamGroupId = groupId
        )
        if (rows.isEmpty()) return emptyList()
        // 批量获取所有 action 的显示名，避免循环中多次查询
        val actions = rows.map { it.action }.distinct()
        val actionNameMap = actions.associateWith { action ->
            runCatching { rbacCommonService.getActionInfo(action).actionName }.getOrNull()
        }
        return rows.map { perm ->
            perm.copy(action = actionNameMap[perm.action] ?: perm.action)
        }
    }

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
    ): SQLPage<AuthResourceGroupMember> {
        val isAdmin = permissionProjectService.checkProjectManager(userId, projectId)
        if (!isAdmin) {
            throw PermissionForbiddenException("非管理员不能查看用户组成员列表")
        }
        val minExpiredTime = minExpiredAt?.let { DateTimeUtil.convertTimestampToLocalDateTime(it / 1000) }
        val maxExpiredTime = maxExpiredAt?.let { DateTimeUtil.convertTimestampToLocalDateTime(it / 1000) }
        val offset = (page - 1) * pageSize
        val total = authResourceGroupMemberDao.countResourceGroupMember(
            dslContext = dslContext,
            projectCode = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode,
            iamGroupId = iamGroupId,
            groupCode = groupCode,
            memberId = memberId,
            memberType = memberType,
            minExpiredTime = minExpiredTime,
            maxExpiredTime = maxExpiredTime
        )
        if (total == 0L) {
            return SQLPage(count = 0, records = emptyList())
        }
        val records = authResourceGroupMemberDao.listResourceGroupMember(
            dslContext = dslContext,
            projectCode = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode,
            iamGroupId = iamGroupId,
            groupCode = groupCode,
            memberId = memberId,
            memberType = memberType,
            minExpiredTime = minExpiredTime,
            maxExpiredTime = maxExpiredTime,
            limit = pageSize,
            offset = offset
        )
        return SQLPage(count = total, records = records)
    }

    override fun getMemberGroupCount(
        userId: String,
        projectId: String,
        memberId: String,
        relatedResourceType: String?,
        relatedResourceCode: String?
    ): List<ResourceType2CountVo> {
        val isAdmin = permissionProjectService.checkProjectManager(userId, projectId)
        if (!isAdmin && userId != memberId) {
            throw PermissionForbiddenException("非管理员不能查看其他成员的用户组数量")
        }
        val channel = if (isAdmin) OperateChannel.MANAGER else OperateChannel.PERSONAL
        return permissionManageFacadeService.getMemberGroupsCount(
            projectCode = projectId,
            memberId = memberId,
            groupName = null,
            minExpiredAt = null,
            maxExpiredAt = null,
            relatedResourceType = relatedResourceType,
            relatedResourceCode = relatedResourceCode,
            action = null,
            operateChannel = channel,
            uniqueManagerGroupsQueryFlag = null
        )
    }

    override fun getMemberGroupsDetails(
        userId: String,
        projectId: String,
        resourceType: String,
        memberId: String,
        relatedResourceType: String?,
        relatedResourceCode: String?,
        page: Int,
        pageSize: Int
    ): SQLPage<GroupDetailsInfoVo> {
        val isAdmin = permissionProjectService.checkProjectManager(userId, projectId)
        if (!isAdmin && userId != memberId) {
            throw PermissionForbiddenException("非管理员不能查看其他成员的用户组详情")
        }
        val channel = if (isAdmin) OperateChannel.MANAGER else OperateChannel.PERSONAL
        return permissionManageFacadeService.getMemberGroupsDetails(
            projectId = projectId,
            resourceType = resourceType,
            memberId = memberId,
            relatedResourceType = relatedResourceType,
            relatedResourceCode = relatedResourceCode,
            operateChannel = channel,
            start = (page - 1) * pageSize,
            limit = pageSize
        )
    }

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
    ): SQLPage<GroupDetailsInfoVo> {
        val isAdmin = permissionProjectService.checkProjectManager(userId, projectId)
        if (!isAdmin && userId != memberId) {
            throw PermissionForbiddenException("非管理员不能查看其他成员的用户组详情")
        }
        val channel = if (isAdmin) OperateChannel.MANAGER else OperateChannel.PERSONAL
        val parsedIamGroupIds = iamGroupIds
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.takeIf { it.isNotEmpty() }
        return permissionManageFacadeService.getMemberGroupsDetails(
            projectId = projectId,
            resourceType = resourceType,
            memberId = memberId,
            iamGroupIds = parsedIamGroupIds,
            groupName = groupName,
            minExpiredAt = minExpiredAt,
            maxExpiredAt = maxExpiredAt,
            relatedResourceType = relatedResourceType,
            relatedResourceCode = relatedResourceCode,
            action = action,
            operateChannel = channel,
            start = (page - 1) * pageSize,
            limit = pageSize
        )
    }

    override fun batchRenewalMembers(
        userId: String,
        projectId: String,
        request: BatchRenewalMembersReq
    ): Boolean {
        val channel = resolveOperateChannel(userId, projectId, request.targetMemberId)
        return permissionManageFacadeService.batchRenewalGroupMembersFromManager(
            userId = userId,
            projectCode = projectId,
            renewalConditionReq = GroupMemberRenewalConditionReq(
                groupIds = request.groupIds.toGroupJoinedDTOs(),
                targetMember = request.targetMemberId.toResourceMemberInfo(),
                operateChannel = channel,
                renewalDuration = request.renewalDuration
            )
        )
    }

    override fun applyRenewalGroupMember(
        userId: String,
        projectId: String,
        groupIds: String,
        renewalDays: Int,
        reason: String
    ): Boolean {
        val groupIdList = groupIds.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
        if (groupIdList.isEmpty()) {
            throw OperationException("用户组ID列表不能为空")
        }
        val nowSec = System.currentTimeMillis() / 1000
        val expiredAt = nowSec + TimeUnit.DAYS.toSeconds(renewalDays.toLong())
        return permissionApplyService.applyToJoinGroup(
            userId = userId,
            applyJoinGroupInfo = ApplyJoinGroupInfo(
                projectCode = projectId,
                groupIds = groupIdList,
                expiredAt = expiredAt.toString(),
                applicant = userId,
                reason = reason
            )
        )
    }

    override fun batchRemoveMembers(
        userId: String,
        projectId: String,
        request: BatchRemoveMembersReq
    ): Boolean {
        val channel = resolveOperateChannel(userId, projectId, request.targetMemberId)
        return permissionManageFacadeService.batchDeleteResourceGroupMembersFromManager(
            userId = userId,
            projectCode = projectId,
            removeMemberDTO = GroupMemberRemoveConditionReq(
                groupIds = request.groupIds.toGroupJoinedDTOs(),
                targetMember = request.targetMemberId.toResourceMemberInfo(),
                operateChannel = channel,
                handoverTo = request.handoverToMemberId?.toResourceMemberInfo()
            )
        )
    }

    override fun batchHandoverMembers(
        userId: String,
        projectId: String,
        request: BatchHandoverMembersReq
    ): Boolean {
        val channel = resolveOperateChannel(userId, projectId, request.targetMemberId)
        return permissionManageFacadeService.batchHandoverGroupMembersFromManager(
            userId = userId,
            projectCode = projectId,
            handoverMemberDTO = GroupMemberHandoverConditionReq(
                groupIds = request.groupIds.toGroupJoinedDTOs(),
                targetMember = request.targetMemberId.toResourceMemberInfo(),
                operateChannel = channel,
                handoverTo = request.handoverToMemberId.toResourceMemberInfo()
            )
        )
    }

    override fun exitGroupsFromPersonal(
        userId: String,
        projectId: String,
        request: BatchRemoveMembersReq
    ): String {
        if (userId != request.targetMemberId) {
            throw PermissionForbiddenException("只能退出自己的用户组")
        }
        return permissionManageFacadeService.batchDeleteResourceGroupMembersFromPersonal(
            userId = userId,
            projectCode = projectId,
            removeMemberDTO = GroupMemberRemoveConditionReq(
                groupIds = request.groupIds.toGroupJoinedDTOs(),
                targetMember = request.targetMemberId.toResourceMemberInfo(),
                operateChannel = OperateChannel.PERSONAL,
                handoverTo = null
            )
        )
    }

    override fun applyHandoverFromPersonal(
        userId: String,
        projectId: String,
        request: BatchHandoverMembersReq
    ): String {
        if (userId != request.targetMemberId) {
            throw PermissionForbiddenException("只能申请交接自己的用户组权限")
        }
        return permissionManageFacadeService.batchHandoverApplicationFromPersonal(
            userId = userId,
            projectCode = projectId,
            handoverMemberDTO = GroupMemberHandoverConditionReq(
                groupIds = request.groupIds.toGroupJoinedDTOs(),
                targetMember = request.targetMemberId.toResourceMemberInfo(),
                operateChannel = OperateChannel.PERSONAL,
                handoverTo = request.handoverToMemberId.toResourceMemberInfo()
            )
        )
    }

    override fun batchOperateCheck(
        userId: String,
        projectId: String,
        batchOperateType: BatchOperateType,
        request: BatchOperateCheckReq
    ): BatchOperateGroupMemberCheckVo {
        val channel = resolveOperateChannel(userId, projectId, request.targetMemberId)
        return permissionManageFacadeService.batchOperateGroupMembersCheck(
            userId = userId,
            projectCode = projectId,
            batchOperateType = batchOperateType,
            conditionReq = GroupMemberCommonConditionReq(
                groupIds = request.groupIds.toGroupJoinedDTOs(),
                targetMember = request.targetMemberId.toResourceMemberInfo(),
                operateChannel = channel
            )
        )
    }

    override fun removeMemberFromProject(
        userId: String,
        projectId: String,
        request: AiRemoveMemberFromProjectReq
    ): List<ResourceMemberInfo> {
        val isAdmin = permissionProjectService.checkProjectManager(userId, projectId)
        if (!isAdmin && userId != request.targetMemberId) {
            throw PermissionForbiddenException("非管理员不能将其他成员移出项目")
        }
        return permissionManageFacadeService.removeMemberFromProject(
            userId = userId,
            projectCode = projectId,
            removeMemberFromProjectReq = RemoveMemberFromProjectReq(
                targetMember = request.targetMemberId.toResourceMemberInfo(),
                handoverTo = request.handoverToMemberId?.toResourceMemberInfo()
            )
        )
    }

    override fun analyzeUserPermissions(
        userId: String,
        projectId: String,
        memberId: String
    ): UserPermissionAnalysisVO {
        val isAdmin = permissionProjectService.checkProjectManager(userId, projectId)
        if (!isAdmin && userId != memberId) {
            throw PermissionForbiddenException("非管理员不能查看其他成员的权限分析")
        }
        val channel = if (isAdmin) OperateChannel.MANAGER else OperateChannel.PERSONAL
        val targetIsAdmin = permissionProjectService.checkProjectManager(memberId, projectId)
        val groupCounts = permissionManageFacadeService.getMemberGroupsCount(
            projectCode = projectId,
            memberId = memberId,
            groupName = null,
            minExpiredAt = null,
            maxExpiredAt = null,
            relatedResourceType = null,
            relatedResourceCode = null,
            action = null,
            operateChannel = channel,
            uniqueManagerGroupsQueryFlag = null
        )
        val totalGroupCount = groupCounts.sumOf { it.count }
        val resourceSummary = groupCounts.map { countVo ->
            ResourceSummaryVO(
                resourceType = countVo.resourceType,
                resourceTypeName = countVo.resourceTypeName,
                groupCount = countVo.count,
                actionSummary = "加入了 ${countVo.count} 个${countVo.resourceTypeName}用户组"
            )
        }
        val now = System.currentTimeMillis()
        val expiredPage = permissionManageFacadeService.getMemberGroupsDetails(
            projectId = projectId,
            resourceType = null,
            memberId = memberId,
            maxExpiredAt = now,
            operateChannel = channel,
            start = 0,
            limit = 1
        )
        val expiredGroupCount = expiredPage.count
        val authorizationCountMap = authAuthorizationDao.countGroupByResourceType(
            dslContext = dslContext,
            projectCode = projectId,
            handoverFrom = memberId
        )
        val resourceTypeNames = rbacCommonService.listResourceTypes()
            .associateBy({ it.resourceType }, { it.name })
        val authorizationSummary = authorizationCountMap.map { (resType, count) ->
            AuthorizationSummaryVO(
                resourceType = resType,
                resourceTypeName = resourceTypeNames[resType] ?: resType,
                count = count
            )
        }.sortedByDescending { it.count }
        val totalAuthorizationCount = authorizationCountMap.values.sum()
        val warnings = mutableListOf<String>()
        if (targetIsAdmin) {
            warnings.add("拥有项目下所有操作权限，请评估权限是否过大")
        }
        if (expiredGroupCount > 0) {
            warnings.add("$expiredGroupCount 个用户组的权限已过期，可清理")
        }
        return UserPermissionAnalysisVO(
            role = if (targetIsAdmin) "MANAGER" else "MEMBER",
            roleDisplayName = if (targetIsAdmin) "项目管理员" else "项目成员",
            totalGroupCount = totalGroupCount,
            expiredGroupCount = expiredGroupCount,
            resourceSummary = resourceSummary,
            authorizationSummary = authorizationSummary,
            totalAuthorizationCount = totalAuthorizationCount,
            hasAllPermissions = targetIsAdmin,
            warnings = warnings
        )
    }

    override fun getResourcePermissionsMatrix(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): ResourcePermissionsMatrixVO {
        val isAdmin = permissionProjectService.checkProjectManager(userId, projectId)
        val resource = permissionResourceService.getResourceByCode(
            projectCode = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode
        ) ?: return ResourcePermissionsMatrixVO(
            resourceName = resourceCode,
            resourceType = resourceType,
            projectId = projectId,
            groups = emptyList(),
            totalGroupCount = 0
        )
        val condition = IamGroupIdsQueryConditionDTO(
            projectCode = projectId,
            relatedResourceType = resourceType,
            relatedResourceCode = resourceCode
        )
        val iamGroupIds = permissionManageFacadeService.listIamGroupIdsByConditions(condition)
        if (iamGroupIds.isEmpty()) {
            return ResourcePermissionsMatrixVO(
                resourceName = resource.resourceName,
                resourceType = resourceType,
                projectId = projectId,
                groups = emptyList(),
                totalGroupCount = 0
            )
        }
        val userGroupIds = if (!isAdmin) {
            permissionResourceMemberService.getMemberGroupsInProject(projectId, userId).toSet()
        } else {
            null
        }
        val allGroups = authResourceGroupDao.listByRelationId(
            dslContext = dslContext,
            projectCode = projectId,
            iamGroupIds = iamGroupIds.map { it.toString() }
        ).map { it.toAuthResourceGroup() }
        val filtered = if (userGroupIds != null) {
            allGroups.filter { it.relationId in userGroupIds }
        } else {
            allGroups
        }
        val filteredGroupIds = filtered.map { it.relationId }
        val permissionsByGroup = authResourceGroupPermissionDao.listByGroupIds(
            dslContext = dslContext,
            projectCode = projectId,
            iamGroupIds = filteredGroupIds
        ).groupBy { it.iamGroupId }
        val groups = filtered.map { group ->
            val groupPermissions = permissionsByGroup[group.relationId] ?: emptyList()
            val permissions = groupPermissions.map { perm ->
                runCatching { rbacCommonService.getActionInfo(perm.action).actionName }
                    .getOrElse { perm.action }
            }.distinct()
            val groupMembers = authResourceGroupMemberDao.listResourceGroupMember(
                dslContext = dslContext,
                projectCode = projectId,
                iamGroupId = group.relationId
            )
            val users = groupMembers
                .filter { it.memberType == MemberType.USER.type }
                .map { it.memberId }
                .distinct()
            val orgDisplayNames = groupMembers
                .filter { it.memberType == MemberType.DEPARTMENT.type }
                .map { m -> m.memberName.ifBlank { m.memberId } }
                .distinct()
            ResourceGroupMatrixVO(
                groupName = group.groupName,
                managementLevel = resolveManagementLevel(group.resourceType),
                managementScope = group.resourceName,
                permissions = permissions,
                userCount = users.size,
                users = users,
                orgCount = orgDisplayNames.size,
                orgs = orgDisplayNames,
                relationId = group.relationId
            )
        }
        return ResourcePermissionsMatrixVO(
            resourceName = resource.resourceName,
            resourceType = resourceType,
            projectId = projectId,
            groups = groups,
            totalGroupCount = groups.size
        )
    }

    override fun recommendGroupsForGrant(
        userId: String,
        projectId: String,
        request: GroupRecommendReq
    ): GroupRecommendationVO {
        val matchingGroupIds = permissionResourceGroupPermissionService.listGroupsByPermissionConditions(
            projectCode = projectId,
            relatedResourceType = request.resourceType,
            relatedResourceCode = request.resourceCode,
            action = request.action
        )
        if (matchingGroupIds.isEmpty()) {
            return GroupRecommendationVO(
                recommendation = "未找到包含该权限的用户组",
                candidateGroups = emptyList()
            )
        }
        val allGroups = authResourceGroupDao.listByRelationId(
            dslContext = dslContext,
            projectCode = projectId,
            iamGroupIds = matchingGroupIds.map { it.toString() }
        ).map { it.toAuthResourceGroup() }
        val targetUserGroups = permissionResourceMemberService
            .getMemberGroupsInProject(projectId, request.targetUserId)
            .toSet()
        val permissionsByGroup = authResourceGroupPermissionDao.listByGroupIds(
            dslContext = dslContext,
            projectCode = projectId,
            iamGroupIds = matchingGroupIds
        ).groupBy { it.iamGroupId }
        val candidates = allGroups
            .sortedBy { resourceTypeLevelSort(it.resourceType) }
            .map { group ->
                val groupPermissions = permissionsByGroup[group.relationId] ?: emptyList()
                val permissions = groupPermissions.map { perm ->
                    runCatching { rbacCommonService.getActionInfo(perm.action).actionName }
                        .getOrElse { perm.action }
                }.distinct()
                val tags = buildRecommendationTags(group.resourceType, request.resourceType)
                val alreadyMember = group.relationId in targetUserGroups
                CandidateGroupVO(
                    groupName = group.groupName,
                    managementLevel = resolveManagementLevel(group.resourceType),
                    managementScope = group.resourceName,
                    permissions = permissions,
                    relationId = group.relationId,
                    tags = if (alreadyMember) {
                        tags + PermissionTagVO(
                            type = PermissionTagType.WARNING,
                            text = "该用户已是此组成员"
                        )
                    } else {
                        tags
                    },
                    alreadyMember = alreadyMember
                )
            }
        return GroupRecommendationVO(
            recommendation = "建议按最小权限原则开通，优先加入资源级别的用户组",
            candidateGroups = candidates
        )
    }

    override fun applyToJoinGroup(
        userId: String,
        projectId: String,
        request: AiApplyJoinGroupReq
    ): Boolean {
        val nowSec = System.currentTimeMillis() / 1000
        val expiredAt = nowSec + TimeUnit.DAYS.toSeconds(request.expiredDays.toLong())
        return permissionApplyService.applyToJoinGroup(
            userId = userId,
            applyJoinGroupInfo = ApplyJoinGroupInfo(
                projectCode = projectId,
                groupIds = request.groupIds,
                expiredAt = expiredAt.toString(),
                applicant = userId,
                reason = request.reason
            )
        )
    }

    private fun resolveOperateChannel(
        userId: String,
        projectId: String,
        targetMemberId: String
    ): OperateChannel {
        val isAdmin = permissionProjectService.checkProjectManager(userId, projectId)
        if (!isAdmin && userId != targetMemberId) {
            throw PermissionForbiddenException("非管理员不能操作其他成员的权限")
        }
        return if (isAdmin) OperateChannel.MANAGER else OperateChannel.PERSONAL
    }

    private fun resourceTypeLevelSort(resourceType: String): Int = when {
        resourceType == "project" -> 2
        resourceType.endsWith("_group") -> 1
        else -> 0
    }

    private fun TAuthResourceGroupRecord.toAuthResourceGroup() = AuthResourceGroup(
        id = id,
        projectCode = projectCode,
        resourceType = resourceType,
        resourceCode = resourceCode,
        resourceName = resourceName,
        iamResourceCode = iamResourceCode,
        groupCode = groupCode,
        groupName = groupName,
        defaultGroup = defaultGroup,
        relationId = relationId.toInt(),
        createTime = createTime,
        updateTime = updateTime,
        description = description,
        iamTemplateId = iamTemplateId
    )

    private fun List<Int>.toGroupJoinedDTOs() = map {
        MemberGroupJoinedDTO(id = it, memberType = MemberType.USER)
    }

    private fun String.toResourceMemberInfo() =
        ResourceMemberInfo(id = this, type = USER_TYPE)

    private fun buildRecommendedCandidates(
        userId: String,
        projectId: String,
        targetMemberId: String,
        authorizationsToHandover: AuthorizationsToHandoverVO,
        hasPendingAuthorizations: Boolean,
        limit: Int
    ): List<HandoverCandidateVO> {
        val managers = permissionProjectService.getProjectUsers(
            projectCode = projectId,
            group = BkAuthGroup.MANAGER
        ).filter { it != targetMemberId }

        val candidates = mutableListOf<HandoverCandidateVO>()
        val targetQualifiedCount = limit.coerceAtLeast(1)

        for (managerId in managers) {
            val validationResult = if (hasPendingAuthorizations) {
                validateHandoverCandidate(
                    userId = userId,
                    projectId = projectId,
                    targetMemberId = targetMemberId,
                    candidateId = managerId,
                    authorizationsToHandover = authorizationsToHandover
                )
            } else {
                CandidateValidationResult(
                    canReceiveAll = true,
                    canReceive = listOf("uniqueManagerGroup"),
                    cannotReceive = emptyList(),
                    cannotReceiveReasons = emptyMap()
                )
            }

            val recommendLevel = when {
                validationResult.canReceiveAll -> "highly_recommended"
                validationResult.cannotReceive.size == 1 -> "recommended"
                else -> "partial"
            }

            val tags = mutableListOf("项目管理员")
            if (validationResult.canReceiveAll && hasPendingAuthorizations) {
                tags.add("可接收全部授权")
            }

            candidates.add(
                HandoverCandidateVO(
                    userId = managerId,
                    displayName = managerId,
                    isManager = true,
                    canReceiveAll = validationResult.canReceiveAll,
                    canReceive = validationResult.canReceive,
                    cannotReceive = validationResult.cannotReceive,
                    cannotReceiveReasons = validationResult.cannotReceiveReasons,
                    requiresValidation = emptyList(),
                    validationHints = emptyMap(),
                    recommendLevel = recommendLevel,
                    tags = tags
                )
            )

            if (candidates.count { it.canReceiveAll } >= targetQualifiedCount) {
                break
            }
        }

        return candidates.sortedWith(
            compareBy(
                { !it.canReceiveAll },
                { it.cannotReceive.size }
            )
        ).take(limit)
    }

    private fun buildRecommendationSuggestion(
        candidates: List<HandoverCandidateVO>
    ): String {
        return buildString {
            if (candidates.isEmpty()) {
                append("暂无可推荐的交接人，请联系其他项目成员")
            } else {
                val best = candidates.first()
                if (best.canReceiveAll) {
                    append("推荐选择管理员 ${best.displayName}，可接收全部授权")
                } else {
                    append("推荐选择管理员 ${best.displayName}")
                    if (best.cannotReceive.isNotEmpty()) {
                        val cannotReceiveNames = best.cannotReceive.map { type ->
                            when (type) {
                                "pipeline" -> "流水线"
                                "repertory" -> "代码库"
                                "envNode" -> "环境节点"
                                else -> type
                            }
                        }
                        append("；部分授权（${cannotReceiveNames.joinToString("、")}）无法接收，需手动处理")
                    }
                }
            }
        }
    }

    private data class CandidateValidationResult(
        val canReceiveAll: Boolean,
        val canReceive: List<String>,
        val cannotReceive: List<String>,
        val cannotReceiveReasons: Map<String, String>
    )

    private fun validateHandoverCandidate(
        userId: String,
        projectId: String,
        targetMemberId: String,
        candidateId: String,
        authorizationsToHandover: AuthorizationsToHandoverVO
    ): CandidateValidationResult {
        val canReceive = mutableListOf<String>()
        val cannotReceive = mutableListOf<String>()
        val cannotReceiveReasons = mutableMapOf<String, String>()

        canReceive.add("uniqueManagerGroup")

        val hasPendingAuthorizations = authorizationsToHandover.pipeline > 0 ||
                authorizationsToHandover.repertory > 0 ||
                authorizationsToHandover.envNode > 0

        if (hasPendingAuthorizations) {
            val failedResourceTypes = permissionAuthorizationService.resetAllResourceAuthorization(
                operator = userId,
                projectCode = projectId,
                condition = ResetAllResourceAuthorizationReq(
                    projectCode = projectId,
                    handoverFrom = targetMemberId,
                    handoverTo = candidateId,
                    preCheck = true,
                    checkPermission = false
                )
            )

            val failedTypeSet = failedResourceTypes.map { it.resourceType }.toSet()

            if (authorizationsToHandover.pipeline > 0) {
                if ("pipeline" in failedTypeSet) {
                    cannotReceive.add("pipeline")
                    val failedInfo = failedResourceTypes.find { it.resourceType == "pipeline" }
                    cannotReceiveReasons["pipeline"] = failedInfo?.name
                        ?: "需要有对应流水线的执行权限（含子流水线）"
                } else {
                    canReceive.add("pipeline")
                }
            }

            if (authorizationsToHandover.repertory > 0) {
                if ("repertory" in failedTypeSet) {
                    cannotReceive.add("repertory")
                    val failedInfo = failedResourceTypes.find { it.resourceType == "repertory" }
                    cannotReceiveReasons["repertory"] = failedInfo?.name
                        ?: "需要是代码库成员（在工蜂/GitLab有下载权限）"
                } else {
                    canReceive.add("repertory")
                }
            }

            if (authorizationsToHandover.envNode > 0) {
                if ("env_node" in failedTypeSet) {
                    cannotReceive.add("envNode")
                    val failedInfo = failedResourceTypes.find { it.resourceType == "env_node" }
                    cannotReceiveReasons["envNode"] = failedInfo?.name
                        ?: "需要是 CMDB 主备负责人"
                } else {
                    canReceive.add("envNode")
                }
            }
        }

        return CandidateValidationResult(
            canReceiveAll = cannotReceive.isEmpty(),
            canReceive = canReceive,
            cannotReceive = cannotReceive,
            cannotReceiveReasons = cannotReceiveReasons
        )
    }

    private fun requireSelfOrManager(userId: String, projectId: String, targetMemberId: String) {
        if (userId != targetMemberId) {
            val isManager = permissionProjectService.checkProjectManager(userId, projectId)
            if (!isManager) {
                throw PermissionForbiddenException("非管理员不能操作其他成员")
            }
        }
    }

    private fun resolveManagementLevel(resourceType: String): String = when {
        resourceType == "project" -> "项目"
        resourceType.endsWith("_group") ->
            resourceType.removeSuffix("_group").replaceFirstChar { it.uppercase() } + "组"

        else -> rbacCommonService.listResourceTypes()
            .firstOrNull { it.resourceType == resourceType }
            ?.name ?: resourceType
    }

    private fun levelSortOrder(managementLevel: String): Int = when (managementLevel) {
        "项目" -> 2
        else -> if (managementLevel.endsWith("组")) 1 else 0
    }

    private fun buildRecommendationTags(
        groupResourceType: String,
        requestedResourceType: String
    ): List<PermissionTagVO> {
        val tags = mutableListOf<PermissionTagVO>()
        if (groupResourceType == requestedResourceType) {
            tags.add(
                PermissionTagVO(
                    type = PermissionTagType.RECOMMEND,
                    text = "资源级用户组，权限范围最小"
                )
            )
        }
        if (groupResourceType.endsWith("_group")) {
            tags.add(
                PermissionTagVO(
                    type = PermissionTagType.WARNING,
                    text = "加入后将获得同组其他资源的权限"
                )
            )
        }
        if (groupResourceType == "project") {
            tags.add(
                PermissionTagVO(
                    type = PermissionTagType.WARNING,
                    text = "项目级用户组，将获得项目下所有同类资源权限"
                )
            )
        }
        return tags
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AuthAiServiceImpl::class.java)
        private const val DEFAULT_EXPIRED_DAYS = 365L
        private const val MAX_CLONE_GROUPS = 500
        private const val MAX_COMPARE_GROUPS = 500
        private const val USER_TYPE = "user"
    }
}
