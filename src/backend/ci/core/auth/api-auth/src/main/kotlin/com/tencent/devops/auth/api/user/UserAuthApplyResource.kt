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
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_AUTH_APPLY"], description = "用户权限申请")
@Path("/user/auth/apply")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface UserAuthApplyResource {
    @GET
    @Path("listResourceTypes")
    @ApiOperation("资源类型列表")
    @BkInterfaceI18n(keyPrefixNames = ["{data[*].resourceType}"])
    fun listResourceTypes(
        @ApiParam(name = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<ResourceTypeInfoVo>>

    @GET
    @Path("listActions")
    @ApiOperation("展示动作列表")
    @BkInterfaceI18n(keyPrefixNames = ["{data[*].action}"])
    fun listActions(
        @ApiParam(name = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("资源类型", required = false)
        @QueryParam("resourceType")
        resourceType: String
    ): Result<List<ActionInfoVo>>

    @POST
    @Path("{projectId}/listGroups/")
    @ApiOperation("展示用户组列表")
    fun listGroups(
        @ApiParam(name = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("搜索用户组实体", required = true)
        searchGroupInfo: SearchGroupInfo
    ): Result<ManagerRoleGroupVO>

    @POST
    @Path("applyToJoinGroup")
    @ApiOperation("申请加入用户组")
    fun applyToJoinGroup(
        @ApiParam(name = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("申请实体", required = true)
        applyJoinGroupInfo: ApplyJoinGroupInfo
    ): Result<Boolean>

    @GET
    @Path("{groupId}/getGroupPermissionDetail")
    @ApiOperation("查询用户组权限详情")
    @BkInterfaceI18n(keyPrefixNames = ["{data[*].actionId}"])
    fun getGroupPermissionDetail(
        @ApiParam(name = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("用户组ID")
        @PathParam("groupId")
        groupId: Int
    ): Result<List<GroupPermissionDetailVo>>

    @GET
    @Path("getRedirectInformation")
    @ApiOperation("获取权限申请重定向信息")
    fun getRedirectInformation(
        @ApiParam(name = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @ApiParam("资源类型", required = true)
        @QueryParam("resourceType")
        resourceType: String,
        @ApiParam("资源实例", required = true)
        @QueryParam("resourceCode")
        resourceCode: String,
        @ApiParam("动作", required = false)
        @QueryParam("action")
        action: String?
    ): Result<AuthApplyRedirectInfoVo>
}
