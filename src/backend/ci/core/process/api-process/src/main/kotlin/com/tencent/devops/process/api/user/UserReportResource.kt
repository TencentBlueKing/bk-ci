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

package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.Report
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_REPORT", description = "用户-自定义报告")
@Path("/user/reports")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserReportResource {
    @Operation(summary = "获取构建报告列表")
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}")
    @Path("/{projectId}/{pipelineId}/{buildId}")
    @GET
    fun get(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "插件ID", required = false)
        @QueryParam("taskId")
        taskId: String?
    ): Result<List<Report>>

    @Operation(summary = "stream获取构建报告列表(不带网关，方便域名替换)")
    @Path("/stream/{projectId}/{pipelineId}/{buildId}")
    @GET
    fun getStream(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<List<Report>>
}
