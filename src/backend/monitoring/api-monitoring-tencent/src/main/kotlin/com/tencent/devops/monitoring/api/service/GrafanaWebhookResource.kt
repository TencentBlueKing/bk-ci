package com.tencent.devops.monitoring.api.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.monitoring.pojo.GrafanaNotification
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_MONITORING"], description = "监控")
@Path("/service/monitoring")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface GrafanaWebhookResource {

    @ApiOperation("grafana的webhook回调接口")
    @POST
    @Path("/grafana/webhook")
    fun webhookCallBack(
        @ApiParam(value = "grafana监控webhook回调通知消息", required = true)
        grafanaNotification: GrafanaNotification
    ): Result<Boolean>
}