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

import com.tencent.devops.auth.pojo.enum.BatchOperateType
import com.tencent.devops.auth.pojo.request.ai.AiBatchRemoveMemberFromProjectReq
import com.tencent.devops.auth.pojo.request.ai.BatchHandoverMembersReq
import com.tencent.devops.auth.pojo.request.ai.BatchOperateCheckReq
import com.tencent.devops.auth.pojo.request.ai.BatchRemoveMembersReq
import com.tencent.devops.auth.pojo.request.ai.BatchRenewalMembersReq
import com.tencent.devops.auth.pojo.request.ai.GroupRecommendReq
import com.tencent.devops.auth.pojo.vo.MemberExitCheckVO
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.pojo.ProjectCreateUserInfo
import io.agentscope.core.tool.Tool
import io.agentscope.core.tool.ToolParam
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Supplier

/**
 * 管理员侧的权限变更工具（授权、续期、移除、移出项目、克隆）。
 */
class AuthAdminMutationTools(
    client: Client,
    userIdSupplier: Supplier<String>
) : AuthAiToolsBase(client, userIdSupplier) {

    override val logger: Logger = LoggerFactory.getLogger(AuthAdminMutationTools::class.java)

    @Tool(
        name = "授予权限",
        description = "为用户授予权限（管理员操作）。支持两种模式：\n" +
                "1. 直接模式：提供 groupId，直接将用户添加到指定用户组\n" +
                "2. 智能模式：提供 resourceType + resourceCode + action，系统自动推荐最合适的用户组\n" +
                "默认 dryRun=true 仅预览，设为 false 执行实际授权。" +
                "仅管理员可使用"
    )
    fun grantPermissions(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "targetUserIds", description = "要授权的用户ID列表，逗号分隔")
        targetUserIds: String,
        @ToolParam(
            name = "groupId",
            description = "用户组ID（直接模式，与智能模式二选一）",
            required = false
        )
        groupId: Int? = null,
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
            description = "过期天数，默认365",
            required = false
        )
        expiredDays: Long? = null,
        @ToolParam(
            name = "dryRun",
            description = "预检查模式，默认 true 仅预览推荐结果，设为 false 执行实际授权",
            required = false
        )
        dryRun: Boolean = true
    ): String {
        val users = parseCommaSeparated(targetUserIds)
        val operatorId = getOperatorUserId()

        return safeOperate(
            "AuthTool", "grantPermissions", mapOf(
                "projectId" to projectId,
                "targetUserIds" to targetUserIds,
                "groupId" to groupId,
                "resourceType" to resourceType,
                "resourceCode" to resourceCode,
                "action" to action,
                "dryRun" to dryRun
            )
        ) {
            val targetGroupId: Int
            val groupInfo: String

            if (groupId != null) {
                targetGroupId = groupId
                groupInfo = "指定用户组 $groupId"
            } else if (resourceType != null && resourceCode != null && action != null) {
                val recommendResult = authAiResource().recommendGroupsForGrant(
                    userId = operatorId,
                    projectId = projectId,
                    request = GroupRecommendReq(
                        resourceType = resourceType,
                        resourceCode = resourceCode,
                        action = action,
                        targetUserId = users.first()
                    )
                )
                val recommendation = recommendResult.data
                    ?: return@safeOperate "推荐失败: ${recommendResult.message}"

                if (recommendation.candidateGroups.isEmpty()) {
                    return@safeOperate "未找到包含权限 $action 的用户组，" +
                            "请检查资源类型和操作是否正确"
                }

                val bestGroup = recommendation.candidateGroups.first()
                targetGroupId = bestGroup.relationId

                val sb = StringBuilder()
                sb.appendLine("=== 智能推荐结果 ===")
                sb.appendLine("目标: $resourceType/$resourceCode 的 $action 权限")
                sb.appendLine("推荐用户组: ${bestGroup.groupName} (ID: ${bestGroup.relationId})")
                sb.appendLine("推荐理由: ${bestGroup.tags.joinToString(", ") { it.text }}")
                if (recommendation.candidateGroups.size > 1) {
                    sb.appendLine(
                        "其他候选用户组: ${
                            recommendation.candidateGroups.drop(1).take(3)
                                .joinToString { "${it.groupName}(${it.relationId})" }
                        }"
                    )
                }
                groupInfo = sb.toString()

                if (dryRun) {
                    return@safeOperate groupInfo + "\n\n预检查模式，未执行实际授权。" +
                            "如需执行，请设置 dryRun=false 或使用推荐的 groupId=$targetGroupId 再次调用"
                }
            } else {
                return@safeOperate "参数错误：请提供 groupId（直接模式）或 " +
                        "resourceType + resourceCode + action（智能模式）"
            }

            if (dryRun) {
                return@safeOperate "预检查模式：将把用户 ${users.joinToString(", ")} " +
                    "添加到$groupInfo。如需执行，请设置 dryRun=false"
            }

            val createInfo = ProjectCreateUserInfo(
                createUserId = null,
                roleName = null,
                roleId = null,
                groupId = targetGroupId,
                userIds = users,
                deptIds = emptyList(),
                resourceType = null,
                resourceCode = null,
                expiredTime = expiredDays ?: DEFAULT_EXPIRED_DAYS
            )
            val result = authAiResource().addGroupMembers(
                userId = operatorId,
                projectId = projectId,
                createInfo = createInfo
            )
            if (result.data == true) {
                "成功将 ${users.joinToString(", ")} 添加到用户组 $targetGroupId"
            } else {
                "授权失败: ${result.message}"
            }
        }
    }

    @Tool(
        name = "续期成员权限",
        description = "仅管理员可使用，续期用户在用户组中的权限有效期。\n" +
                "默认 dryRun=true 仅预览续期信息，设为 false 执行实际续期。" +
                "仅管理员均可使用"
    )
    fun renewPermissions(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "groupIds", description = "用户组ID列表，逗号分隔")
        groupIds: String,
        @ToolParam(name = "targetMemberId", description = "目标成员ID")
        targetMemberId: String,
        @ToolParam(name = "renewalDays", description = "续期天数")
        renewalDays: Int,
        @ToolParam(
            name = "dryRun",
            description = "预检查模式，默认 true 仅预览，设为 false 执行续期",
            required = false
        )
        dryRun: Boolean = true
    ): String {
        val operatorId = getOperatorUserId()
        val ids = parseCommaSeparatedInts(groupIds)

        return safeOperate(
            "AuthTool", "renewPermissions", mapOf(
                "projectId" to projectId,
                "groupIds" to groupIds,
                "targetMemberId" to targetMemberId,
                "renewalDays" to renewalDays,
                "dryRun" to dryRun
            )
        ) {
            if (dryRun) {
                val sb = StringBuilder()
                sb.appendLine("=== 续期预览 ===")
                sb.appendLine("目标用户: $targetMemberId")
                sb.appendLine("续期用户组数: ${ids.size}")
                sb.appendLine("续期天数: $renewalDays")
                sb.appendLine("\n预览模式，未执行续期。如需执行，请设置 dryRun=false")
                return@safeOperate sb.toString()
            }

            val result = authAiResource().batchRenewalMembers(
                userId = operatorId,
                projectId = projectId,
                request = BatchRenewalMembersReq(
                    groupIds = ids,
                    targetMemberId = targetMemberId,
                    renewalDuration = renewalDays
                )
            )
            if (result.data == true) {
                "成功为用户 $targetMemberId 续期 ${ids.size} 个用户组，续期 $renewalDays 天"
            } else {
                "续期失败: ${result.message}"
            }
        }
    }

    @Tool(
        name = "撤销/移除成员权限",
        description = "移除用户的权限或将权限交接给他人。内置预检查和影响分析。\n" +
                "提供 handoverTo 时执行交接，不提供时直接移除。\n" +
                "默认 dryRun=true 仅预检查影响，设为 false 执行实际操作。" +
                "仅管理员可使用"
    )
    fun revokePermissions(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "groupIds", description = "用户组ID列表，逗号分隔")
        groupIds: String,
        @ToolParam(name = "targetMemberId", description = "目标成员ID")
        targetMemberId: String,
        @ToolParam(
            name = "handoverTo",
            description = "交接人ID（可选，提供时执行交接而非移除）",
            required = false
        )
        handoverTo: String? = null,
        @ToolParam(
            name = "dryRun",
            description = "预检查模式，默认 true 仅分析影响，设为 false 执行操作",
            required = false
        )
        dryRun: Boolean = true
    ): String {
        val operatorId = getOperatorUserId()
        val ids = parseCommaSeparatedInts(groupIds)
        val isHandover = !handoverTo.isNullOrBlank()
        val operateType = if (isHandover) BatchOperateType.HANDOVER else BatchOperateType.REMOVE

        return safeOperate(
            "AuthTool", "revokePermissions", mapOf(
                "projectId" to projectId,
                "groupIds" to groupIds,
                "targetMemberId" to targetMemberId,
                "handoverTo" to handoverTo,
                "dryRun" to dryRun
            )
        ) {
            val checkResult = authAiResource().batchOperateCheck(
                userId = operatorId,
                projectId = projectId,
                batchOperateType = operateType,
                request = BatchOperateCheckReq(
                    groupIds = ids,
                    targetMemberId = targetMemberId
                )
            )
            val check = checkResult.data

            val sb = StringBuilder()
            sb.appendLine("=== ${if (isHandover) "交接" else "移除"}预检查 ===")
            sb.appendLine("目标用户: $targetMemberId")
            sb.appendLine("涉及用户组数: ${ids.size}")
            if (isHandover) {
                sb.appendLine("交接给: $handoverTo")
            }

            var hasRisk = false
            if (check != null) {
                if ((check.uniqueManagerCount ?: 0) > 0) {
                    sb.appendLine("⚠️ 风险: ${check.uniqueManagerCount} 个唯一管理员组")
                    hasRisk = true
                }
                if ((check.invalidPipelineAuthorizationCount ?: 0) > 0) {
                    sb.appendLine("⚠️ 风险: ${check.invalidPipelineAuthorizationCount} 个流水线授权将失效")
                    hasRisk = true
                }
                if ((check.invalidRepositoryAuthorizationCount ?: 0) > 0) {
                    sb.appendLine("⚠️ 风险: ${check.invalidRepositoryAuthorizationCount} 个代码库授权将失效")
                    hasRisk = true
                }
                if ((check.invalidEnvNodeAuthorizationCount ?: 0) > 0) {
                    sb.appendLine("⚠️ 风险: ${check.invalidEnvNodeAuthorizationCount} 个环境节点授权将失效")
                    hasRisk = true
                }
                if (check.needToHandover == true) {
                    sb.appendLine("⚠️ 需要交接: 可交接 ${check.canHandoverCount ?: 0} 个用户组")
                    hasRisk = true
                }
            }

            if (hasRisk && !isHandover) {
                sb.appendLine("\n建议: 存在风险，建议指定 handoverTo 进行交接而非直接移除")
            }

            if (dryRun) {
                sb.appendLine("\n预检查模式，未执行操作。如需执行，请设置 dryRun=false")
                return@safeOperate sb.toString()
            }

            if (isHandover) {
                val result = authAiResource().batchHandoverMembers(
                    userId = operatorId,
                    projectId = projectId,
                    request = BatchHandoverMembersReq(
                        groupIds = ids,
                        targetMemberId = targetMemberId,
                        handoverToMemberId = handoverTo
                    )
                )
                if (result.data == true) {
                    "成功将用户 $targetMemberId 在 ${ids.size} 个用户组中的权限交接给 $handoverTo"
                } else {
                    "交接失败: ${result.message}"
                }
            } else {
                val result = authAiResource().batchRemoveMembers(
                    userId = operatorId,
                    projectId = projectId,
                    request = BatchRemoveMembersReq(
                        groupIds = ids,
                        targetMemberId = targetMemberId,
                        handoverToMemberId = null
                    )
                )
                if (result.data == true) {
                    "成功将用户 $targetMemberId 从 ${ids.size} 个用户组中移除"
                } else {
                    "移除失败: ${result.message}"
                }
            }
        }
    }

    @Tool(
        name = "移出项目成员",
        description = "管理员将用户从项目中移出（支持批量）。内置检查和交接人推荐。\n" +
                "默认 dryRun=true 仅检查是否可移出并推荐交接人，设为 false 执行移出。" +
                "仅管理员可使用"
    )
    fun removeMemberFromProject(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(
            name = "targetMemberIds",
            description = "要移出的成员ID列表，逗号分隔（支持批量）"
        )
        targetMemberIds: String,
        @ToolParam(
            name = "handoverTo",
            description = "交接人用户ID（不能直接移出时必填）",
            required = false
        )
        handoverTo: String? = null,
        @ToolParam(
            name = "dryRun",
            description = "预检查模式，默认 true 仅检查，设为 false 执行移出",
            required = false
        )
        dryRun: Boolean = true
    ): String {
        val operatorId = getOperatorUserId()
        val memberIds = parseCommaSeparated(targetMemberIds)

        return safeOperate(
            "AuthTool", "removeMemberFromProject", mapOf(
                "projectId" to projectId,
                "targetMemberIds" to targetMemberIds,
                "handoverTo" to handoverTo,
                "dryRun" to dryRun
            )
        ) {
            val sb = StringBuilder()
            sb.appendLine("=== 移出成员检查 ===")
            sb.appendLine("项目: $projectId")
            sb.appendLine("目标成员: ${memberIds.joinToString(", ")}")

            var allCanDirectExit = true
            val memberChecks = mutableMapOf<String, MemberExitCheckVO>()

            for (memberId in memberIds) {
                val checkResult = authAiResource().checkMemberExitWithRecommendation(
                    userId = operatorId,
                    projectId = projectId,
                    targetMemberId = memberId,
                    handoverTo = handoverTo,
                    groupIds = null,
                    recommendLimit = 3
                )
                val check = checkResult.data
                if (check != null) {
                    memberChecks[memberId] = check
                    if (!check.canExitDirectly) {
                        allCanDirectExit = false
                    }
                }
            }

            sb.appendLine("可直接移出: $allCanDirectExit")

            var anyHandoverBlocked = false
            memberChecks.forEach { (memberId, check) ->
                sb.appendLine("\n--- 成员 $memberId ---")
                sb.appendLine("可直接移出: ${check.canExitDirectly}")

                if (!check.canExitDirectly) {
                    val auth = check.authorizationsToHandover
                    if (auth != null) {
                        sb.appendLine("需要交接:")
                        if (auth.uniqueManagerGroups > 0) {
                            sb.appendLine("  - 唯一管理员组: ${auth.uniqueManagerGroups}")
                        }
                        if (auth.pipeline > 0) {
                            sb.appendLine("  - 流水线授权: ${auth.pipeline}")
                        }
                        if (auth.repertory > 0) {
                            sb.appendLine("  - 代码库授权: ${auth.repertory}")
                        }
                        if (auth.envNode > 0) {
                            sb.appendLine("  - 环境节点授权: ${auth.envNode}")
                        }
                    }
                }

                if (check.hasDepartmentJoined) {
                    sb.appendLine("⚠️ 通过组织加入了部分用户组 (${check.departments})")
                    sb.appendLine("   移出后可能仍保留组织带来的权限")
                }

                if (handoverTo != null && !check.canExitDirectly) {
                    if (check.handoverToCanReceiveAll) {
                        sb.appendLine("✓ 交接人 $handoverTo 可接收该成员全部授权")
                    } else if (check.handoverToCannotReceive.isNotEmpty()) {
                        anyHandoverBlocked = true
                        sb.appendLine("✗ 交接人 $handoverTo 无法接收以下授权:")
                        check.handoverToCannotReceive.forEach { authType ->
                            val reason = check.handoverToCannotReceiveReasons[authType] ?: "未知原因"
                            sb.appendLine("  - ${translateAuthType(authType)}: $reason")
                        }
                    }
                }

                if (!check.suggestion.isNullOrBlank()) {
                    sb.appendLine("建议: ${check.suggestion}")
                }
            }

            if (handoverTo != null && anyHandoverBlocked) {
                sb.appendLine("\n请先前往「授权管理」页面处理以上授权问题，或更换交接人后重试")
                val allCandidates = memberChecks.values
                    .flatMap { it.recommendedCandidates }
                    .distinctBy { it.userId }
                    .sortedByDescending { it.canReceiveAll }
                if (allCandidates.isNotEmpty()) {
                    sb.appendLine("\n其他推荐交接人:")
                    allCandidates.take(3).forEachIndexed { i, c ->
                        val managerTag = if (c.isManager) " [管理员]" else ""
                        val receiveTag = if (c.canReceiveAll) " ✓" else " ⚠️"
                        sb.appendLine("${i + 1}. ${c.userId}$managerTag$receiveTag")
                    }
                }
            } else if (!allCanDirectExit && handoverTo == null) {
                val allCandidates = memberChecks.values
                    .flatMap { it.recommendedCandidates }
                    .distinctBy { it.userId }
                    .sortedByDescending { it.canReceiveAll }
                if (allCandidates.isNotEmpty()) {
                    sb.appendLine("\n推荐交接人:")
                    allCandidates.take(3).forEachIndexed { i, c ->
                        val managerTag = if (c.isManager) " [管理员]" else ""
                        val receiveTag = if (c.canReceiveAll) " ✓ 可接收所有授权" else " ⚠️ 部分授权无法接收"
                        sb.appendLine("${i + 1}. ${c.userId}$managerTag$receiveTag")
                    }
                }
            }

            if (dryRun) {
                sb.appendLine("\n预检查模式，未执行移出。如需执行，请设置 dryRun=false")
                return@safeOperate sb.toString()
            }

            if (!allCanDirectExit && handoverTo.isNullOrBlank()) {
                return@safeOperate sb.toString() + "\n无法直接移出，请指定交接人 handoverTo"
            }

            if (anyHandoverBlocked) {
                sb.appendLine("\n❌ 无法执行移出：交接人 $handoverTo 无法接收部分成员的全部授权")
                sb.appendLine("请先前往「授权管理」页面处理授权问题，或更换交接人后重试")
                return@safeOperate sb.toString()
            }

            val result = authAiResource().batchRemoveMemberFromProject(
                userId = operatorId,
                projectId = projectId,
                request = AiBatchRemoveMemberFromProjectReq(
                    targetMemberIds = memberIds,
                    handoverToMemberId = handoverTo?.takeIf { it.isNotBlank() }
                )
            )
            val response = result.data
            if (response == null) {
                "移出失败: ${result.message}"
            } else {
                val total = response.users.size + response.departments.size
                "已成功将 ${memberIds.joinToString(", ")} 从项目 $projectId 中移出" +
                        if (total > 0) "，涉及 ${response.users.size} 个用户和 ${response.departments.size} 个部门" else ""
            }
        }
    }

    @Tool(
        name = "权限克隆",
        description = "将一个用户的权限复制给另一个用户（管理员操作）。\n" +
                "默认 dryRun=true 仅预览要复制的权限，设为 false 执行克隆。"
    )
    fun clonePermissions(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "sourceUserId", description = "来源用户ID")
        sourceUserId: String,
        @ToolParam(name = "targetUserId", description = "目标用户ID")
        targetUserId: String,
        @ToolParam(
            name = "resourceTypes",
            description = "限定资源类型，逗号分隔（可选，不传则复制所有）",
            required = false
        )
        resourceTypes: String? = null,
        @ToolParam(
            name = "dryRun",
            description = "预检查模式，默认 true 仅预览，设为 false 执行克隆",
            required = false
        )
        dryRun: Boolean = true
    ): String {
        return safeOperate(
            "AuthTool", "clonePermissions", mapOf(
                "projectId" to projectId,
                "sourceUserId" to sourceUserId,
                "targetUserId" to targetUserId,
                "resourceTypes" to resourceTypes,
                "dryRun" to dryRun
            )
        ) {
            val result = authAiResource().clonePermissions(
                userId = getOperatorUserId(),
                projectId = projectId,
                sourceUserId = sourceUserId,
                targetUserId = targetUserId,
                resourceTypes = resourceTypes,
                dryRun = dryRun
            )
            val vo = result.data ?: return@safeOperate "克隆失败: ${result.message}"

            val sb = StringBuilder()
            sb.appendLine("=== 权限克隆${if (dryRun) "预览" else "结果"} ===")
            sb.appendLine("来源用户: $sourceUserId")
            sb.appendLine("目标用户: $targetUserId")
            sb.appendLine("要克隆的用户组数: ${vo.groupsToClone.size}")

            if (vo.groupsToClone.isNotEmpty()) {
                sb.appendLine("\n用户组列表:")
                vo.groupsToClone.take(10).forEach { group ->
                    sb.appendLine("- ${group.groupName} (${group.resourceType})")
                }
                if (vo.groupsToClone.size > 10) {
                    sb.appendLine("... 还有 ${vo.groupsToClone.size - 10} 个")
                }
            }

            if (vo.skippedGroups.isNotEmpty()) {
                sb.appendLine("\n跳过的用户组（目标用户已有）: ${vo.skippedGroups.size}")
            }

            if (dryRun) {
                sb.appendLine("\n预检查模式，未执行克隆。如需执行，请设置 dryRun=false")
            } else {
                sb.appendLine("\n✓ 克隆完成")
            }
            sb.toString()
        }
    }
}
