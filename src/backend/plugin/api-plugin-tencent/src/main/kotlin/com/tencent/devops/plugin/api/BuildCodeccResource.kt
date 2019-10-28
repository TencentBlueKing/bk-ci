package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_CODECC"], description = "用户-codecc相关")
@Path("/build/codecc")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildCodeccResource {

    @ApiOperation("记录codecc扫描任务")
    @POST
    @Path("/save/task/{projectId}/{pipelineId}/{buildId}")
    fun saveCodeccTask(
        @ApiParam("项目ID", required = true)
        @PathParam(value = "projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam(value = "pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam(value = "buildId")
        buildId: String
    ): Result<Int>

    @ApiOperation("查询Codecc任务链接")
    @GET
    @Path("/task/{projectId}/{pipelineId}/{buildId}/url")
    fun queryCodeccTaskDetailUrl(
        @ApiParam("项目ID", required = true)
        @PathParam(value = "projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam(value = "pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam(value = "buildId")
        buildId: String
    ): String
}
