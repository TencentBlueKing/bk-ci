package com.tencent.devops.notify.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.notify.model.BaseMessage
import com.tencent.devops.notify.model.EmailNotifyMessage
import com.tencent.devops.notify.model.NotificationResponseWithPage
import com.tencent.devops.notify.model.RtxNotifyMessage
import com.tencent.devops.notify.model.SmsNotifyMessage
import com.tencent.devops.notify.model.WechatNotifyMessage
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_NOTIFIES"], description = "通知")
@Path("/op/notifies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpNotifyResource {

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

    @ApiOperation("列出所有的通知")
    @GET
    @Path("/listNotifications")
    fun listNotifications(
        @ApiParam(value = "通知方式，包括 rtx, email, wechat, sms 四种", required = true)
        @QueryParam("type")
        type: String?,
        @ApiParam(value = "开始页数，从1开始", required = false, defaultValue = "0")
        @QueryParam("page")
        page: Int,
        @ApiParam(value = "每页数据条数", required = false, defaultValue = "10")
        @QueryParam("pageSize")
        pageSize: Int,
        @ApiParam(value = "结果是否是成功的", required = false)
        @QueryParam("success")
        success: Boolean?,
        @ApiParam(value = "源系统id", required = false)
        @QueryParam("fromSysId")
        fromSysId: String?,
        @ApiParam(value = "创建时间排序规则，传 'descend' 则递减排序，不传或传其他值递增排序）", required = false)
        @QueryParam("createdTimeSortOrder")
        createdTimeSortOrder: String?
    ): Result<NotificationResponseWithPage<BaseMessage>?>
}