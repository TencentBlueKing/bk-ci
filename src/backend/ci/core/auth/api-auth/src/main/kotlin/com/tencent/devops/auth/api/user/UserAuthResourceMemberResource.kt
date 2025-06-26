package com.tencent.devops.auth.api.user

import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.enum.BatchOperateType
import com.tencent.devops.auth.pojo.enum.OperateChannel
import com.tencent.devops.auth.pojo.request.BatchRemoveMemberFromProjectReq
import com.tencent.devops.auth.pojo.request.BatchRemoveMemberFromProjectResponse
import com.tencent.devops.auth.pojo.request.GroupMemberCommonConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberHandoverConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberRemoveConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberRenewalConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberSingleRenewalReq
import com.tencent.devops.auth.pojo.request.ProjectMembersQueryConditionReq
import com.tencent.devops.auth.pojo.request.RemoveMemberFromProjectReq
import com.tencent.devops.auth.pojo.vo.BatchOperateGroupMemberCheckVo
import com.tencent.devops.auth.pojo.vo.GroupDetailsInfoVo
import com.tencent.devops.auth.pojo.vo.MemberExitsProjectCheckVo
import com.tencent.devops.auth.pojo.vo.ResourceType2CountVo
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
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

@Tag(name = "AUTH_RESOURCE_MEMBER", description = "用户态-iam用户")
@Path("/user/auth/resource/member/{projectId}/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("LongParameterList")
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

    @POST
    @Path("/listProjectMembersByCondition")
    @Operation(summary = "根据条件获取项目下全体成员")
    fun listProjectMembersByCondition(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "查询条件", required = true)
        projectMembersQueryConditionReq: ProjectMembersQueryConditionReq
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
    @Operation(summary = "批量续期组成员权限--管理员视角")
    fun batchRenewalGroupMembersFromManager(
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
    @Operation(summary = "批量移除用户组成员--管理员视角")
    fun batchRemoveGroupMembersFromManager(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "批量移除成员请求实体")
        removeMemberDTO: GroupMemberRemoveConditionReq
    ): Result<Boolean>

    @DELETE
    @Path("/batch/personal/remove")
    @Operation(summary = "批量退出用户组成员--个人视角")
    fun batchRemoveGroupMembersFromPersonal(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "批量移除成员请求实体")
        removeMemberDTO: GroupMemberRemoveConditionReq
    ): Result<String>

    @DELETE
    @Path("/single/{groupId}/{operateChannel}/remove")
    @Operation(summary = "退出单个组")
    fun deleteResourceGroupMembers(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "组ID", required = true)
        @PathParam("groupId")
        groupId: Int,
        @Parameter(description = "操作渠道", required = true)
        @PathParam("operateChannel")
        operateChannel: OperateChannel,
        @Parameter(description = "操作对象", required = true)
        targetMember: ResourceMemberInfo
    ): Result<Boolean>

    @PUT
    @Path("/batch/handover")
    @Operation(summary = "批量交接用户组成员--管理员视角")
    fun batchHandoverGroupMembersFromManager(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "批量交接成员请求实体")
        handoverMemberDTO: GroupMemberHandoverConditionReq
    ): Result<Boolean>

    @PUT
    @Path("/batch/personal/handover")
    @Operation(summary = "批量交接用户组成员--个人视角")
    fun batchHandoverApplicationFromPersonal(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "批量交接成员请求实体")
        handoverMemberDTO: GroupMemberHandoverConditionReq
    ): Result<String>

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

    @PUT
    @Path("/batchRemoveMemberFromProject")
    @Operation(summary = "批量将用户移出项目")
    fun batchRemoveMemberFromProject(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "批量将用户移出项目")
        removeMemberFromProjectReq: BatchRemoveMemberFromProjectReq
    ): Result<BatchRemoveMemberFromProjectResponse>

    @POST
    @Path("/removeMemberFromProjectCheck")
    @Operation(summary = "将用户移出项目检查")
    fun removeMemberFromProjectCheck(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "一键移出用户出项目")
        removeMemberFromProjectReq: RemoveMemberFromProjectReq
    ): Result<Boolean>

    @POST
    @Path("/batchRemoveMemberFromProjectCheck")
    @Operation(summary = "批量将用户移出项目检查")
    fun batchRemoveMemberFromProjectCheck(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "目标用户列表")
        targetMembers: List<ResourceMemberInfo>
    ): Result<Boolean>

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
        memberId: String,
        @QueryParam("groupName")
        @Parameter(description = "用户组名称")
        groupName: String?,
        @QueryParam("minExpiredAt")
        @Parameter(description = "最小过期时间")
        minExpiredAt: Long?,
        @QueryParam("maxExpiredAt")
        @Parameter(description = "最大过期时间")
        maxExpiredAt: Long?,
        @QueryParam("relatedResourceType")
        @Parameter(description = "资源类型")
        relatedResourceType: String?,
        @QueryParam("relatedResourceCode")
        @Parameter(description = "资源ID")
        relatedResourceCode: String?,
        @QueryParam("action")
        @Parameter(description = "操作")
        action: String?,
        @QueryParam("operateChannel")
        @Parameter(description = "操作渠道")
        operateChannel: OperateChannel?,
        @QueryParam("uniqueManagerGroupsQueryFlag")
        @Parameter(description = "是否查询唯一管理员组")
        uniqueManagerGroupsQueryFlag: Boolean?
    ): Result<List<ResourceType2CountVo>>

    @GET
    @Path("/checkMemberExitsProject")
    @Operation(summary = "用户主动退出项目检查")
    fun checkMemberExitsProject(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String
    ): Result<MemberExitsProjectCheckVo>

    @POST
    @Path("/memberExitsProject")
    @Operation(summary = "用户主动退出项目")
    fun memberExitsProject(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String,
        @Parameter(description = "请求体", required = true)
        request: RemoveMemberFromProjectReq
    ): Result<String>
}
