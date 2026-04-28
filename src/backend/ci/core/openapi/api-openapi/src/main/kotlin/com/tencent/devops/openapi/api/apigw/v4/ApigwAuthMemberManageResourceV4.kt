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
        @Parameter(description = "应用Code(OpenAPI调用方标识)", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "用户组查询条件(含IAM用户组ID、资源维度等筛选)", required = true)
        condition: IamGroupIdsQueryConditionDTO
    ): Result<SQLPage<AuthResourceGroup>>

    @GET
    @Path("/groups/{groupId}/permission_detail")
    @Operation(
        summary = "查询用户组权限详情",
        tags = ["v4_app_get_auth_group_permission_detail", "v4_user_get_auth_group_permission_detail"]
    )
    fun getGroupPermissionDetail(
        @Parameter(description = "应用Code(OpenAPI调用方标识)", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "权限中心用户组ID", required = true)
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
        @Parameter(description = "应用Code(OpenAPI调用方标识)", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "资源类型(如流水线、代码库等)", required = false)
        @QueryParam("resourceType")
        resourceType: String? = null,
        @Parameter(description = "资源Code", required = false)
        @QueryParam("resourceCode")
        resourceCode: String? = null,
        @Parameter(description = "IAM用户组ID", required = false)
        @QueryParam("iamGroupId")
        iamGroupId: Int? = null,
        @Parameter(description = "用户组Code,CI管理员为CI_MANAGER", required = false)
        @QueryParam("groupCode")
        groupCode: String? = null,
        @Parameter(description = "成员ID(用户名或用户组标识等)", required = false)
        @QueryParam("memberId")
        memberId: String? = null,
        @Parameter(description = "成员类型(如USER、DEPARTMENT等)", required = false)
        @QueryParam("memberType")
        memberType: String? = null,
        @Parameter(description = "权限过期时间下限(毫秒时间戳)", required = false)
        @QueryParam("minExpiredAt")
        minExpiredAt: Long? = null,
        @Parameter(description = "权限过期时间上限(毫秒时间戳)", required = false)
        @QueryParam("maxExpiredAt")
        maxExpiredAt: Long? = null,
        @Parameter(description = "页码,默认1", required = false)
        @QueryParam("page")
        page: Int = 1,
        @Parameter(description = "每页条数,默认20", required = false)
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
        @Parameter(description = "应用Code(OpenAPI调用方标识)", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "成员类型(如USER、DEPARTMENT等)", required = false)
        @QueryParam("memberType")
        memberType: String? = null,
        @Parameter(description = "用户名(模糊匹配)", required = false)
        @QueryParam("userName")
        userName: String? = null,
        @Parameter(description = "是否已离职,true表示仅查离职用户", required = false)
        @QueryParam("departed")
        departedFlag: Boolean? = null,
        @Parameter(description = "页码,默认1", required = false)
        @QueryParam("page")
        page: Int = 1,
        @Parameter(description = "每页条数,默认20", required = false)
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
        @Parameter(description = "应用Code(OpenAPI调用方标识)", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "目标成员ID(用户名等)", required = true)
        @QueryParam("memberId")
        memberId: String,
        @Parameter(description = "关联资源类型(按资源维度统计时传入)", required = false)
        @QueryParam("relatedResourceType")
        relatedResourceType: String? = null,
        @Parameter(description = "关联资源Code", required = false)
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
        @Parameter(description = "应用Code(OpenAPI调用方标识)", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "资源类型(路径参数,与权限模型中的资源类型一致)", required = true)
        @PathParam("resourceType")
        resourceType: String,
        @Parameter(description = "目标成员ID(用户名等)", required = true)
        @QueryParam("memberId")
        memberId: String,
        @Parameter(description = "关联资源类型", required = false)
        @QueryParam("relatedResourceType")
        relatedResourceType: String? = null,
        @Parameter(description = "关联资源Code", required = false)
        @QueryParam("relatedResourceCode")
        relatedResourceCode: String? = null,
        @Parameter(description = "页码,默认1", required = false)
        @QueryParam("page")
        page: Int = 1,
        @Parameter(description = "每页条数,默认20", required = false)
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
        @Parameter(description = "应用Code(OpenAPI调用方标识)", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "目标成员ID(用户名等)", required = true)
        @QueryParam("memberId")
        memberId: String,
        @Parameter(description = "资源类型筛选", required = false)
        @QueryParam("resourceType")
        resourceType: String? = null,
        @Parameter(description = "IAM用户组ID列表(多个以逗号分隔)", required = false)
        @QueryParam("iamGroupIds")
        iamGroupIds: String? = null,
        @Parameter(description = "用户组名称(模糊匹配)", required = false)
        @QueryParam("groupName")
        groupName: String? = null,
        @Parameter(description = "权限过期时间下限(毫秒时间戳)", required = false)
        @QueryParam("minExpiredAt")
        minExpiredAt: Long? = null,
        @Parameter(description = "权限过期时间上限(毫秒时间戳)", required = false)
        @QueryParam("maxExpiredAt")
        maxExpiredAt: Long? = null,
        @Parameter(description = "关联资源类型", required = false)
        @QueryParam("relatedResourceType")
        relatedResourceType: String? = null,
        @Parameter(description = "关联资源Code", required = false)
        @QueryParam("relatedResourceCode")
        relatedResourceCode: String? = null,
        @Parameter(description = "权限动作/操作标识筛选", required = false)
        @QueryParam("action")
        action: String? = null,
        @Parameter(description = "页码,默认1", required = false)
        @QueryParam("page")
        page: Int = 1,
        @Parameter(description = "每页条数,默认500", required = false)
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
        @Parameter(description = "应用Code(OpenAPI调用方标识)", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "批量添加成员请求体(含用户组、成员列表等)", required = true)
        createInfo: ProjectCreateUserInfo
    ): Result<Boolean>

    @PUT
    @Path("/members/batch_renewal")
    @Operation(
        summary = "批量续期用户组成员",
        tags = ["v4_app_batch_renewal_auth_members", "v4_user_batch_renewal_auth_members"]
    )
    fun batchRenewalMembers(
        @Parameter(description = "应用Code(OpenAPI调用方标识)", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "批量续期请求体(用户组与续期天数等)", required = true)
        request: BatchRenewalMembersReq
    ): Result<Boolean>

    @POST
    @Path("/members/apply_renewal")
    @Operation(
        summary = "普通用户申请续期权限",
        tags = ["v4_app_apply_auth_member_renewal", "v4_user_apply_auth_member_renewal"]
    )
    fun applyRenewalGroupMember(
        @Parameter(description = "应用Code(OpenAPI调用方标识)", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "待续期的用户组ID列表(多个以逗号分隔)", required = true)
        @QueryParam("groupIds")
        groupIds: String,
        @Parameter(description = "申请续期的天数", required = true)
        @QueryParam("renewalDays")
        renewalDays: Int,
        @Parameter(description = "申请原因说明", required = true)
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
        @Parameter(description = "应用Code(OpenAPI调用方标识)", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "批量移除成员请求体(用户组与成员等)", required = true)
        request: BatchRemoveMembersReq
    ): Result<Boolean>

    @PUT
    @Path("/members/batch_handover")
    @Operation(
        summary = "批量交接用户组成员",
        tags = ["v4_app_batch_handover_auth_members", "v4_user_batch_handover_auth_members"]
    )
    fun batchHandoverMembers(
        @Parameter(description = "应用Code(OpenAPI调用方标识)", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "批量交接成员请求体(原成员、接手人等)", required = true)
        request: BatchHandoverMembersReq
    ): Result<Boolean>

    @DELETE
    @Path("/members/exit_groups")
    @Operation(summary = "成员自助退出用户组", tags = ["v4_app_exit_auth_groups", "v4_user_exit_auth_groups"])
    fun exitGroupsFromPersonal(
        @Parameter(description = "应用Code(OpenAPI调用方标识)", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "退出用户组请求体(用户组与成员等)", required = true)
        request: BatchRemoveMembersReq
    ): Result<String>

    @POST
    @Path("/members/apply_handover")
    @Operation(
        summary = "成员自助申请交接用户组",
        tags = ["v4_app_apply_auth_group_handover", "v4_user_apply_auth_group_handover"]
    )
    fun applyHandoverFromPersonal(
        @Parameter(description = "应用Code(OpenAPI调用方标识)", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "申请交接用户组请求体(交接对象等)", required = true)
        request: BatchHandoverMembersReq
    ): Result<String>

    @POST
    @Path("/members/{batchOperateType}/check")
    @Operation(
        summary = "批量操作成员检查",
        tags = ["v4_app_check_auth_member_operate", "v4_user_check_auth_member_operate"]
    )
    fun batchOperateCheck(
        @Parameter(description = "应用Code(OpenAPI调用方标识)", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "批量操作类型(续期、移除、交接等)", required = true)
        @PathParam("batchOperateType")
        batchOperateType: BatchOperateType,
        @Parameter(description = "批量操作前置检查请求体", required = true)
        request: BatchOperateCheckReq
    ): Result<BatchOperateGroupMemberCheckVo>

    @PUT
    @Path("/members/remove_from_project")
    @Operation(
        summary = "将用户移出项目",
        tags = ["v4_app_remove_auth_member_from_project", "v4_user_remove_auth_member_from_project"]
    )
    fun removeMemberFromProject(
        @Parameter(description = "应用Code(OpenAPI调用方标识)", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "将成员移出项目请求体", required = true)
        request: AiRemoveMemberFromProjectReq
    ): Result<List<ResourceMemberInfo>>

    @POST
    @Path("/apply_to_join_group")
    @Operation(summary = "申请加入用户组", tags = ["v4_app_apply_join_auth_group", "v4_user_apply_join_auth_group"])
    fun applyToJoinGroup(
        @Parameter(description = "应用Code(OpenAPI调用方标识)", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "申请加入用户组请求体", required = true)
        request: AiApplyJoinGroupReq
    ): Result<Boolean>

    @GET
    @Path("/members/exits_project_check")
    @Operation(
        summary = "用户主动退出项目检查",
        tags = ["v4_app_check_auth_member_exit_project", "v4_user_check_auth_member_exit_project"]
    )
    fun checkMemberExitsProject(
        @Parameter(description = "应用Code(OpenAPI调用方标识)", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
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
        @Parameter(description = "应用Code(OpenAPI调用方标识)", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "用户主动退出项目请求体", required = true)
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
        @Parameter(description = "应用Code(OpenAPI调用方标识)", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "待移出项目的目标成员用户ID列表", required = true)
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
        @Parameter(description = "应用Code(OpenAPI调用方标识)", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "批量将成员移出项目请求体", required = true)
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
        @Parameter(description = "应用Code(OpenAPI调用方标识)", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "网关类型,取值为apigw-user、apigw-app或apigw", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "操作人用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "搜索关键词(用户名等)", required = true)
        @QueryParam("keyword")
        keyword: String,
        @Parameter(description = "限定搜索范围的项目ID(项目英文名),不传则不限定项目", required = false)
        @QueryParam("projectId")
        queryProjectId: String? = null,
        @Parameter(description = "返回条数上限,默认10", required = false)
        @QueryParam("limit")
        limit: Int = 10
    ): Result<UserSearchResultVO>
}
