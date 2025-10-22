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

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.webhook.pojo.code.github.GithubWebhook
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.code.WebhookCommit
import com.tencent.devops.process.pojo.webhook.PipelineWebhook
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_SCM", description = "服务-SCM")
@Path("/service/scm")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceScmWebhookResource {

    @Operation(summary = "Github仓库提交")
    @POST
    @Path("/github/commit")
    fun webHookCodeGithubCommit(
        webhook: GithubWebhook
    ): Result<Boolean>

    @Operation(summary = "Webhook代码库提交")
    @POST
    @Path("/webhook/commit")
    fun webhookCommit(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        webhookCommit: WebhookCommit
    ): Result<String>

    @Operation(summary = "Webhook代码库提交")
    @POST
    @Path("/webhook/commit/new")
    fun webhookCommitNew(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        webhookCommit: WebhookCommit
    ): Result<BuildId?>

    @Operation(summary = "获取流水线的webhook列表")
    @GET
    @Path("/{projectId}/{pipelineId}")
    fun listScmWebhook(
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页大小", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<PipelineWebhook>>

    @Operation(summary = "添加scm灰度白名单仓库")
    @PUT
    @Path("/{scmCode}/addGrayRepoWhite")
    fun addGrayRepoWhite(
        @Parameter(description = "代码库标识", required = true)
        @PathParam("scmCode")
        scmCode: String,
        @Parameter(description = "是否只开启pac仓库", required = true)
        @QueryParam("pac")
        pac: Boolean,
        @Parameter(description = "服务端代码仓库id列表", required = true)
        serverRepoNames: List<String>
    ): Result<Boolean>
}
