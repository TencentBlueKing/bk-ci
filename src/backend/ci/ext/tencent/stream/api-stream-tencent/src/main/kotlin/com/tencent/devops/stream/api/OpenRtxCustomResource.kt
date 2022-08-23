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

package com.tencent.devops.stream.api

import com.tencent.devops.common.api.pojo.Result
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

@Api(tags = ["OPEN_STREAM_RTX_CUSTOM"], description = "企业微信客服号回调接口")
@Path("/open/rtxcustom")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpenRtxCustomResource {

    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_XML)
    @ApiOperation("接受企业微信客服号回调信息(POST)")
    @POST
    @Path("/push")
    fun getCustomInfo(
        @ApiParam(value = "消息体签名", required = true)
        @QueryParam(value = "msg_signature")
        signature: String,
        @ApiParam(value = "时间戳", required = true)
        @QueryParam(value = "timestamp")
        timestamp: Long,
        @ApiParam(value = "随机数字串", required = true)
        @QueryParam(value = "nonce")
        nonce: String,
        @ApiParam(value = "回调密文", required = true)
        reqData: String?
    ): Result<Boolean>

    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_XML)
    @ApiOperation("接受企业微信客服号回调信息(GET)")
    @GET
    @Path("/push")
    fun getCustomInfo(
        @ApiParam(value = "消息体签名", required = true)
        @QueryParam(value = "msg_signature")
        signature: String,
        @ApiParam(value = "时间戳", required = true)
        @QueryParam(value = "timestamp")
        timestamp: Long,
        @ApiParam(value = "随机数字串", required = true)
        @QueryParam(value = "nonce")
        nonce: String,
        @ApiParam(value = "随机加密字符串", required = true)
        @QueryParam(value = "echostr")
        echoStr: String,
        @ApiParam(value = "回调密文", required = false)
        reqData: String?
    ): Result<String>
}
