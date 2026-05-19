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

package com.tencent.devops.ai.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

/**
 * 运营联调：将权限 Agent 四类工具（与 Agent 内调用逻辑一致）的文本结果
 * 通过 OP 接口暴露。
 * 调用方需在 Header 传入操作人 [AUTH_HEADER_USER_ID]，
 * 行为与 Agent 工具内「当前用户」一致。
 */
@Tag(name = "OP_AI_AUTH_AGENT_TOOLS", description = "运营-权限 Agent 工具联调")
@Path("/op/ai/auth-agent-tools")
@Produces(MediaType.APPLICATION_JSON)
interface OpAiAuthAgentToolsResource {

    // region AuthGroupMemberQueryTools

    @Operation(summary = "[Query] checkRole")
    @POST
    @Path("/query/check-role")
    fun checkRole(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("memberId") memberId: String?
    ): Result<String>

    @Operation(summary = "[Query] listGroups")
    @POST
    @Path("/query/list-groups")
    fun listGroups(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("resourceType") resourceType: String?,
        @QueryParam("resourceCode") resourceCode: String?,
        @QueryParam("groupName") groupName: String?,
        @QueryParam("page") page: Int?,
        @QueryParam("pageSize") pageSize: Int?
    ): Result<String>

    @Operation(summary = "[Query] getGroupPermissionDetail")
    @POST
    @Path("/query/group-permission-detail")
    fun getGroupPermissionDetail(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("groupId") groupId: Int
    ): Result<String>

    @Operation(summary = "[Query] listGroupMembers")
    @POST
    @Path("/query/list-group-members")
    fun listGroupMembers(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("resourceType") resourceType: String?,
        @QueryParam("resourceCode") resourceCode: String?,
        @QueryParam("iamGroupId") iamGroupId: Int?,
        @QueryParam("groupCode") groupCode: String?,
        @QueryParam("memberType") memberType: String?,
        @QueryParam("expiredStatus") expiredStatus: String?,
        @QueryParam("expireWithinDays") expireWithinDays: Int?,
        @QueryParam("page") page: Int?,
        @QueryParam("pageSize") pageSize: Int?
    ): Result<String>

    @Operation(summary = "[Query] listProjectMembers")
    @POST
    @Path("/query/list-project-members")
    fun listProjectMembers(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("userName") userName: String?,
        @QueryParam("page") page: Int?,
        @QueryParam("pageSize") pageSize: Int?
    ): Result<String>

    @Operation(summary = "[Query] getMemberGroupCount")
    @POST
    @Path("/query/member-group-count")
    fun getMemberGroupCount(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("memberId") memberId: String
    ): Result<String>

    @Operation(summary = "[Query] getAllMemberGroups")
    @POST
    @Path("/query/all-member-groups")
    fun getAllMemberGroups(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("memberId") memberId: String,
        @QueryParam("resourceType") resourceType: String?,
        @QueryParam("groupName") groupName: String?,
        @QueryParam("expiredStatus") expiredStatus: String?,
        @QueryParam("expireWithinDays") expireWithinDays: Int?,
        @QueryParam("relatedResourceType") relatedResourceType: String?,
        @QueryParam("relatedResourceCode") relatedResourceCode: String?,
        @QueryParam("action") action: String?,
        @QueryParam("page") page: Int?,
        @QueryParam("pageSize") pageSize: Int?
    ): Result<String>

    @Operation(summary = "[Query] searchUsers")
    @POST
    @Path("/query/search-users")
    fun searchUsers(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("keyword") keyword: String,
        @QueryParam("projectId") projectId: String?,
        @QueryParam("limit") limit: Int?
    ): Result<String>

    // endregion

    // region AuthPermissionInsightTools

    @Operation(summary = "[Insight] analyzeUserPermissions")
    @POST
    @Path("/insight/analyze-user-permissions")
    fun analyzeUserPermissions(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("memberId") memberId: String
    ): Result<String>

    @Operation(summary = "[Insight] getResourcePermissionsMatrix")
    @POST
    @Path("/insight/resource-permissions-matrix")
    fun getResourcePermissionsMatrix(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("resourceType") resourceType: String,
        @QueryParam("resourceCode") resourceCode: String
    ): Result<String>

    @Operation(summary = "[Insight] recommendGroupsForGrant")
    @POST
    @Path("/insight/recommend-groups-for-grant")
    fun recommendGroupsForGrant(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("resourceType") resourceType: String,
        @QueryParam("resourceCode") resourceCode: String,
        @QueryParam("action") action: String,
        @QueryParam("targetUserId") targetUserId: String
    ): Result<String>

    @Operation(summary = "[Insight] diagnosePermission")
    @POST
    @Path("/insight/diagnose-permission")
    fun diagnosePermission(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("memberId") memberId: String,
        @QueryParam("resourceType") resourceType: String,
        @QueryParam("resourceCode") resourceCode: String,
        @QueryParam("action") action: String
    ): Result<String>

    @Operation(summary = "[Insight] comparePermissions")
    @POST
    @Path("/insight/compare-permissions")
    fun comparePermissions(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("userIdA") userIdA: String,
        @QueryParam("userIdB") userIdB: String,
        @QueryParam("resourceType") resourceType: String?
    ): Result<String>

    @Operation(summary = "[Insight] checkAuthorizationHealth")
    @POST
    @Path("/insight/authorization-health")
    fun checkAuthorizationHealth(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String
    ): Result<String>

    // endregion

    // region AuthMemberSelfServiceTools

    @Operation(summary = "[Self] applyRenewal")
    @POST
    @Path("/self/apply-renewal")
    fun applyRenewal(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("reason") reason: String,
        @QueryParam("groupIds") groupIds: String?,
        @QueryParam("expiredStatus") expiredStatus: String?,
        @QueryParam("expireWithinDays") expireWithinDays: Int?,
        @QueryParam("renewalDays") renewalDays: Int?,
        @QueryParam("dryRun") dryRun: Boolean?
    ): Result<String>

    @Operation(summary = "[Self] applyPermissions")
    @POST
    @Path("/self/apply-permissions")
    fun applyPermissions(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("reason") reason: String,
        @QueryParam("groupIds") groupIds: String?,
        @QueryParam("resourceType") resourceType: String?,
        @QueryParam("resourceCode") resourceCode: String?,
        @QueryParam("action") action: String?,
        @QueryParam("expiredDays") expiredDays: Int?,
        @QueryParam("dryRun") dryRun: Boolean?
    ): Result<String>

    @Operation(summary = "[Self] exitGroups")
    @POST
    @Path("/self/exit-groups")
    fun exitGroups(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("groupIds") groupIds: String,
        @QueryParam("handoverTo") handoverTo: String?,
        @QueryParam("dryRun") dryRun: Boolean?
    ): Result<String>

    @Operation(summary = "[Self] exitProject")
    @POST
    @Path("/self/exit-project")
    fun exitProject(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("handoverTo") handoverTo: String?,
        @QueryParam("dryRun") dryRun: Boolean?
    ): Result<String>

    @Operation(summary = "[Self] checkMemberExitWithRecommendation")
    @POST
    @Path("/self/check-member-exit")
    fun checkMemberExitWithRecommendation(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("targetMemberId") targetMemberId: String?,
        @QueryParam("handoverTo") handoverTo: String?,
        @QueryParam("groupIds") groupIds: String?
    ): Result<String>

    // endregion

    // region AuthAdminMutationTools

    @Operation(summary = "[Admin] grantPermissions")
    @POST
    @Path("/admin/grant-permissions")
    fun grantPermissions(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("targetUserIds") targetUserIds: String,
        @QueryParam("groupId") groupId: Int?,
        @QueryParam("resourceType") resourceType: String?,
        @QueryParam("resourceCode") resourceCode: String?,
        @QueryParam("action") action: String?,
        @QueryParam("expiredDays") expiredDays: Long?,
        @QueryParam("dryRun") dryRun: Boolean?
    ): Result<String>

    @Operation(summary = "[Admin] renewPermissions")
    @POST
    @Path("/admin/renew-permissions")
    fun renewPermissions(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("groupIds") groupIds: String,
        @QueryParam("targetMemberId") targetMemberId: String,
        @QueryParam("renewalDays") renewalDays: Int,
        @QueryParam("dryRun") dryRun: Boolean?
    ): Result<String>

    @Operation(summary = "[Admin] revokePermissions")
    @POST
    @Path("/admin/revoke-permissions")
    fun revokePermissions(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("groupIds") groupIds: String,
        @QueryParam("targetMemberId") targetMemberId: String,
        @QueryParam("handoverTo") handoverTo: String?,
        @QueryParam("dryRun") dryRun: Boolean?
    ): Result<String>

    @Operation(summary = "[Admin] removeMemberFromProject")
    @POST
    @Path("/admin/remove-member-from-project")
    fun removeMemberFromProject(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("targetMemberIds") targetMemberIds: String,
        @QueryParam("handoverTo") handoverTo: String?,
        @QueryParam("dryRun") dryRun: Boolean?
    ): Result<String>

    @Operation(summary = "[Admin] clonePermissions")
    @POST
    @Path("/admin/clone-permissions")
    fun clonePermissions(
        @Parameter(description = "操作人用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId") projectId: String,
        @QueryParam("sourceUserId") sourceUserId: String,
        @QueryParam("targetUserId") targetUserId: String,
        @QueryParam("resourceTypes") resourceTypes: String?,
        @QueryParam("dryRun") dryRun: Boolean?
    ): Result<String>

    // endregion
}
