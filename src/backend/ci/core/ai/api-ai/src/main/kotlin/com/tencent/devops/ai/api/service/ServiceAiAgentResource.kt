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

package com.tencent.devops.ai.api.service

import com.tencent.devops.ai.pojo.AgentInfo
import com.tencent.devops.ai.pojo.ServiceAgentRunRequest
import com.tencent.devops.ai.pojo.ServiceAgentRunResponse
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
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

@Tag(
    name = "SERVICE_AI_AGENT",
    description = "服务间调用-AI智能体"
)
@Path("/service/ai/agent")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceAiAgentResource {

    @GET
    @Path("/list")
    @Operation(summary = "获取可用智能体列表")
    fun listAgents(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String
    ): Result<List<AgentInfo>>

    @POST
    @Path("/{agentName}/run")
    @Operation(
        summary = "同步调用指定智能体",
        description = "以同步方式调用指定名称的子智能体，" +
            "等待智能体执行完成后返回文本结果。" +
            "适用于服务间调用场景。"
    )
    fun runAgent(
        @HeaderParam(AUTH_HEADER_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        @PathParam("agentName")
        @Parameter(
            description = "智能体名称（toolName）",
            required = true
        )
        agentName: String,
        @Parameter(
            description = "调用请求体",
            required = true
        )
        request: ServiceAgentRunRequest
    ): Result<ServiceAgentRunResponse>
}
