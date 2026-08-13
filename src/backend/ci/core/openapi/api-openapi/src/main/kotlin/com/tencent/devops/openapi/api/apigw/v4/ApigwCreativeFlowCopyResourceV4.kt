package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.openapi.BkApigwApi
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

@Tag(name = "OPENAPI_CREATIVE_FLOW_COPY_V4", description = "OPENAPI-创作流跨空间复制")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/projects/{projectId}/creative_flows")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@BkApigwApi(version = "v4")
interface ApigwCreativeFlowCopyResourceV4 {

    @Operation(
        summary = "按分享授权跨空间复制创作流",
        tags = ["v4_user_creative_flow_copy_across_project"]
    )
    @POST
    @Path("/copy_across_project")
    fun copyAcrossProject(
        @Parameter(description = "appCode", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "目标项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "复制请求", required = true)
        request: CreativeFlowCopyRequest
    ): Result<CreativeFlowCopyResult>

    @Operation(summary = "查询复制溯源记录", tags = ["v4_user_creative_flow_copy_traces"])
    @GET
    @Path("/copy_traces")
    fun listCopyTraces(
        @Parameter(description = "appCode", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "目标项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "分享命名空间ID")
        @QueryParam("shareId")
        shareId: String?,
        @Parameter(description = "分享条目ID")
        @QueryParam("flowId")
        flowId: String?,
        @Parameter(description = "目标流水线ID")
        @QueryParam("targetPipelineId")
        targetPipelineId: String?
    ): Result<List<CreativeFlowCopyTraceVo>>
}
