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
import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.RepositoryInfo
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.repository.pojo.git.GitOperationFile
import com.tencent.devops.repository.pojo.git.UpdateGitProjectInfo
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitProjectInfo
import com.tencent.devops.scm.pojo.GitRepositoryDirItem
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_GIT_REPOSITORY", description = "服务-git代码库资源")
@Path("/service/repositories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceGitRepositoryResource {

    @Operation(summary = "创建git代码库")
    @POST
    @Path("/git/create/repository")
    fun createGitCodeRepository(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目编码", required = false)
        @QueryParam("projectCode")
        projectCode: String?,
        @Parameter(description = "代码库名称", required = true)
        @QueryParam("repositoryName")
        repositoryName: String,
        @Parameter(description = "样例工程路径", required = false)
        @QueryParam("sampleProjectPath")
        sampleProjectPath: String?,
        @Parameter(description = "命名空间ID", required = false)
        @QueryParam("namespaceId")
        namespaceId: Int?,
        @Parameter(description = "项目可视范围", required = false)
        @QueryParam("visibilityLevel")
        visibilityLevel: VisibilityLevelEnum?,
        @Parameter(description = "token类型 1：oauth 2:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum,
        @Parameter(description = "前端UI渲染方式", required = false)
        @QueryParam("frontendType")
        frontendType: FrontendTypeEnum? = null
    ): Result<RepositoryInfo?>

    @Operation(summary = "根据仓库ID更新git代码库信息")
    @PUT
    @Path("/git/update/repository/repoId")
    fun updateGitCodeRepository(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "仓库id", required = true)
        @QueryParam("repoId")
        repoId: String,
        @Parameter(description = "代码库更新信息", required = true)
        updateGitProjectInfo: UpdateGitProjectInfo,
        @Parameter(description = "token类型 1：oauth 2:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<Boolean>

    @Operation(summary = "根据工蜂项目名称更新git代码库信息")
    @PUT
    @Path("/git/update/repository/projectName")
    fun updateGitCodeRepositoryByProjectName(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目名称", required = true)
        @QueryParam("projectName")
        projectName: String,
        @Parameter(description = "更新git项目信息", required = true)
        updateGitProjectInfo: UpdateGitProjectInfo,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<Boolean>

    @Operation(summary = "为项目成员赋予代码库权限")
    @POST
    @Path("/git/repository/members/add")
    fun addGitProjectMember(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "增加的用户列表", required = true)
        @QueryParam("userIdList")
        userIdList: List<String>,
        @Parameter(description = "仓库id", required = true)
        @QueryParam("repoId")
        repoId: String,
        @Parameter(description = "git访问权限", required = true)
        @QueryParam("gitAccessLevel")
        gitAccessLevel: GitAccessLevelEnum,
        @Parameter(description = "token类型 1：oauth 2:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<Boolean>

    @Operation(summary = "删除项目成员的代码库权限")
    @DELETE
    @Path("/git/repository/members/delete")
    fun deleteGitProjectMember(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "删除的用户列表", required = true)
        @QueryParam("userIdList")
        userIdList: List<String>,
        @Parameter(description = "仓库id", required = true)
        @QueryParam("repoId")
        repoId: String,
        @Parameter(description = "token类型 1：oauth 2:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<Boolean>

    @Operation(summary = "更新代码库用户信息")
    @PUT
    @Path("/git/repository/user/info/update")
    fun updateRepositoryUserInfo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目编码", required = true)
        @QueryParam("projectCode")
        projectCode: String,
        @Parameter(description = "代码库HashId", required = true)
        @QueryParam("repositoryHashId")
        repositoryHashId: String
    ): Result<Boolean>

    @Operation(summary = "把项目迁移到指定项目组下")
    @GET
    @Path("/git/move/repository/group")
    fun moveGitProjectToGroup(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目组代码", required = false)
        @QueryParam("groupCode")
        groupCode: String?,
        @Parameter(description = "仓库id", required = true)
        @QueryParam("repoId")
        repoId: String,
        @Parameter(description = "token类型 1：oauth 2:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<GitProjectInfo?>

    @Operation(summary = "获取代码仓库单个文件内容")
    @GET
    @Path("/{repoId}/getFileContent")
    fun getFileContent(
        @Parameter(description = "仓库id", required = true)
        @PathParam("repoId")
        repoId: String,
        @Parameter(description = "文件路径", required = true)
        @QueryParam("filePath")
        filePath: String,
        @Parameter(description = "版本号（svn）", required = false)
        @QueryParam("reversion")
        reversion: String? = null,
        @Parameter(description = "分支（git）", required = false)
        @QueryParam("branch")
        branch: String? = null,
        @Parameter(description = "代码库请求类型", required = false)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType? = null,
        @Parameter(description = "项目ID", required = false)
        @QueryParam("projectId")
        projectId: String? = null
    ): Result<String>

    @Operation(summary = "根据远程仓库ID获取代码仓库单个文件内容")
    @GET
    @Path("/{remoteRepoId}/file/content/get")
    fun getFileContent(
        @Parameter(description = "远程仓库id", required = true)
        @PathParam("remoteRepoId")
        remoteRepoId: String,
        @Parameter(description = "文件路径", required = true)
        @QueryParam("filePath")
        filePath: String,
        @Parameter(description = "认证方式", required = false)
        @QueryParam("authType")
        authType: RepoAuthType? = null,
        @Parameter(description = "oauth认证用戶ID", required = false)
        @QueryParam("oauthUserId")
        oauthUserId: String? = null,
        @Parameter(description = "token", required = false)
        @QueryParam("token")
        token: String? = null,
        @Parameter(description = "分支（git）", required = false)
        @QueryParam("branch")
        branch: String? = null
    ): Result<String>

    @Operation(summary = "更新代码仓库单个文件内容")
    @PUT
    @Path("/{repoId}/updateFileContent")
    fun updateTGitFileContent(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "仓库id")
        @PathParam("repoId")
        repoId: String,
        @Parameter(description = "代码库请求类型", required = true)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?,
        @Parameter(description = "更新文件内容")
        gitOperationFile: GitOperationFile
    ): Result<Boolean>

    @Operation(summary = "删除代码库")
    @DELETE
    @Path("/{projectId}/{repositoryHashId}")
    fun delete(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "代码库哈希ID", required = true)
        @PathParam("repositoryHashId")
        repositoryHashId: String,
        @Parameter(description = "token类型 1：oauth 2:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<Boolean>

    @Operation(summary = "获取版本库文件和目录列表")
    @GET
    @Path("/git/repository/tree/Info")
    fun getGitRepositoryTreeInfo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "仓库id", required = true)
        @QueryParam("repoId")
        repoId: String,
        @Parameter(description = "commit hash值、分支 或 tag，默认：默认分支", required = false)
        @QueryParam("refName")
        refName: String?,
        @Parameter(description = "文件路径", required = false)
        @QueryParam("path")
        path: String?,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<List<GitRepositoryDirItem>?>

    @Operation(summary = "获取授权路径")
    @GET
    @Path("/getAuthUrl")
    fun getAuthUrl(
        @Parameter(description = "参数json串", required = true)
        @QueryParam("authParamJsonStr")
        authParamJsonStr: String
    ): Result<String>

    @Operation(summary = "获取仓库最近一次提交信息")
    @GET
    @Path("/git/repository/recent/commit/info")
    fun getRepoRecentCommitInfo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "仓库id", required = true)
        @QueryParam("repoId")
        repoId: String,
        @Parameter(description = "hash值、分支名或tag", required = true)
        @QueryParam("sha")
        sha: String,
        @Parameter(description = "token类型 1：oauth 2:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<GitCommit?>

    @Operation(summary = "创建tag")
    @POST
    @Path("/git/tag/create")
    fun createGitTag(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "仓库id", required = true)
        @QueryParam("repoId")
        repoId: String,
        @Parameter(description = "tag名称", required = true)
        @QueryParam("tagName")
        tagName: String,
        @Parameter(description = "关联项", required = true)
        @QueryParam("ref")
        ref: String,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<Boolean>
}
