package com.tencent.devops.notify.api.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.notify.model.SendNotifyMessageTemplateRequest
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_NOTIFY_MESSAGE_TEMPLATE"], description = "通知模板")
@Path("/service/notify/message/template")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceNotifyMessageTemplateResource {

    @ApiOperation("使用模板发送消息通知")
    @POST
    @Path("/send")
    fun sendNotifyMessageByTemplate(
        @ApiParam("使用模板发送消息通知请求报文体", required = true)
        sendNotifyMessageTemplateRequest: SendNotifyMessageTemplateRequest
    ): Result<Boolean>
}