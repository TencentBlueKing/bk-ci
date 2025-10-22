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

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.pojo.BuildFormValue
import com.tencent.devops.scm.pojo.RevisionInfo
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_SCM", description = "用户-scm相关接口")
@Path("/user/scm")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserScmResource {

    @Operation(summary = "获取仓库最新版本")
    @GET
    // @Path("/projects/{projectId}/repositories/{repositoryId}/latestRevision")
    @Path("/{projectId}/{repositoryId}/latestRevision")
    fun getLatestRevision(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "repo hash id or repo name", required = true)
        @PathParam("repositoryId")
        repositoryId: String,
        @Parameter(description = "branch name", required = false)
        @QueryParam("branchName")
        branchName: String? = null,
        @Parameter(description = "SVN additional path", required = false)
        @QueryParam("additionalPath")
        additionalPath: String? = null,
        @Parameter(description = "代码库请求类型", required = false)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?
    ): Result<RevisionInfo>

    @Operation(summary = "列出仓库所有分支")
    @GET
    // @Path("/projects/{projectId}/repositories/{repositoryId}/branches")
    @Path("/{projectId}/{repositoryId}/branches")
    fun listBranches(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "repo hash id", required = true)
        @PathParam("repositoryId")
        repositoryId: String,
        @Parameter(description = "代码库请求类型", required = true)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?,
        @Parameter(description = "搜索条件", required = false)
        @QueryParam("search")
        search: String?
    ): Result<List<String>>

    @Operation(summary = "列出仓库所有分支")
    @GET
    // @Path("/projects/{projectId}/repositories/{repositoryId}/tags")
    @Path("/{projectId}/{repositoryId}/tags")
    fun listTags(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "repo hash id", required = true)
        @PathParam("repositoryId")
        repositoryId: String,
        @Parameter(description = "代码库请求类型", required = true)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?
    ): Result<List<String>>

    @Operation(summary = "列出仓库分支和tag集合")
    @GET
    @Path("/{projectId}/{repositoryId}/refs")
    @Deprecated(
        replaceWith = ReplaceWith("UserBuildParametersResource.listGitRefs"),
        message = "流水线下拉参数,统一到UserBuildParametersResource维护"
    )
    fun listRefs(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "repo hash id", required = true)
        @PathParam("repositoryId")
        repositoryId: String,
        @Parameter(description = "代码库请求类型", required = false)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?,
        @Parameter(description = "搜索条件", required = false)
        @QueryParam("search")
        search: String?
    ): Result<List<BuildFormValue>>
}
