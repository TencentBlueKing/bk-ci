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

package com.tencent.devops.repository.api.scm

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.enums.GitCodeBranchesSort
import com.tencent.devops.repository.pojo.enums.GitCodeProjectsOrder
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.git.GitCodeProjectInfo
import com.tencent.devops.repository.pojo.git.GitUserInfo
import com.tencent.devops.scm.code.git.api.GitBranch
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.scm.pojo.ChangeFileInfo
import com.tencent.devops.scm.pojo.GitFileInfo
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.servlet.http.HttpServletResponse
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_SCM_GIT", description = "Service Code GIT resource")
@Path("/service/tgit/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface ServiceTGitResource {

    @Operation(summary = "获取用户所有git分支")
    @GET
    @Path("/getBranch")
    fun getBranch(
        @Parameter(description = "accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @Parameter(description = "用户id", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "仓库ID", required = true)
        @QueryParam("repository")
        repository: String,
        @Parameter(description = "第几页", required = true)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数据条数", required = true)
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(description = "搜索条件", required = true)
        @QueryParam("search")
        search: String?
    ): Result<List<GitBranch>>

    @Operation(summary = "获取git文件内容")
    @GET
    @Path("/getGitFileContent")
    fun getGitFileContent(
        @Parameter(description = "仓库名字")
        @QueryParam("repoName")
        repoName: String,
        @Parameter(description = "文件路径")
        @QueryParam("filePath")
        filePath: String,
        @Parameter(description = "认证方式")
        @QueryParam("authType")
        authType: RepoAuthType?,
        @Parameter(description = "token")
        @QueryParam("token")
        token: String,
        @Parameter(description = "提交id 或者 分支")
        @QueryParam("ref")
        ref: String
    ): Result<String>

    @Operation(summary = "下载git原始文件内容")
    @GET
    @Path("/downloadGitFile")
    fun downloadGitFile(
        @Parameter(description = "仓库名字")
        @QueryParam("repoId")
        repoId: String,
        @Parameter(description = "文件路径")
        @QueryParam("filePath")
        filePath: String,
        @Parameter(description = "认证方式")
        @QueryParam("authType")
        authType: RepoAuthType?,
        @Parameter(description = "提交id 或者 分支")
        @QueryParam("ref")
        ref: String,
        @Context
        response: HttpServletResponse
    )

    @Operation(summary = "获取用户所有git项目，分页方式获取")
    @GET
    @Path("/getGitCodeProjectList")
    fun getGitCodeProjectList(
        @Parameter(description = "accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @Parameter(description = "第几页", required = true)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数据条数", required = true)
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(description = "搜索条件，模糊匹配path,name")
        @QueryParam("search")
        search: String?,
        @Parameter(description = "排序字段")
        @QueryParam("orderBy")
        orderBy: GitCodeProjectsOrder?,
        @Parameter(description = "排序方式")
        @QueryParam("sort")
        sort: GitCodeBranchesSort?,
        @Parameter(description = "若为true，返回的是当前用户个人namespace下的project，以及owner为当前用户的group下的所有project")
        @QueryParam("owned")
        owned: Boolean?,
        @Parameter(description = "指定最小访问级别，返回的project列表中，当前用户的project访问级别大于或者等于指定值")
        @QueryParam("minAccessLevel")
        minAccessLevel: GitAccessLevelEnum?
    ): Result<List<GitCodeProjectInfo>>

    @Operation(summary = "获取git文件目录列表")
    @GET
    @Path("/getGitFileTree")
    fun getGitFileTree(
        @Parameter(description = "gitProjectId")
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @Parameter(description = "目录路径")
        @QueryParam("path")
        path: String,
        @Parameter(description = "token")
        @QueryParam("token")
        token: String,
        @Parameter(description = "提交id 或者 分支")
        @QueryParam("ref")
        ref: String?,
        @Parameter(description = "是否支持递归目录结构")
        @QueryParam("recursive")
        recursive: Boolean? = false,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<List<GitFileInfo>>

    @Operation(summary = "获取用户的基本信息")
    @GET
    @Path("/getUserInfoByToken")
    fun getUserInfoByToken(
        @Parameter(description = "用户id", required = true)
        @QueryParam("token")
        token: String,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum = TokenTypeEnum.OAUTH
    ): Result<GitUserInfo>

    @Operation(summary = "获取两次提交的差异文件列表")
    @GET
    @Path("/getChangeFileList")
    fun getChangeFileList(
        @Parameter(description = "token")
        @QueryParam("token")
        token: String,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum,
        @Parameter(description = "gitProjectId")
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @Parameter(description = "旧commit")
        @QueryParam("from")
        from: String,
        @Parameter(description = "新commit")
        @QueryParam("to")
        to: String,
        @Parameter(description = "true：两个点比较差异，false：三个点比较差异。默认是 false")
        @QueryParam("straight")
        straight: Boolean? = false,
        @Parameter(description = "页码")
        @QueryParam("page")
        page: Int,
        @Parameter(description = "每页大小")
        @QueryParam("pageSize")
        pageSize: Int,
        @Parameter(description = "代码库url")
        @QueryParam("url")
        url: String
    ): Result<List<ChangeFileInfo>>
}
