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

package com.tencent.devops.ai.agent.auth

import com.tencent.devops.auth.api.service.ServiceAuthApplyResource
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.auth.pojo.SearchGroupInfo
import com.tencent.devops.auth.pojo.enum.GroupLevel
import com.tencent.devops.auth.pojo.request.ai.AiApplyJoinGroupReq
import com.tencent.devops.auth.pojo.request.ai.AiMemberExitsProjectReq
import com.tencent.devops.auth.pojo.request.ai.BatchHandoverMembersReq
import com.tencent.devops.auth.pojo.request.ai.BatchRemoveMembersReq
import com.tencent.devops.auth.pojo.request.ai.GroupRecommendReq
import com.tencent.devops.common.client.Client
import io.agentscope.core.tool.Tool
import io.agentscope.core.tool.ToolParam
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Supplier

/**
 * 成员自助：申请权限/续期、退出项目、退出与交接预检查。
 */
class AuthMemberSelfServiceTools(
    client: Client,
    userIdSupplier: Supplier<String>
) : AuthAiToolsBase(client, userIdSupplier) {

    override val logger: Logger = LoggerFactory.getLogger(AuthMemberSelfServiceTools::class.java)

    @Tool(
        name = "申请续期权限",
        description = "普通用户申请续期权限（提交审批流程）。\n" +
                "支持两种模式：\n" +
                "1. 直接模式：提供 groupIds，申请续期指定用户组\n" +
                "2. 智能模式：提供 expiredStatus=expiring_soon，申请续期即将过期的权限\n" +
                "默认 dryRun=true 仅预览，设为 false 提交申请。" +
                "大部分场景用于普通成员，管理员可以使用续期成员权限接口续期自身权限"
    )
    fun applyRenewal(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "reason", description = "申请理由")
        reason: String,
        @ToolParam(
            name = "groupIds",
            description = "用户组ID列表，逗号分隔（直接模式）",
            required = false
        )
        groupIds: String? = null,
        @ToolParam(
            name = "expiredStatus",
            description = "过期状态筛选（智能模式）：expiring_soon=即将过期（默认30天内）",
            required = false
        )
        expiredStatus: String? = null,
        @ToolParam(
            name = "expireWithinDays",
            description = "即将过期天数（智能模式），配合 expiredStatus 使用，默认30天",
            required = false
        )
        expireWithinDays: Int? = null,
        @ToolParam(
            name = "renewalDays",
            description = "续期天数，默认180天",
            required = false
        )
        renewalDays: Int? = null,
        @ToolParam(
            name = "dryRun",
            description = "预检查模式，默认 true 仅预览，设为 false 提交申请",
            required = false
        )
        dryRun: Boolean = true
    ): String {
        val operatorId = getOperatorUserId()
        val actualRenewalDays = renewalDays ?: AuthAiToolsBase.DEFAULT_APPLY_DAYS

        return safeOperate(
            "AuthTool", "applyRenewal", mapOf(
                "projectId" to projectId,
                "reason" to reason,
                "groupIds" to groupIds,
                "expiredStatus" to expiredStatus,
                "renewalDays" to actualRenewalDays,
                "dryRun" to dryRun
            )
        ) {
            val targetGroupIds = resolveRenewalGroups(
                operatorId = operatorId,
                projectId = projectId,
                groupIds = groupIds,
                expiredStatus = expiredStatus,
                expireWithinDays = expireWithinDays
            )

            if (targetGroupIds.isEmpty()) {
                return@safeOperate when {
                    !groupIds.isNullOrBlank() -> "未找到你加入的这些用户组: $groupIds"
                    expiredStatus == "expiring_soon" -> "你没有即将过期的权限，无需续期"
                    else -> "请指定要续期的用户组ID（groupIds）或使用 expiredStatus=expiring_soon 申请续期即将过期的权限"
                }
            }

            if (dryRun) {
                return@safeOperate buildString {
                    appendLine("=== 续期申请预览 ===")
                    appendLine("待续期用户组: ${targetGroupIds.size} 个")
                    appendLine("用户组ID: ${targetGroupIds.joinToString(", ")}")
                    appendLine("续期天数: $actualRenewalDays 天")
                    appendLine("申请理由: $reason")
                    appendLine()
                    appendLine("如需提交申请，请设置 dryRun=false")
                }.trim()
            }

            val result = authAiResource().applyRenewalGroupMember(
                userId = operatorId,
                projectId = projectId,
                groupIds = targetGroupIds.joinToString(","),
                renewalDays = actualRenewalDays,
                reason = reason
            )

            if (result.data == true) {
                "续期申请已提交，共申请续期 ${targetGroupIds.size} 个用户组，请等待管理员审批"
            } else {
                "申请失败: ${result.message}"
            }
        }
    }

    private fun resolveRenewalGroups(
        operatorId: String,
        projectId: String,
        groupIds: String?,
        expiredStatus: String?,
        expireWithinDays: Int?
    ): List<Int> {
        if (!groupIds.isNullOrBlank()) {
            val requestedIds = parseCommaSeparatedInts(groupIds)
            val joinedGroupIds = fetchUserJoinedGroupIds(operatorId, projectId)
            return requestedIds.filter { it in joinedGroupIds }
        }

        if (expiredStatus == "expiring_soon") {
            val days = expireWithinDays ?: DEFAULT_EXPIRE_WITHIN_DAYS
            val now = System.currentTimeMillis()
            val maxExpiredAt = now + days * 24 * 60 * 60 * 1000L
            val result = authAiResource().getAllMemberGroupsDetails(
                userId = operatorId,
                projectId = projectId,
                memberId = operatorId,
                resourceType = null,
                iamGroupIds = null,
                groupName = null,
                minExpiredAt = now,
                maxExpiredAt = maxExpiredAt,
                relatedResourceType = null,
                relatedResourceCode = null,
                action = null,
                page = 1,
                pageSize = 500
            )
            return result.data?.records?.map { it.groupId } ?: emptyList()
        }

        return emptyList()
    }

    @Tool(
        name = "申请权限",
        description = "普通用户申请权限（提交审批流程）。支持两种模式：\n" +
                "1. 直接模式：提供 groupIds，申请加入指定用户组\n" +
                "2. 智能模式：提供 resourceType + resourceCode + action，系统推荐最佳用户组\n" +
                "默认 dryRun=true 仅预览推荐结果，设为 false 提交申请。"
    )
    fun applyPermissions(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "reason", description = "申请理由")
        reason: String,
        @ToolParam(
            name = "groupIds",
            description = "用户组ID列表，逗号分隔（直接模式）",
            required = false
        )
        groupIds: String? = null,
        @ToolParam(
            name = "resourceType",
            description = "资源类型（智能模式），如 pipeline、credential",
            required = false
        )
        resourceType: String? = null,
        @ToolParam(
            name = "resourceCode",
            description = "资源Code（智能模式）",
            required = false
        )
        resourceCode: String? = null,
        @ToolParam(
            name = "action",
            description = "目标操作权限（智能模式），如 pipeline_execute",
            required = false
        )
        action: String? = null,
        @ToolParam(
            name = "expiredDays",
            description = "期望时长（天），默认180",
            required = false
        )
        expiredDays: Int? = null,
        @ToolParam(
            name = "dryRun",
            description = "预检查模式，默认 true 仅预览，设为 false 提交申请",
            required = false
        )
        dryRun: Boolean = true
    ): String {
        val operatorId = getOperatorUserId()

        return safeOperate(
            "AuthTool", "applyPermissions", mapOf(
                "projectId" to projectId,
                "reason" to reason,
                "groupIds" to groupIds,
                "resourceType" to resourceType,
                "resourceCode" to resourceCode,
                "action" to action,
                "dryRun" to dryRun
            )
        ) {
            val applyContext = resolveApplyContext(
                operatorId = operatorId,
                projectId = projectId,
                groupIds = groupIds,
                resourceType = resourceType,
                resourceCode = resourceCode,
                action = action
            )

            if (applyContext.errorMsg != null) {
                return@safeOperate applyContext.errorMsg
            }
            if (applyContext.targetGroupIds.isEmpty()) {
                return@safeOperate applyContext.previewInfo
            }

            if (dryRun) {
                return@safeOperate buildString {
                    appendLine("=== 预检查结果 ===")
                    append(applyContext.previewInfo)
                    appendLine("申请理由: $reason")
                    appendLine("如需提交申请，请设置 dryRun=false")
                }.trim()
            }

            val result = authAiResource().applyToJoinGroup(
                userId = operatorId,
                projectId = projectId,
                request = AiApplyJoinGroupReq(
                    groupIds = applyContext.targetGroupIds,
                    reason = reason,
                    expiredDays = expiredDays ?: DEFAULT_APPLY_DAYS
                )
            )
            if (result.data == true) {
                "权限申请已提交，共申请加入 ${applyContext.targetGroupIds.size} 个用户组，请等待管理员审批"
            } else {
                "申请失败: ${result.message}"
            }
        }
    }

    private data class ApplyContext(
        val targetGroupIds: List<Int>,
        val skippedGroupIds: List<Int>,
        val previewInfo: String,
        val errorMsg: String? = null
    )

    private fun resolveApplyContext(
        operatorId: String,
        projectId: String,
        groupIds: String?,
        resourceType: String?,
        resourceCode: String?,
        action: String?
    ): ApplyContext {
        return when {
            !groupIds.isNullOrBlank() -> resolveDirectMode(
                groupIds = groupIds,
                joinedGroupIds = fetchUserJoinedGroupIds(operatorId, projectId)
            )
            resourceType != null && resourceCode != null && action != null ->
                resolveSmartMode(
                    operatorId = operatorId,
                    projectId = projectId,
                    resourceType = resourceType,
                    resourceCode = resourceCode,
                    action = action
                )
            else -> ApplyContext(
                targetGroupIds = emptyList(),
                skippedGroupIds = emptyList(),
                previewInfo = "",
                errorMsg = "参数错误：请提供 groupIds（直接模式）或 resourceType + resourceCode + action（智能模式）"
            )
        }
    }

    private fun fetchUserJoinedGroupIds(userId: String, projectId: String): Set<Int> {
        if (!isProjectMember(userId, projectId)) {
            return emptySet()
        }
        val result = authAiResource().getAllMemberGroupsDetails(
            userId = userId,
            projectId = projectId,
            memberId = userId,
            resourceType = null,
            iamGroupIds = null,
            groupName = null,
            minExpiredAt = System.currentTimeMillis(),
            maxExpiredAt = null,
            relatedResourceType = null,
            relatedResourceCode = null,
            action = null,
            page = 1,
            pageSize = 500
        )
        return result.data?.records?.map { it.groupId }?.toSet() ?: emptySet()
    }

    private fun resolveDirectMode(groupIds: String, joinedGroupIds: Set<Int>): ApplyContext {
        val requestedIds = parseCommaSeparatedInts(groupIds)
        val alreadyIn = requestedIds.filter { it in joinedGroupIds }
        val toApply = requestedIds.filter { it !in joinedGroupIds }

        if (toApply.isEmpty()) {
            return ApplyContext(
                targetGroupIds = emptyList(),
                skippedGroupIds = alreadyIn,
                previewInfo = "你已经是这些用户组的成员，无需重复申请: $groupIds"
            )
        }

        val preview = buildString {
            appendLine("待申请用户组: ${toApply.joinToString(", ")}")
            if (alreadyIn.isNotEmpty()) {
                appendLine("已跳过（你已是成员）: ${alreadyIn.joinToString(", ")}")
            }
        }
        return ApplyContext(
            targetGroupIds = toApply,
            skippedGroupIds = alreadyIn,
            previewInfo = preview.trim()
        )
    }

    private fun resolveSmartMode(
        operatorId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String,
        action: String
    ): ApplyContext {
        if (!isProjectMember(operatorId, projectId)) {
            return resolveProjectLevelFallback(
                operatorId = operatorId,
                projectId = projectId,
                resourceType = resourceType,
                resourceCode = resourceCode,
                action = action,
                joinedGroupIds = emptySet()
            )
        }

        val recommendResult = authAiResource().recommendGroupsForGrant(
            userId = operatorId,
            projectId = projectId,
            request = GroupRecommendReq(
                resourceType = resourceType,
                resourceCode = resourceCode,
                action = action,
                targetUserId = operatorId
            )
        )
        val recommendation = recommendResult.data
            ?: return ApplyContext(
                targetGroupIds = emptyList(),
                skippedGroupIds = emptyList(),
                previewInfo = "",
                errorMsg = "推荐失败: ${recommendResult.message}"
            )

        if (recommendation.candidateGroups.isEmpty()) {
            return ApplyContext(
                targetGroupIds = emptyList(),
                skippedGroupIds = emptyList(),
                previewInfo = "",
                errorMsg = "未找到包含权限 $action 的用户组"
            )
        }

        val bestGroup = recommendation.candidateGroups.first()
        if (bestGroup.alreadyMember) {
            return ApplyContext(
                targetGroupIds = emptyList(),
                skippedGroupIds = listOf(bestGroup.relationId),
                previewInfo = "你已经是用户组「${bestGroup.groupName}」的成员，" +
                        "已拥有 $resourceType/$resourceCode 的 $action 权限，无需重复申请"
            )
        }

        val preview = buildString {
            appendLine("目标权限: $resourceType/$resourceCode 的 $action")
            appendLine("推荐用户组: ${bestGroup.groupName} (ID: ${bestGroup.relationId})")
            if (bestGroup.tags.isNotEmpty()) {
                appendLine("推荐理由: ${bestGroup.tags.joinToString(", ") { it.text }}")
            }
        }
        return ApplyContext(
            targetGroupIds = listOf(bestGroup.relationId),
            skippedGroupIds = emptyList(),
            previewInfo = preview.trim()
        )
    }

    private fun isProjectMember(userId: String, projectId: String): Boolean {
        return service(ServiceProjectAuthResource::class)
            .isProjectMember(userId = userId, projectCode = projectId)
            .data == true
    }

    private fun resolveProjectLevelFallback(
        operatorId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String,
        action: String,
        joinedGroupIds: Set<Int>
    ): ApplyContext {
        val result = service(ServiceAuthApplyResource::class).listGroupsForApply(
            userId = operatorId,
            projectId = projectId,
            searchGroupInfo = SearchGroupInfo(
                groupLevel = GroupLevel.PROJECT,
                page = 1,
                pageSize = 10
            )
        )
        val projectLevelGroups = result.data?.results
            ?.filter { it.resourceType == "project" }
            ?.take(10)
            .orEmpty()
        if (projectLevelGroups.isEmpty()) {
            return ApplyContext(
                targetGroupIds = emptyList(),
                skippedGroupIds = emptyList(),
                previewInfo = "",
                errorMsg = "当前不是项目成员，且未找到可申请的项目级用户组"
            )
        }

        val alreadyJoinedGroups = projectLevelGroups.filter { it.id in joinedGroupIds }
        val toApplyGroups = projectLevelGroups.filter { it.id !in joinedGroupIds }
        if (toApplyGroups.isEmpty()) {
            return ApplyContext(
                targetGroupIds = emptyList(),
                skippedGroupIds = alreadyJoinedGroups.map { it.id },
                previewInfo = "你已经是这些项目级用户组的成员，无需重复申请: " +
                    alreadyJoinedGroups.joinToString(", ") { "${it.name}(ID: ${it.id})" }
            )
        }

        val preview = buildString {
            appendLine("目标权限: $resourceType/$resourceCode 的 $action")
            appendLine("当前不是项目成员，已自动降级为申请项目级用户组")
            toApplyGroups.forEachIndexed { index, group ->
                appendLine("${index + 1}. ${group.name} (ID: ${group.id})")
            }
            if (alreadyJoinedGroups.isNotEmpty()) {
                appendLine(
                    "已跳过（你已是成员）: " +
                        alreadyJoinedGroups.joinToString(", ") { "${it.name}(ID: ${it.id})" }
                )
            }
            appendLine("说明: 加入项目后，如仍缺少资源级权限，可再继续申请")
        }
        return ApplyContext(
            targetGroupIds = toApplyGroups.map { it.id },
            skippedGroupIds = alreadyJoinedGroups.map { it.id },
            previewInfo = preview.trim()
        )
    }

    @Tool(
        name = "退出/交接用户组",
        description = "当前用户退出指定用户组（自助操作，非管理员场景）。内置检查、推荐交接人、执行退出/交接。\n" +
                "默认 dryRun=true 仅检查是否可退出并推荐交接人，设为 false 执行实际操作。\n" +
                "提供 handoverTo 时发起交接申请（需审批），不提供时直接退出。\n" +
                "如需退出整个项目，请使用「退出项目」工具。"
    )
    fun exitGroups(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "groupIds", description = "用户组ID列表，逗号分隔")
        groupIds: String,
        @ToolParam(
            name = "handoverTo",
            description = "交接人用户ID（可选，提供时发起交接申请，需审批通过后生效）",
            required = false
        )
        handoverTo: String? = null,
        @ToolParam(
            name = "dryRun",
            description = "预检查模式，默认 true 仅检查并推荐交接人，设为 false 执行退出/发起交接申请",
            required = false
        )
        dryRun: Boolean = true
    ): String {
        val operatorId = getOperatorUserId()
        val ids = parseCommaSeparatedInts(groupIds)

        return safeOperate(
            "AuthTool", "exitGroups", mapOf(
                "projectId" to projectId,
                "groupIds" to groupIds,
                "handoverTo" to handoverTo,
                "dryRun" to dryRun
            )
        ) {
            val checkResult = authAiResource().checkMemberExitWithRecommendation(
                userId = operatorId,
                projectId = projectId,
                targetMemberId = operatorId,
                handoverTo = handoverTo,
                groupIds = groupIds,
                recommendLimit = 3
            )
            val check = checkResult.data ?: return@safeOperate "检查失败: ${checkResult.message}"

            val sb = StringBuilder()
            sb.appendLine("=== 退出用户组检查 ===")
            sb.appendLine("涉及用户组: ${ids.size} 个")
            sb.appendLine("可直接退出: ${check.canExitDirectly}")

            if (!check.canExitDirectly) {
                sb.appendLine("\n需要交接的内容:")
                val auth = check.authorizationsToHandover
                if (auth != null) {
                    if (auth.uniqueManagerGroups > 0) {
                        sb.appendLine("- 唯一管理员组: ${auth.uniqueManagerGroups}")
                    }
                    if (auth.pipeline > 0) {
                        sb.appendLine("- 流水线授权: ${auth.pipeline}")
                    }
                    if (auth.repertory > 0) {
                        sb.appendLine("- 代码库授权: ${auth.repertory}")
                        sb.appendLine("  ⚠️ 注意: 代码库授权交接前，需要先到工蜂代码库将交接人添加为代码库成员")
                    }
                    if (auth.envNode > 0) {
                        sb.appendLine("- 环境节点授权: ${auth.envNode}")
                        sb.appendLine("  ⚠️ 注意: 环境授权交接前，需到bkcc平台添加对方为主备负责人")
                    }
                }
            }

            if (check.hasDepartmentJoined) {
                sb.appendLine("\n⚠️ 注意: 你通过组织加入了部分用户组 (${check.departments})")
                sb.appendLine("这些权限无法通过本工具移除")
            }

            if (handoverTo != null) {
                if (check.handoverToCanReceiveAll) {
                    sb.appendLine("\n✓ 交接人 $handoverTo 可以接收所有权限和授权")
                } else if (check.handoverToCannotReceive.isNotEmpty()) {
                    sb.appendLine("\n✗ 交接人 $handoverTo 无法接收以下授权:")
                    check.handoverToCannotReceive.forEach { authType ->
                        val reason = check.handoverToCannotReceiveReasons[authType] ?: "未知原因"
                        sb.appendLine("  - ${translateAuthType(authType)}: $reason")
                    }
                    sb.appendLine("请先处理这些授权问题，或更换交接人")
                }
            }

            if (check.recommendedCandidates.isNotEmpty() && handoverTo == null && !check.canExitDirectly) {
                sb.appendLine("\n推荐交接人:")
                check.recommendedCandidates.take(3).forEachIndexed { i, candidate ->
                    val managerTag = if (candidate.isManager) " [管理员]" else ""
                    val receiveTag = if (candidate.canReceiveAll) " ✓ 可接收所有授权" else " ⚠️ 部分授权无法接收"
                    sb.appendLine("${i + 1}. ${candidate.userId}$managerTag$receiveTag")
                }
            }

            if (!check.suggestion.isNullOrBlank()) {
                sb.appendLine("\n建议: ${check.suggestion}")
            }

            if (dryRun) {
                sb.appendLine("\n预检查模式，未执行操作。如需执行，请设置 dryRun=false")
                return@safeOperate sb.toString()
            }

            if (!check.canExitDirectly && handoverTo.isNullOrBlank()) {
                return@safeOperate sb.toString() + "\n无法直接退出，请指定交接人 handoverTo"
            }

            if (handoverTo != null && !check.handoverToCanReceiveAll &&
                check.handoverToCannotReceive.isNotEmpty()
            ) {
                sb.appendLine("\n❌ 无法执行：交接人 $handoverTo 无法接收全部授权")
                sb.appendLine("请先处理授权问题，或更换交接人后重试")
                return@safeOperate sb.toString()
            }

            val isHandover = !handoverTo.isNullOrBlank()
            if (isHandover) {
                val result = authAiResource().applyHandoverFromPersonal(
                    userId = operatorId,
                    projectId = projectId,
                    request = BatchHandoverMembersReq(
                        groupIds = ids,
                        targetMemberId = operatorId,
                        handoverToMemberId = handoverTo
                    )
                )
                val msg = result.data
                if (msg.isNullOrBlank() || result.isNotOk()) {
                    "交接申请提交失败: ${result.message}"
                } else {
                    "已发起交接申请，将 ${ids.size} 个用户组的权限交接给 $handoverTo。\n" +
                        "申请ID: $msg\n" +
                        "审批通过后生效。"
                }
            } else {
                val result = authAiResource().exitGroupsFromPersonal(
                    userId = operatorId,
                    projectId = projectId,
                    request = BatchRemoveMembersReq(
                        groupIds = ids,
                        targetMemberId = operatorId,
                        handoverToMemberId = null
                    )
                )
                val msg = result.data
                if (msg.isNullOrBlank() || result.isNotOk()) {
                    "退出失败: ${result.message}"
                } else {
                    "已成功退出 ${ids.size} 个用户组"
                }
            }
        }
    }

    @Tool(
        name = "退出项目",
        description = "当前用户退出整个项目。内置检查逻辑和交接人推荐。\n" +
                "默认 dryRun=true 仅检查是否可退出并推荐交接人，设为 false 执行退出。\n" +
                "如需退出特定用户组而非整个项目，请使用「退出用户组」工具。"
    )
    fun exitProject(
        @ToolParam(name = "projectId", description = "项目ID（英文标识）")
        projectId: String,
        @ToolParam(
            name = "handoverTo",
            description = "交接人用户ID（不能直接退出时必填）",
            required = false
        )
        handoverTo: String? = null,
        @ToolParam(
            name = "dryRun",
            description = "预检查模式，默认 true 仅检查，设为 false 执行退出",
            required = false
        )
        dryRun: Boolean = true
    ): String {
        val operatorId = getOperatorUserId()

        return safeOperate(
            "AuthTool", "exitProject", mapOf(
                "projectId" to projectId,
                "handoverTo" to handoverTo,
                "dryRun" to dryRun
            )
        ) {
            val checkResult = authAiResource().checkMemberExitWithRecommendation(
                userId = operatorId,
                projectId = projectId,
                targetMemberId = operatorId,
                handoverTo = handoverTo,
                groupIds = null,
                recommendLimit = 5
            )
            val check = checkResult.data ?: return@safeOperate "检查失败: ${checkResult.message}"

            val sb = StringBuilder()
            sb.appendLine("=== 退出项目检查 ===")
            sb.appendLine("项目: $projectId")
            sb.appendLine("可直接退出: ${check.canExitDirectly}")

            if (!check.canExitDirectly) {
                sb.appendLine("\n需要进行交接:")
                val auth = check.authorizationsToHandover
                if (auth != null) {
                    if (auth.uniqueManagerGroups > 0) {
                        sb.appendLine("- 唯一管理员组: ${auth.uniqueManagerGroups}")
                    }
                    if (auth.pipeline > 0) {
                        sb.appendLine("- 流水线授权: ${auth.pipeline}")
                    }
                    if (auth.repertory > 0) {
                        sb.appendLine("- 代码库授权: ${auth.repertory}")
                        sb.appendLine("  ⚠️ 注意: 代码库授权交接前，需要先到工蜂代码库将交接人添加为代码库成员")
                    }
                    if (auth.envNode > 0) {
                        sb.appendLine("- 环境节点授权: ${auth.envNode}")
                        sb.appendLine(
                            "  ⚠️ 注意: 环境授权交接前，需要到bkcc平台添加为对方为主备负责人，待10分钟后重试（平台间延迟）"
                        )
                    }
                }
            }

            if (check.hasDepartmentJoined) {
                sb.appendLine("\n⚠️ 注意: 你通过组织加入了部分用户组 (${check.departments})")
                sb.appendLine("这些权限无法通过本工具移除，退出后可能仍有部分权限")
            }

            if (handoverTo != null) {
                if (check.handoverToCanReceiveAll) {
                    sb.appendLine("\n✓ 交接人 $handoverTo 可以接收所有权限和授权")
                } else if (check.handoverToCannotReceive.isNotEmpty()) {
                    sb.appendLine("\n✗ 交接人 $handoverTo 无法接收以下授权:")
                    check.handoverToCannotReceive.forEach { authType ->
                        val reason = check.handoverToCannotReceiveReasons[authType] ?: "未知原因"
                        sb.appendLine("  - ${translateAuthType(authType)}: $reason")
                    }
                    sb.appendLine("请先处理这些授权问题，或前往「授权管理」页面手动重置")
                }
            }

            if (check.recommendedCandidates.isNotEmpty() && handoverTo == null) {
                sb.appendLine("\n推荐交接人:")
                check.recommendedCandidates.take(3).forEachIndexed { i, candidate ->
                    val managerTag = if (candidate.isManager) " [管理员]" else ""
                    val receiveTag = if (candidate.canReceiveAll) " ✓ 可接收所有授权" else " ⚠️ 部分授权无法接收"
                    sb.appendLine("${i + 1}. ${candidate.userId}$managerTag$receiveTag")
                }
            }

            if (!check.suggestion.isNullOrBlank()) {
                sb.appendLine("\n建议: ${check.suggestion}")
            }

            if (dryRun) {
                sb.appendLine("\n预检查模式，未执行退出。如需执行，请设置 dryRun=false")
                return@safeOperate sb.toString()
            }

            if (!check.canExitDirectly && handoverTo.isNullOrBlank()) {
                return@safeOperate sb.toString() + "\n无法直接退出，请指定交接人 handoverTo"
            }

            if (handoverTo != null && !check.handoverToCanReceiveAll &&
                check.handoverToCannotReceive.isNotEmpty()
            ) {
                sb.appendLine("\n❌ 无法执行退出：交接人 $handoverTo 无法接收全部授权")
                sb.appendLine("请先前往「授权管理」页面处理以下授权，或更换交接人后重试")
                return@safeOperate sb.toString()
            }

            val exitResult = authAiResource().memberExitsProject(
                userId = operatorId,
                projectId = projectId,
                request = AiMemberExitsProjectReq(
                    handoverToMemberId = handoverTo?.takeIf { it.isNotBlank() }
                )
            )
            val msg = exitResult.data
            if (msg.isNullOrBlank()) {
                "已成功退出项目 $projectId"
            } else {
                msg
            }
        }
    }

    @Tool(
        name = "检查退出/交接",
        description = "检查用户退出项目或交接权限的可行性，并推荐合适的交接人。\n" +
                "支持检查退出整个项目或仅退出特定用户组。\n" +
                "管理员可检查任意成员，普通用户只能检查自己。"
    )
    fun checkMemberExitWithRecommendation(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(
            name = "targetMemberId",
            description = "目标成员ID（不传则检查当前用户自己）",
            required = false
        )
        targetMemberId: String? = null,
        @ToolParam(
            name = "handoverTo",
            description = "指定的交接人ID（可选，用于验证该人是否可接收）",
            required = false
        )
        handoverTo: String? = null,
        @ToolParam(
            name = "groupIds",
            description = "用户组ID列表，逗号分隔（可选，不传则检查退出整个项目）",
            required = false
        )
        groupIds: String? = null
    ): String {
        val operatorId = getOperatorUserId()
        val actualTargetId = targetMemberId ?: operatorId

        return safeQuery("AuthTool", "checkMemberExitWithRecommendation") {
            val result = authAiResource().checkMemberExitWithRecommendation(
                userId = operatorId,
                projectId = projectId,
                targetMemberId = actualTargetId,
                handoverTo = handoverTo,
                groupIds = groupIds,
                recommendLimit = 5
            )
            val check = result.data ?: return@safeQuery "检查失败: ${result.message}"

            val sb = StringBuilder()
            sb.appendLine("=== 退出/交接检查 ===")
            sb.appendLine("目标成员: $actualTargetId")
            sb.appendLine("范围: ${if (groupIds.isNullOrBlank()) "整个项目" else "指定用户组"}")
            sb.appendLine("可直接退出: ${check.canExitDirectly}")

            if (!check.canExitDirectly) {
                sb.appendLine("\n需要交接的内容:")
                val auth = check.authorizationsToHandover
                if (auth != null) {
                    if (auth.uniqueManagerGroups > 0) {
                        sb.appendLine("- 唯一管理员组: ${auth.uniqueManagerGroups}")
                    }
                    if (auth.pipeline > 0) {
                        sb.appendLine("- 流水线授权: ${auth.pipeline}")
                    }
                    if (auth.repertory > 0) {
                        sb.appendLine("- 代码库授权: ${auth.repertory}")
                        sb.appendLine("  ⚠️ 注意: 代码库授权交接前，需要先到工蜂代码库将交接人添加为代码库成员")
                    }
                    if (auth.envNode > 0) {
                        sb.appendLine("- 环境节点授权: ${auth.envNode}")
                    }
                }
            }

            if (check.hasDepartmentJoined) {
                sb.appendLine("\n⚠️ 通过组织加入: ${check.departments}")
            }

            if (handoverTo != null) {
                sb.appendLine("\n交接人验证 ($handoverTo):")
                if (check.handoverToCanReceiveAll) {
                    sb.appendLine("✓ 可以接收所有权限和授权")
                } else if (check.handoverToCannotReceive.isNotEmpty()) {
                    sb.appendLine("✗ 无法接收以下授权:")
                    check.handoverToCannotReceive.take(5).forEach { authType ->
                        val reason = check.handoverToCannotReceiveReasons[authType] ?: "未知原因"
                        sb.appendLine("  - ${translateAuthType(authType)}: $reason")
                    }
                }
            }

            if (check.recommendedCandidates.isNotEmpty()) {
                sb.appendLine("\n推荐交接人:")
                check.recommendedCandidates.forEachIndexed { i, c ->
                    sb.append("${i + 1}. ${c.userId}")
                    if (c.isManager) sb.append(" [管理员]")
                    if (c.canReceiveAll) {
                        sb.appendLine(" ✓ 可接收所有授权")
                    } else {
                        sb.appendLine(" ⚠️ 部分授权无法接收")
                    }
                }
            }

            if (!check.suggestion.isNullOrBlank()) {
                sb.appendLine("\n建议: ${check.suggestion}")
            }

            sb.toString()
        }
    }
}
