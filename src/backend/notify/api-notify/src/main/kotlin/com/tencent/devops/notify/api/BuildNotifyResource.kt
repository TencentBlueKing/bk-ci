package com.tencent.devops.notify.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import com.tencent.devops.notify.pojo.RtxNotifyMessage
import com.tencent.devops.notify.pojo.SmsNotifyMessage
import com.tencent.devops.notify.pojo.WechatNotifyMessage
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

/**
 * @author ajackyu
 *  date 2019-06-27
 */

@Api(tags = ["SERVICE_NOTIFIES"], description = "通知")
@Path("/build/notifies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildNotifyResource {

    @ApiOperation("发送RTX信息通知")
    @POST
    @Path("/rtx")
    fun sendRtxNotify(
        @ApiParam(value = "RTX信息内容", required = true)
        message: RtxNotifyMessage

    ): Result<Boolean>

    @ApiOperation("发送电子邮件通知")
    @POST
    @Path("/email")
    fun sendEmailNotify(@ApiParam(value = "电子邮件信息内容", required = true) message: EmailNotifyMessage): Result<Boolean>

    @ApiOperation("发送微信通知")
    @POST
    @Path("/wechat")
    fun sendWechatNotify(@ApiParam(value = "微信信息内容", required = true) message: WechatNotifyMessage): Result<Boolean>

    @ApiOperation("发送短信通知")
    @POST
    @Path("/sms")
    fun sendSmsNotify(@ApiParam(value = "短信信息内容", required = true) message: SmsNotifyMessage): Result<Boolean>
}