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

package com.tencent.devops.stream.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.stream.pojo.message.UserMessageRecord
import com.tencent.devops.stream.pojo.message.UserMessageType
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_STREAM_USER_MESSAGE", description = "user-消息中心页面")
@Path("/user/messages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserStreamUserMessageResource {

    @Operation(summary = "获取用户消息")
    @GET
    @Path("")
    fun getUserMessages(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "蓝盾项目ID", required = false)
        @QueryParam("projectId")
        projectId: String?,
        @Parameter(description = "消息类型")
        @QueryParam("messageType")
        messageType: UserMessageType?,
        @Parameter(description = "是否已读")
        @QueryParam("haveRead")
        haveRead: Boolean?,
        @Parameter(description = "消息唯一id")
        @QueryParam("messageId")
        messageId: String?,
        @Parameter(description = "触发人")
        @QueryParam("triggerUserId")
        triggerUserId: String?,
        @Parameter(description = "页码")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<UserMessageRecord>>

    @Operation(summary = "获取用户未读消息数量")
    @GET
    @Path("/noread")
    fun getUserMessagesNoreadCount(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "蓝盾项目ID", required = false)
        @QueryParam("projectId")
        projectId: String?
    ): Result<Int>

    @Operation(summary = "读取消息")
    @PUT
    @Path("/{id}/read")
    fun readMessage(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "消息ID")
        @PathParam("id")
        id: Int,
        @Parameter(description = "蓝盾项目ID", required = false)
        @QueryParam("projectId")
        projectCode: String?
    ): Result<Boolean>

    @Operation(summary = "读取所有消息")
    @PUT
    @Path("/read")
    fun readAllMessages(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "蓝盾项目ID", required = false)
        @QueryParam("projectId")
        projectCode: String?
    ): Result<Boolean>
}
