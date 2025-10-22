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

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.slave.SlaveGateway
import com.tencent.devops.environment.pojo.thirdpartyagent.AgentShared
import com.tencent.devops.environment.pojo.thirdpartyagent.UpdateAgentRequest
import com.tencent.devops.environment.pojo.thirdpartyagent.pipeline.PipelineCreate
import com.tencent.devops.environment.pojo.thirdpartyagent.pipeline.PipelineResponse
import com.tencent.devops.environment.pojo.thirdpartyagent.pipeline.PipelineSeqId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_ENVIRONMENT_THIRD_PARTY_AGENT", description = "第三方构建机资源")
@Path("/op/thirdPartyAgent")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpThirdPartyAgentResource {

    @Operation(summary = "启动或者禁止第三方构建机接入")
    @PUT
    @Path("/projects/enable")
    fun enableProject(
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "启动或禁止", required = true)
        @QueryParam("enable")
        enable: Boolean
    ): Result<Boolean>

    @Operation(summary = "获取所有启动第三方构建机")
    @GET
    @Path("/projects")
    fun listEnableProjects(): Result<List<String>>

    @Operation(summary = "执行第三方构建机管道")
    @POST
    @Path("/agents/{nodeId}/pipelines")
    fun scheduleAgentPipeline(
        @Parameter(description = "user id", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @HeaderParam("projectId")
        projectId: String,
        @Parameter(description = "node id", required = true)
        @PathParam("nodeId")
        nodeId: String,
        @Parameter(description = "pipeline", required = true)
        pipeline: PipelineCreate
    ): Result<PipelineSeqId>

    @Operation(summary = "获取第三方构建机管道结果")
    @GET
    @Path("/agents/{nodeId}/pipelines")
    fun getAgentPipelineResponse(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam("projectId")
        projectId: String,
        @Parameter(description = "node id", required = true)
        @PathParam("nodeId")
        nodeId: String,
        @Parameter(description = "seqId", required = true)
        @QueryParam("seqId")
        seqId: String
    ): Result<PipelineResponse>

    @Operation(summary = "设置Agent网关")
    @POST
    @Path("/agents/updateAgentGateway")
    fun updateAgentGateway(
        @Parameter(description = "内容", required = false)
        updateAgentRequest: UpdateAgentRequest
    ): Result<Boolean>

    @Operation(summary = "查询agent下载网关")
    @GET
    @Path("/gateways")
    fun getGateways(): Result<List<SlaveGateway>>

    @Operation(summary = "新增agent下载网关")
    @POST
    @Path("/gateways")
    fun addGateway(
        @Parameter(description = "gateway", required = true)
        gateway: SlaveGateway
    ): Result<Boolean>

    @Operation(summary = "修改agent下载网关")
    @PUT
    @Path("/gateways")
    fun updateGateway(
        @Parameter(description = "gateway", required = true)
        gateway: SlaveGateway
    ): Result<Boolean>

    @Operation(summary = "删除agent下载网关")
    @DELETE
    @Path("/gateways/{zoneName}")
    fun deleteGateway(
        @Parameter(description = "zoneName", required = true)
        @PathParam("zoneName")
        zoneName: String
    ): Result<Boolean>

    @Operation(summary = "新增 agent 分享给项目")
    @POST
    @Path("/shared")
    fun addAgentShared(
        shares: AgentShared
    ): Result<Boolean>

    @Operation(summary = "删除 agent 分享给项目")
    @DELETE
    @Path("/shared")
    fun deleteAgentShared(
        shares: AgentShared
    ): Result<Boolean>
}
