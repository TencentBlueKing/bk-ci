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

package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.utils.PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_DEFAULT
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_PIPELINE_SETTINGS", description = "OP-流水线-设置")
@Path("/op/pipeline/settings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpPipelineSettingResource {

    @Operation(summary = "更新流水线设置")
    @PUT
    @Path("/update")
    fun updateSetting(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "流水线设置", required = true)
        setting: PipelineSetting
    ): Result<String>

    @Operation(summary = "获取流水线设置")
    @GET
    @Path("/get")
    fun getSetting(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "流水线id", required = true)
        @QueryParam("pipelineId")
        pipelineId: String
    ): Result<PipelineSetting>

    @Operation(summary = "最大并发队列大小")
    @POST
    @Path("/updateMaxConRunningQueueSize")
    fun updateMaxConRunningQueueSize(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "流水线id", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "最大并发队列大小", required = true)
        @QueryParam("maxConRunningQueueSize")
        maxConRunningQueueSize: Int = PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_DEFAULT
    ): Result<String>

    @Operation(summary = "更新YAML流水线配置")
    @POST
    @Path("/updatePipelineAsCodeSettings")
    fun updatePipelineAsCodeSettings(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "流水线id", required = true)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(description = "YAML流水线设置", required = true)
        pipelineAsCodeSettings: PipelineAsCodeSettings
    ): Result<Int>

    @Operation(summary = "更新构建指标配置")
    @POST
    @Path("/updateBuildMetricsSettings")
    fun updateBuildMetricsSettings(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "是否开启", required = true)
        @QueryParam("enabled")
        enabled: Boolean
    ): Result<Boolean>
}
