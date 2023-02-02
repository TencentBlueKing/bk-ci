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

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.scm.pojo.GitMember
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["EXTERNAL_REPO"], description = "外部-codecc-仓库资源")
@Path("/external/codecc/repo/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface ExternalCodeccRepoResource {

    @ApiOperation("获取仓库单个文件内容")
    @GET
    @Path("/{repoId}/getFileContent")
    fun getFileContent(
        @ApiParam(value = "仓库id")
        @PathParam("repoId")
        repoId: String,
        @ApiParam(value = "文件路径")
        @QueryParam("filePath")
        filePath: String,
        @ApiParam(value = "版本号（svn）")
        @QueryParam("reversion")
        reversion: String?,
        @ApiParam(value = "分支（git）")
        @QueryParam("branch")
        branch: String?,
        @ApiParam(value = "子模块项目名称")
        @QueryParam("subModule")
        subModule: String? = null,
        @ApiParam("代码库请求类型", required = true)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?
    ): Result<String>

    @ApiOperation("获取仓库单个文件内容")
    @GET
    @Path("/repositories/{repoId}/v2/getFileContent")
    fun getFileContentV2(
        @ApiParam(value = "仓库id")
        @PathParam("repoId")
        repoId: String,
        @ApiParam(value = "文件路径")
        @QueryParam("filePath")
        filePath: String,
        @ApiParam(value = "版本号（svn）")
        @QueryParam("reversion")
        reversion: String?,
        @ApiParam(value = "分支（git）")
        @QueryParam("branch")
        branch: String?,
        @ApiParam(value = "子模块项目名称")
        @QueryParam("subModule")
        subModule: String? = null,
        @ApiParam("代码库请求类型", required = true)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?
    ): Result<String>

    @ApiOperation("获取仓库单个文件内容")
    @GET
    @Path("/getGitFileContentCommon")
    fun getGitFileContentCommon(
        @ApiParam(value = "代码库url")
        @QueryParam("repoUrl")
        repoUrl: String,
        @ApiParam(value = "文件路径")
        @QueryParam("filePath")
        filePath: String,
        @ApiParam(value = "分支或者commit id（git）")
        @QueryParam("branch")
        ref: String?,
        @ApiParam(value = "调用api的token")
        @QueryParam("token")
        token: String,
        @ApiParam(value = "代码块认证方式，默认http")
        @QueryParam("authType")
        authType: RepoAuthType? = RepoAuthType.HTTP,
        @ApiParam(value = "子模块项目名称")
        @QueryParam("subModule")
        subModule: String? = null
    ): Result<String>

    @ApiOperation("获取仓库单个文件内容")
    @GET
    @Path("/oauth/git_file_content")
    fun getGitFileContentOAuth(
        @ApiParam(value = "用户id")
        @QueryParam("userId")
        userId: String,
        @ApiParam(value = "代码库url")
        @QueryParam("repoName")
        repoName: String,
        @ApiParam(value = "文件路径")
        @QueryParam("filePath")
        filePath: String,
        @ApiParam(value = "分支或者commit id（git）")
        @QueryParam("ref")
        ref: String?
    ): Result<String>

    @ApiOperation("获取代码库成员列表")
    @GET
    @Path("/members")
    fun getRepoMembers(
        @ApiParam(value = "代码库url")
        @QueryParam("repoName")
        repoUrl: String,
        @ApiParam(value = "用户id")
        @QueryParam("userId")
        userId: String
    ): Result<List<GitMember>>

    @ApiOperation("获取代码库有权限成员列表")
    @GET
    @Path("/members/all")
    fun getRepoAllMembers(
        @ApiParam(value = "代码库url")
        @QueryParam("repoName")
        repoUrl: String,
        @ApiParam(value = "用户id")
        @QueryParam("userId")
        userId: String
    ): Result<List<GitMember>>

    @ApiOperation("获取代码库有权限成员列表")
    @GET
    @Path("/isProjectMember")
    fun isProjectMember(
        @ApiParam(value = "代码库url")
        @QueryParam("repoName")
        repoUrl: String,
        @ApiParam(value = "用户id")
        @QueryParam("userId")
        userId: String
    ): Result<Boolean>

    @ApiOperation("通过凭证Id获取文件内容")
    @GET
    @Path("/{projectId}/getFileContentByUrl")
    fun getFileContentByUrl(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "代码库url")
        @QueryParam("repoUrl")
        repoUrl: String,
        @ApiParam(value = "代码库类型")
        @QueryParam("scmType")
        scmType: ScmType,
        @ApiParam(value = "文件路径")
        @QueryParam("filePath")
        filePath: String,
        @ApiParam(value = "版本号（svn/p4）")
        @QueryParam("reversion")
        reversion: String?,
        @ApiParam(value = "分支（git）")
        @QueryParam("branch")
        branch: String?,
        @ApiParam(value = "子模块项目名称")
        @QueryParam("subModule")
        subModule: String? = null,
        @ApiParam(value = "代码库ticketId")
        @QueryParam("credentialId")
        credentialId: String
    ): Result<String>
}
