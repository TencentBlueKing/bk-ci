/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.tencent.devops.notify.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.notify.pojo.BaseMessage
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import com.tencent.devops.notify.pojo.NotificationResponseWithPage
import com.tencent.devops.notify.pojo.RtxNotifyMessage
import com.tencent.devops.notify.pojo.SmsNotifyMessage
import com.tencent.devops.notify.pojo.WechatNotifyMessage
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_NOTIFIES", description = "通知")
@Path("/op/notifies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface OpNotifyResource {

    @Operation(summary = "发送RTX信息通知")
    @POST
    @Path("/rtx")
    fun sendRtxNotify(
        @Parameter(description = "RTX信息内容", required = true)
        message: RtxNotifyMessage
    ): Result<Boolean>

    @Operation(summary = "发送电子邮件通知")
    @POST
    @Path("/email")
    fun sendEmailNotify(@Parameter(description = "电子邮件信息内容", required = true) message: EmailNotifyMessage): Result<Boolean>

    @Operation(summary = "发送微信通知")
    @POST
    @Path("/wechat")
    fun sendWechatNotify(@Parameter(description = "微信信息内容", required = true) message: WechatNotifyMessage): Result<Boolean>

    @Operation(summary = "发送短信通知")
    @POST
    @Path("/sms")
    fun sendSmsNotify(@Parameter(description = "短信信息内容", required = true) message: SmsNotifyMessage): Result<Boolean>

    @Operation(summary = "列出所有的通知")
    @GET
    @Path("/listNotifications")
    fun listNotifications(
        @Parameter(description = "通知方式，包括 rtx, email, wechat, sms 四种", required = true)
        @QueryParam("type")
        type: String?,
        @Parameter(description = "开始页数，从1开始", required = false, example = "0")
        @QueryParam("page")
        page: Int,
        @Parameter(description = "每页数据条数", required = false, example = "10")
        @QueryParam("pageSize")
        pageSize: Int,
        @Parameter(description = "结果是否是成功的", required = false)
        @QueryParam("success")
        success: Boolean?,
        @Parameter(description = "源系统id", required = false)
        @QueryParam("fromSysId")
        fromSysId: String?,
        @Parameter(description = "创建时间排序规则，传 'descend' 则递减排序，不传或传其他值递增排序）", required = false)
        @QueryParam("createdTimeSortOrder")
        createdTimeSortOrder: String?
    ): Result<NotificationResponseWithPage<BaseMessage>?>
}
