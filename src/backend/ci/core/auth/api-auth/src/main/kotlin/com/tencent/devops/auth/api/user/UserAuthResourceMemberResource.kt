package com.tencent.devops.auth.api.user

import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.enum.BatchOperateType
import com.tencent.devops.auth.pojo.request.GroupMemberCommonConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberHandoverConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberRenewalConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberSingleRenewalReq
import com.tencent.devops.auth.pojo.request.RemoveMemberFromProjectReq
import com.tencent.devops.auth.pojo.vo.BatchOperateGroupMemberCheckVo
import com.tencent.devops.auth.pojo.vo.GroupDetailsInfoVo
import com.tencent.devops.auth.pojo.vo.MemberGroupCountWithPermissionsVo
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "AUTH_RESOURCE_MEMBER", description = "用户态-iam用户")
@Path("/user/auth/resource/member/{projectId}/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserAuthResourceMemberResource {
    @GET
    @Path("/listProjectMembers")
    @Operation(summary = "获取项目下全体成员")
    @Suppress("LongParameterList")
    fun listProjectMembers(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "成员类型")
        @QueryParam("memberType")
        memberType: String?,
        @Parameter(description = "用户名称搜索")
        @QueryParam("userName")
        userName: String?,
        @Parameter(description = "组织搜索")
        @QueryParam("deptName")
        deptName: String?,
        @Parameter(description = "是否展示离职标识")
        @QueryParam("departedFlag")
        departedFlag: Boolean?,
        @Parameter(description = "第几页")
        @QueryParam("page")
        page: Int,
        @Parameter(description = "每页多少条")
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<SQLPage<ResourceMemberInfo>>

    @PUT
    @Path("/renewal")
    @Operation(summary = "续期单个组成员权限--无需进行审批")
    fun renewalGroupMember(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "续期成员请求实体")
        renewalConditionReq: GroupMemberSingleRenewalReq
    ): Result<GroupDetailsInfoVo>

    @PUT
    @Path("/batch/renewal")
    @Operation(summary = "批量续期组成员权限--无需进行审批")
    fun batchRenewalGroupMembers(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "批量续期成员请求实体")
        renewalConditionReq: GroupMemberRenewalConditionReq
    ): Result<Boolean>

    @DELETE
    @Path("/batch/remove")
    @Operation(summary = "批量移除用户组成员")
    fun batchRemoveGroupMembers(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "批量移除成员请求实体")
        removeMemberDTO: GroupMemberCommonConditionReq
    ): Result<Boolean>

    @PUT
    @Path("/batch/handover")
    @Operation(summary = "批量交接用户组成员")
    fun batchHandoverGroupMembers(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "批量交接成员请求实体")
        handoverMemberDTO: GroupMemberHandoverConditionReq
    ): Result<Boolean>

    @POST
    @Path("/batch/{batchOperateType}/check/")
    @Operation(summary = "批量操作用户组检查")
    fun batchOperateGroupMembersCheck(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "批量操作类型", required = true)
        @PathParam("batchOperateType")
        batchOperateType: BatchOperateType,
        @Parameter(description = "批量操作成员检查请求体")
        conditionReq: GroupMemberCommonConditionReq
    ): Result<BatchOperateGroupMemberCheckVo>

    @PUT
    @Path("/removeMemberFromProject")
    @Operation(summary = "将用户移出项目")
    fun removeMemberFromProject(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "一键移出用户出项目")
        removeMemberFromProjectReq: RemoveMemberFromProjectReq
    ): Result<List<ResourceMemberInfo>>

    @GET
    @Path("/getMemberGroupCount")
    @Operation(summary = "获取项目成员有权限的用户组数量--以资源类型进行分类")
    fun getMemberGroupCount(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @QueryParam("memberId")
        @Parameter(description = "组织ID/成员ID")
        memberId: String
    ): Result<List<MemberGroupCountWithPermissionsVo>>
}
