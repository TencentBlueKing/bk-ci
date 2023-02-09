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

package com.tencent.devops.stream.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.stream.pojo.message.UserMessageRecord
import com.tencent.devops.stream.pojo.message.UserMessageType
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_STREAM_USER_MESSAGE"], description = "user-消息中心页面")
@Path("/user/messages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserStreamUserMessageResource {

    @ApiOperation("获取用户消息")
    @GET
    @Path("")
    fun getUserMessages(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "蓝盾项目ID", required = false)
        @QueryParam("projectId")
        projectId: String?,
        @ApiParam(value = "消息类型")
        @QueryParam("messageType")
        messageType: UserMessageType?,
        @ApiParam(value = "是否已读")
        @QueryParam("haveRead")
        haveRead: Boolean?,
        @ApiParam(value = "消息唯一id")
        @QueryParam("messageId")
        messageId: String?,
        @ApiParam(value = "触发人")
        @QueryParam("triggerUserId")
        triggerUserId: String?,
        @ApiParam(value = "页码")
        @QueryParam("page")
        page: Int?,
        @ApiParam(value = "每页数量")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<UserMessageRecord>>

    @ApiOperation("获取用户未读消息数量")
    @GET
    @Path("/noread")
    fun getUserMessagesNoreadCount(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "蓝盾项目ID", required = false)
        @QueryParam("projectId")
        projectId: String?
    ): Result<Int>

    @ApiOperation("读取消息")
    @PUT
    @Path("/{id}/read")
    fun readMessage(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "消息ID")
        @PathParam("id")
        id: Int,
        @ApiParam(value = "蓝盾项目ID", required = false)
        @QueryParam("projectId")
        projectCode: String?
    ): Result<Boolean>

    @ApiOperation("读取所有消息")
    @PUT
    @Path("/read")
    fun readAllMessages(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "蓝盾项目ID", required = false)
        @QueryParam("projectId")
        projectCode: String?
    ): Result<Boolean>
}
