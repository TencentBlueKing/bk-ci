package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.enum.BatchOperateType
import com.tencent.devops.auth.pojo.vo.BatchOperateGroupMemberCheckVo
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.openapi.BkApigwApi
import com.tencent.devops.auth.pojo.request.ai.BatchHandoverMembersReq
import com.tencent.devops.auth.pojo.request.ai.BatchOperateCheckReq
import com.tencent.devops.auth.pojo.request.ai.BatchRemoveMembersReq
import com.tencent.devops.auth.pojo.request.ai.BatchRenewalMembersReq
import com.tencent.devops.auth.pojo.request.ai.AiRemoveMemberFromProjectReq
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OPENAPI_AUTH_MEMBER_V4", description = "OPENAPI-权限成员管理")
@Path(
    "/{apigwType:apigw-user|apigw-app|apigw}/v4/auth/member/{projectId}"
)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
@BkApigwApi(version = "v4")
interface ApigwAuthMemberResourceV4 {
    @PUT
    @Path("/batch_renewal_members")
    @Operation(
        summary = "批量续期用户组成员",
        tags = [
            "v4_app_batch_renewal_members",
            "v4_user_batch_renewal_members"
        ]
    )
    fun batchRenewalMembers(
        @Parameter(
            description = "appCode",
            required = true,
            example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "批量续期请求体", required = true)
        request: BatchRenewalMembersReq
    ): Result<Boolean>

    @DELETE
    @Path("/batch_remove_members")
    @Operation(
        summary = "批量移除用户组成员",
        tags = [
            "v4_app_batch_remove_members",
            "v4_user_batch_remove_members"
        ]
    )
    fun batchRemoveMembers(
        @Parameter(
            description = "appCode",
            required = true,
            example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "批量移除请求体", required = true)
        request: BatchRemoveMembersReq
    ): Result<Boolean>

    @POST
    @Path("/batch_operate_check/{batchOperateType}")
    @Operation(
        summary = "批量操作用户组成员检查",
        tags = [
            "v4_app_batch_operate_members_check",
            "v4_user_batch_operate_members_check"
        ]
    )
    fun batchOperateMembersCheck(
        @Parameter(
            description = "appCode",
            required = true,
            example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(
            description = "批量操作类型(RENEWAL/REMOVE/HANDOVER)",
            required = true
        )
        @PathParam("batchOperateType")
        batchOperateType: BatchOperateType,
        @Parameter(description = "检查请求体", required = true)
        request: BatchOperateCheckReq
    ): Result<BatchOperateGroupMemberCheckVo>

    @PUT
    @Path("/batch_handover_members")
    @Operation(
        summary = "批量交接用户组成员",
        tags = [
            "v4_app_batch_handover_members",
            "v4_user_batch_handover_members"
        ]
    )
    fun batchHandoverMembers(
        @Parameter(
            description = "appCode",
            required = true,
            example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "批量交接请求体", required = true)
        request: BatchHandoverMembersReq
    ): Result<Boolean>

    @PUT
    @Path("/remove_member_from_project")
    @Operation(
        summary = "将用户移出项目",
        tags = [
            "v4_app_remove_member_from_project",
            "v4_user_remove_member_from_project"
        ]
    )
    fun removeMemberFromProject(
        @Parameter(
            description = "appCode",
            required = true,
            example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "移出项目请求体", required = true)
        request: AiRemoveMemberFromProjectReq
    ): Result<List<ResourceMemberInfo>>

    @POST
    @Path("/remove_member_from_project_check")
    @Operation(
        summary = "将用户移出项目检查",
        tags = [
            "v4_app_remove_member_from_project_check",
            "v4_user_remove_member_from_project_check"
        ]
    )
    fun removeMemberFromProjectCheck(
        @Parameter(
            description = "appCode",
            required = true,
            example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "要检查的成员ID", required = true)
        @QueryParam("targetMemberId")
        targetMemberId: String
    ): Result<Boolean>
}
