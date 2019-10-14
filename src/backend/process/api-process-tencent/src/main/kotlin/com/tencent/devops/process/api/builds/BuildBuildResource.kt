package com.tencent.devops.process.api.builds

import com.tencent.devops.common.api.auth.AUTH_HEADER_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_VM_NAME
import com.tencent.devops.common.api.auth.AUTH_HEADER_VM_SEQ_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildTaskResult
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_BUILD"], description = "构建-构建资源")
@Path("/build/builds")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildBuildResource {
    @ApiOperation("构建机器启动成功")
    @PUT
    @Path("/started")
    fun setStarted(
        @ApiParam(value = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @ApiParam(value = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_VM_SEQ_ID)
        vmSeqId: String,
        @ApiParam(value = "构建机名称", required = true)
        @HeaderParam(AUTH_HEADER_VM_NAME)
        vmName: String
    ): Result<BuildVariables>

    @ApiOperation("构建机请求任务")
    @GET
    @Path("/claim")
    fun claimTask(
        @ApiParam(value = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @ApiParam(value = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_VM_SEQ_ID)
        vmSeqId: String,
        @ApiParam(value = "构建机名称", required = true)
        @HeaderParam(AUTH_HEADER_VM_NAME)
        vmName: String
    ): Result<BuildTask>

    @ApiOperation("构建机完成任务")
    @POST
    @Path("/complete")
    fun completeTask(
        @ApiParam(value = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @ApiParam(value = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_VM_SEQ_ID)
        vmSeqId: String,
        @ApiParam(value = "构建机名称", required = true)
        @HeaderParam(AUTH_HEADER_VM_NAME)
        vmName: String,
        @ApiParam(value = "执行结果", required = true)
        result: BuildTaskResult
    ): Result<Boolean>

    @ApiOperation("End the seq build")
    @POST
    @Path("/end")
    fun endTask(
        @ApiParam(value = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @ApiParam(value = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_VM_SEQ_ID)
        vmSeqId: String,
        @ApiParam(value = "构建机名称", required = true)
        @HeaderParam(AUTH_HEADER_VM_NAME)
        vmName: String
    ): Result<Boolean>

    @ApiOperation("Heartbeat")
    @POST
    @Path("/heartbeat")
    fun heartbeat(
        @ApiParam(value = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @ApiParam(value = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_VM_SEQ_ID)
        vmSeqId: String,
        @ApiParam(value = "构建机名称", required = true)
        @HeaderParam(AUTH_HEADER_VM_NAME)
        vmName: String
    ): Result<Boolean>

    @ApiOperation("获取流水线构建单条历史")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/buildNums/{buildNum}/history")
    fun getSingleHistoryBuild(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("流水线buildNum", required = true)
        @PathParam("buildNum")
        buildNum: String,
        @ApiParam("渠道号，默认为DS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode?
    ): Result<BuildHistory?>

    @ApiOperation("获取构建详情")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/detail")
    fun getBuildDetail(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("渠道号，默认为DS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode
    ): Result<ModelDetail>

    @ApiOperation("获取Manual Action")
    @GET
    @Path("/builds/{buildId}/tasks/{taskId}/manualAction")
    fun getManualAction(
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("任务Id", required = true)
        @PathParam("taskId")
        taskId: String
    ): Result<String?>

    @ApiOperation("设置Task status")
    @POST
    @Path("/builds/{buildId}/tasks/{taskId}/status/{status}")
    fun updateTaskStatus(
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("任务Id", required = true)
        @PathParam("taskId")
        taskId: String,
        @ApiParam("状态", required = true)
        @PathParam("status")
        status: BuildStatus
    )
}