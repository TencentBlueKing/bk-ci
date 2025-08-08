package com.tencent.devops.auth.api.service

import com.tencent.devops.auth.pojo.AuthResourceGroup
import com.tencent.devops.auth.pojo.dto.GroupAddDTO
import com.tencent.devops.auth.pojo.request.CustomGroupCreateReq
import com.tencent.devops.auth.pojo.vo.GroupDetailsInfoVo
import com.tencent.devops.auth.pojo.vo.GroupPermissionDetailVo
import com.tencent.devops.common.api.annotation.BkInterfaceI18n
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "AUTH_SERVICE_RESOURCE_GROUP", description = "权限--用户组相关")
@Path("/service/auth/resource/group")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceResourceGroupResource {
    @GET
    @Path("/{projectCode}/{groupId}/getGroupPermissionDetail")
    @Operation(summary = "查询用户组权限详情")
    @BkInterfaceI18n(keyPrefixNames = ["{data[*].actionId}"], responseDataCacheFlag = true)
    fun getGroupPermissionDetail(
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "用户组ID")
        @PathParam("groupId")
        groupId: Int
    ): Result<Map<String, List<GroupPermissionDetailVo>>>

    @GET
    @Path("/{projectCode}/{resourceType}/getMemberGroupsDetails")
    @Operation(summary = "获取项目成员有权限的用户组详情")
    fun getMemberGroupsDetails(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "资源类型")
        @PathParam("resourceType")
        resourceType: String,
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
        @Parameter(description = "起始位置,从0开始")
        @QueryParam("start")
        start: Int?,
        @Parameter(description = "每页多少条")
        @QueryParam("limit")
        limit: Int?
    ): Result<SQLPage<GroupDetailsInfoVo>>

    @POST
    @Path("/{projectCode}/createGroupByGroupCode/")
    @Operation(summary = "根据groupCode添加用户组")
    fun createGroupByGroupCode(
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "资源类型", required = true)
        @QueryParam("resourceType")
        resourceType: String,
        @Parameter(description = "用户组code,CI管理员为CI_MANAGER", required = true)
        @QueryParam("groupCode")
        groupCode: BkAuthGroup,
        @Parameter(description = "用户组名称", required = true)
        @QueryParam("groupName")
        groupName: String?,
        @Parameter(description = "用户组描述", required = true)
        @QueryParam("groupDesc")
        groupDesc: String?
    ): Result<Int>

    @POST
    @Path("/{projectCode}/createCustomGroupAndPermissions/")
    @Operation(summary = "创建自定义用户组和权限")
    fun createCustomGroupAndPermissions(
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "自定义组创建请求体", required = true)
        customGroupCreateReq: CustomGroupCreateReq
    ): Result<Int>

    @POST
    @Path("/{projectCode}/createGroup/")
    @Operation(summary = "创建自定义组(不包含权限，空权限组)")
    fun createGroup(
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "创建组DTO", required = true)
        groupAddDTO: GroupAddDTO
    ): Result<Int>

    @DELETE
    @Path("/{projectCode}/deleteGroup/")
    @Operation(summary = "删除用户组")
    fun deleteGroup(
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "资源类型", required = true)
        @QueryParam("resourceType")
        resourceType: String,
        @Parameter(description = "用户组ID", required = true)
        @QueryParam("groupId")
        groupId: Int
    ): Result<Boolean>

    @GET
    @Path("/{projectCode}/getByGroupCode")
    @Operation(summary = "根据GroupCode获取用户组")
    fun getByGroupCode(
        @Parameter(description = "项目Id", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "资源类型", required = true)
        @QueryParam("resourceType")
        resourceType: String,
        @Parameter(description = "资源Code", required = true)
        @QueryParam("resourceCode")
        resourceCode: String,
        @Parameter(description = "组Code", required = true)
        @QueryParam("groupCode")
        groupCode: BkAuthGroup
    ): Result<AuthResourceGroup?>
}
