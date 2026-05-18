/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲫持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲫持续集成平台 is licensed under the MIT license.
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

package com.tencent.devops.ai.api.op

import com.tencent.devops.ai.pojo.AiMcpServerCreate
import com.tencent.devops.ai.pojo.AiMcpServerInfo
import com.tencent.devops.ai.pojo.AiMcpServerUpdate
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_AI_MCP_SERVER", description = "运营-MCP服务器管理")
@Path("/op/ai/mcp/servers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpAiMcpServerResource {

    @Operation(summary = "获取MCP服务器列表")
    @GET
    @Path("/")
    fun list(): Result<List<AiMcpServerInfo>>

    @Operation(summary = "创建MCP服务器")
    @POST
    @Path("/")
    fun create(
        @Parameter(description = "创建请求", required = true)
        request: AiMcpServerCreate
    ): Result<AiMcpServerInfo>

    @Operation(summary = "更新MCP服务器")
    @PUT
    @Path("/{serverId}")
    fun update(
        @Parameter(description = "服务器ID", required = true)
        @PathParam("serverId")
        serverId: String,
        @Parameter(description = "更新请求", required = true)
        request: AiMcpServerUpdate
    ): Result<Boolean>

    @Operation(summary = "删除MCP服务器")
    @DELETE
    @Path("/{serverId}")
    fun delete(
        @Parameter(description = "服务器ID", required = true)
        @PathParam("serverId")
        serverId: String
    ): Result<Boolean>
}
