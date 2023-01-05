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

package com.tencent.devops.support.api.service

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
