package com.tencent.devops.dockerhost.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dockerhost.pojo.DockerBuildParam
import com.tencent.devops.dockerhost.pojo.DockerRunParam
import com.tencent.devops.dockerhost.pojo.DockerRunResponse
import com.tencent.devops.dockerhost.pojo.DockerLogsResponse
import com.tencent.devops.dockerhost.pojo.Status
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.DELETE
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType

@Api(tags = ["DOCKER_HOST"], description = "DockerHost")
@Path("/docker")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface TXServiceDockerHostResource {


    @ApiOperation("Docker run")
    @POST
    @Path("/run/{projectId}/{pipelineId}/{vmSeqId}/{buildId}")
    fun dockerRun(
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
        dockerRunParam: DockerRunParam,
        @Context request: HttpServletRequest
    ): Result<DockerRunResponse>

    @ApiOperation("get docker log")
    @GET
    @Path("/runlog/{projectId}/{pipelineId}/{vmSeqId}/{buildId}/{containerId}/{logStartTimeStamp}")
    fun getDockerRunLogs(
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
        @ApiParam("containerId", required = true)
        @PathParam("containerId")
        containerId: String,
        @ApiParam("logStartTimeStamp", required = true)
        @PathParam("logStartTimeStamp")
        logStartTimeStamp: Int,
        @Context request: HttpServletRequest
    ): Result<DockerLogsResponse>

    @ApiOperation("stop docker run")
    @DELETE
    @Path("/run/{projectId}/{pipelineId}/{vmSeqId}/{buildId}/{containerId}")
    fun dockerStop(
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
        @ApiParam("containerId", required = true)
        @PathParam("containerId")
        containerId: String,
        @Context request: HttpServletRequest
    ): Result<Boolean>
}