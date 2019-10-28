package com.tencent.devops.dockerhost.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.pojo.DockerHostBuildInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.DELETE
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["IDC_DOCKER_HOST"], description = "DockerHost in IDC")
@Path("/idc/docker")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceIdcDockerHostResource {

    @ApiOperation("启动流水线构建")
    @POST
    @Path("/build/start")
    fun startBuild(
        @ApiParam("构建任务", required = true)
        dockerHostBuildInfo: DockerHostBuildInfo
    ): Result<String>

    @ApiOperation("终止流水线构建")
    @DELETE
    @Path("/build/end")
    fun endBuild(
        @ApiParam("构建任务", required = true)
        dockerHostBuildInfo: DockerHostBuildInfo
    ): Result<Boolean>

    @ApiOperation("获取容器数量")
    @GET
    @Path("/container/count")
    fun getContainerCount(): Result<Int>
}