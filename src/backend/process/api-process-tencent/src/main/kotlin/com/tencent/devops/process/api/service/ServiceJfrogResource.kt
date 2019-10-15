package com.tencent.devops.process.api.service

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
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_JFROG"], description = "服务-jfrog资源")
@Path("/service/jfrog")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceJfrogResource {

    @ApiOperation("根据流水线id获取流水线名字")
    @POST
    @Path("/projects/{projectId}/getPipelineNames")
    fun getPipelineNameByIds(
        @ApiParam("项目id", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线id列表", required = true)
        pipelineIds: Set<String>
    ): Result<Map<String, String>>

    @ApiOperation("根据构建id获取构建号")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/getBuildNos")
    fun getBuildNoByBuildIds(
        @ApiParam("项目id", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建id列表", required = true)
        buildIds: Set<String>
    ): Result<Map<String, String>>

    @ApiOperation("根据流水线id和构建id对，获取build num")
    @POST
    @Path("/projects/{projectId}/getBuildNoByBuildIds")
    fun getBuildNoByBuildIds(
        @ApiParam("项目id", required = true)
        buildIds: Set<String>
    ): Result<Map<String, String>>

    @ApiOperation("获取时间段内有构建产物的构建数量")
    @GET
    @Path("/countArtifactoryByTime")
    fun getArtifactoryCountFromHistory(
        @ApiParam("起始时间", required = true)
        @QueryParam("startTime")
        startTime: Long,
        @ApiParam("终止时间", required = true)
        @QueryParam("endTime")
        endTime: Long
    ): Result<Int>
}