package com.tencent.devops.project.api.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.project.pojo.label.ProjectLabel
import com.tencent.devops.project.pojo.label.ProjectLabelRequest
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_PROJECT_LABEL"], description = "OP-项目标签")
@Path("/op/project/label")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpProjectLabelResource {

    @ApiOperation("获取所有项目标签信息")
    @GET
    @Path("/")
    fun getAllProjectLabel(): Result<List<ProjectLabel>>

    @ApiOperation("获取项目对应的项目标签信息")
    @GET
    @Path("/project/{projectId}")
    fun getProjectLabelByProjectId(
        @ApiParam(value = "项目id", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<ProjectLabel>>

    @ApiOperation("获取项目标签信息")
    @GET
    @Path("/{id}")
    fun getProjectLabel(
        @ApiParam(value = "项目标签id", required = true)
        @PathParam("id")
        id: String
    ): Result<ProjectLabel?>

    @ApiOperation("新增项目标签信息")
    @POST
    @Path("/")
    fun addProjectLabel(
        @ApiParam(value = "项目标签请求报文体", required = true)
        projectLabelRequest: ProjectLabelRequest
    ): Result<Boolean>

    @ApiOperation("更新项目标签信息")
    @PUT
    @Path("/{id}")
    fun updateProjectLabel(
        @ApiParam(value = "项目标签id", required = true)
        @PathParam("id")
        id: String,
        @ApiParam(value = "项目标签请求报文体", required = true)
        projectLabelRequest: ProjectLabelRequest
    ): Result<Boolean>

    @ApiOperation("删除项目标签信息")
    @DELETE
    @Path("/{id}")
    fun deleteProjectLabel(
        @ApiParam(value = "项目标签id", required = true)
        @PathParam("id")
        id: String
    ): Result<Boolean>
}