package com.tencent.devops.auth.api.service

import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.enum.BatchOperateType
import com.tencent.devops.auth.pojo.request.GroupMemberCommonConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberHandoverConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberRemoveConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberRenewalConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberSingleRenewalReq
import com.tencent.devops.auth.pojo.request.RemoveMemberFromProjectReq
import com.tencent.devops.auth.pojo.vo.BatchOperateGroupMemberCheckVo
import com.tencent.devops.auth.pojo.vo.ResourceType2CountVo
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BK_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.project.pojo.ProjectCreateUserInfo
import com.tencent.devops.project.pojo.ProjectDeleteUserInfo
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

@Tag(name = "AUTH_SERVICE_RESOURCE", description = "权限--资源相关接口")
@Path("/service/auth/resource/member")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceResourceMemberResource {
    /**
     * @param resourceType 是个枚举类型详见 AuthResourceType
     * @see AuthResourceType
     */
    @GET
    @Path("/{projectCode}/getResourceGroupUsers")
    @Operation(summary = "获取特定资源下用户组成员")
    fun getResourceGroupMembers(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String,
        @QueryParam("resourceType")
        @Parameter(description = "资源类型", required = false)
        resourceType: String,
        @QueryParam("resourceCode")
        @Parameter(description = "资源code", required = false)
        resourceCode: String,
        @QueryParam("group")
        @Parameter(description = "资源用户组类型", required = false)
        group: BkAuthGroup? = null
    ): Result<List<String>>

    @GET
    @Path("/{projectCode}/getResourceUsers")
    @Operation(summary = "拉取资源下所有成员，并按项目角色组分组成员信息返回")
    fun getResourceGroupAndMembers(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String,
        @QueryParam("resourceType")
        @Parameter(description = "资源类型", required = false)
        resourceType: String,
        @QueryParam("resourceCode")
        @Parameter(description = "资源code", required = false)
        resourceCode: String
    ): Result<List<BkAuthGroupAndUserList>>

    @POST
    @Path("/{projectCode}/batchAddResourceGroupMembers/")
    @Operation(summary = "用户组添加成员")
    fun batchAddResourceGroupMembers(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String,
        @Parameter(description = "用户组添加成员请求体", required = true)
        projectCreateUserInfo: ProjectCreateUserInfo
    ): Result<Boolean>

    @DELETE
    @Path("/{projectCode}/batchDeleteResourceGroupMembers/")
    @Operation(summary = "用户组删除成员")
    fun batchDeleteResourceGroupMembers(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String,
        @Parameter(description = "用户组删除成员请求体", required = true)
        projectDeleteUserInfo: ProjectDeleteUserInfo
    ): Result<Boolean>

    @PUT
    @Path("/{projectCode}/renewal")
    @Operation(summary = "续期单个组成员权限--无需进行审批")
    fun renewalGroupMember(
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        @Parameter(description = "认证token", required = true)
        token: String,
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "续期成员请求实体")
        renewalConditionReq: GroupMemberSingleRenewalReq
    ): Result<Boolean>

    @GET
    @Path("/{projectCode}/listProjectMembers")
    @Operation(summary = "获取项目下全体成员")
    @Suppress("LongParameterList")
    fun listProjectMembers(
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String,
        @QueryParam("memberType")
        @Parameter(description = "成员类型")
        memberType: String?,
        @QueryParam("userName")
        @Parameter(description = "用户名称搜索")
        userName: String?,
        @QueryParam("deptName")
        @Parameter(description = "组织搜索")
        deptName: String?,
        @QueryParam("departedFlag")
        @Parameter(description = "是否展示离职标识")
        departedFlag: Boolean?,
        @QueryParam("page")
        @Parameter(description = "第几页")
        page: Int,
        @QueryParam("pageSize")
        @Parameter(description = "每页多少条")
        pageSize: Int
    ): Result<SQLPage<ResourceMemberInfo>>

    @GET
    @Path("/{projectCode}/getMemberGroupCount")
    @Operation(summary = "获取项目成员有权限的用户组数量")
    @Suppress("LongParameterList")
    fun getMemberGroupCount(
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String,
        @QueryParam("memberId")
        @Parameter(description = "成员ID", required = true)
        memberId: String,
        @QueryParam("relatedResourceType")
        @Parameter(description = "资源类型")
        relatedResourceType: String?,
        @QueryParam("relatedResourceCode")
        @Parameter(description = "资源code")
        relatedResourceCode: String?
    ): Result<List<ResourceType2CountVo>>

    @PUT
    @Path("/{projectCode}/removeMemberFromProject")
    @Operation(summary = "将用户移出项目")
    fun removeMemberFromProject(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户名", required = true)
        userId: String,
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String,
        @Parameter(description = "移出项目请求体", required = true)
        removeMemberFromProjectReq: RemoveMemberFromProjectReq
    ): Result<List<ResourceMemberInfo>>

    @POST
    @Path("/{projectCode}/removeMemberFromProjectCheck")
    @Operation(summary = "将用户移出项目检查")
    fun removeMemberFromProjectCheck(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户名", required = true)
        userId: String,
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String,
        @Parameter(description = "移出项目请求体", required = true)
        removeMemberFromProjectReq: RemoveMemberFromProjectReq
    ): Result<Boolean>

    @PUT
    @Path("/{projectCode}/batch/renewal")
    @Operation(summary = "批量续期组成员权限")
    fun batchRenewalGroupMembersFromManager(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户名", required = true)
        userId: String,
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String,
        @Parameter(description = "批量续期成员请求实体", required = true)
        renewalConditionReq: GroupMemberRenewalConditionReq
    ): Result<Boolean>

    @DELETE
    @Path("/{projectCode}/batch/remove")
    @Operation(summary = "批量移除用户组成员")
    fun batchRemoveGroupMembersFromManager(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户名", required = true)
        userId: String,
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String,
        @Parameter(description = "批量移除成员请求实体", required = true)
        removeMemberDTO: GroupMemberRemoveConditionReq
    ): Result<Boolean>

    @POST
    @Path("/{projectCode}/batch/{batchOperateType}/check")
    @Operation(summary = "批量操作用户组成员检查")
    fun batchOperateGroupMembersCheck(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户名", required = true)
        userId: String,
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String,
        @PathParam("batchOperateType")
        @Parameter(description = "批量操作类型", required = true)
        batchOperateType: BatchOperateType,
        @Parameter(description = "批量操作成员检查请求体", required = true)
        conditionReq: GroupMemberCommonConditionReq
    ): Result<BatchOperateGroupMemberCheckVo>

    @PUT
    @Path("/{projectCode}/batch/handover")
    @Operation(summary = "批量交接用户组成员")
    fun batchHandoverGroupMembersFromManager(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户名", required = true)
        userId: String,
        @PathParam("projectCode")
        @Parameter(description = "项目Code", required = true)
        projectCode: String,
        @Parameter(description = "批量交接成员请求实体", required = true)
        handoverMemberDTO: GroupMemberHandoverConditionReq
    ): Result<Boolean>
}
