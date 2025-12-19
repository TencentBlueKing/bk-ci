package com.tencent.devops.dispatch.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.pojo.thirdpartyagent.JobIdAndName
import com.tencent.devops.dispatch.pojo.thirdpartyagent.PipelineIdAndName
import com.tencent.devops.dispatch.pojo.thirdpartyagent.TPAPipelineBuildCountResp
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_AGENT", description = "用户-Agent")
@Path("/user/agents")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserAgentResource {

    @Operation(summary = "获取agent任务详情列表")
    @GET
    @Path("/listAgentPipelineJobs")
    fun listAgentPipelineJobs(
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "agent Hash ID", required = false)
        @QueryParam("agentId")
        agentId: String?,
        @Parameter(description = "env Hash ID", required = false)
        @QueryParam("envId")
        envId: String?,
        @Parameter(description = "第几页", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false)
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(description = "开按执行时间", required = false)
        @QueryParam("startTime")
        startTime: Long?,
        @Parameter(description = "结束执行时间", required = false)
        @QueryParam("endTime")
        endTime: Long?,
        @Parameter(description = "pipeline ID", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(description = "job ID", required = false)
        @QueryParam("jobId")
        jobId: String?,
        @Parameter(description = "执行人", required = false)
        @QueryParam("creator")
        creator: String?,
    ): Result<TPAPipelineBuildCountResp>

    @Operation(summary = "获取agent任务详情列表-查询条件pipelineName")
    @GET
    @Path("/listAgentPipelineJobs/searchByPipelineName")
    fun listAgentPipelineJobsByPipelineName(
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "agent Hash ID", required = false)
        @QueryParam("agentId")
        agentId: String?,
        @Parameter(description = "env Hash ID", required = false)
        @QueryParam("envId")
        envId: String?,
        @Parameter(description = "搜索名称")
        @QueryParam("pipelineName")
        pipelineName: String?
    ): Result<List<PipelineIdAndName>>

    @Operation(summary = "获取agent任务详情列表-查询条件jobName")
    @GET
    @Path("/listAgentPipelineJobs/searchByJobName")
    fun listAgentPipelineJobsByJobName(
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "agent Hash ID", required = false)
        @QueryParam("agentId")
        agentId: String?,
        @Parameter(description = "env Hash ID", required = false)
        @QueryParam("envId")
        envId: String?,
        @Parameter(description = "搜索名称")
        @QueryParam("jobName")
        jobName: String?
    ): Result<List<JobIdAndName>>

    @Operation(summary = "获取agent任务详情列表-查询条件creator")
    @GET
    @Path("/listAgentPipelineJobs/searchByCreator")
    fun listAgentPipelineJobsByCreator(
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "agent Hash ID", required = false)
        @QueryParam("agentId")
        agentId: String?,
        @Parameter(description = "env Hash ID", required = false)
        @QueryParam("envId")
        envId: String?,
        @Parameter(description = "搜索名称")
        @QueryParam("creator")
        creator: String?
    ): Result<List<String>>
}