package com.tencent.devops.environment.api.job

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.environment.pojo.job.JobResult
import com.tencent.devops.environment.pojo.job.QueryJobInstanceLogsReq
import com.tencent.devops.environment.pojo.job.QueryJobInstanceLogsResult
import com.tencent.devops.environment.pojo.job.QueryJobInstanceStatusResult
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

@Api(tags = ["USER_JOB"], description = "用户-JOB")
@Path("/user/job")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserJobResource {
    @ApiOperation("查询任务状态的Job接口")
    @GET
    @Path("/{projectId}/query_job_instance_status")
    fun queryJobInstanceStatus(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "作业实例ID", required = true)
        @QueryParam("jobInstanceId")
        jobInstanceId: Long,
        @ApiParam(value = "是否返回每个ip上的任务详情，默认false", required = true)
        @QueryParam("returnIpResult")
        returnIpResult: Boolean? = false
    ): JobResult<QueryJobInstanceStatusResult>

    @ApiOperation("批量查询日志的Job接口")
    @POST
    @Path("/{projectId}/query_job_instance_logs")
    fun queryJobInstanceLogs(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "批量查询日志的请求信息", required = true)
        queryLogsReq: QueryJobInstanceLogsReq
    ): JobResult<QueryJobInstanceLogsResult>
}