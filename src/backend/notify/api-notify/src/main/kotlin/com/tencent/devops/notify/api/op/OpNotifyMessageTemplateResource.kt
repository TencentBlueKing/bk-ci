package com.tencent.devops.notify.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.notify.model.NotifyMessageCommonTemplate
import com.tencent.devops.notify.model.NotifyTemplateMessageRequest
import com.tencent.devops.notify.pojo.NotifyMessageTemplate
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

    @ApiOperation("获取消息通知模板")
    @GET
    @Path("/ids/{templateId}")
    fun getNotifyMessageTemplate(
        @ApiParam("模板ID", required = true)
        @PathParam("templateId")
        templateId: String
    ): Result<NotifyMessageTemplate?>

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

    @ApiOperation("获取消息通知模板")
    @GET
//    @Path("/ids/{templateId}")
    @Path("/ids/templateId/{templateId}")
    fun getNotifyMessageTemplateV2(
        @ApiParam("模板ID", required = true)
        @PathParam("templateId")
        templateId: String
    ): Result<NotifyMessageTemplate?>

    @ApiOperation("更新消息通知模板")
    @PUT
//    @Path("/ids/{templateId}")
    @Path("/ids/templateId/{templateId}")
    fun updateNotifyMessageTemplateV2(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @ApiParam("消息通知更新请求报文体", required = true)
        notifyMessageTemplateRequest: NotifyTemplateMessageRequest
    ): Result<Boolean>

    @ApiOperation("删除消息通知模板")
    @DELETE
//    @Path("/ids/sub/{templateId}/{notifyType}")
    @Path("/ids/sub/templateId/{templateId}/notifyType/{notifyType}")
    fun deleteNotifyMessageTemplateV2(
        @ApiParam("模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @ApiParam("模板通知类型", required = true)
        @PathParam("notifyType")
        notifyType: String
    ): Result<Boolean>

    @ApiOperation("删除公共消息通知模板")
    @DELETE
//    @Path("/commons/ids/{templateId}")
    @Path("/commons/ids/templateId/{templateId}")
    fun deleteCommonNotifyMessageTemplateV2(
        @ApiParam("模板ID", required = true)
        @PathParam("templateId")
        templateId: String
    ): Result<Boolean>
}