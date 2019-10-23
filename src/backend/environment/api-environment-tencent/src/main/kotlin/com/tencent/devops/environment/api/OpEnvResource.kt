package com.tencent.devops.environment.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.ProjectConfig
import com.tencent.devops.environment.pojo.ProjectConfigPage
import com.tencent.devops.environment.pojo.ProjectConfigParam
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_ENV"], description = "环境管理")
@Path("/op/env")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpEnvResource {
    @ApiOperation("保存项目配置")
    @POST
    @Path("/project/saveProjectConfig")
    fun saveProjectConfig(
        @ApiParam("项目配置", required = true)
        projectConfigParam: ProjectConfigParam
    ): Result<Boolean>

    @ApiOperation("项目配置列表")
    @GET
    @Path("/project/listProjectConfig")
    fun listProjectConfig(): Result<List<ProjectConfig>>

    @ApiOperation("项目配置列表（分页）")
    @GET
    @Path("/projects/{projectId}/projectConfigs")
    fun list(
        @ApiParam(value = "第几页，从1开始", required = true)
        @QueryParam("page")
        page: Int,
        @ApiParam(value = "每页条数", required = true)
        @QueryParam("pageSize")
        pageSize: Int,
        @ApiParam(value = "项目ID", required = false)
        @PathParam("projectId")
        projectId: String?
    ): Result<ProjectConfigPage>
}