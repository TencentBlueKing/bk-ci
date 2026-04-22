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

import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.auth.pojo.dto.IamGroupIdsQueryConditionDTO
import com.tencent.devops.common.client.Client
import io.agentscope.core.tool.Tool
import io.agentscope.core.tool.ToolParam
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Supplier

/**
 * 用户组与成员关系的只读查询工具。
 */
class AuthGroupMemberQueryTools(
    client: Client,
    userIdSupplier: Supplier<String>
) : AuthAiToolsBase(client, userIdSupplier) {

    override val logger: Logger = LoggerFactory.getLogger(AuthGroupMemberQueryTools::class.java)

    @Tool(
        name = "检查用户角色",
        description = "检查用户在项目中的角色和成员身份。" +
                "不传 memberId 时检查当前用户自己的角色（当系统提示词中用户角色为'待确定'时必须先调用此工具）；" +
                "传 memberId 时检查指定用户的角色和成员身份。" +
                "返回：角色（管理员/普通成员）、是否是项目成员。"
    )
    fun checkRole(
        @ToolParam(name = "projectId", description = "项目ID（英文标识）")
        projectId: String,
        @ToolParam(
            name = "memberId",
            description = "要检查的用户ID（可选，不传则检查当前用户自己）",
            required = false
        )
        memberId: String? = null
    ): String {
        val targetUserId = memberId ?: getOperatorUserId()
        return safeQuery("AuthTool", "checkRole") {
            val isMember = try {
                client.get(ServiceProjectAuthResource::class)
                    .isProjectMember(userId = targetUserId, projectCode = projectId)
                    .data ?: false
            } catch (e: Exception) {
                logger.debug("[AuthTool] Failed to check member status for {}", targetUserId, e)
                false
            }

            if (!isMember) {
                return@safeQuery "用户 $targetUserId 不是项目 $projectId 的成员"
            }

            val isManager = try {
                client.get(ServiceProjectAuthResource::class)
                    .checkProjectManagerAndMessage(userId = targetUserId, projectId = projectId)
                true
            } catch (e: Exception) {
                logger.debug("[AuthTool] User {} is not manager of {}", targetUserId, projectId)
                false
            }

            val role = if (isManager) "管理员" else "普通成员"
            val userDesc = if (memberId == null) "你" else "用户 $targetUserId"
            "$userDesc 是项目 $projectId 的成员，角色: $role"
        }
    }

    @Tool(
        name = "查询用户组列表",
        description = "查询项目下的用户组列表（分页返回，避免一次数据过大）。" +
                "可根据资源类型、资源Code、组名过滤。" +
                "relationId 即用户组的 IAM 关联ID，用于后续操作（如添加/移除成员）。" +
                "total 为匹配总数，records 为本页数据；翻页增大 page。" +
                "普通成员/管理员均可使用"
    )
    fun listGroups(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(
            name = "resourceType",
            description = "资源类型标识，如 project、pipeline 等（可选）",
            required = false
        )
        resourceType: String? = null,
        @ToolParam(
            name = "resourceCode",
            description = "资源Code（可选）",
            required = false
        )
        resourceCode: String? = null,
        @ToolParam(
            name = "groupName",
            description = "用户组名称搜索（可选）",
            required = false
        )
        groupName: String? = null,
        @ToolParam(
            name = "page",
            description = "页码，从 1 开始，默认 1",
            required = false
        )
        page: Int? = null,
        @ToolParam(
            name = "pageSize",
            description = "每页条数，默认 20，最大 50",
            required = false
        )
        pageSize: Int? = null
    ): String {
        return safeQuery("AuthTool", "listGroups") {
            val actualPageSize = (pageSize ?: AuthAiToolsBase.DEFAULT_PAGE_SIZE)
                .coerceIn(1, AuthAiToolsBase.MAX_PAGE_SIZE)
            val condition = IamGroupIdsQueryConditionDTO(
                projectCode = projectId,
                relatedResourceType = resourceType,
                relatedResourceCode = resourceCode,
                groupName = groupName,
                page = (page ?: 1).coerceAtLeast(1),
                pageSize = actualPageSize
            )
            val result = authAiResource().listGroups(
                userId = getOperatorUserId(),
                projectId = projectId,
                condition = condition
            )
            val pageData = result.data
                ?: return@safeQuery "查询用户组失败：服务无返回数据"
            if (pageData.count == 0L) {
                return@safeQuery "未找到符合条件的用户组"
            }
            if (pageData.records.isEmpty()) {
                return@safeQuery "本页无数据（共 ${pageData.count} 条），请调整 page 后重试"
            }
            toJson(pageData)
        }
    }

    @Tool(
        name = "查询用户组权限详情",
        description = "查询用户组的权限详情，" +
                "包括该组拥有哪些资源的哪些操作权限。" +
                "普通成员/管理员均可使用"
    )
    fun getGroupPermissionDetail(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "groupId", description = "用户组ID（IAM组ID/relationId）")
        groupId: Int
    ): String {
        return safeQuery("AuthTool", "getGroupPermissionDetail") {
            val result = authAiResource().getGroupPermissionDetail(
                userId = getOperatorUserId(),
                projectId = projectId,
                groupId = groupId
            )
            val permMap = result.data
            if (permMap.isNullOrEmpty()) return@safeQuery "用户组 $groupId 暂无权限配置"
            toJson(permMap)
        }
    }

    @Tool(
        name = "查询用户组成员列表-",
        description = "查询用户组成员详情列表，支持丰富的筛选条件。" +
                "返回成员详细信息（含过期时间、成员类型等）。" +
                "可按资源类型/Code、用户组ID/类型、成员ID/类型、过期时间范围筛选。" +
                "仅管理员身份可使用"
    )
    fun listGroupMembers(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(
            name = "resourceType",
            description = "资源类型过滤，如 project、pipeline（可选）",
            required = false
        )
        resourceType: String? = null,
        @ToolParam(
            name = "resourceCode",
            description = "资源Code过滤（可选）",
            required = false
        )
        resourceCode: String? = null,
        @ToolParam(
            name = "iamGroupId",
            description = "用户组ID（IAM组ID/relationId）过滤（可选）",
            required = false
        )
        iamGroupId: Int? = null,
        @ToolParam(
            name = "groupCode",
            description = "用户组类型过滤，如 manager/developer/viewer（可选）",
            required = false
        )
        groupCode: String? = null,
        @ToolParam(
            name = "memberType",
            description = "成员类型过滤：user/department/template（可选）",
            required = false
        )
        memberType: String? = null,
        @ToolParam(
            name = "expiredStatus",
            description = "过期状态过滤（可选）：expired=已过期，valid=未过期，expiring_soon=即将过期（默认30天内），不传=全部",
            required = false
        )
        expiredStatus: String? = null,
        @ToolParam(
            name = "expireWithinDays",
            description = "过期天数（可选），配合 expiredStatus=expiring_soon 使用，默认30天",
            required = false
        )
        expireWithinDays: Int? = null,
        @ToolParam(name = "page", description = "页码，默认1", required = false)
        page: Int? = null,
        @ToolParam(
            name = "pageSize",
            description = "每页条数，默认20，最大50",
            required = false
        )
        pageSize: Int? = null
    ): String {
        return safeQuery("AuthTool", "listGroupMembers") {
            val (minExpiredAt, maxExpiredAt) = expiredAtMillisRange(expiredStatus, expireWithinDays)
            val actualPageSize = (pageSize ?: AuthAiToolsBase.DEFAULT_PAGE_SIZE)
                .coerceIn(1, AuthAiToolsBase.MAX_PAGE_SIZE)
            val result = authAiResource().listGroupMembers(
                userId = getOperatorUserId(),
                projectId = projectId,
                resourceType = resourceType,
                resourceCode = resourceCode,
                iamGroupId = iamGroupId,
                groupCode = groupCode,
                memberType = memberType,
                minExpiredAt = minExpiredAt,
                maxExpiredAt = maxExpiredAt,
                page = page ?: 1,
                pageSize = actualPageSize
            )
            val data = result.data ?: return@safeQuery "查询失败"
            if (data.records.isEmpty()) {
                return@safeQuery "未找到符合条件的用户组成员"
            }
            toJson(data)
        }
    }

    @Tool(
        name = "获取项目成员列表",
        description = "获取项目全体成员列表。可按用户名搜索或按成员类型过滤。" +
                "仅管理员可使用"
    )
    fun listProjectMembers(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(
            name = "departedFlag",
            description = "是否查询离职人员,true只查询离职人员；" +
                    "默认false，查询所有",
            required = false
        )
        departedFlag: Boolean? = false,
        @ToolParam(
            name = "userName",
            description = "用户名搜索（可选）",
            required = false
        )
        userName: String? = null,
        @ToolParam(name = "page", description = "页码，默认1", required = false)
        page: Int? = null,
        @ToolParam(
            name = "pageSize",
            description = "每页条数，默认20，最大50",
            required = false
        )
        pageSize: Int? = null
    ): String {
        return safeQuery("AuthTool", "listProjectMembers") {
            val actualPageSize = (pageSize ?: AuthAiToolsBase.DEFAULT_PAGE_SIZE)
                .coerceIn(1, AuthAiToolsBase.MAX_PAGE_SIZE)
            val result = authAiResource().listProjectMembers(
                userId = getOperatorUserId(),
                projectId = projectId,
                memberType = null,
                userName = userName,
                departedFlag = departedFlag,
                page = page ?: 1,
                pageSize = actualPageSize
            )
            val data = result.data ?: return@safeQuery "查询失败"
            if (data.records.isEmpty()) return@safeQuery "项目 $projectId 暂无成员"
            toJson(data)
        }
    }

    @Tool(
        name = "获取成员用户组数量",
        description = "获取某个成员在项目中各资源类型下的用户组数量，用于了解成员权限分布概况。" +
                "普通成员/管理员均可使用"
    )
    fun getMemberGroupCount(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "memberId", description = "成员ID")
        memberId: String
    ): String {
        return safeQuery("AuthTool", "getMemberGroupCount") {
            val result = authAiResource().getMemberGroupCount(
                userId = getOperatorUserId(),
                projectId = projectId,
                memberId = memberId
            )
            val counts = result.data ?: emptyList()
            if (counts.isEmpty()) return@safeQuery "用户 $memberId 在项目 $projectId 下无用户组"
            toJson(counts)
        }
    }

    @Tool(
        name = "查询成员所有用户组",
        description = "管理员可以查询所有成员，普通成员仅可以查询自己的。" +
                "一次性查询成员的用户组详情，支持多种过滤条件。" +
                "比按资源类型逐个查询效率更高，推荐用于权限概览、过期权限查询等场景。"
    )
    fun getAllMemberGroups(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "memberId", description = "成员ID")
        memberId: String,
        @ToolParam(
            name = "resourceType",
            description = "资源类型过滤（可选），如 pipeline、credential。不传则查询所有资源类型",
            required = false
        )
        resourceType: String? = null,
        @ToolParam(
            name = "groupName",
            description = "用户组名称搜索（可选）",
            required = false
        )
        groupName: String? = null,
        @ToolParam(
            name = "expiredStatus",
            description = "过期状态过滤（可选）：" +
                    "expired=已过期，expiring_soon=即将过期（默认30天内），valid=有效未过期，不传=全部",
            required = false
        )
        expiredStatus: String? = null,
        @ToolParam(
            name = "expireWithinDays",
            description = "过期天数（可选），配合 expiredStatus=expiring_soon 使用，指定查询多少天内即将过期，默认30天",
            required = false
        )
        expireWithinDays: Int? = null,
        @ToolParam(
            name = "relatedResourceType",
            description = "关联资源类型过滤（可选），如查询与某个流水线相关的用户组",
            required = false
        )
        relatedResourceType: String? = null,
        @ToolParam(
            name = "relatedResourceCode",
            description = "关联资源Code过滤（可选），需配合 relatedResourceType 使用",
            required = false
        )
        relatedResourceCode: String? = null,
        @ToolParam(
            name = "action",
            description = "操作权限过滤（可选），如 pipeline_execute 表示查询有流水线执行权限的用户组",
            required = false
        )
        action: String? = null,
        @ToolParam(name = "page", description = "页码，默认1", required = false)
        page: Int? = null,
        @ToolParam(
            name = "pageSize",
            description = "每页条数，默认20，最大50",
            required = false
        )
        pageSize: Int? = null
    ): String {
        return safeQuery("AuthTool", "getAllMemberGroups") {
            val days = expireWithinDays ?: AuthAiToolsBase.DEFAULT_EXPIRE_WITHIN_DAYS
            val (minExpiredAt, maxExpiredAt) = expiredAtMillisRange(expiredStatus, expireWithinDays)

            val actualPageSize = (pageSize ?: AuthAiToolsBase.DEFAULT_PAGE_SIZE)
                .coerceIn(1, AuthAiToolsBase.MAX_PAGE_SIZE)
            val result = authAiResource().getAllMemberGroupsDetails(
                userId = getOperatorUserId(),
                projectId = projectId,
                memberId = memberId,
                resourceType = resourceType,
                groupName = groupName,
                minExpiredAt = minExpiredAt,
                maxExpiredAt = maxExpiredAt,
                relatedResourceType = relatedResourceType,
                relatedResourceCode = relatedResourceCode,
                action = action,
                page = page ?: 1,
                pageSize = actualPageSize
            )
            val data = result.data ?: return@safeQuery "查询失败"
            if (data.records.isEmpty()) {
                return@safeQuery buildEmptyResultMessage(memberId, resourceType, expiredStatus, days)
            }
            toJson(data)
        }
    }

    private fun buildEmptyResultMessage(
        memberId: String,
        resourceType: String? = null,
        expiredStatus: String? = null,
        days: Int
    ): String {
        val statusDesc = when (expiredStatus?.lowercase()) {
            "expired" -> "已过期的"
            "expiring_soon" -> "${days}天内即将过期的"
            "valid" -> "有效的"
            else -> null
        }
        return when {
            statusDesc != null && resourceType != null ->
                "用户 $memberId 在资源类型 $resourceType 下没有${statusDesc}用户组"

            statusDesc != null ->
                "用户 $memberId 没有${statusDesc}权限"

            resourceType != null ->
                "用户 $memberId 在资源类型 $resourceType 下无用户组"

            else ->
                "用户 $memberId 在项目中无用户组"
        }
    }

    @Tool(
        name = "搜索用户",
        description = "根据关键词搜索用户。可限定在项目成员中搜索。\n" +
                "适用场景：确认用户是否存在、查找正确的用户ID。"
    )
    fun searchUsers(
        @ToolParam(name = "keyword", description = "搜索关键词（用户ID或姓名）")
        keyword: String,
        @ToolParam(
            name = "projectId",
            description = "项目ID（可选，传入则只在项目成员中搜索）",
            required = false
        )
        projectId: String? = null,
        @ToolParam(
            name = "limit",
            description = "返回数量限制，默认10",
            required = false
        )
        limit: Int? = null
    ): String {
        return safeQuery("AuthTool", "searchUsers") {
            val result = authAiResource().searchUsers(
                userId = getOperatorUserId(),
                keyword = keyword,
                projectId = projectId,
                limit = limit ?: 10
            )
            val vo = result.data ?: return@safeQuery "搜索失败: ${result.message}"

            if (vo.users.isEmpty()) {
                return@safeQuery "未找到匹配的用户"
            }

            val sb = StringBuilder()
            sb.appendLine("=== 用户搜索结果 ===")
            sb.appendLine("关键词: $keyword")
            if (projectId != null) sb.appendLine("范围: 项目 $projectId 成员")
            sb.appendLine("找到 ${vo.totalCount} 个用户:")
            sb.appendLine()

            vo.users.forEach { user ->
                sb.append("• ${user.userId}")
                if (user.displayName.isNotBlank() && user.displayName != user.userId) {
                    sb.append(" (${user.displayName})")
                }
                if (user.isProjectMember == true) sb.append(" [项目成员]")
                sb.appendLine()
            }
            sb.toString()
        }
    }
}
