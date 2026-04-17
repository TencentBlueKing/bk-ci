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

import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_AI_AGENT_SYS_PROMPT", description = "运营-智能体系统提示词管理")
@Path("/op/ai/agent/sys-prompts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpAiAgentSysPromptResource {

    @Operation(summary = "获取智能体系统提示词列表")
    @GET
    @Path("/")
    fun list(): Result<Map<String, String>>

    @Operation(summary = "设置智能体系统提示词")
    @PUT
    @Path("/{agentName}")
    fun update(
        @Parameter(description = "智能体名称", required = true)
        @PathParam("agentName")
        agentName: String,
        @Parameter(description = "提示词模板", required = true)
        promptTemplate: String
    ): Result<Boolean>

    @Operation(summary = "删除智能体系统提示词")
    @DELETE
    @Path("/{agentName}")
    fun delete(
        @Parameter(description = "智能体名称", required = true)
        @PathParam("agentName")
        agentName: String
    ): Result<Boolean>
}
