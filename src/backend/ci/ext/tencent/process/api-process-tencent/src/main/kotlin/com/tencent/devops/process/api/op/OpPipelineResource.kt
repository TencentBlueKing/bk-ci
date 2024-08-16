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

package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.DockerEnableProject
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.app.PipelinePage
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "OP_PIPELINE", description = "OP-流水线")
@Path("/op/pipelines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpPipelineResource {

    @Operation(summary = "根据项目id获取所有的流水线")
    @GET
    @Path("/list")
    fun list(
        @Parameter(description = "项目ID", required = false, example = "")
        @QueryParam("projectId")
        projectId: String?,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(description = "流水线排序", required = false, example = "CREATE_TIME")
        @QueryParam("sortType")
        sortType: PipelineSortType? = PipelineSortType.CREATE_TIME
    ): Result<PipelinePage<Pipeline>>

    @Operation(summary = "项目启动docker构建方式")
    @PUT
    @Path("/dockers/enable")
    fun enableDocker(
        @Parameter(description = "项目ID", required = true, example = "")
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "是否启动", required = true, example = "true")
        @QueryParam("enable")
        enable: Boolean
    ): Result<Boolean>

    @Operation(summary = "获取所有灰度docker构建的项目")
    @GET
    @Path("/dockers")
    fun getAllEnableDockerProjects(): Result<List<DockerEnableProject>>
}
