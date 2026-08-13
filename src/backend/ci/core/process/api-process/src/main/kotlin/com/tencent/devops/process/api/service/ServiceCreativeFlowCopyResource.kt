package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.creative.CreativeFlowCopyRequest
import com.tencent.devops.process.pojo.creative.CreativeFlowCopyResult
import com.tencent.devops.process.pojo.creative.CreativeFlowCopyTraceVo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_CREATIVE_FLOW_COPY", description = "服务-创作流跨空间复制")
@Path("/service/creative/flows/projects/{projectId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceCreativeFlowCopyResource {

    @Operation(summary = "按分享授权跨空间复制创作流")
    @POST
    @Path("/copy_across_project")
    fun copyAcrossProject(
        @Parameter(description = "操作人（聘用者）", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "目标项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "复制请求", required = true)
        request: CreativeFlowCopyRequest
    ): Result<CreativeFlowCopyResult>

    @Operation(summary = "查询复制溯源记录")
    @GET
    @Path("/copy_traces")
    fun listCopyTraces(
        @Parameter(description = "查询人", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "目标项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "分享ID")
        @QueryParam("shareId")
        shareId: String?,
        @Parameter(description = "创作流条目ID")
        @QueryParam("flowId")
        flowId: String?,
        @Parameter(description = "目标流水线ID")
        @QueryParam("targetPipelineId")
        targetPipelineId: String?
    ): Result<List<CreativeFlowCopyTraceVo>>
}
