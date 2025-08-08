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

package com.tencent.devops.environment.api.thirdpartyagent

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_AGENT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.pojo.agent.NewHeartbeatInfo
import com.tencent.devops.environment.pojo.thirdpartyagent.HeartbeatResponse
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentHeartbeatInfo
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentPipeline
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentStartInfo
import com.tencent.devops.environment.pojo.thirdpartyagent.pipeline.PipelineResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "BUILD_ENVIRONMENT_THIRD_PARTY_AGENT", description = "第三方构建机资源")
@Path("/buildAgent/agent/thirdPartyAgent")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildAgentThirdPartyAgentResource {

    @Operation(summary = "Agent启动")
    @POST
    @Path("/startup")
    fun agentStartup(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @Parameter(description = "秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String,
        @Parameter(description = "Agent Start Info", required = true)
        startInfo: ThirdPartyAgentStartInfo
    ): Result<AgentStatus>

    @Operation(summary = "Agent关闭")
    @POST
    @Path("/shutdown")
    fun agentShutdown(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @Parameter(description = "秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String,
        @Parameter(description = "正常关闭", required = true)
        @QueryParam("shutdownNormal")
        shutdownNormal: Boolean
    ): Result<AgentStatus>

    @Operation(summary = "查询Agent状态")
    @GET
    @Path("/status")
    fun getAgentStatus(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @Parameter(description = "秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String
    ): Result<AgentStatus>

    @Operation(summary = "上报Agent心跳")
    @POST
    @Path("/agents/heartbeat")
    fun agentHeartbeat(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @Parameter(description = "秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String,
        @Parameter(description = "内容", required = false)
        heartbeatInfo: ThirdPartyAgentHeartbeatInfo?
    ): Result<AgentStatus>

    @Operation(summary = "上报Agent心跳")
    @POST
    @Path("/agents/newHeartbeat")
    fun newHeartbeat(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @Parameter(description = "秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String,
        @Parameter(description = "内容", required = false)
        heartbeatInfo: NewHeartbeatInfo
    ): Result<HeartbeatResponse>

    @Operation(summary = "查询Agent的管道")
    @GET
    @Path("agents/pipelines")
    fun getPipelines(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @Parameter(description = "秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String
    ): Result<ThirdPartyAgentPipeline?>

    @Operation(summary = "更改Agent的管道状态")
    @PUT
    @Path("agents/pipelines")
    fun updatePipelineStatus(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @Parameter(description = "秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String,
        @Parameter(description = "管道状态信息", required = true)
        response: PipelineResponse
    ): Result<Boolean>

    @Operation(summary = "接受telegraf监控数据")
    @POST
    @Path("/agents/metrics")
    fun reportAgentMetrics(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @Parameter(description = "秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String,
        @Parameter(description = "数据", required = true)
        data: String
    ): Result<Boolean>
}
