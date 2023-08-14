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

package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.engine.pojo.ContainerStartInfo
import com.tencent.devops.process.engine.pojo.PipelineModelTask
import com.tencent.devops.process.pojo.PipelineProjectRel
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_PIPELINE"], description = "服务-流水线-任务资源")
@Path("/service/pipelineTasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServicePipelineTaskResource {

    @ApiOperation("获取流水线所有插件")
    @POST
    // @Path("/projects/{projectId}/list")
    @Path("/{projectId}/list")
    fun list(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线id集合", required = true)
        pipelineIds: Collection<String>
    ): Result<Map<String, List<PipelineModelTask>>>

    @ApiOperation("获取使用指定插件的流水线")
    @GET
    @Path("/atoms/{atomCode}")
    fun listByAtomCode(
        @ApiParam("插件标识", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam("项目标识", required = false)
        @QueryParam("projectCode")
        projectCode: String?,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<PipelineProjectRel>>

    @ApiOperation("获取使用插件的流水线数量")
    @POST
    @Path("/listPipelineNumByAtomCodes")
    fun listPipelineNumByAtomCodes(
        @ApiParam("项目ID", required = false)
        @QueryParam("projectId")
        projectId: String? = null,
        @ApiParam("插件标识集合", required = true)
        atomCodes: List<String>
    ): Result<Map<String, Int>>

    @ApiOperation("获取流水线指定任务的构建状态")
    @GET
    @Path("/projects/{projectId}/builds/{buildId}/tasks/{taskId}")
    fun getTaskStatus(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("任务ID", required = true)
        @PathParam("taskId")
        taskId: String
    ): Result<BuildStatus?>

    @ApiOperation("获取流水线指定Job的构建状态")
    @GET
    @Path("/projects/{projectId}/builds/{buildId}/containers/{containerId}/tasks/{taskId}")
    fun getContainerStartupInfo(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("任务ID", required = true)
        @PathParam("containerId")
        containerId: String,
        @ApiParam("任务ID", required = true)
        @PathParam("taskId")
        taskId: String
    ): Result<ContainerStartInfo?>
}
