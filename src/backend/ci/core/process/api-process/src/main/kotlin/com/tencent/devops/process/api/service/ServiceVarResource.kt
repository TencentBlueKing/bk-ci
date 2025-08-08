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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.SetContextVarData
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_VARIABLE", description = "服务-构建参数")
@Path("/service/variable")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceVarResource {

    @Operation(summary = "获取指定构建或指定流水线下的构建变量")
    @Path("/get_build_variable")
    @GET
    fun getBuildVar(
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "构建ID", required = true)
        @QueryParam("buildId")
        buildId: String,
        @Parameter(description = "变量名称", required = false)
        @QueryParam("varName")
        varName: String?,
        @Parameter(description = "流水线ID", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?
    ): Result<Map<String, String>>

    @Operation(summary = "获取指定构建或指定构建下的上下文变量")
    @Path("/get_build_context")
    @GET
    fun getContextVar(
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @QueryParam("buildId")
        buildId: String,
        @Parameter(description = "变量名称", required = false)
        @QueryParam("contextName")
        contextName: String?
    ): Result<Map<String, String>>

    @Operation(summary = "针对构建写入上下文变量")
    @Path("/set_build_context")
    @POST
    fun setContextVar(
        data: SetContextVarData
    )

    @Operation(summary = "获取指定构建或指定流水线下的构建变量")
    @Path("/var/data/get")
    @POST
    fun getBuildVars(
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @QueryParam("buildId")
        buildId: String,
        @Parameter(description = "字段key集合", required = false)
        keys: Set<String>? = null
    ): Result<Map<String, String>>
}
