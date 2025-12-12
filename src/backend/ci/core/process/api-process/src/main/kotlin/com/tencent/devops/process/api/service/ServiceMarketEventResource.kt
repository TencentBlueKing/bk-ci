package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_CDS_IP
import com.tencent.devops.common.api.auth.AUTH_HEADER_EVENT_TYPE
import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_WORKSPACE_NAME
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_MARKET_EVENT", description = "服务-研发商店-事件触发")
@Path("/service/market/event/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceMarketEventResource {

    @Operation(summary = "云桌面webhook事件推送")
    @POST
    @Path("{eventCode}/webhook/cds")
    fun cdsWebhook(
        @Parameter(description = "用户ID")
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID")
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @Parameter(description = "x-devops-workspace-name")
        @HeaderParam(AUTH_HEADER_WORKSPACE_NAME)
        workspaceName: String,
        @Parameter(description = "x-devops-cds-ip")
        @HeaderParam(AUTH_HEADER_CDS_IP)
        cdsIp: String,
        @Parameter(description = AUTH_HEADER_EVENT_TYPE)
        @HeaderParam("x-devops-cds-event-type")
        eventType: String,
        @Parameter(description = "事件编码")
        @PathParam("eventCode")
        eventCode: String,
        body: String
    ): Result<Boolean>
}
