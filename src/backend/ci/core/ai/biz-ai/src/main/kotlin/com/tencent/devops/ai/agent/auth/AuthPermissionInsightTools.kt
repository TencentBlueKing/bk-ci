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
 */

package com.tencent.devops.ai.agent.auth

import com.tencent.devops.auth.pojo.enum.JoinedType
import com.tencent.devops.auth.pojo.request.ai.GroupRecommendReq
import com.tencent.devops.auth.pojo.vo.PermissionDiagnoseVO
import com.tencent.devops.auth.pojo.vo.UserPermissionAnalysisVO
import com.tencent.devops.common.client.Client
import io.agentscope.core.tool.Tool
import io.agentscope.core.tool.ToolParam
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Supplier

/**
 * 权限分析、资源权限视图与提效类只读工具。
 */
class AuthPermissionInsightTools(
    client: Client,
    userIdSupplier: Supplier<String>
) : AuthAiToolsBase(client, userIdSupplier) {

    override val logger: Logger = LoggerFactory.getLogger(AuthPermissionInsightTools::class.java)

    @Tool(
        name = "分析用户权限",
        description = "分析用户在项目中的权限概况，生成权限分析报告。" +
                "返回：角色、各资源类型用户组数、过期统计、继承来源摘要、告警。" +
                "管理员可查任意成员，普通用户只能查自己。"
    )
    fun analyzeUserPermissions(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "memberId", description = "目标成员ID（普通用户只能查自己）")
        memberId: String
    ): String {
        return safeQuery("AuthTool", "analyzeUserPermissions") {
            val result = authAiResource().analyzeUserPermissions(
                userId = getOperatorUserId(),
                projectId = projectId,
                memberId = memberId
            )
            val vo = result.data ?: return@safeQuery "分析失败: ${result.message}"
            renderUserPermissionAnalysis(memberId, vo)
        }
    }

    @Tool(
        name = "查询资源权限矩阵",
        description = "查询某个资源（如流水线）的权限矩阵，展示所有关联用户组及其权限和成员。" +
                "管理员可查任意资源，普通用户只能看到自己所在的用户组。"
    )
    fun getResourcePermissionsMatrix(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "resourceType", description = "资源类型，如 pipeline")
        resourceType: String,
        @ToolParam(name = "resourceCode", description = "资源Code")
        resourceCode: String
    ): String {
        return safeQuery("AuthTool", "getResourcePermissionsMatrix") {
            val result = authAiResource().getResourcePermissionsMatrix(
                userId = getOperatorUserId(),
                projectId = projectId,
                resourceType = resourceType,
                resourceCode = resourceCode
            )
            val vo = result.data ?: return@safeQuery "查询失败: ${result.message}"
            if (vo.groups.isEmpty()) return@safeQuery "资源 ${vo.resourceName} 暂无关联用户组"
            toJson(vo)
        }
    }

    @Tool(
        name = "智能推荐用户组",
        description = "智能推荐用户组。根据目标资源、操作权限和用户，推荐最合适的用户组，" +
                "按最小权限原则排序并附带推荐/警告标签。" +
                "普通成员/管理员均可使用。"
    )
    fun recommendGroupsForGrant(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "resourceType", description = "资源类型，如 pipeline")
        resourceType: String,
        @ToolParam(name = "resourceCode", description = "资源Code")
        resourceCode: String,
        @ToolParam(name = "action", description = "目标操作权限，如 pipeline_execute")
        action: String,
        @ToolParam(name = "targetUserId", description = "要授权的目标用户ID")
        targetUserId: String
    ): String {
        return safeQuery("AuthTool", "recommendGroupsForGrant") {
            val result = authAiResource().recommendGroupsForGrant(
                userId = getOperatorUserId(),
                projectId = projectId,
                request = GroupRecommendReq(
                    resourceType = resourceType,
                    resourceCode = resourceCode,
                    action = action,
                    targetUserId = targetUserId
                )
            )
            val vo = result.data ?: return@safeQuery "推荐失败: ${result.message}"
            if (vo.candidateGroups.isEmpty()) return@safeQuery "未找到包含该权限的用户组"
            toJson(vo)
        }
    }

    @Tool(
        name = "权限诊断",
        description = "诊断用户为什么有或没有某个权限。分析原因并给出解决建议。\n" +
                "适用场景：解释权限来源，或用户反馈无权限时快速定位问题原因。"
    )
    fun diagnosePermission(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "memberId", description = "目标用户ID")
        memberId: String,
        @ToolParam(name = "resourceType", description = "资源类型，如 pipeline")
        resourceType: String,
        @ToolParam(name = "resourceCode", description = "资源Code")
        resourceCode: String,
        @ToolParam(name = "action", description = "操作类型，如 pipeline_execute")
        action: String
    ): String {
        return safeQuery("AuthTool", "diagnosePermission") {
            val result = authAiResource().diagnosePermission(
                userId = getOperatorUserId(),
                projectId = projectId,
                memberId = memberId,
                resourceType = resourceType,
                resourceCode = resourceCode,
                action = action
            )
            val vo = result.data ?: return@safeQuery "诊断失败: ${result.message}"
            renderPermissionDiagnosis(memberId, vo)
        }
    }

    @Tool(
        name = "权限对比",
        description = "对比两个用户的权限差异。\n" +
                "返回：共同拥有的权限、用户A独有、用户B独有。"
    )
    fun comparePermissions(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "userIdA", description = "用户A的ID")
        userIdA: String,
        @ToolParam(name = "userIdB", description = "用户B的ID")
        userIdB: String,
        @ToolParam(
            name = "resourceType",
            description = "限定资源类型（可选）",
            required = false
        )
        resourceType: String? = null
    ): String {
        return safeQuery("AuthTool", "comparePermissions") {
            val result = authAiResource().comparePermissions(
                userId = getOperatorUserId(),
                projectId = projectId,
                userIdA = userIdA,
                userIdB = userIdB,
                resourceType = resourceType
            )
            val vo = result.data ?: return@safeQuery "对比失败: ${result.message}"

            val sb = StringBuilder()
            sb.appendLine("=== 权限对比结果 ===")
            sb.appendLine("用户A: $userIdA")
            sb.appendLine("用户B: $userIdB")

            sb.appendLine("\n共同权限 (${vo.commonGroups.size}):")
            if (vo.commonGroups.isEmpty()) {
                sb.appendLine("  无")
            } else {
                vo.commonGroups.take(5).forEach { sb.appendLine("  - ${it.groupName}") }
                if (vo.commonGroups.size > 5) sb.appendLine("  ... 还有 ${vo.commonGroups.size - 5} 个")
            }

            sb.appendLine("\n$userIdA 独有 (${vo.onlyInA.size}):")
            if (vo.onlyInA.isEmpty()) {
                sb.appendLine("  无")
            } else {
                vo.onlyInA.take(5).forEach { sb.appendLine("  - ${it.groupName}") }
                if (vo.onlyInA.size > 5) sb.appendLine("  ... 还有 ${vo.onlyInA.size - 5} 个")
            }

            sb.appendLine("\n$userIdB 独有 (${vo.onlyInB.size}):")
            if (vo.onlyInB.isEmpty()) {
                sb.appendLine("  无")
            } else {
                vo.onlyInB.take(5).forEach { sb.appendLine("  - ${it.groupName}") }
                if (vo.onlyInB.size > 5) sb.appendLine("  ... 还有 ${vo.onlyInB.size - 5} 个")
            }

            sb.toString()
        }
    }

    @Tool(
        name = "授权健康检查",
        description = "扫描项目的授权健康状况（管理员操作）。\n" +
                "检查：授权分布、风险授权（离职人员持有等）、即将过期的权限等。"
    )
    fun checkAuthorizationHealth(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String
    ): String {
        return safeQuery("AuthTool", "checkAuthorizationHealth") {
            val result = authAiResource().checkAuthorizationHealth(
                userId = getOperatorUserId(),
                projectId = projectId
            )
            val vo = result.data ?: return@safeQuery "检查失败: ${result.message}"

            val sb = StringBuilder()
            sb.appendLine("=== 授权健康检查 ===")
            sb.appendLine("项目: $projectId")
            sb.appendLine("健康状态: ${vo.healthStatus}")
            sb.appendLine("风险数: ${vo.riskCount}, 警告数: ${vo.warningCount}")

            val stats = vo.authorizationStats
            sb.appendLine("\n授权统计:")
            sb.appendLine("- 流水线授权: ${stats.pipelineAuthorizationCount}")
            sb.appendLine("- 代码库授权: ${stats.repertoryAuthorizationCount}")
            sb.appendLine("- 环境节点授权: ${stats.envNodeAuthorizationCount}")
            sb.appendLine("- 唯一管理员组: ${stats.uniqueManagerGroupCount}")
            sb.appendLine("- 授权人总数: ${stats.totalAuthorizerCount}")

            if (vo.risks.isNotEmpty()) {
                sb.appendLine("\n⚠️ 风险项 (${vo.risks.size}):")
                vo.risks.take(5).forEach { risk ->
                    sb.appendLine("- [${risk.level}] ${risk.description}")
                    if (!risk.suggestion.isNullOrBlank()) {
                        sb.appendLine("  建议: ${risk.suggestion}")
                    }
                }
                if (vo.risks.size > 5) sb.appendLine("... 还有 ${vo.risks.size - 5} 个")
            }

            if (vo.suggestions.isNotEmpty()) {
                sb.appendLine("\n建议:")
                vo.suggestions.forEach { sb.appendLine("• $it") }
            }

            sb.toString()
        }
    }
}

internal fun renderUserPermissionAnalysis(
    memberId: String,
    analysis: UserPermissionAnalysisVO
): String {
    val sb = StringBuilder()
    sb.appendLine("=== 用户权限分析 ===")
    sb.appendLine("用户: $memberId")
    sb.appendLine("角色: ${analysis.roleDisplayName}")
    sb.appendLine("用户组总数: ${analysis.totalGroupCount}")
    sb.appendLine("过期用户组: ${analysis.expiredGroupCount}")
    sb.appendLine("授权总数: ${analysis.totalAuthorizationCount}")
    sb.appendLine("项目全量权限: ${if (analysis.hasAllPermissions) "是" else "否"}")

    if (analysis.resourceSummary.isNotEmpty()) {
        sb.appendLine("\n资源类型概况:")
        analysis.resourceSummary.forEach { summary ->
            sb.appendLine("- ${summary.resourceTypeName}: ${summary.groupCount} 个用户组")
        }
    }

    if (analysis.inheritedGroups.isNotEmpty()) {
        sb.appendLine("\n代表性的继承来源:")
        analysis.inheritedGroups.forEach { group ->
            val source = formatJoinSource(
                joinedType = group.joinedType,
                joinedMemberName = group.joinedMemberName,
                joinedMemberId = group.joinedMemberId,
                groupName = group.groupName
            )
            sb.appendLine("- $source (${group.managementLevel}: ${group.resourceName})")
        }
    }

    if (analysis.warnings.isNotEmpty()) {
        sb.appendLine("\n告警:")
        analysis.warnings.forEach { warning ->
            sb.appendLine("- $warning")
        }
    }
    return sb.toString()
}

@Suppress("NestedBlockDepth")
internal fun renderPermissionDiagnosis(
    memberId: String,
    diagnose: PermissionDiagnoseVO
): String {
    val sb = StringBuilder()
    sb.appendLine("=== 权限诊断结果 ===")
    sb.appendLine("用户: $memberId")
    sb.appendLine("资源: ${diagnose.resourceName} (${diagnose.resourceType})")
    sb.appendLine("操作: ${diagnose.actionName} (${diagnose.action})")
    sb.appendLine("当前状态: ${if (diagnose.hasPermission) "✓ 有权限" else "✗ 无权限"}")

    if (diagnose.hasPermission) {
        if (diagnose.permissionSourceGroups.isNotEmpty()) {
            sb.appendLine("\n权限来源:")
            diagnose.permissionSourceGroups.forEach { group ->
                val joinSource = formatJoinSource(
                    joinedType = group.joinedType,
                    joinedMemberName = group.joinedMemberName,
                    joinedMemberId = group.joinedMemberId,
                    groupName = group.groupName
                )
                val permissions = group.permissions.joinToString("、")
                sb.appendLine("- $joinSource")
                sb.appendLine("  范围: ${group.managementLevel} / ${group.managementScope}")
                if (permissions.isNotBlank()) {
                    sb.appendLine("  权限: $permissions")
                }
            }
        }
    } else {
        if (!diagnose.missingReason.isNullOrBlank()) {
            sb.appendLine("\n缺失原因: ${diagnose.missingReason}")
        }
        if (diagnose.applicableGroups.isNotEmpty()) {
            sb.appendLine("\n可申请的用户组:")
            diagnose.applicableGroups.take(5).forEach { group ->
                sb.appendLine("- ${group.groupName} (ID: ${group.groupId})")
            }
        }
        if (diagnose.groupManagers.isNotEmpty()) {
            sb.appendLine("\n可联系管理员: ${diagnose.groupManagers.joinToString(", ")}")
        }
    }

    if (!diagnose.suggestion.isNullOrBlank()) {
        sb.appendLine("\n建议: ${diagnose.suggestion}")
    }
    return sb.toString()
}

private fun formatJoinSource(
    joinedType: JoinedType,
    joinedMemberName: String?,
    joinedMemberId: String?,
    groupName: String
): String {
    val sourceName = joinedMemberName ?: joinedMemberId
    return when (joinedType) {
        JoinedType.DEPARTMENT -> "通过组织 $sourceName 继承到用户组 $groupName"
        JoinedType.TEMPLATE -> "通过用户组模板 $sourceName 继承到用户组 $groupName"
        JoinedType.DIRECT -> "直接加入用户组 $groupName"
    }
}
