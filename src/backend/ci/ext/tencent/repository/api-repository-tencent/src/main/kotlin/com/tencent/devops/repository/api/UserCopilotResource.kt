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

package com.tencent.devops.repository.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.scm.enums.AISummaryRateType
import com.tencent.devops.scm.pojo.CodeGitCopilotSummary
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "USER_OAUTH_GIT", description = "用户-git的oauth")
@Path("/user/copilot/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserCopilotResource {

    @Operation(summary = "根据用户ID, 获取工蜂Copilot Open Token")
    @GET
    @Path("/tgit/getCopilotOpenToken")
    fun getCopilotOpenToken(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "是否刷新token", required = false)
        @QueryParam("refresh")
        refresh: Boolean? = false
    ): Result<String>

    @Operation(summary = "生成AI摘要（异步）")
    @GET
    @Path("/createSummary")
    fun createSummary(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "蓝盾项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @QueryParam("buildId")
        buildId: String,
        @Parameter(description = "插件ID", required = true)
        @QueryParam("elementId")
        elementId: String,
        @Parameter(description = "重新生成", required = false)
        @QueryParam("refresh")
        refresh: Boolean? = false
    ): Result<CodeGitCopilotSummary>

    @Operation(summary = "获取AI摘要结果")
    @GET
    @Path("/getSummary")
    fun getSummary(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "蓝盾项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @QueryParam("buildId")
        buildId: String,
        @Parameter(description = "插件ID", required = true)
        @QueryParam("elementId")
        elementId: String
    ): Result<CodeGitCopilotSummary>

    @Operation(summary = "评价AI结果")
    @GET
    @Path("/summary/rate")
    fun rateSummary(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "仓库名", required = true)
        @QueryParam("projectName")
        projectName: String,
        @Parameter(description = "AI摘要任务ID", required = true)
        @QueryParam("processId")
        processId: String,
        @Parameter(description = "评价类型", required = true)
        @QueryParam("type")
        type: AISummaryRateType,
        @Parameter(description = "更多反馈", required = false)
        @QueryParam("feedback")
        feedback: String? = null
    ): Result<Boolean>
}
