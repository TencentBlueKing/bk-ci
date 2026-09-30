package com.tencent.devops.process.api.external

import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

/**
 * 外部 webhook 事件触发统一入口（按平台区分子路径 /external/webhook/xxx）
 */
@Tag(name = "EXTERNAL_WEBHOOK", description = "外部-webhook-事件触发")
@Path("/external/webhook")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ExternalWebhookResource {

    @Operation(summary = "制品库 webhook 事件推送")
    @POST
    @Path("/artifact")
    fun artifactWebhook(
        @Parameter(description = "事件类型")
        @HeaderParam("X-BKREPO-EVENT")
        eventType: String?,
        @Parameter(description = "webhook 密钥")
        @HeaderParam("X-BKREPO-WEBHOOK-SECRET")
        secret: String?,
        body: String
    ): Result<Boolean>
}
