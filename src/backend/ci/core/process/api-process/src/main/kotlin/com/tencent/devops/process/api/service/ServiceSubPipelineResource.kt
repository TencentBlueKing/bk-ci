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
 *
 */

package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.pipeline.PipelineBuildParamFormProp
import com.tencent.devops.process.pojo.pipeline.ProjectBuildId
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_SUBPIPELINE", description = "服务-流水线调用")
@Path("/service/subpipeline")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceSubPipelineResource {

    @Operation(summary = "获取子流水线启动参数")
    @GET
    @Path("/pipelines/{pipelineId}/manualStartupInfo")
    fun subpipManualStartupInfo(
        @Parameter(description = "用户ID", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @Parameter(description = "流水线ID", required = false, example = "")
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<List<PipelineBuildParamFormProp>>

    @Operation(summary = "从构建机启动指定项目的子流水线")
    @POST
    @Path("/pipelines/{callPipelineId}/atoms/{atomCode}/startByPipeline")
    fun callOtherProjectPipelineStartup(
        @Parameter(description = "要启动的流水线ID", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        callProjectId: String,
        @Parameter(description = "要启动的流水线ID", required = true)
        @PathParam("callPipelineId")
        callPipelineId: String,
        @Parameter(description = "插件标识", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "父项目ID", required = true)
        @QueryParam("parentProjectId")
        parentProjectId: String,
        @Parameter(description = "父流水线ID", required = true)
        @QueryParam("parentPipelineId")
        parentPipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @QueryParam("buildId")
        buildId: String,
        @Parameter(description = "当前流水线执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?,
        @Parameter(description = "插件ID", required = true)
        @QueryParam("taskId")
        taskId: String,
        @Parameter(description = "运行方式", required = true)
        @QueryParam("runMode")
        runMode: String,
        @Parameter(description = "启动参数", required = true)
        values: Map<String, String>
    ): Result<ProjectBuildId>
}
