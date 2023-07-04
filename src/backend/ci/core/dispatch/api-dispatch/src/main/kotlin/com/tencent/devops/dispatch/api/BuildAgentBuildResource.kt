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

package com.tencent.devops.dispatch.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_AGENT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.pojo.AgentResult
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.pojo.agent.UpgradeItem
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.dispatch.pojo.thirdPartyAgent.ThirdPartyBuildInfo
import com.tencent.devops.dispatch.pojo.thirdPartyAgent.ThirdPartyBuildWithStatus
import com.tencent.devops.dispatch.pojo.thirdPartyAgent.ThirdPartyDockerDebugDoneInfo
import com.tencent.devops.dispatch.pojo.thirdPartyAgent.ThirdPartyDockerDebugInfo
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentUpgradeByVersionInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_AGENT_BUILD"], description = "第三方接入agent资源")
@Path("/buildAgent/agent/thirdPartyAgent")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildAgentBuildResource {

    @ApiOperation("尝试启动构建")
    @GET
    @Path("/startup")
    fun startBuild(
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @ApiParam("Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @ApiParam("秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String,
        @ApiParam("尝试启动构建的类型", required = false)
        @QueryParam("buildType")
        buildType: String?
    ): AgentResult<ThirdPartyBuildInfo?>

    @ApiOperation("是否更新")
    @GET
    @Path("/upgrade")
    fun upgrade(
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        @BkField(required = true)
        projectId: String,
        @ApiParam("Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @ApiParam("秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String,
        @ApiParam("Agent 版本号", required = false)
        @QueryParam("version")
        version: String?,
        @ApiParam("masterAgent 版本号", required = false)
        @QueryParam("masterVersion")
        masterVersion: String?
    ): AgentResult<Boolean>

    @ApiOperation("是否更新NEW")
    @POST
    @Path("/upgradeNew")
    fun upgradeNew(
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        @BkField(required = true)
        projectId: String,
        @ApiParam("Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @ApiParam("秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String,
        @ApiParam("检查版本升级上报的信息", required = false)
        info: ThirdPartyAgentUpgradeByVersionInfo
    ): AgentResult<UpgradeItem>

    @ApiOperation("更新完成")
    @DELETE
    @Path("/upgrade")
    fun finishUpgrade(
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @ApiParam("Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @ApiParam("秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String,
        @ApiParam("升级是否成功", required = true)
        @QueryParam("success")
        success: Boolean
    ): AgentResult<Boolean>

    @ApiOperation("worker构建结束")
    @POST
    @Path("/workerBuildFinish")
    fun workerBuildFinish(
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @ApiParam("Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @ApiParam("秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String,
        @ApiParam("构建信息", required = true)
        buildInfo: ThirdPartyBuildWithStatus
    ): Result<Boolean>

    @ApiOperation("尝试启动Docker登录调试")
    @GET
    @Path("/docker/startupDebug")
    fun dockerStartDebug(
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @ApiParam("Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @ApiParam("秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String
    ): AgentResult<ThirdPartyDockerDebugInfo?>

    @ApiOperation("启动Docker登录完成")
    @POST
    @Path("/docker/startupDebug")
    fun dockerStartDebugDone(
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @ApiParam("Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @ApiParam("秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String,
        @ApiParam("构建信息", required = true)
        debugInfo: ThirdPartyDockerDebugDoneInfo
    ): Result<Boolean>

    @ApiOperation("获取登录调试任务状态")
    @GET
    @Path("/docker/debug/status")
    fun dockerDebugStatus(
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @ApiParam("Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @ApiParam("秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String,
        @ApiParam("debugId", required = true)
        @QueryParam("debugId")
        debugId: Long
    ): Result<String?>
}
