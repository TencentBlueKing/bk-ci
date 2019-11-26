package com.tencent.devops.gitci.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.gitci.pojo.GitCIServicesConf
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.DELETE
import javax.ws.rs.QueryParam
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.PUT
import javax.ws.rs.core.MediaType

@Api(tags = arrayOf("OP_GIT_CI_SERVICES"), description = "git ci services管理")
@Path("/op/services")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpGitCIServicesResource {

    @ApiOperation("添加git项目")
    @POST
    @Path("/create")
    fun create(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam(value = "是否可以启用工蜂CI", required = true)
        gitCIServicesConf: GitCIServicesConf
    ): Result<Boolean>

    @ApiOperation("修改git项目")
    @PUT
    @Path("/update")
    fun update(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam(value = "服务ID", required = false)
        @QueryParam("id")
        id: Long,
        @ApiParam(value = "是否可以启用工蜂CI", required = false)
        @QueryParam("enable")
        enable: Boolean?
    ): Result<Boolean>

    @ApiOperation("删除git项目")
    @DELETE
    @Path("/delete")
    fun delete(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam(value = "服务ID", required = true)
        @QueryParam("id")
        id: Long
    ): Result<Boolean>

    @ApiOperation("列出git项目")
    @GET
    @Path("/list")
    fun list(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<List<GitCIServicesConf>>
}