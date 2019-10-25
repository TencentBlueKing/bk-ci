package com.tencent.devops.openapi.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.project.pojo.ProjectVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import com.tencent.devops.project.pojo.Result
import javax.ws.rs.QueryParam

@Api(tags = ["OPEN_API_PROJECT"], description = "OPEN-API-项目资源")
@Path("/{apigw:apigw-user|apigw-app|apigw}/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwProjectResource {

    @ApiOperation("获取单个项目信息")
    @GET
    @Path("/{projectId}")
    fun getProject(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<ProjectVO?>

    @ApiOperation("获取项目信息列表")
    @GET
    @Path("/getProjectByUser")
    fun getProjectByUser(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<List<ProjectVO>>

    @GET
    @Path("/getProjectByGroup")
    @ApiOperation("根据组织架构查询所有项目")
    fun getProjectByGroup(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("bgName", required = false)
        @QueryParam("bgName")
        bgName: String?,
        @ApiParam("deptName", required = false)
        @QueryParam("deptName")
        deptName: String?,
        @ApiParam("centerName", required = false)
        @QueryParam("centerName")
        centerName: String
    ): Result<List<ProjectVO>>
}