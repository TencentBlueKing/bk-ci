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
import com.tencent.devops.common.api.pojo.Result
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
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_ENVIRONMENT_THIRD_PARTY_AGENT"], description = "第三方构建机资源")
@Path("/op/thirdPartyAgent")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpThirdPartyAgentResource {

    @ApiOperation("设置Agent升级")
    @PUT
    @Path("/agents/upgrade/{version}")
    fun setWorkerVersion(
        @ApiParam("版本号", required = true)
        @PathParam("version")
        version: String
    ): Result<Boolean>

    @ApiOperation("设置Agent Master版本")
    @PUT
    @Path("/agents/masterVersion/{version}")
    fun setMasterVersion(
        @ApiParam("版本号", required = true)
        @PathParam("version")
        version: String
    ): Result<Boolean>

    @ApiOperation("获取当前Agent版本")
    @GET
    @Path("/agent/upgrade")
    fun getWorkerVersion(): Result<String?>

    @ApiOperation("获取当前Master版本")
    @GET
    @Path("/agent/masterVersion")
    fun getMasterVersion(): Result<String?>

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

    @ApiOperation("设置agent强制升级")
    @POST
    @Path("/agents/setForceUpdateAgents")
    fun setForceUpdateAgents(
        @ApiParam("agentIds", required = true)
        agentIds: List<Long>
    ): Result<Boolean>

    @ApiOperation("取消agent强制升级")
    @POST
    @Path("/agents/unsetForceUpdateAgents")
    fun unsetForceUpdateAgents(
        @ApiParam("agentIds", required = true)
        agentIds: List<Long>
    ): Result<Boolean>

    @ApiOperation("获取所有强制升级agent")
    @POST
    @Path("/agents/getAllForceUpgradeAgents")
    fun getAllForceUpgradeAgents(): Result<List<Long>>

    @ApiOperation("取消所有强制升级agent")
    @POST
    @Path("/agents/cleanAllForceUpgradeAgents")
    fun cleanAllForceUpgradeAgents(): Result<Boolean>

    @ApiOperation("设置agent锁定升级")
    @POST
    @Path("/agents/setLockUpdateAgents")
    fun setLockUpdateAgents(
        @ApiParam("agentIds", required = true)
        agentIds: List<Long>
    ): Result<Boolean>

    @ApiOperation("取消agent锁定升级")
    @POST
    @Path("/agents/unsetLockUpdateAgents")
    fun unsetLockUpdateAgents(
        @ApiParam("agentIds", required = true)
        agentIds: List<Long>
    ): Result<Boolean>

    @ApiOperation("获取所有强制锁定agent")
    @POST
    @Path("/agents/getAllLockUpgradeAgents")
    fun getAllLockUpgradeAgents(): Result<List<Long>>

    @ApiOperation("取消所有强制锁定agent")
    @POST
    @Path("/agents/cleanAllLockUpgradeAgents")
    fun cleanAllLockUpgradeAgents(): Result<Boolean>

    @ApiOperation("设置agent最大并发升级数量")
    @POST
    @Path("/agents/setMaxParallelUpgradeCount")
    fun setMaxParallelUpgradeCount(
        @ApiParam("maxParallelUpgradeCount", required = true)
        maxParallelUpgradeCount: Int
    ): Result<Boolean>

    @ApiOperation("获取agent最大并发升级数量")
    @POST
    @Path("/agents/getMaxParallelUpgradeCount")
    fun getMaxParallelUpgradeCount(): Result<Int?>
}