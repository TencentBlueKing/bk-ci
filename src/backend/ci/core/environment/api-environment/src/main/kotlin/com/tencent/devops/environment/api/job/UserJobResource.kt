package com.tencent.devops.environment.api.job

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.environment.pojo.job.req.QueryJobInstanceLogsReq
import com.tencent.devops.environment.pojo.job.resp.GetStepInstanceDetailResult
import com.tencent.devops.environment.pojo.job.resp.GetStepInstanceStatusResult
import com.tencent.devops.environment.pojo.job.resp.JobResult
import com.tencent.devops.environment.pojo.job.resp.QueryJobInstanceStatusResult
import com.tencent.devops.environment.pojo.job.resp.QueryJobInstanceLogsResult
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
        queryJobInstanceLogsReq: QueryJobInstanceLogsReq
    ): JobResult<QueryJobInstanceLogsResult>

    @ApiOperation("请求上云版job - 获取步骤实例详情数据")
    @GET
    @Path("/{projectId}/get_step_instance_detail")
    fun getStepInstanceDetail(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "作业实例ID")
        @QueryParam("jobInstanceId")
        jobInstanceId: Long,
        @ApiParam(value = "步骤实例ID")
        @QueryParam("stepInstanceId")
        stepInstanceId: Long
    ): JobResult<GetStepInstanceDetailResult>

    @ApiOperation("请求上云版job - 获取步骤实例中各主机的任务执行状态数据")
    @GET
    @Path("/{projectId}/get_step_instance_status")
    fun getStepInstanceStatus(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "作业实例ID", required = true)
        @QueryParam("jobInstanceId")
        jobInstanceId: Long,
        @ApiParam(value = "步骤实例ID", required = true)
        @QueryParam("stepInstanceId")
        stepInstanceId: Long,
        @ApiParam(value = "步骤重试次数")
        @QueryParam("executeCount")
        executeCount: Int?,
        @ApiParam(value = "滚动批次")
        @QueryParam("batch")
        batch: Int?,
        @ApiParam(value = "每个分组里的最大主机数量，不传则返回全量数据。")
        @QueryParam("maxHostNumPerGroup")
        maxHostNumPerGroup: Int?,
        @ApiParam(value = "日志搜索关键字")
        @QueryParam("keyword")
        keyword: String?,
        @ApiParam(value = "主机IP/IPv6搜索关键字")
        @QueryParam("searchIp")
        searchIp: String?,
        @ApiParam(value = "执行状态")
        @QueryParam("status")
        status: Int?,
        @ApiParam(value = "结果标签")
        @QueryParam("tag")
        tag: String?
    ): JobResult<GetStepInstanceStatusResult>
}