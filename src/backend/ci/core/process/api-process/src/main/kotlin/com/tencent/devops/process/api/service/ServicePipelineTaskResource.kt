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

package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.web.annotation.BkApiPermission
import com.tencent.devops.common.web.constant.BkApiHandleType
import com.tencent.devops.process.engine.pojo.ContainerStartInfo
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.PipelineModelTask
import com.tencent.devops.process.pojo.PipelineProjectRel
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_PIPELINE", description = "服务-流水线-任务资源")
@Path("/service/pipelineTasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServicePipelineTaskResource {

    @Operation(summary = "获取流水线所有插件")
    @POST
    // @Path("/projects/{projectId}/list")
    @Path("/{projectId}/list")
    @BkApiPermission([BkApiHandleType.API_NO_AUTH_CHECK])
    fun list(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线id集合", required = true)
        pipelineIds: Collection<String>
    ): Result<Map<String, List<PipelineModelTask>>>

    @Operation(summary = "获取使用指定插件的流水线")
    @GET
    @Path("/atoms/{atomCode}")
    fun listByAtomCode(
        @Parameter(description = "插件标识", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "项目标识", required = false)
        @QueryParam("projectCode")
        projectCode: String?,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<PipelineProjectRel>>

    @Operation(summary = "获取使用插件的流水线数量")
    @POST
    @Path("/listPipelineNumByAtomCodes")
    @BkApiPermission([BkApiHandleType.API_NO_AUTH_CHECK])
    fun listPipelineNumByAtomCodes(
        @Parameter(description = "项目ID", required = false)
        @QueryParam("projectId")
        projectId: String? = null,
        @Parameter(description = "插件标识集合", required = true)
        atomCodes: List<String>
    ): Result<Map<String, Int>>

    @Operation(summary = "获取流水线指定任务的构建状态")
    @GET
    @Path("/projects/{projectId}/builds/{buildId}/tasks/{taskId}")
    fun getTaskStatus(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "任务ID", required = true)
        @PathParam("taskId")
        taskId: String
    ): Result<BuildStatus?>

    @Operation(summary = "获取流水线指定任务的构建详情")
    @GET
    @Path("/projects/{projectId}/builds/{buildId}/task_detail")
    fun getTaskBuildDetail(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "任务ID", required = false)
        @QueryParam("taskId")
        taskId: String?,
        @Parameter(description = "任务ID", required = false)
        @QueryParam("stepId")
        stepId: String?,
        @Parameter(description = "执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?
    ): Result<PipelineBuildTask?>

    @Operation(summary = "获取流水线指定Job的构建状态")
    @GET
    @Path("/projects/{projectId}/builds/{buildId}/containers/{containerId}/tasks/{taskId}")
    fun getContainerStartupInfo(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "任务ID", required = true)
        @PathParam("containerId")
        containerId: String,
        @Parameter(description = "任务ID", required = true)
        @PathParam("taskId")
        taskId: String
    ): Result<ContainerStartInfo?>
}
