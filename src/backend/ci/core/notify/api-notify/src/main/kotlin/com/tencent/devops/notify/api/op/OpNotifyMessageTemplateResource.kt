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

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.notify.pojo.NotifyMessageCommonTemplate
import com.tencent.devops.notify.pojo.NotifyTemplateMessageRequest
import com.tencent.devops.notify.pojo.SubNotifyMessageTemplate
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_NOTIFY_MESSAGE_TEMPLATE", description = "通知模板")
@Path("/op/notify/message/template")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpNotifyMessageTemplateResource {

    @Operation(summary = "查找消息通知模板主体消息")
    @GET
    @Path("/list/common")
    fun getCommonNotifyMessageTemplates(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "模板代码", required = false)
        @QueryParam("templateCode")
        templateCode: String?,
        @Parameter(description = "模板名称", required = false)
        @QueryParam("templateName")
        templateName: String?,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<NotifyMessageCommonTemplate>>

    @Operation(summary = "查找消息通知模板列表")
    @GET
    @Path("/list/sub")
    fun getNotifyMessageTemplates(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "模板ID", required = false)
        @QueryParam("templateId")
        templateId: String
    ): Result<Page<SubNotifyMessageTemplate>>

    @Operation(summary = "添加消息通知模板")
    @POST
    @Path("/")
    fun addNotifyMessageTemplate(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "消息通知新增请求报文体", required = true)
        notifyMessageTemplateRequest: NotifyTemplateMessageRequest
    ): Result<Boolean>

    @Operation(summary = "更新消息通知模板")
    @PUT
    @Path("/ids/{templateId}")
    fun updateNotifyMessageTemplate(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "消息通知更新请求报文体", required = true)
        notifyMessageTemplateRequest: NotifyTemplateMessageRequest
    ): Result<Boolean>

    @Operation(summary = "匹配腾讯云ses模板id要邮件模板库")
    @PUT
    @Path("/tencent_cloud_ses_template_id")
    fun updateTXSESTemplateId(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "公共模板ID", required = true)
        @QueryParam("commonTemplateId")
        templateId: String,
        @Parameter(description = "腾讯云ses 模板id", required = true)
        @QueryParam("sesTemplateId")
        sesTemplateId: Int?
    ): Result<Boolean>

    @Operation(summary = "删除消息通知模板")
    @DELETE
    @Path("/ids/sub/{templateId}/{notifyType}")
    fun deleteNotifyMessageTemplate(
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "模板通知类型", required = true)
        @PathParam("notifyType")
        notifyType: String
    ): Result<Boolean>

    @Operation(summary = "删除公共消息通知模板")
    @DELETE
    @Path("/commons/ids/{templateId}")
    fun deleteCommonNotifyMessageTemplate(
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String
    ): Result<Boolean>
}
