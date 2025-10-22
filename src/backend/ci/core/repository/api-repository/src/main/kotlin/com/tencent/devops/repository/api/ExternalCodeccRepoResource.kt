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

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.scm.pojo.GitMember
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

@Tag(name = "EXTERNAL_REPO", description = "外部-codecc-仓库资源")
@Path("/external/codecc/repo/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface ExternalCodeccRepoResource {

    @Operation(summary = "获取仓库单个文件内容")
    @GET
    @Path("/{repoId}/getFileContent")
    fun getFileContent(
        @Parameter(description = "仓库id")
        @PathParam("repoId")
        repoId: String,
        @Parameter(description = "文件路径")
        @QueryParam("filePath")
        filePath: String,
        @Parameter(description = "版本号（svn）")
        @QueryParam("reversion")
        reversion: String?,
        @Parameter(description = "分支（git）")
        @QueryParam("branch")
        branch: String?,
        @Parameter(description = "子模块项目名称")
        @QueryParam("subModule")
        subModule: String? = null,
        @Parameter(description = "代码库请求类型", required = true)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?
    ): Result<String>

    @Operation(summary = "获取仓库单个文件内容")
    @GET
    @Path("/repositories/{repoId}/v2/getFileContent")
    fun getFileContentV2(
        @Parameter(description = "仓库id")
        @PathParam("repoId")
        repoId: String,
        @Parameter(description = "文件路径")
        @QueryParam("filePath")
        filePath: String,
        @Parameter(description = "版本号（svn）")
        @QueryParam("reversion")
        reversion: String?,
        @Parameter(description = "分支（git）")
        @QueryParam("branch")
        branch: String?,
        @Parameter(description = "子模块项目名称")
        @QueryParam("subModule")
        subModule: String? = null,
        @Parameter(description = "代码库请求类型", required = true)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?
    ): Result<String>

    @Operation(summary = "获取仓库单个文件内容")
    @GET
    @Path("/getGitFileContentCommon")
    fun getGitFileContentCommon(
        @Parameter(description = "代码库url")
        @QueryParam("repoUrl")
        repoUrl: String,
        @Parameter(description = "文件路径")
        @QueryParam("filePath")
        filePath: String,
        @Parameter(description = "分支或者commit id（git）")
        @QueryParam("branch")
        ref: String?,
        @Parameter(description = "调用api的token")
        @QueryParam("token")
        token: String,
        @Parameter(description = "代码块认证方式，默认http")
        @QueryParam("authType")
        authType: RepoAuthType? = RepoAuthType.HTTP,
        @Parameter(description = "子模块项目名称")
        @QueryParam("subModule")
        subModule: String? = null
    ): Result<String>

    @Operation(summary = "获取仓库单个文件内容")
    @GET
    @Path("/oauth/git_file_content")
    fun getGitFileContentOAuth(
        @Parameter(description = "用户id")
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "代码库url")
        @QueryParam("repoName")
        repoName: String,
        @Parameter(description = "文件路径")
        @QueryParam("filePath")
        filePath: String,
        @Parameter(description = "分支或者commit id（git）")
        @QueryParam("ref")
        ref: String?
    ): Result<String>

    @Operation(summary = "获取代码库成员列表")
    @GET
    @Path("/members")
    fun getRepoMembers(
        @Parameter(description = "代码库url")
        @QueryParam("repoName")
        repoUrl: String,
        @Parameter(description = "用户id")
        @QueryParam("userId")
        userId: String
    ): Result<List<GitMember>>

    @Operation(summary = "获取代码库有权限成员列表")
    @GET
    @Path("/members/all")
    fun getRepoAllMembers(
        @Parameter(description = "代码库url")
        @QueryParam("repoName")
        repoUrl: String,
        @Parameter(description = "用户id")
        @QueryParam("userId")
        userId: String
    ): Result<List<GitMember>>

    @Operation(summary = "获取代码库有权限成员列表")
    @GET
    @Path("/isProjectMember")
    fun isProjectMember(
        @Parameter(description = "代码库url")
        @QueryParam("repoName")
        repoUrl: String,
        @Parameter(description = "用户id")
        @QueryParam("userId")
        userId: String
    ): Result<Boolean>

    @Operation(summary = "通过凭证Id获取文件内容")
    @GET
    @Path("/{projectId}/getFileContentByUrl")
    fun getFileContentByUrl(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "代码库url")
        @QueryParam("repoUrl")
        repoUrl: String,
        @Parameter(description = "代码库类型")
        @QueryParam("scmType")
        scmType: ScmType,
        @Parameter(description = "文件路径")
        @QueryParam("filePath")
        filePath: String,
        @Parameter(description = "版本号（svn/p4）")
        @QueryParam("reversion")
        reversion: String?,
        @Parameter(description = "分支（git）")
        @QueryParam("branch")
        branch: String?,
        @Parameter(description = "子模块项目名称")
        @QueryParam("subModule")
        subModule: String? = null,
        @Parameter(description = "代码库ticketId")
        @QueryParam("credentialId")
        credentialId: String
    ): Result<String>
}
