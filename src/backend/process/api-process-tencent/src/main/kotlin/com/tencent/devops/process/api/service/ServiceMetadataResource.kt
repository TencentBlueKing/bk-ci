package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.Property
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

@Api(tags = ["SERVICE_METADATA"], description = "服务-元数据")
@Path("/service/metadatas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceMetadataResource {
    @ApiOperation("创建元数据")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/")
    fun create(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("元数据", required = true)
        metadatas: List<Property>
    ): Result<Boolean>

    @ApiOperation("获取构建元数据")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/")
    fun list(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<List<Property>>
}