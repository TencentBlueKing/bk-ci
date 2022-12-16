package com.tencent.devops.auth.api.user

import com.tencent.bk.sdk.iam.dto.application.ApplicationDTO
import com.tencent.bk.sdk.iam.dto.manager.vo.V2ManagerRoleGroupVO
import com.tencent.devops.auth.pojo.vo.ActionInfoVo
import com.tencent.devops.auth.pojo.vo.ResourceTypeInfoVo
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
    fun listResourceTypes(
        @ApiParam(name = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<ResourceTypeInfoVo>>

    @GET
    @Path("listActions")
    @ApiOperation("展示动作列表")
    fun listActions(
        @ApiParam(name = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("资源类型", required = false)
        @QueryParam("resourceType")
        resourceType: String
    ): Result<List<ActionInfoVo>>

    @GET
    @Path("{projectId}/listGroups/")
    @ApiOperation("展示用户组列表")
    fun listGroups(
        @ApiParam(name = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("分级管理员是否继承查询二级管理员的用户组", required = false)
        @QueryParam("inherit")
        inherit: Boolean? = true,
        @ApiParam("操作id筛选", required = false)
        @QueryParam("actionId")
        actionId: String?,
        @ApiParam("资源类型筛选", required = false)
        @QueryParam("resourceType")
        resourceType: String?,
        @ApiParam("资源实例筛选", required = false)
        @QueryParam("resourceCode")
        resourceCode: String?,
        @ApiParam("资源实例筛选", required = false)
        @QueryParam("bkIamPath")
        bkIamPath: String?,
        @ApiParam("用户组名称", required = false)
        @QueryParam("name")
        name: String?,
        @ApiParam("用户组描述", required = false)
        @QueryParam("description")
        description: String?,
        @ApiParam("page", required = false)
        @QueryParam("page")
        page: Int,
        @ApiParam("pageSize", required = false)
        @QueryParam("pageSize")
        pageSize: Int,
    ): Result<V2ManagerRoleGroupVO>

    @POST
    @Path("/applyToJoinGroup")
    @ApiOperation("申请加入用户组")
    fun applyToJoinGroup(
        @ApiParam(name = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("申请实体", required = true)
        applicationDTO: ApplicationDTO
    ): Result<Boolean>
}
