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

package com.tencent.devops.dispatch.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_AGENT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.pojo.AgentResult
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.pojo.agent.UpgradeItem
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.dispatch.pojo.thirdpartyagent.ThirdPartyAskInfo
import com.tencent.devops.dispatch.pojo.thirdpartyagent.ThirdPartyAskResp
import com.tencent.devops.dispatch.pojo.thirdpartyagent.ThirdPartyBuildInfo
import com.tencent.devops.dispatch.pojo.thirdpartyagent.ThirdPartyBuildWithStatus
import com.tencent.devops.dispatch.pojo.thirdpartyagent.ThirdPartyDockerDebugDoneInfo
import com.tencent.devops.dispatch.pojo.thirdpartyagent.ThirdPartyDockerDebugInfo
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentUpgradeByVersionInfo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "BUILD_AGENT_BUILD", description = "第三方接入agent资源")
@Path("/buildAgent/agent/thirdPartyAgent")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildAgentBuildResource {

    @Operation(summary = "尝试启动构建")
    @GET
    @Path("/startup")
    fun startBuild(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @Parameter(description = "秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String,
        @Parameter(description = "尝试启动构建的类型", required = false)
        @QueryParam("buildType")
        buildType: String?
    ): AgentResult<ThirdPartyBuildInfo?>

    @Operation(summary = "是否更新")
    @GET
    @Path("/upgrade")
    fun upgrade(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        @BkField(required = true)
        projectId: String,
        @Parameter(description = "Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @Parameter(description = "秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String,
        @Parameter(description = "Agent 版本号", required = false)
        @QueryParam("version")
        version: String?,
        @Parameter(description = "masterAgent 版本号", required = false)
        @QueryParam("masterVersion")
        masterVersion: String?
    ): AgentResult<Boolean>

    @Operation(summary = "是否更新NEW")
    @POST
    @Path("/upgradeNew")
    fun upgradeNew(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        @BkField(required = true)
        projectId: String,
        @Parameter(description = "Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @Parameter(description = "秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String,
        @Parameter(description = "检查版本升级上报的信息", required = false)
        info: ThirdPartyAgentUpgradeByVersionInfo
    ): AgentResult<UpgradeItem>

    @Operation(summary = "更新完成")
    @DELETE
    @Path("/upgrade")
    fun finishUpgrade(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @Parameter(description = "秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String,
        @Parameter(description = "升级是否成功", required = true)
        @QueryParam("success")
        success: Boolean
    ): AgentResult<Boolean>

    @Operation(summary = "worker构建结束")
    @POST
    @Path("/workerBuildFinish")
    fun workerBuildFinish(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @Parameter(description = "秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String,
        @Parameter(description = "构建信息", required = true)
        buildInfo: ThirdPartyBuildWithStatus
    ): Result<Boolean>

    @Operation(summary = "尝试启动Docker登录调试")
    @GET
    @Path("/docker/startupDebug")
    fun dockerStartDebug(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @Parameter(description = "秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String
    ): AgentResult<ThirdPartyDockerDebugInfo?>

    @Operation(summary = "启动Docker登录完成")
    @POST
    @Path("/docker/startupDebug")
    fun dockerStartDebugDone(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @Parameter(description = "秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String,
        @Parameter(description = "构建信息", required = true)
        debugInfo: ThirdPartyDockerDebugDoneInfo
    ): Result<Boolean>

    @Operation(summary = "获取登录调试任务状态")
    @GET
    @Path("/docker/debug/status")
    fun dockerDebugStatus(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @Parameter(description = "秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String,
        @Parameter(description = "debugId", required = true)
        @QueryParam("debugId")
        debugId: Long
    ): Result<String?>

    @Operation(summary = "第三方构建机请求")
    @POST
    @Path("/ask")
    fun thirdPartyAgentAsk(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @Parameter(description = "秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String,
        @Parameter(description = "ask信息", required = true)
        data: ThirdPartyAskInfo
    ): AgentResult<ThirdPartyAskResp>
}
