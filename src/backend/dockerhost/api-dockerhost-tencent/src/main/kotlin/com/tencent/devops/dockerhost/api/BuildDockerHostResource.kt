package com.tencent.devops.dockerhost.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dockerhost.pojo.DockerBuildParamNew
import com.tencent.devops.dockerhost.pojo.Status
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

@Api(tags = ["DOCKER_HOST"], description = "DockerHost")
@Path("/dockernew")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildDockerHostResource {

    @ApiOperation("Docker build")
    @POST
    @Path("/build/{projectId}/{pipelineId}/{vmSeqId}/{buildId}")
    fun dockerBuild(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "流水线Id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam(value = "vmSeqId", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String,
        @ApiParam(value = "buildId", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("镜像名称", required = true)
        dockerBuildParamNew: DockerBuildParamNew
    ): Result<Boolean>

    @ApiOperation("Docker build")
    @GET
    @Path("/build/{vmSeqId}/{buildId}")
    fun getDockerBuildStatus(
        @ApiParam(value = "vmSeqId", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String,
        @ApiParam(value = "buildId", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<Pair<Status, String>>
}