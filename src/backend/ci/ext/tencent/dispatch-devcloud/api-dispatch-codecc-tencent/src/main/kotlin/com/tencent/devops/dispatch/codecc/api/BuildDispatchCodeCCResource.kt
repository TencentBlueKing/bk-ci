package com.tencent.devops.dispatch.codecc.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.codecc.pojo.DockerIpInfoVO
import com.tencent.devops.dispatch.codecc.pojo.codecc.DockerHostBuildInfo
import com.tencent.devops.dispatch.codecc.pojo.codecc.DockerResourceOptionsVO
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

@Api(tags = ["BUILD_DISPATCH_CODECC"], description = "BUILD-CODECC构建机接口")
@Path("/dockerhost")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildDispatchCodeCCResource {

    @POST
    @Path("/containerId")
    @ApiOperation("上报容器id")
    fun reportContainerId(
        @ApiParam("buildId", required = true)
        @QueryParam("buildId")
        buildId: String,
        @ApiParam("vmSeqId", required = true)
        @QueryParam("vmSeqId")
        vmSeqId: String,
        @ApiParam("containerId", required = true)
        @QueryParam("containerId")
        containerId: String,
        @ApiParam("hostTag", required = true)
        @QueryParam("hostTag")
        hostTag: String? = null
    ): Result<Boolean>

    @ApiOperation("轮询开始任务")
    @POST
    @Path("/startBuild")
    fun startBuild(
        @ApiParam("dockerHost标识", required = true)
        @QueryParam("hostTag")
        hostTag: String
    ): Result<DockerHostBuildInfo>?

    @ApiOperation("轮询结束任务")
    @POST
    @Path("/endBuild")
    fun endBuild(
        @ApiParam("dockerHost标识", required = true)
        @QueryParam("hostTag")
        hostTag: String
    ): Result<DockerHostBuildInfo>?

    @ApiOperation("回滚任务到队列里面")
    @POST
    @Path("/rollbackBuild")
    fun rollbackBuild(
        @ApiParam("buildId", required = true)
        @QueryParam("buildId")
        buildId: String,
        @ApiParam("vmSeqId", required = true)
        @QueryParam("vmSeqId")
        vmSeqId: Int,
        @ApiParam("shutdown", required = true)
        @QueryParam("shutdown")
        shutdown: Boolean?
    ): Result<Boolean>?

    @ApiOperation("上报日志信息")
    @POST
    @Path("/postlog")
    fun postLog(
        @ApiParam("buildId", required = true)
        @QueryParam("buildId")
        buildId: String,
        @ApiParam("red", required = false)
        @QueryParam("red")
        red: Boolean,
        @ApiParam("message", required = true)
        message: String,
        @ApiParam("tag", required = false)
        @QueryParam("tag")
        tag: String?,
        @ApiParam("jobId", required = false)
        @QueryParam("jobId")
        jobId: String?
    ): Result<Boolean>?

    @POST
    @Path("/dockerIp/{dockerIp}/refresh")
    @ApiOperation("刷新Docker构建机状态")
    fun refresh(
        @ApiParam("构建机信息", required = true)
        @PathParam("dockerIp")
        dockerIp: String,
        @ApiParam("构建机信息", required = true)
        dockerIpInfoVO: DockerIpInfoVO
    ): Result<Boolean>

    @GET
    @Path("/resource-config/pipelines/{pipelineId}/vmSeqs/{vmSeqId}")
    @ApiOperation("获取蓝盾项目的docker性能配置")
    fun getResourceConfig(
        @ApiParam("蓝盾流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("JOB ID", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String
    ): Result<DockerResourceOptionsVO>

    @GET
    @Path("/qpc/projects/{projectId}/builds/{buildId}/vmSeqs/{vmSeqId}")
    @ApiOperation("获取蓝盾项目的docker性能配置")
    fun getQpcGitProjectList(
        @ApiParam("蓝盾项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("JOB ID", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String,
        @ApiParam("POOLNo", required = true)
        @QueryParam("poolNo")
        poolNo: Int
    ): Result<List<String>>
}
