package com.tencent.devops.stream.api.app

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.stream.pojo.AppTriggerBuildResult
import com.tencent.devops.stream.pojo.V2AppTriggerBuildReq
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "APP_STREAM_TRIGGER", description = "app-TriggerBuild页面")
@Path("/app/trigger/build")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface AppStreamTriggerResource {
    @Operation(summary = "人工TriggerBuild启动构建")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/startup")
    fun triggerStartup(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "蓝盾项目ID")
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "TriggerBuild请求", required = true)
        triggerBuildReq: V2AppTriggerBuildReq
    ): Result<AppTriggerBuildResult>
}
