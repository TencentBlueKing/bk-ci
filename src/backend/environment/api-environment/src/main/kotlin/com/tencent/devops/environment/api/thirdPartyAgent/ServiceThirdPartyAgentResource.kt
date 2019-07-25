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

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.AgentResult
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgent
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentInfo
import com.tencent.devops.environment.pojo.thirdPartyAgent.pipeline.PipelineCreate
import com.tencent.devops.environment.pojo.thirdPartyAgent.pipeline.PipelineResponse
import com.tencent.devops.environment.pojo.thirdPartyAgent.pipeline.PipelineSeqId
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_ENVIRONMENT_THIRD_PARTY_AGENT"], description = "第三方构建机资源")
@Path("/service/environment/thirdPartyAgent")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceThirdPartyAgentResource {

    @ApiOperation("根据ID获取Agent信息")
    @GET
    @Path("/projects/{projectId}/agents/{agentId}")
    fun getAgentById(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("Agent Hash ID", required = true)
        @PathParam("agentId")
        agentId: String
    ): AgentResult<ThirdPartyAgent?>

    @ApiOperation("根据环境名称获取Agent信息")
    @GET
    @Path("/projects/{projectId}/displayNames")
    fun getAgentByDisplayName(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("Display Name", required = true)
        @QueryParam("displayName")
        displayName: String
    ): AgentResult<ThirdPartyAgent?>

    @ApiOperation("根据环境ID获取Agent列表")
    @GET
    @Path("/projects/{projectId}/envs/{envId}")
    fun getAgentsByEnvId(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("Environment Hash ID", required = true)
        @PathParam("envId")
        envId: String
    ): Result<List<ThirdPartyAgent>>

    @ApiOperation("根据环境名称获取Agent列表")
    @GET
    @Path("/projects/{projectId}/envNames/{envName}")
    fun getAgentsByEnvName(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("Environment name", required = true)
        @PathParam("envName")
        envName: String
    ): Result<List<ThirdPartyAgent>>

    @ApiOperation("Agent是否能升级")
    @GET
    @Path("/projects/{projectId}/agents/{agentId}/upgradeByVersion")
    fun upgradeByVersion(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("Agent Hash ID", required = true)
        @PathParam("agentId")
        agentId: String,
        @ApiParam("Agent secret key", required = true)
        @QueryParam("secretKey")
        secretKey: String,
        @ApiParam("agent版本号", required = false)
        @QueryParam("version")
        version: String?,
        @ApiParam("masterAgent版本号", required = false)
        @QueryParam("masterVersion")
        masterVersion: String?
    ): AgentResult<Boolean>

    @ApiOperation("执行第三方构建机管道")
    @POST
    @Path("/agents/{nodeId}/pipelines")
    fun scheduleAgentPipeline(
        @ApiParam("user id", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @HeaderParam("projectId")
        projectId: String,
        @ApiParam("node id", required = true)
        @PathParam("nodeId")
        nodeId: String,
        @ApiParam("pipeline", required = true)
        pipeline: PipelineCreate
    ): Result<PipelineSeqId>

    @ApiOperation("获取第三方构建机管道结果")
    @GET
    @Path("/agents/{nodeId}/pipelines")
    fun getAgentPipelineResponse(
        @ApiParam("项目ID", required = true)
        @HeaderParam("projectId")
        projectId: String,
        @ApiParam("node id", required = true)
        @PathParam("nodeId")
        nodeId: String,
        @ApiParam("seqId", required = true)
        @QueryParam("seqId")
        seqId: String
    ): Result<PipelineResponse>

    @ApiOperation("查看所有的Agent")
    @GET
    @Path("/projects/{projectId}/os/{os}/list")
    fun listAgents(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("操作系统", required = true)
        @PathParam("os")
        os: OS
    ): Result<List<ThirdPartyAgentInfo>>
}