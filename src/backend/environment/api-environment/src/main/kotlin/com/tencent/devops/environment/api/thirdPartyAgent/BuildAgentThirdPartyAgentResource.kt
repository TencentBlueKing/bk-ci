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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.environment.api.thirdPartyAgent

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_AGENT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.thirdPartyAgent.HeartbeatInfo
import com.tencent.devops.environment.pojo.thirdPartyAgent.HeartbeatResponse
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentPipeline
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentStartInfo
import com.tencent.devops.environment.pojo.thirdPartyAgent.pipeline.PipelineResponse
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_ENVIRONMENT_THIRD_PARTY_AGENT"], description = "第三方构建机资源")
@Path("/buildAgent/agent/thirdPartyAgent")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildAgentThirdPartyAgentResource {

    @ApiOperation("Agent启动")
    @POST
    @Path("/startup")
    fun agentStartup(
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @ApiParam("Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @ApiParam("秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String,
        @ApiParam("Agent Start Info", required = true)
        startInfo: ThirdPartyAgentStartInfo
    ): Result<AgentStatus>

    @ApiOperation("查询Agent状态")
    @GET
    @Path("/status")
    fun getAgentStatus(
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @ApiParam("Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @ApiParam("秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String
    ): Result<AgentStatus>

    @ApiOperation("上报Agent心跳")
    @POST
    @Path("/agents/heartbeat")
    fun heartbeat(
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @ApiParam("Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @ApiParam("秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String,
        @ApiParam("内容", required = false)
        heartbeatInfo: HeartbeatInfo
    ): Result<HeartbeatResponse>

    @ApiOperation("查询Agent的管道")
    @GET
    @Path("agents/pipelines")
    fun getPipelines(
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @ApiParam("Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @ApiParam("秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String
    ): Result<ThirdPartyAgentPipeline?>

    @ApiOperation("更改Agent的管道状态")
    @PUT
    @Path("agents/pipelines")
    fun updatePipelineStatus(
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @ApiParam("Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @ApiParam("秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String,
        @ApiParam("管道状态信息", required = true)
        response: PipelineResponse
    ): Result<Boolean>
}