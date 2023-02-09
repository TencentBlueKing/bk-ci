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
package com.tencent.devops.notify.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.notify.pojo.NotifyMessageCommonTemplate
import com.tencent.devops.notify.pojo.NotifyTemplateMessageRequest
import com.tencent.devops.notify.pojo.SubNotifyMessageTemplate
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_NOTIFY_MESSAGE_TEMPLATE"], description = "通知模板")
@Path("/op/notify/message/template")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpNotifyMessageTemplateResource {

    @ApiOperation("查找消息通知模板主体消息")
    @GET
    @Path("/list/common")
    fun getCommonNotifyMessageTemplates(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("模板代码", required = false)
        @QueryParam("templateCode")
        templateCode: String?,
        @ApiParam("模板名称", required = false)
        @QueryParam("templateName")
        templateName: String?,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<NotifyMessageCommonTemplate>>

    @ApiOperation("查找消息通知模板列表")
    @GET
    @Path("/list/sub")
    fun getNotifyMessageTemplates(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("模板ID", required = false)
        @QueryParam("templateId")
        templateId: String
    ): Result<Page<SubNotifyMessageTemplate>>

    @ApiOperation("添加消息通知模板")
    @POST
    @Path("/")
    fun addNotifyMessageTemplate(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("消息通知新增请求报文体", required = true)
        notifyMessageTemplateRequest: NotifyTemplateMessageRequest
    ): Result<Boolean>

    @ApiOperation("更新消息通知模板")
    @PUT
    @Path("/ids/{templateId}")
    fun updateNotifyMessageTemplate(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @ApiParam("消息通知更新请求报文体", required = true)
        notifyMessageTemplateRequest: NotifyTemplateMessageRequest
    ): Result<Boolean>

    @ApiOperation("匹配腾讯云ses模板id要邮件模板库")
    @PUT
    @Path("/tencent_cloud_ses_template_id")
    fun updateTXSESTemplateId(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("公共模板ID", required = true)
        @QueryParam("commonTemplateId")
        templateId: String,
        @ApiParam("腾讯云ses 模板id", required = true)
        @QueryParam("sesTemplateId")
        sesTemplateId: Int?
    ): Result<Boolean>

    @ApiOperation("删除消息通知模板")
    @DELETE
    @Path("/ids/sub/{templateId}/{notifyType}")
    fun deleteNotifyMessageTemplate(
        @ApiParam("模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @ApiParam("模板通知类型", required = true)
        @PathParam("notifyType")
        notifyType: String
    ): Result<Boolean>

    @ApiOperation("删除公共消息通知模板")
    @DELETE
    @Path("/commons/ids/{templateId}")
    fun deleteCommonNotifyMessageTemplate(
        @ApiParam("模板ID", required = true)
        @PathParam("templateId")
        templateId: String
    ): Result<Boolean>
}
