package com.tencent.devops.process.api.builds

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PIPELINE_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.pipeline.SubPipelineStatus
import com.tencent.devops.process.pojo.pipeline.ProjectBuildId
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_SUBPIPELINE"], description = "构建-流水线调用")
@Path("/build/subpipeline")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildSubPipelineResource {
    @ApiOperation("获取子流水线状态")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/detail")
    fun getSubPipelineStatus(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<SubPipelineStatus>

    @ApiOperation("从构建机启动子流水线")
    @POST
    @Path("/pipelines/{callPipelineId}/atoms/{atomCode}/startByPipeline")
    fun callPipelineStartup(
        @ApiParam(value = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @ApiParam("当前流水线ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PIPELINE_ID)
        parentPipelineId: String,
        @ApiParam("构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @ApiParam("要启动的流水线ID", required = true)
        @PathParam("callPipelineId")
        callPipelineId: String,
        @ApiParam("插件标识", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam("插件ID", required = true)
        @QueryParam("taskId")
        taskId: String,
        @ApiParam("运行方式", required = true)
        @QueryParam("runMode")
        runMode: String,
        @ApiParam("启动参数", required = true)
        values: Map<String, String>
    ): Result<ProjectBuildId>
}