package com.tencent.devops.dispatcher.bcs.api.builds

import com.tencent.devops.common.api.auth.AUTH_HEADER_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.bcs.pojo.DispatchBuildStatusResp
import com.tencent.devops.dispatch.bcs.pojo.DispatchJobLogResp
import com.tencent.devops.dispatch.bcs.pojo.DispatchJobReq
import com.tencent.devops.dispatch.bcs.pojo.DispatchTaskResp
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

@Api(tags = ["BUILD_BCS"], description = "构建-bcs资源操作")
@Path("/build/bcs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildBcsResource {

    @ApiOperation("启动job")
    @POST
    @Path("/job")
    fun createJob(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("projectId", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @ApiParam("构建id", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @ApiParam("Job结构", required = true)
        jobReq: DispatchJobReq
    ): Result<DispatchTaskResp>

    @ApiOperation("获取job状态")
    @GET
    @Path("/job/{jobName}/status")
    fun getJobStatus(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("jobName", required = true)
        @PathParam("jobName")
        jobName: String
    ): Result<DispatchBuildStatusResp>

    @ApiOperation("获取job日志")
    @GET
    @Path("/job/{jobName}/logs")
    fun getJobLogs(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("jobName", required = true)
        @PathParam("jobName")
        jobName: String,
        @ApiParam("sinceTime", required = true)
        @QueryParam("sinceTime")
        sinceTime: Int?
    ): Result<DispatchJobLogResp>

    @ApiOperation("获取任务状态")
    @GET
    @Path("/task/status")
    fun getTaskStatus(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("taskId", required = true)
        @QueryParam("taskId")
        taskId: String
    ): Result<DispatchBuildStatusResp>
}
