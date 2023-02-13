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

package com.tencent.devops.process.api.builds

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PIPELINE_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.pojo.PipelineId
import com.tencent.devops.process.pojo.SubPipelineRefTree
import com.tencent.devops.process.pojo.pipeline.ProjectBuildId
import com.tencent.devops.process.pojo.pipeline.SubPipelineStartUpInfo
import com.tencent.devops.process.pojo.pipeline.SubPipelineStatus
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

@Api(tags = ["BUILD_SUBPIPELINE"], description = "构建-流水线调用")
@Path("/build/subpipeline")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface BuildSubPipelineResource {
    @ApiOperation("获取子流水线状态")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/detail")
    @Path("/subPipeline/{projectId}/{pipelineId}/{buildId}/detail")
    fun getSubPipelineStatus(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<SubPipelineStatus>

    @ApiOperation("从构建机启动子流水线")
    @POST
    // @Path("/pipelines/{callPipelineId}/atoms/{atomCode}/startByPipeline")
    @Path("/pipelines/{callPipelineId}/{atomCode}/startByPipeline")
    fun callPipelineStartup(
        @ApiParam(value = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @ApiParam("当前流水线ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PIPELINE_ID)
        parentPipelineId: String,
        @ApiParam("构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @ApiParam("要启动的流水线ID", required = true)
        @PathParam("callPipelineId")
        callPipelineId: String,
        @ApiParam("插件标识", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam("插件ID", required = true)
        @QueryParam("taskId")
        taskId: String,
        @ApiParam("运行方式", required = true)
        @QueryParam("runMode")
        runMode: String,
        @QueryParam("channelCode")
        channelCode: ChannelCode?,
        @ApiParam("启动参数", required = true)
        values: Map<String, String>
    ): Result<ProjectBuildId>

    @ApiOperation("从构建机启动指定项目的子流水线")
    @POST
    // @Path("/pipelines/{callPipelineId}/atoms/{atomCode}/startByPipeline")
    @Path("/projects/{callProjectId}/pipelines/{callPipelineId}/atoms/{atomCode}/startByPipeline")
    fun callOtherProjectPipelineStartup(
        @ApiParam(value = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @ApiParam("当前流水线ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PIPELINE_ID)
        parentPipelineId: String,
        @ApiParam("构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @ApiParam("要启动的流水线ID", required = true)
        @PathParam("callProjectId")
        callProjectId: String,
        @ApiParam("要启动的流水线ID", required = true)
        @PathParam("callPipelineId")
        callPipelineId: String,
        @ApiParam("插件标识", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam("插件ID", required = true)
        @QueryParam("taskId")
        taskId: String,
        @ApiParam("运行方式", required = true)
        @QueryParam("runMode")
        runMode: String,
        @ApiParam("启动参数", required = true)
        values: Map<String, String>
    ): Result<ProjectBuildId>

    @ApiOperation("获取子流水线启动参数")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/manualStartupInfo")
    fun subpipManualStartupInfo(
        @ApiParam(value = "用户ID", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = false, defaultValue = "")
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<List<SubPipelineStartUpInfo>>

    @ApiOperation("根据流水线名称获取流水线ID")
    @GET
    @Path("/projects/{projectId}/pipelines/getPipelineIdByName")
    fun getPipelineByName(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线名称", required = false, defaultValue = "")
        @QueryParam("pipelineName")
        pipelineName: String
    ): Result<List<PipelineId?>>

    @ApiOperation("获取子流水线状态")
    @GET
    @Path("/subPipeline/{projectId}/{pipelineId}/{buildId}/call/detail")
    fun getSubPipelinesStatus(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<SubPipelineRefTree?>
}
