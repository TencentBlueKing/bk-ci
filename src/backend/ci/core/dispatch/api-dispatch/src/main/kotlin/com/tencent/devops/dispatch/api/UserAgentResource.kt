package com.tencent.devops.dispatch.api

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.dispatch.pojo.thirdpartyagent.TPAPipelineBuild
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import com.tencent.devops.common.api.pojo.Result
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
        @Parameter(description = "第几页")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<TPAPipelineBuild>>
}