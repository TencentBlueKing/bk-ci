package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.process.pojo.webhook.WebhookTriggerParams
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "SERVICE_BUILD", description = "服务-WEBHOOK构建资源")
@Path("/service/builds")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceWebhookBuildResource {

    @Operation(summary = "webhook触发启动流水线")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/webhook/trigger")
    fun webhookTrigger(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "启动参数", required = true)
        params: WebhookTriggerParams,
        @Parameter(description = "渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode = ChannelCode.BS,
        @Parameter(description = "启动类型", required = false)
        @QueryParam("startType")
        startType: StartType = StartType.WEB_HOOK
    ): Result<String?>
}
