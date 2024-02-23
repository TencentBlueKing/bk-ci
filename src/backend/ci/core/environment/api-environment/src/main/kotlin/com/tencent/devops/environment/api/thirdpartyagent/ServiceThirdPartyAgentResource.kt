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

package com.tencent.devops.environment.api.thirdpartyagent

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_AGENT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.AgentResult
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.pojo.agent.NewHeartbeatInfo
import com.tencent.devops.common.api.pojo.agent.UpgradeItem
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.environment.pojo.AgentPipelineRefRequest
import com.tencent.devops.environment.pojo.slave.SlaveGateway
import com.tencent.devops.environment.pojo.thirdpartyagent.AgentBuildDetail
import com.tencent.devops.environment.pojo.thirdpartyagent.AgentPipelineRef
import com.tencent.devops.environment.pojo.thirdpartyagent.AskHeartbeatResponse
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgent
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentDetail
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentInfo
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentPipeline
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentUpgradeByVersionInfo
import com.tencent.devops.environment.pojo.thirdpartyagent.pipeline.PipelineCreate
import com.tencent.devops.environment.pojo.thirdpartyagent.pipeline.PipelineResponse
import com.tencent.devops.environment.pojo.thirdpartyagent.pipeline.PipelineSeqId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "SERVICE_ENVIRONMENT_THIRD_PARTY_AGENT", description = "第三方构建机资源")
@Path("/service/environment/thirdPartyAgent")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceThirdPartyAgentResource {

    @Operation(summary = "根据ID获取Agent信息")
    @GET
    @Path("/projects/{projectId}/agents/{agentId}")
    fun getAgentById(
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "Agent Hash ID", required = true)
        @PathParam("agentId")
        agentId: String
    ): AgentResult<ThirdPartyAgent?>

    @Operation(summary = "根据ID获取Agent信息,全局")
    @GET
    @Path("/projects/{projectId}/agents/{agentId}/global")
    @Deprecated("getAgentById")
    fun getAgentByIdGlobal(
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "Agent Hash ID", required = true)
        @PathParam("agentId")
        agentId: String
    ): AgentResult<ThirdPartyAgent?>

    @Operation(summary = "根据环境名称获取Agent信息")
    @GET
    @Path("/projects/{projectId}/displayNames")
    fun getAgentByDisplayName(
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "Display Name", required = true)
        @QueryParam("displayName")
        displayName: String
    ): AgentResult<ThirdPartyAgent?>

    @Operation(summary = "根据环境ID获取Agent列表")
    @GET
    @Path("/projects/{projectId}/envs/{envId}")
    fun getAgentsByEnvId(
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "Environment Hash ID", required = true)
        @PathParam("envId")
        envId: String
    ): Result<List<ThirdPartyAgent>>

    @Operation(summary = "根据环境名称获取Agent列表")
    @GET
    @Path("/projects/{projectId}/envNames/{envName}")
    fun getAgentsByEnvName(
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "Environment name", required = true)
        @PathParam("envName")
        envName: String
    ): Result<List<ThirdPartyAgent>>

    @Operation(summary = "Agent是否能升级")
    @GET
    @Path("/projects/{projectId}/agents/{agentId}/upgrade")
    fun upgrade(
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "Agent Hash ID", required = true)
        @PathParam("agentId")
        agentId: String,
        @Parameter(name = "Agent secret key", required = true)
        @QueryParam("secretKey")
        secretKey: String,
        @Parameter(name = "agent.jar的MD5", required = true)
        @QueryParam("tag")
        tag: String
    ): AgentResult<Boolean>

    @Operation(summary = "Agent是否能升级")
    @GET
    @Path("/projects/{projectId}/agents/{agentId}/upgradeByVersion")
    fun upgradeByVersion(
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "Agent Hash ID", required = true)
        @PathParam("agentId")
        agentId: String,
        @Parameter(name = "Agent secret key", required = true)
        @QueryParam("secretKey")
        secretKey: String,
        @Parameter(name = "agent版本号", required = false)
        @QueryParam("version")
        version: String?,
        @Parameter(name = "masterAgent版本号", required = false)
        @QueryParam("masterVersion")
        masterVersion: String?
    ): AgentResult<Boolean>

    @Operation(summary = "Agent是否能升级new")
    @POST
    @Path("/projects/{projectId}/agents/{agentId}/upgradeByVersionNew")
    fun upgradeByVersionNew(
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "Agent Hash ID", required = true)
        @PathParam("agentId")
        agentId: String,
        @Parameter(name = "Agent secret key", required = true)
        @QueryParam("secretKey")
        secretKey: String,
        @Parameter(name = "检查版本升级上报的信息", required = false)
        info: ThirdPartyAgentUpgradeByVersionInfo
    ): AgentResult<UpgradeItem>

    @Operation(summary = "执行第三方构建机管道")
    @POST
    @Path("/projects/{projectId}/agents/{nodeId}/pipelines")
    fun scheduleAgentPipeline(
        @Parameter(name = "user id", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "node id", required = true)
        @PathParam("nodeId")
        nodeId: String,
        @Parameter(name = "pipeline", required = true)
        pipeline: PipelineCreate
    ): Result<PipelineSeqId>

    @Operation(summary = "获取第三方构建机管道结果")
    @GET
    @Path("/projects/{projectId}/agents/{nodeId}/pipelines")
    fun getAgentPipelineResponse(
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "node id", required = true)
        @PathParam("nodeId")
        nodeId: String,
        @Parameter(name = "seqId", required = true)
        @QueryParam("seqId")
        seqId: String
    ): Result<PipelineResponse>

    @Operation(summary = "查看所有的Agent")
    @GET
    @Path("/projects/{projectId}/os/{os}/list")
    fun listAgents(
        @Parameter(name = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        @BkField(minLength = 1, maxLength = 128)
        userId: String,
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        @BkField(minLength = 1, maxLength = 128)
        projectId: String,
        @Parameter(name = "操作系统", required = true)
        @PathParam("os")
        os: OS
    ): Result<List<ThirdPartyAgentInfo>>

    @Operation(summary = "构建任务已认领")
    @POST
    @Path("/projects/{projectId}/agents/{agentId}/taskStarted")
    fun agentTaskStarted(
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "pipeline ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @Parameter(name = "build ID", required = true)
        @QueryParam("buildId")
        buildId: String,
        @Parameter(name = "VM SEQ ID", required = true)
        @QueryParam("vmSeqId")
        vmSeqId: String,
        @Parameter(name = "agent Hash ID", required = true)
        @PathParam("agentId")
        agentId: String
    ): Result<Boolean>

    @Operation(summary = "获取构建机流水线引用信息")
    @GET
    @Path("/projects/{projectId}/agents/{nodeHashId}/listPipelineRef")
    fun listPipelineRef(
        @Parameter(name = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        @BkField(minLength = 1, maxLength = 128)
        userId: String,
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        @BkField(minLength = 1, maxLength = 128)
        projectId: String,
        @Parameter(name = "Node Hash ID", required = true)
        @PathParam("nodeHashId")
        @BkField(minLength = 1, maxLength = 128)
        nodeHashId: String,
        @Parameter(name = "排序字段, pipelineName|lastBuildTime", required = true)
        @QueryParam("sortBy")
        sortBy: String? = null,
        @Parameter(name = "排序方向, ASC|DESC", required = true)
        @QueryParam("sortDirection")
        sortDirection: String? = null
    ): Result<List<AgentPipelineRef>>

    @Operation(summary = "更新构建机流水线引用信息")
    @POST
    @Path("/projects/{projectId}/updatePipelineRef")
    fun updatePipelineRef(
        @Parameter(name = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "流水线引用信息", required = true)
        request: AgentPipelineRefRequest
    ): Result<Boolean>

    @Operation(summary = "获取构建机详情")
    @GET
    @Path("/projects/{projectId}/agents/{agentId}/detail")
    fun getAgentDetail(
        @Parameter(name = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        @BkField(minLength = 1, maxLength = 128)
        userId: String,
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        @BkField(minLength = 1, maxLength = 128)
        projectId: String,
        @Parameter(name = "Node Hash ID/Agent Id", required = true)
        @PathParam("agentId")
        @BkField(minLength = 3, maxLength = 32)
        agentHashId: String
    ): Result<ThirdPartyAgentDetail?>

    @Operation(summary = "获取Gateway列表")
    @GET
    @Path("/gateways")
    fun getGateways(): Result<List<SlaveGateway>>

    @Operation(summary = "获取构建机详情(by node id)")
    @GET
    @Path("/projects/{projectId}/agent_detail_by_node_id")
    fun getNodeDetail(
        @Parameter(name = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        @BkField(minLength = 1, maxLength = 128)
        userId: String,
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        @BkField(minLength = 1, maxLength = 128)
        projectId: String,
        @Parameter(name = "Node Hash ID", required = false)
        @QueryParam("nodeHashId")
        nodeHashId: String?,
        @Parameter(name = "Node 别名", required = false)
        @QueryParam("nodeName")
        nodeName: String?
    ): Result<ThirdPartyAgentDetail?>

    @Operation(summary = "获取第三方构建机任务")
    @GET
    @Path("/projects/{projectId}/listAgentBuilds")
    fun listAgentBuilds(
        @Parameter(name = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "Node Hash ID", required = false)
        @QueryParam("nodeHashId")
        nodeHashId: String?,
        @Parameter(name = "Node 别名", required = false)
        @QueryParam("nodeName")
        nodeName: String?,
        @Parameter(name = "agent Hash ID", required = false)
        @QueryParam("agentHashId")
        agentHashId: String?,
        @Parameter(name = "筛选此状态，支持4种输入(QUEUE,RUNNING,DONE,FAILURE)", required = false)
        @QueryParam("status")
        status: String?,
        @Parameter(name = "筛选此pipelineId", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(name = "第几页", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(name = "每页条数", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<AgentBuildDetail>>

    @Operation(summary = "上报Agent心跳")
    @POST
    @Path("/agents/newHeartbeat")
    fun newHeartbeat(
        @Parameter(name = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(name = "Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @Parameter(name = "秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String,
        @Parameter(name = "内容", required = false)
        heartbeatInfo: NewHeartbeatInfo
    ): Result<AskHeartbeatResponse>

    @Operation(summary = "查询Agent的管道")
    @GET
    @Path("/agents/pipelines")
    fun getPipelines(
        @Parameter(name = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(name = "Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @Parameter(name = "秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String
    ): Result<ThirdPartyAgentPipeline?>

    @Operation(summary = "根据环境名称获取Agent列表,并返回环境ID")
    @GET
    @Path("/projects/{projectId}/envNames/{envName}/withId")
    fun getAgentsByEnvNameWithId(
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "Environment name", required = true)
        @PathParam("envName")
        envName: String
    ): Result<Pair<Long?, List<ThirdPartyAgent>>>
}
