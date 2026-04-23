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

import com.tencent.devops.ai.pojo.AiMcpServerCreate
import com.tencent.devops.ai.pojo.AiMcpServerInfo
import com.tencent.devops.ai.pojo.AiMcpServerUpdate
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
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_AI_MCP_SERVER", description = "用户-AI MCP服务器配置")
@Path("/user/ai/mcp/servers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserAiMcpServerResource {

    @Operation(summary = "添加个人MCP服务器配置")
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
        @Parameter(description = "创建MCP服务器配置请求体", required = true)
        request: AiMcpServerCreate
    ): Result<AiMcpServerInfo>

    @Operation(summary = "获取可用MCP列表（系统级+个人级合并）")
    @GET
    @Path("/")
    fun list(
        @Parameter(
            description = "用户ID",
            required = true,
            example = AUTH_HEADER_USER_ID_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<AiMcpServerInfo>>

    @Operation(summary = "更新个人MCP服务器配置")
    @PUT
    @Path("/{serverId}")
    fun update(
        @Parameter(
            description = "用户ID",
            required = true,
            example = AUTH_HEADER_USER_ID_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "配置ID", required = true)
        @PathParam("serverId")
        serverId: String,
        @Parameter(description = "更新MCP服务器配置请求体", required = true)
        request: AiMcpServerUpdate
    ): Result<Boolean>

    @Operation(summary = "删除个人MCP服务器配置")
    @DELETE
    @Path("/{serverId}")
    fun delete(
        @Parameter(
            description = "用户ID",
            required = true,
            example = AUTH_HEADER_USER_ID_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "配置ID", required = true)
        @PathParam("serverId")
        serverId: String
    ): Result<Boolean>
}
