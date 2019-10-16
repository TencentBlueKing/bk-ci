package com.tencent.devops.support.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.glassfish.jersey.media.multipart.FormDataParam
import javax.ws.rs.core.MediaType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.support.model.wechatwork.enums.UploadMediaType
import com.tencent.devops.support.model.wechatwork.message.ImageMessage
import com.tencent.devops.support.model.wechatwork.message.TextMessage
import com.tencent.devops.support.model.wechatwork.result.UploadMediaResult
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextMessage
import java.io.InputStream
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam

/**
 * Created by Freyzheng on 2018/8/2.
 */

@Api(tags = ["SERVICE_WECHART_WORK"], description = "服务-企业微信")
@Path("/service/wechat-work")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceWechatWorkResource {
    @ApiOperation("发送企业微信会话的文本信息")
    @POST
    @Path("/message/text")
    fun sendTextMessage(
        @ApiParam(value = "文本内容", required = true)
        textMessage: TextMessage
    ): Result<Boolean>

    @ApiOperation("发送企业微信会话的富文本信息")
    @POST
    @Path("/message/richtext")
    fun sendRichtextMessage(
        @ApiParam(value = "富文本文内容", required = true)
        richitextMessage: RichtextMessage
    ): Result<Boolean>

    @ApiOperation("发送企业微信会话的图片信息")
    @POST
    @Path("/message/image")
    fun sendImageMessage(
        @ApiParam(value = "图片内容", required = true)
        imageMessage: ImageMessage
    ): Result<Boolean>

    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @ApiOperation("上传企业微信临时素材")
    @POST
    @Path("/media")
    fun uploadMedia(
        @ApiParam("临时素材类型", required = true)
        @QueryParam(value = "mediaType")
        uploadMediaType: UploadMediaType,
        @ApiParam("临时素材名字", required = true)
        @QueryParam(value = "mediaName")
        mediaName: String,
        @ApiParam("临时素材文件", required = true)
        @FormDataParam("media")
        mediaInputStream: InputStream
    ): Result<UploadMediaResult?>
}