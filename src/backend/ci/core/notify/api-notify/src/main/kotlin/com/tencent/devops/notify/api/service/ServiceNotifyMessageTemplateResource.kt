/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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
package com.tencent.devops.notify.api.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.notify.pojo.NotifyContext
import com.tencent.devops.notify.pojo.NotifyMessageContextRequest
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
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
        request: SendNotifyMessageTemplateRequest
    ): Result<Boolean>

    @ApiOperation("获取模板填充后消息内容")
    @POST
    @Path("/getContext")
    fun getNotifyMessageByTemplate(
        @ApiParam("使用模板获取消息内容请求", required = true)
        request: NotifyMessageContextRequest
    ): Result<NotifyContext?>

    /**
     * 使用模板取消消息通知
     * @param request 使用模板发送消息通知请求报文体
     */
    @ApiOperation("使用模板发送消息取消通知")
    @POST
    @Path("/complete")
    fun completeNotifyMessageByTemplate(
        @ApiParam("使用模板获取消息内容请求", required = true)
        request: SendNotifyMessageTemplateRequest
    ): Result<Boolean>
}
