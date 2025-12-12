package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.common.api.auth.AUTH_HEADER_CDS_IP
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_EVENT_TYPE
import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_WORKSPACE_NAME
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.openapi.BkApigwApi
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

@Tag(name = "OPEN_API_MARKET_EVENT_V4", description = "OPEN-API-研发商店-触发事件")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/market/event")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@BkApigwApi(version = "v4")
interface ApigwMarketEventResourceV4 {

    @Operation(summary = "云桌面webhook事件推送", tags = ["v4_app_cds_webhook", "v4_user_cds_webhook"])
    @POST
    @Path("{eventCode}/webhook/cds")
    fun cdsWebhook(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID")
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID")
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @Parameter(description = "实例化id")
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
