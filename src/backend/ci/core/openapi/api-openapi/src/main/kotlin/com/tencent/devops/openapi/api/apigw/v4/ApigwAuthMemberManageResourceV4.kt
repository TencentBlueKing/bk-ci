package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.auth.pojo.AuthResourceGroup
import com.tencent.devops.auth.pojo.AuthResourceGroupMember
import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.dto.IamGroupIdsQueryConditionDTO
import com.tencent.devops.auth.pojo.dto.ResourceGroupPermissionDTO
import com.tencent.devops.auth.pojo.enum.BatchOperateType
import com.tencent.devops.auth.pojo.request.BatchRemoveMemberFromProjectResponse
import com.tencent.devops.auth.pojo.request.ai.AiApplyJoinGroupReq
import com.tencent.devops.auth.pojo.request.ai.AiBatchRemoveMemberFromProjectReq
import com.tencent.devops.auth.pojo.request.ai.AiMemberExitsProjectReq
import com.tencent.devops.auth.pojo.request.ai.AiRemoveMemberFromProjectReq
import com.tencent.devops.auth.pojo.request.ai.BatchHandoverMembersReq
import com.tencent.devops.auth.pojo.request.ai.BatchOperateCheckReq
import com.tencent.devops.auth.pojo.request.ai.BatchRemoveMembersReq
import com.tencent.devops.auth.pojo.request.ai.BatchRenewalMembersReq
import com.tencent.devops.auth.pojo.vo.BatchOperateGroupMemberCheckVo
import com.tencent.devops.auth.pojo.vo.GroupDetailsInfoVo
import com.tencent.devops.auth.pojo.vo.MemberExitsProjectCheckVo
import com.tencent.devops.auth.pojo.vo.ResourceType2CountVo
import com.tencent.devops.auth.pojo.vo.UserSearchResultVO
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.openapi.BkApigwApi
import com.tencent.devops.project.pojo.ProjectCreateUserInfo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OPENAPI_AUTH_MEMBER_MANAGE_V4", description = "OPENAPI-权限成员治理")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/auth/project/{projectId}/member_manage")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL", "LongParameterList")
@BkApigwApi(version = "v4")
interface ApigwAuthMemberManageResourceV4 {

    @POST
    @Path("/list_groups")
    @Operation(summary = "查询用户组列表", tags = ["v4_app_list_auth_groups", "v4_user_list_auth_groups"])
    fun listGroups(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "查询条件", required = true)
        condition: IamGroupIdsQueryConditionDTO
    ): Result<SQLPage<AuthResourceGroup>>

    @GET
    @Path("/groups/{groupId}/permission_detail")
    @Operation(
        summary = "查询用户组权限详情",
        tags = ["v4_app_get_auth_group_permission_detail", "v4_user_get_auth_group_permission_detail"]
    )
    fun getGroupPermissionDetail(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "用户组ID", required = true)
        @PathParam("groupId")
        groupId: Int
    ): Result<List<ResourceGroupPermissionDTO>>

    @GET
    @Path("/list_group_members")
    @Operation(
        summary = "查询用户组成员详情列表",
        tags = ["v4_app_list_auth_group_members", "v4_user_list_auth_group_members"]
    )
    fun listGroupMembers(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @QueryParam("resourceType")
        resourceType: String? = null,
        @QueryParam("resourceCode")
        resourceCode: String? = null,
        @QueryParam("iamGroupId")
        iamGroupId: Int? = null,
        @QueryParam("groupCode")
        groupCode: String? = null,
        @QueryParam("memberId")
        memberId: String? = null,
        @QueryParam("memberType")
        memberType: String? = null,
        @QueryParam("minExpiredAt")
        minExpiredAt: Long? = null,
        @QueryParam("maxExpiredAt")
        maxExpiredAt: Long? = null,
        @QueryParam("page")
        page: Int = 1,
        @QueryParam("pageSize")
        pageSize: Int = 20
    ): Result<SQLPage<AuthResourceGroupMember>>

    @GET
    @Path("/list_project_members")
    @Operation(
        summary = "获取项目全体成员",
        tags = ["v4_app_list_auth_project_members", "v4_user_list_auth_project_members"]
    )
    fun listProjectMembers(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @QueryParam("memberType")
        memberType: String? = null,
        @QueryParam("userName")
        userName: String? = null,
        @QueryParam("departed")
        departedFlag: Boolean? = null,
        @QueryParam("page")
        page: Int = 1,
        @QueryParam("pageSize")
        pageSize: Int = 20
    ): Result<SQLPage<ResourceMemberInfo>>

    @GET
    @Path("/members/group_count")
    @Operation(
        summary = "获取成员用户组数量",
        tags = ["v4_app_get_auth_member_group_count", "v4_user_get_auth_member_group_count"]
    )
    fun getMemberGroupCount(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @QueryParam("memberId")
        memberId: String,
        @QueryParam("relatedResourceType")
        relatedResourceType: String? = null,
        @QueryParam("relatedResourceCode")
        relatedResourceCode: String? = null
    ): Result<List<ResourceType2CountVo>>

    @GET
    @Path("/members/{resourceType}/groups")
    @Operation(
        summary = "获取成员用户组详情",
        tags = ["v4_app_get_auth_member_groups_detail", "v4_user_get_auth_member_groups_detail"]
    )
    fun getMemberGroupsDetails(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @PathParam("resourceType")
        resourceType: String,
        @QueryParam("memberId")
        memberId: String,
        @QueryParam("relatedResourceType")
        relatedResourceType: String? = null,
        @QueryParam("relatedResourceCode")
        relatedResourceCode: String? = null,
        @QueryParam("page")
        page: Int = 1,
        @QueryParam("pageSize")
        pageSize: Int = 20
    ): Result<SQLPage<GroupDetailsInfoVo>>

    @GET
    @Path("/members/all_groups")
    @Operation(
        summary = "获取成员所有用户组详情",
        tags = ["v4_app_get_all_auth_member_groups", "v4_user_get_all_auth_member_groups"]
    )
    fun getAllMemberGroupsDetails(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @QueryParam("memberId")
        memberId: String,
        @QueryParam("resourceType")
        resourceType: String? = null,
        @QueryParam("iamGroupIds")
        iamGroupIds: String? = null,
        @QueryParam("groupName")
        groupName: String? = null,
        @QueryParam("minExpiredAt")
        minExpiredAt: Long? = null,
        @QueryParam("maxExpiredAt")
        maxExpiredAt: Long? = null,
        @QueryParam("relatedResourceType")
        relatedResourceType: String? = null,
        @QueryParam("relatedResourceCode")
        relatedResourceCode: String? = null,
        @QueryParam("action")
        action: String? = null,
        @QueryParam("page")
        page: Int = 1,
        @QueryParam("pageSize")
        pageSize: Int = 500
    ): Result<SQLPage<GroupDetailsInfoVo>>

    @POST
    @Path("/members/add")
    @Operation(
        summary = "批量添加用户组成员",
        tags = ["v4_app_add_auth_group_members", "v4_user_add_auth_group_members"]
    )
    fun addGroupMembers(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "添加成员信息", required = true)
        createInfo: ProjectCreateUserInfo
    ): Result<Boolean>

    @PUT
    @Path("/members/batch_renewal")
    @Operation(
        summary = "批量续期用户组成员",
        tags = ["v4_app_batch_renewal_auth_members", "v4_user_batch_renewal_auth_members"]
    )
    fun batchRenewalMembers(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        request: BatchRenewalMembersReq
    ): Result<Boolean>

    @POST
    @Path("/members/apply_renewal")
    @Operation(
        summary = "普通用户申请续期权限",
        tags = ["v4_app_apply_auth_member_renewal", "v4_user_apply_auth_member_renewal"]
    )
    fun applyRenewalGroupMember(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @QueryParam("groupIds")
        groupIds: String,
        @QueryParam("renewalDays")
        renewalDays: Int,
        @QueryParam("reason")
        reason: String
    ): Result<Boolean>

    @DELETE
    @Path("/members/batch_remove")
    @Operation(
        summary = "批量移除用户组成员",
        tags = ["v4_app_batch_remove_auth_members", "v4_user_batch_remove_auth_members"]
    )
    fun batchRemoveMembers(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        request: BatchRemoveMembersReq
    ): Result<Boolean>

    @PUT
    @Path("/members/batch_handover")
    @Operation(
        summary = "批量交接用户组成员",
        tags = ["v4_app_batch_handover_auth_members", "v4_user_batch_handover_auth_members"]
    )
    fun batchHandoverMembers(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        request: BatchHandoverMembersReq
    ): Result<Boolean>

    @DELETE
    @Path("/members/exit_groups")
    @Operation(summary = "成员自助退出用户组", tags = ["v4_app_exit_auth_groups", "v4_user_exit_auth_groups"])
    fun exitGroupsFromPersonal(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        request: BatchRemoveMembersReq
    ): Result<String>

    @POST
    @Path("/members/apply_handover")
    @Operation(
        summary = "成员自助申请交接用户组",
        tags = ["v4_app_apply_auth_group_handover", "v4_user_apply_auth_group_handover"]
    )
    fun applyHandoverFromPersonal(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        request: BatchHandoverMembersReq
    ): Result<String>

    @POST
    @Path("/members/{batchOperateType}/check")
    @Operation(
        summary = "批量操作成员检查",
        tags = ["v4_app_check_auth_member_operate", "v4_user_check_auth_member_operate"]
    )
    fun batchOperateCheck(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @PathParam("batchOperateType")
        batchOperateType: BatchOperateType,
        request: BatchOperateCheckReq
    ): Result<BatchOperateGroupMemberCheckVo>

    @PUT
    @Path("/members/remove_from_project")
    @Operation(
        summary = "将用户移出项目",
        tags = ["v4_app_remove_auth_member_from_project", "v4_user_remove_auth_member_from_project"]
    )
    fun removeMemberFromProject(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        request: AiRemoveMemberFromProjectReq
    ): Result<List<ResourceMemberInfo>>

    @POST
    @Path("/apply_to_join_group")
    @Operation(summary = "申请加入用户组", tags = ["v4_app_apply_join_auth_group", "v4_user_apply_join_auth_group"])
    fun applyToJoinGroup(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        request: AiApplyJoinGroupReq
    ): Result<Boolean>

    @GET
    @Path("/members/exits_project_check")
    @Operation(
        summary = "用户主动退出项目检查",
        tags = ["v4_app_check_auth_member_exit_project", "v4_user_check_auth_member_exit_project"]
    )
    fun checkMemberExitsProject(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<MemberExitsProjectCheckVo>

    @PUT
    @Path("/members/exits_project")
    @Operation(
        summary = "用户主动退出项目",
        tags = ["v4_app_auth_member_exit_project", "v4_user_auth_member_exit_project"]
    )
    fun memberExitsProject(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        request: AiMemberExitsProjectReq
    ): Result<String>

    @POST
    @Path("/members/batch_remove_from_project_check")
    @Operation(
        summary = "批量将用户移出项目检查",
        tags = ["v4_app_check_batch_remove_auth_members",
            "v4_user_check_batch_remove_auth_members"]
    )
    fun batchRemoveMemberFromProjectCheck(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        targetMemberIds: List<String>
    ): Result<Boolean>

    @PUT
    @Path("/members/batch_remove_from_project")
    @Operation(
        summary = "批量将用户移出项目",
        tags = ["v4_app_batch_remove_auth_members_from_project",
            "v4_user_batch_remove_auth_members_from_project"]
    )
    fun batchRemoveMemberFromProject(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        request: AiBatchRemoveMemberFromProjectReq
    ): Result<BatchRemoveMemberFromProjectResponse>

    @GET
    @Path("/users/search")
    @Operation(
        summary = "根据关键词搜索用户",
        tags = ["v4_app_search_auth_users",
            "v4_user_search_auth_users"]
    )
    fun searchUsers(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @QueryParam("keyword")
        keyword: String,
        @QueryParam("projectId")
        queryProjectId: String? = null,
        @QueryParam("limit")
        limit: Int = 10
    ): Result<UserSearchResultVO>
}
