package com.tencent.devops.quality.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.quality.pojo.Group
import com.tencent.devops.quality.pojo.GroupCreate
import com.tencent.devops.quality.pojo.GroupSummaryWithPermission
import com.tencent.devops.quality.pojo.GroupUpdate
import com.tencent.devops.quality.pojo.GroupUsers
import com.tencent.devops.quality.pojo.ProjectGroupAndUsers
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
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

@Api(tags = ["USER_GROUP"], description = "质量红线-用户组")
@Path("/user/groups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserGroupResource {
    @ApiOperation("获取用户组列表")
    @Path("/{projectId}/list")
    @GET
    fun list(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("页目", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数目", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<GroupSummaryWithPermission>>

    @ApiOperation("获取项目用户组信息和组所有人员")
    @Path("/{projectId}/projectGroupAndUsers")
    @GET
    fun projectGroupAndUsers(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<ProjectGroupAndUsers>>

    @ApiOperation("创建用户组")
    @Path("/{projectId}/")
    @POST
    fun create(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("用户组", required = true)
        group: GroupCreate
    ): Result<Boolean>

    @ApiOperation("获取用户组")
    @Path("/{projectId}/{groupHashId}")
    @GET
    fun get(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("用户组HashID", required = true)
        @PathParam("groupHashId")
        groupHashId: String
    ): Result<Group>

    @ApiOperation("获取用户组用户")
    @Path("/{projectId}/{groupHashId}/users")
    @GET
    fun getUsers(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("用户组HashID", required = true)
        @PathParam("groupHashId")
        groupHashId: String
    ): Result<GroupUsers>

    @ApiOperation("修改用户组")
    @Path("/{projectId}/{groupHashId}")
    @PUT
    fun edit(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("用户组HashID", required = true)
        @PathParam("groupHashId")
        groupHashId: String,
        @ApiParam("用户组", required = true)
        group: GroupUpdate
    ): Result<Boolean>

    @ApiOperation("删除用户组")
    @Path("/{projectId}/{groupHashId}")
    @DELETE
    fun delete(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("用户组HashID", required = true)
        @PathParam("groupHashId")
        groupHashId: String
    ): Result<Boolean>
}