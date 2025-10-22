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

package com.tencent.devops.repository.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_AGENT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_BUILD_ID
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.commit.CommitData
import com.tencent.devops.repository.pojo.commit.CommitResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_COMMIT", description = "git提交记录")
@Path("/service/commit/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceCommitResource {

    @Operation(summary = "获取流水线的最新一次commit")
    @GET
    @Path("/getLatestCommit")
    fun getLatestCommit(
        @Parameter(description = "流水线ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "原子ID", required = true)
        @QueryParam("elementId")
        elementId: String,
        @Parameter(description = "仓库ID或者仓库名", required = true)
        @QueryParam("repoId")
        repositoryId: String,
        @Parameter(description = "代码库请求类型", required = false)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?
    ): Result<CommitData?>

    @Operation(summary = "根据构建ID获取提交记录")
    @GET
    @Path("/getCommitsByBuildId")
    fun getCommitsByBuildId(
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @Parameter(description = "Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_AGENT_ID)
        agentId: String
    ): Result<List<CommitResponse>>
}
