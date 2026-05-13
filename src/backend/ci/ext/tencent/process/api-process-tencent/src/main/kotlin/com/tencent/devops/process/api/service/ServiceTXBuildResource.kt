package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildManualStartupInfo
import com.tencent.devops.process.pojo.pipeline.IMateBuildStartRequest
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
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_BUILD", description = "服务-构建资源")
@Path("/service/builds")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceTXBuildResource {

    @Operation(summary = "IMate消息提醒启动流水线(可见性校验)")
    @POST
    @Path("/{projectId}/{pipelineId}/iMateBuildStart")
    fun iMateBuildStart(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "IMate启动请求", required = true)
        request: IMateBuildStartRequest
    ): Result<BuildId>

    @Operation(summary = "获取可见性流水线手动启动参数")
    @GET
    @Path("/{projectId}/{pipelineId}/visibility/manualStartupInfo")
    fun visibilityManualStartupInfo(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<BuildManualStartupInfo>
}
