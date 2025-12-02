package com.tencent.devops.process.api.external

import com.tencent.devops.common.api.auth.AUTH_HEADER_CDS_IP
import com.tencent.devops.common.api.auth.AUTH_HEADER_EVENT_TYPE
import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_WORKSPACE_NAME
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType

@Tag(name = "EXTERNAL_EVENT", description = "外部-事件触发")
@Path("/external/event/{eventCode}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ExternalEventResource {

    @Operation(summary = "云桌面webhook事件推送")
    @POST
    @Path("/remoteDev/webhook")
    fun remoteDevWebhook(
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
        @HeaderParam("x-devops-event-type")
        eventType: String,
        @Parameter(description = "事件编码")
        @PathParam("eventCode")
        eventCode: String,
        body: String
    ): Result<Boolean>

    @Operation(summary = "通用webhook事件推送")
    @POST
    @Path("/generic/webhook")
    fun genericWebhook(
        @Context
        request: HttpServletRequest,
        @Parameter(description = "事件编码")
        @PathParam("eventCode")
        eventCode: String,
        body: String
    ): Result<Boolean>
}
