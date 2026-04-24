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

package com.tencent.devops.ai.api.user

import com.tencent.devops.ai.pojo.AiChatRunStatus
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.glassfish.jersey.server.ChunkedOutput

@Tag(name = "USER_AI_CHAT", description = "用户-AI对话(AG-UI)")
@Path("/user/ai/chat")
@Consumes(MediaType.APPLICATION_JSON)
interface UserAiChatResource {

    @Operation(summary = "AG-UI 流式对话")
    @POST
    @Path("/run")
    @Produces("text/event-stream")
    fun run(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(description = "AG-UI RunAgentInput", required = true)
        body: String
    ): ChunkedOutput<String>

    @Operation(summary = "查询对话运行状态")
    @GET
    @Path("/status/{threadId}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getRunStatus(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(
            description = "会话线程ID",
            required = true
        )
        @PathParam("threadId")
        threadId: String
    ): Result<AiChatRunStatus>

    @Operation(summary = "停止正在进行的对话")
    @POST
    @Path("/stop/{threadId}")
    @Produces(MediaType.APPLICATION_JSON)
    fun stopRun(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(
            description = "会话线程ID",
            required = true
        )
        @PathParam("threadId")
        threadId: String
    ): Result<Boolean>

    @Operation(summary = "重连到进行中的SSE流")
    @GET
    @Path("/stream/{threadId}")
    @Produces("text/event-stream")
    fun reconnectStream(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(
            description = "会话线程ID",
            required = true
        )
        @PathParam("threadId")
        threadId: String
    ): ChunkedOutput<String>
}
