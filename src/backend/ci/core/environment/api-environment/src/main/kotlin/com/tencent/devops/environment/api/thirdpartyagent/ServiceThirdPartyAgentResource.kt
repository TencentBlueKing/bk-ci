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
import com.tencent.devops.environment.pojo.EnvVar
import com.tencent.devops.environment.pojo.slave.SlaveGateway
import com.tencent.devops.environment.pojo.thirdpartyagent.AgentBuildDetail
import com.tencent.devops.environment.pojo.thirdpartyagent.AgentPipelineRef
import com.tencent.devops.environment.pojo.thirdpartyagent.AskHeartbeatResponse
import com.tencent.devops.environment.pojo.thirdpartyagent.BatchFetchAgentData
import com.tencent.devops.environment.pojo.thirdpartyagent.BatchUpdateAgentEnvVar
import com.tencent.devops.environment.pojo.thirdpartyagent.EnvNodeAgent
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
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_ENVIRONMENT_THIRD_PARTY_AGENT", description = "第三方构建机资源")
@Path("/service/environment/thirdPartyAgent")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceThirdPartyAgentResource {

    @Operation(summary = "根据ID获取Agent信息")
    @GET
    @Path("/projects/{projectId}/agents/{agentId}")
    fun getAgentById(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "Agent Hash ID", required = true)
        @PathParam("agentId")
        agentId: String
    ): AgentResult<ThirdPartyAgent?>

    @Operation(summary = "根据ID获取Agent信息,全局")
    @GET
    @Path("/projects/{projectId}/agents/{agentId}/global")
    @Deprecated("getAgentById")
    fun getAgentByIdGlobal(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "Agent Hash ID", required = true)
        @PathParam("agentId")
        agentId: String
    ): AgentResult<ThirdPartyAgent?>

    @Operation(summary = "根据环境名称获取Agent信息")
    @GET
    @Path("/projects/{projectId}/displayNames")
    fun getAgentByDisplayName(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "Display Name", required = true)
        @QueryParam("displayName")
        displayName: String
    ): AgentResult<ThirdPartyAgent?>

    @Operation(summary = "根据环境ID获取Agent列表")
    @GET
    @Path("/projects/{projectId}/envs/{envId}")
    fun getAgentsByEnvId(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "Environment Hash ID", required = true)
        @PathParam("envId")
        envId: String
    ): Result<List<EnvNodeAgent>>

    @Operation(summary = "根据环境名称获取Agent列表")
    @GET
    @Path("/projects/{projectId}/envNames/{envName}")
    fun getAgentsByEnvName(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "Environment name", required = true)
        @PathParam("envName")
        envName: String
    ): Result<List<EnvNodeAgent>>

    @Operation(summary = "Agent是否能升级")
    @GET
    @Path("/projects/{projectId}/agents/{agentId}/upgrade")
    fun upgrade(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "Agent Hash ID", required = true)
        @PathParam("agentId")
        agentId: String,
        @Parameter(description = "Agent secret key", required = true)
        @QueryParam("secretKey")
        secretKey: String,
        @Parameter(description = "agent.jar的MD5", required = true)
        @QueryParam("tag")
        tag: String
    ): AgentResult<Boolean>

    @Operation(summary = "Agent是否能升级")
    @GET
    @Path("/projects/{projectId}/agents/{agentId}/upgradeByVersion")
    fun upgradeByVersion(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "Agent Hash ID", required = true)
        @PathParam("agentId")
        agentId: String,
        @Parameter(description = "Agent secret key", required = true)
        @QueryParam("secretKey")
        secretKey: String,
        @Parameter(description = "agent版本号", required = false)
        @QueryParam("version")
        version: String?,
        @Parameter(description = "masterAgent版本号", required = false)
        @QueryParam("masterVersion")
        masterVersion: String?
    ): AgentResult<Boolean>

    @Operation(summary = "Agent是否能升级new")
    @POST
    @Path("/projects/{projectId}/agents/{agentId}/upgradeByVersionNew")
    fun upgradeByVersionNew(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "Agent Hash ID", required = true)
        @PathParam("agentId")
        agentId: String,
        @Parameter(description = "Agent secret key", required = true)
        @QueryParam("secretKey")
        secretKey: String,
        @Parameter(description = "检查版本升级上报的信息", required = false)
        info: ThirdPartyAgentUpgradeByVersionInfo
    ): AgentResult<UpgradeItem>

    @Operation(summary = "执行第三方构建机管道")
    @POST
    @Path("/projects/{projectId}/agents/{nodeId}/pipelines")
    fun scheduleAgentPipeline(
        @Parameter(description = "user id", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "node id", required = true)
        @PathParam("nodeId")
        nodeId: String,
        @Parameter(description = "pipeline", required = true)
        pipeline: PipelineCreate
    ): Result<PipelineSeqId>

    @Operation(summary = "获取第三方构建机管道结果")
    @GET
    @Path("/projects/{projectId}/agents/{nodeId}/pipelines")
    fun getAgentPipelineResponse(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "node id", required = true)
        @PathParam("nodeId")
        nodeId: String,
        @Parameter(description = "seqId", required = true)
        @QueryParam("seqId")
        seqId: String
    ): Result<PipelineResponse>

    @Operation(summary = "查看所有的Agent")
    @GET
    @Path("/projects/{projectId}/os/{os}/list")
    fun listAgents(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        @BkField(minLength = 1, maxLength = 128)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        @BkField(minLength = 1, maxLength = 128)
        projectId: String,
        @Parameter(description = "操作系统", required = true)
        @PathParam("os")
        os: OS
    ): Result<List<ThirdPartyAgentInfo>>

    @Operation(summary = "构建任务已认领")
    @POST
    @Path("/projects/{projectId}/agents/{agentId}/taskStarted")
    fun agentTaskStarted(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "pipeline ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "build ID", required = true)
        @QueryParam("buildId")
        buildId: String,
        @Parameter(description = "VM SEQ ID", required = true)
        @QueryParam("vmSeqId")
        vmSeqId: String,
        @Parameter(description = "agent Hash ID", required = true)
        @PathParam("agentId")
        agentId: String
    ): Result<Boolean>

    @Operation(summary = "获取构建机流水线引用信息")
    @GET
    @Path("/projects/{projectId}/agents/{nodeHashId}/listPipelineRef")
    fun listPipelineRef(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        @BkField(minLength = 1, maxLength = 128)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        @BkField(minLength = 1, maxLength = 128)
        projectId: String,
        @Parameter(description = "Node Hash ID", required = true)
        @PathParam("nodeHashId")
        @BkField(minLength = 1, maxLength = 128)
        nodeHashId: String,
        @Parameter(description = "排序字段, pipelineName|lastBuildTime", required = true)
        @QueryParam("sortBy")
        sortBy: String? = null,
        @Parameter(description = "排序方向, ASC|DESC", required = true)
        @QueryParam("sortDirection")
        sortDirection: String? = null
    ): Result<List<AgentPipelineRef>>

    @Operation(summary = "更新构建机流水线引用信息")
    @POST
    @Path("/projects/{projectId}/updatePipelineRef")
    fun updatePipelineRef(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线引用信息", required = true)
        request: AgentPipelineRefRequest
    ): Result<Boolean>

    @Operation(summary = "获取构建机详情")
    @GET
    @Path("/projects/{projectId}/agents/{agentId}/detail")
    fun getAgentDetail(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        @BkField(minLength = 1, maxLength = 128)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        @BkField(minLength = 1, maxLength = 128)
        projectId: String,
        @Parameter(description = "Node Hash ID/Agent Id", required = true)
        @PathParam("agentId")
        @BkField(minLength = 3, maxLength = 32)
        agentHashId: String,
        @Parameter(description = "是否校验权限", required = false)
        @QueryParam("checkPermission")
        @DefaultValue("false")
        checkPermission: Boolean? = false
    ): Result<ThirdPartyAgentDetail?>

    @Operation(summary = "获取Gateway列表")
    @GET
    @Path("/gateways")
    fun getGateways(): Result<List<SlaveGateway>>

    @Operation(summary = "获取构建机详情(by node id)")
    @GET
    @Path("/projects/{projectId}/agent_detail_by_node_id")
    fun getNodeDetail(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        @BkField(minLength = 1, maxLength = 128)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        @BkField(minLength = 1, maxLength = 128)
        projectId: String,
        @Parameter(description = "Node Hash ID", required = false)
        @QueryParam("nodeHashId")
        nodeHashId: String?,
        @Parameter(description = "Node 别名", required = false)
        @QueryParam("nodeName")
        nodeName: String?
    ): Result<ThirdPartyAgentDetail?>

    @Operation(summary = "获取第三方构建机任务")
    @GET
    @Path("/projects/{projectId}/listAgentBuilds")
    fun listAgentBuilds(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "Node Hash ID", required = false)
        @QueryParam("nodeHashId")
        nodeHashId: String?,
        @Parameter(description = "Node 别名", required = false)
        @QueryParam("nodeName")
        nodeName: String?,
        @Parameter(description = "agent Hash ID", required = false)
        @QueryParam("agentHashId")
        agentHashId: String?,
        @Parameter(description = "筛选此状态，支持4种输入(QUEUE,RUNNING,DONE,FAILURE)", required = false)
        @QueryParam("status")
        status: String?,
        @Parameter(description = "筛选此pipelineId", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(description = "第几页", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页条数", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<AgentBuildDetail>>

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
    ): Result<AskHeartbeatResponse>

    @Operation(summary = "查询Agent的管道")
    @GET
    @Path("/agents/pipelines")
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

    @Operation(summary = "根据环境名称获取Agent列表,并返回环境ID")
    @GET
    @Path("/projects/{projectId}/envNames/{envName}/withId")
    fun getAgentsByEnvNameWithId(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "Environment name", required = true)
        @PathParam("envName")
        envName: String
    ): Result<Pair<Long?, List<EnvNodeAgent>>>

    @Operation(summary = "批量查询Agent环境变量")
    @POST
    @Path("/projects/{projectId}/env")
    fun fetchAgentEnv(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "查询数据", required = true)
        data: BatchFetchAgentData
    ): Result<Map<String, List<EnvVar>>>

    @Operation(summary = "批量修改Agent环境变量")
    @POST
    @Path("/projects/{projectId}/batch_update_env")
    fun batchUpdateEnv(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "修改数据", required = true)
        data: BatchUpdateAgentEnvVar
    ): Result<Boolean>
}
