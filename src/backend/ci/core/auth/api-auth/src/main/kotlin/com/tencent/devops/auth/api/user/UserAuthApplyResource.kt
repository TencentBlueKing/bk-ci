package com.tencent.devops.auth.api.user

import com.tencent.devops.auth.pojo.ApplyJoinGroupInfo
import com.tencent.devops.auth.pojo.SearchGroupInfo
import com.tencent.devops.auth.pojo.vo.ActionInfoVo
import com.tencent.devops.auth.pojo.vo.AuthApplyRedirectInfoVo
import com.tencent.devops.auth.pojo.vo.GroupPermissionDetailVo
import com.tencent.devops.auth.pojo.vo.ManagerRoleGroupVO
import com.tencent.devops.auth.pojo.vo.ResourceTypeInfoVo
import com.tencent.devops.common.api.annotation.BkInterfaceI18n
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.IgnoreUserApiPermission
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_AUTH_APPLY", description = "用户权限申请")
@Path("/user/auth/apply")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface UserAuthApplyResource {
    @GET
    @Path("listResourceTypes")
    @Operation(summary = "资源类型列表")
    @BkInterfaceI18n(keyPrefixNames = ["{data[*].resourceType}"], responseDataCacheFlag = true)
    fun listResourceTypes(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<ResourceTypeInfoVo>>

    @GET
    @Path("listActions")
    @Operation(summary = "展示动作列表")
    @BkInterfaceI18n(keyPrefixNames = ["{data[*].action}"], responseDataCacheFlag = true)
    fun listActions(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "资源类型", required = false)
        @QueryParam("resourceType")
        resourceType: String
    ): Result<List<ActionInfoVo>>

    @POST
    @Path("{projectId}/listGroups/")
    @IgnoreUserApiPermission
    @Operation(summary = "展示用户组列表")
    fun listGroups(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "搜索用户组实体", required = true)
        searchGroupInfo: SearchGroupInfo
    ): Result<ManagerRoleGroupVO>

    @POST
    @Path("applyToJoinGroup")
    @Operation(summary = "申请加入用户组")
    fun applyToJoinGroup(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "申请实体", required = true)
        applyJoinGroupInfo: ApplyJoinGroupInfo
    ): Result<Boolean>

    @GET
    @Path("{groupId}/getGroupPermissionDetail")
    @Operation(summary = "查询用户组权限详情")
    @BkInterfaceI18n(keyPrefixNames = ["{data[*].actionId}"], responseDataCacheFlag = true)
    fun getGroupPermissionDetail(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "用户组ID")
        @PathParam("groupId")
        groupId: Int
    ): Result<Map<String, List<GroupPermissionDetailVo>>>

    @GET
    @Path("getRedirectInformation")
    @IgnoreUserApiPermission
    @Operation(summary = "获取权限申请重定向信息")
    fun getRedirectInformation(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "资源类型", required = true)
        @QueryParam("resourceType")
        resourceType: String,
        @Parameter(description = "资源实例", required = true)
        @QueryParam("resourceCode")
        resourceCode: String,
        @Parameter(description = "动作", required = false)
        @QueryParam("action")
        action: String?
    ): Result<AuthApplyRedirectInfoVo>
}
