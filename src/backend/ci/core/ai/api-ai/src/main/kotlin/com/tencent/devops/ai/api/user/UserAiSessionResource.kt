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

import com.tencent.devops.ai.pojo.AiSessionCreate
import com.tencent.devops.ai.pojo.AiSessionInfo
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
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

@Tag(name = "USER_AI_SESSION", description = "用户-AI会话管理")
@Path("/user/ai/sessions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserAiSessionResource {

    @Operation(summary = "创建会话")
    @POST
    @Path("/")
    fun create(
        @Parameter(
            description = "用户ID",
            required = true,
            example = AUTH_HEADER_USER_ID_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "创建会话请求体", required = true)
        sessionCreate: AiSessionCreate
    ): Result<AiSessionInfo>

    @Operation(summary = "获取会话列表")
    @GET
    @Path("/")
    fun list(
        @Parameter(
            description = "用户ID",
            required = true,
            example = AUTH_HEADER_USER_ID_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID，空查公共会话", required = false)
        @QueryParam("projectId")
        projectId: String?
    ): Result<List<AiSessionInfo>>

    @Operation(summary = "获取最新会话")
    @GET
    @Path("/latest")
    fun getLatest(
        @Parameter(
            description = "用户ID",
            required = true,
            example = AUTH_HEADER_USER_ID_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID，空查公共会话", required = false)
        @QueryParam("projectId")
        projectId: String?
    ): Result<AiSessionInfo?>

    @Operation(summary = "更新会话标题")
    @PUT
    @Path("/{sessionId}/title")
    fun updateTitle(
        @Parameter(
            description = "用户ID",
            required = true,
            example = AUTH_HEADER_USER_ID_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "会话ID", required = true)
        @PathParam("sessionId")
        sessionId: String,
        @Parameter(description = "新标题", required = true)
        @QueryParam("title")
        title: String
    ): Result<Boolean>

    @Operation(summary = "删除会话")
    @DELETE
    @Path("/{sessionId}")
    fun delete(
        @Parameter(
            description = "用户ID",
            required = true,
            example = AUTH_HEADER_USER_ID_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "会话ID", required = true)
        @PathParam("sessionId")
        sessionId: String
    ): Result<Boolean>
}
