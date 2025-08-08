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

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.enums.GitCodeBranchesSort
import com.tencent.devops.repository.pojo.enums.GitCodeProjectsOrder
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.repository.pojo.git.GitCodeFileInfo
import com.tencent.devops.repository.pojo.git.GitCodeProjectInfo
import com.tencent.devops.scm.pojo.GitCreateBranch
import com.tencent.devops.scm.pojo.GitCreateMergeRequest
import com.tencent.devops.repository.pojo.git.GitMrChangeInfo
import com.tencent.devops.repository.pojo.git.GitOperationFile
import com.tencent.devops.repository.pojo.git.GitUserInfo
import com.tencent.devops.repository.pojo.git.UpdateGitProjectInfo
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.scm.code.git.api.GitBranch
import com.tencent.devops.scm.code.git.api.GitTag
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.scm.enums.GitProjectsOrderBy
import com.tencent.devops.scm.enums.GitSortAscOrDesc
import com.tencent.devops.scm.pojo.ChangeFileInfo
import com.tencent.devops.scm.pojo.Commit
import com.tencent.devops.scm.pojo.GitCodeGroup
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitDiff
import com.tencent.devops.scm.pojo.GitFileInfo
import com.tencent.devops.scm.pojo.GitListMergeRequest
import com.tencent.devops.scm.pojo.GitMember
import com.tencent.devops.scm.pojo.GitMrInfo
import com.tencent.devops.scm.pojo.GitMrReviewInfo
import com.tencent.devops.scm.pojo.GitProjectGroupInfo
import com.tencent.devops.scm.pojo.GitProjectInfo
import com.tencent.devops.scm.pojo.GitRepositoryResp
import com.tencent.devops.scm.pojo.Project
import com.tencent.devops.scm.pojo.TapdWorkItem
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.servlet.http.HttpServletResponse
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_SCM_GIT", description = "Service Code GIT resource")
@Path("/service/git/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface ServiceGitResource {

    @Operation(summary = "获取用户所有git项目")
    @GET
    @Path("/getProject")
    fun getProject(
        @Parameter(description = "accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @Parameter(description = "用户id", required = true)
        @QueryParam("userId")
        userId: String
    ): Result<List<Project>>

    @Operation(summary = "获取用户所有git项目，分页方式获取")
    @GET
    @Path("/getProjectList")
    fun getProjectList(
        @Parameter(description = "accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @Parameter(description = "用户id", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "第几页", required = true)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数据条数", required = true)
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(description = "搜索条件，模糊匹配path,name")
        @QueryParam("search")
        search: String? = null,
        @Parameter(description = "排序字段")
        @QueryParam("orderBy")
        orderBy: GitProjectsOrderBy? = null,
        @Parameter(description = "排序方式")
        @QueryParam("sort")
        sort: GitSortAscOrDesc? = null,
        @Parameter(description = "若为true，返回的是当前用户个人namespace下的project，以及owner为当前用户的group下的所有project")
        @QueryParam("owned")
        owned: Boolean? = null,
        @Parameter(description = "指定最小访问级别，返回的project列表中，当前用户的project访问级别大于或者等于指定值")
        @QueryParam("minAccessLevel")
        minAccessLevel: GitAccessLevelEnum? = null
    ): Result<List<Project>>

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

    @Operation(summary = "获取用户所有git TAG")
    @GET
    @Path("/getTag")
    fun getTag(
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
        pageSize: Int?
    ): Result<List<GitTag>>

    @Operation(summary = "刷新用户的token")
    @PUT
    @Path("/refreshToken")
    fun refreshToken(
        @Parameter(description = "用户id", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "accessToken", required = true)
        accessToken: GitToken
    ): Result<GitToken>

    @Operation(summary = "获取授权路径")
    @GET
    @Path("/getAuthUrl")
    fun getAuthUrl(
        @Parameter(description = "参数json串", required = true)
        @QueryParam("authParamJsonStr")
        authParamJsonStr: String
    ): Result<String>

    @Operation(summary = "获取用户的token")
    @GET
    @Path("/getToken")
    fun getToken(
        @Parameter(description = "用户id", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "code", required = true)
        @QueryParam("code")
        code: String
    ): Result<GitToken>

    @Operation(summary = "获取转发地址")
    @GET
    @Path("/getRedirectUrl")
    fun getRedirectUrl(
        @Parameter(description = "参数json串", required = true)
        @QueryParam("authParamJsonStr")
        authParamJsonStr: String
    ): Result<String>

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

    @Operation(summary = "获取gitlab文件内容")
    @GET
    @Path("/getGitlabFileContent")
    fun getGitlabFileContent(
        @Parameter(description = "仓库Url")
        @QueryParam("repoUrl")
        repoUrl: String,
        @Parameter(description = "仓库名字")
        @QueryParam("repoName")
        repoName: String,
        @Parameter(description = "文件路径")
        @QueryParam("filePath")
        filePath: String,
        @Parameter(description = "提交id 或者 分支")
        @QueryParam("ref")
        ref: String,
        @Parameter(description = "accessToken")
        @QueryParam("accessToken")
        accessToken: String
    ): Result<String>

    @Operation(summary = "更新git代码库信息")
    @PUT
    @Path("/updateGitCodeRepository")
    fun updateGitCodeRepository(
        @Parameter(description = "token", required = true)
        @QueryParam("token")
        token: String,
        @Parameter(description = "项目名称", required = true)
        @QueryParam("projectName")
        projectName: String,
        @Parameter(description = "更新git项目信息", required = true)
        updateGitProjectInfo: UpdateGitProjectInfo,
        @Parameter(description = "token类型 1：oauth 2:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<Boolean>

    @Operation(summary = "把项目迁移到指定项目组下")
    @POST
    @Path("/moveProjectToGroup")
    fun moveProjectToGroup(
        @Parameter(description = "token", required = true)
        @QueryParam("token")
        token: String,
        @Parameter(description = "项目组代码", required = true)
        @QueryParam("groupCode")
        groupCode: String,
        @Parameter(description = "代码库名称", required = true)
        @QueryParam("repositoryName")
        repositoryName: String,
        @Parameter(description = "token类型 1：oauth 2:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<GitProjectInfo?>

    @Operation(summary = "创建git代码库")
    @POST
    @Path("/createGitCodeRepository")
    fun createGitCodeRepository(
        @Parameter(description = "用户id", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "token", required = true)
        @QueryParam("token")
        token: String,
        @Parameter(description = "代码库名称", required = true)
        @QueryParam("repositoryName")
        repositoryName: String,
        @Parameter(description = "样例工程路径", required = true)
        @QueryParam("sampleProjectPath")
        sampleProjectPath: String,
        @Parameter(description = "命名空间ID", required = false)
        @QueryParam("namespaceId")
        namespaceId: Int?,
        @Parameter(description = "项目可视范围", required = false)
        @QueryParam("visibilityLevel")
        visibilityLevel: VisibilityLevelEnum?,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum,
        @Parameter(description = "前端UI渲染方式", required = false)
        @QueryParam("frontendType")
        frontendType: FrontendTypeEnum?
    ): Result<GitRepositoryResp?>

    @Operation(summary = "为项目成员赋予代码库权限")
    @POST
    @Path("/addGitProjectMember")
    fun addGitProjectMember(
        @Parameter(description = "增加的用户列表", required = true)
        @QueryParam("userIdList")
        userIdList: List<String>,
        @Parameter(description = "代码库命名空间名称", required = true)
        @QueryParam("repositorySpaceName")
        repositorySpaceName: String,
        @Parameter(description = "git访问权限", required = true)
        @QueryParam("gitAccessLevel")
        gitAccessLevel: GitAccessLevelEnum,
        @Parameter(description = "token", required = true)
        @QueryParam("token")
        token: String,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<Boolean>

    @Operation(summary = "删除项目成员的代码库权限")
    @DELETE
    @Path("/deleteGitProjectMember")
    fun deleteGitProjectMember(
        @Parameter(description = "删除的用户列表", required = true)
        @QueryParam("userIdList")
        userIdList: List<String>,
        @Parameter(description = "代码库命名空间名称", required = true)
        @QueryParam("repositorySpaceName")
        repositorySpaceName: String,
        @Parameter(description = "token", required = true)
        @QueryParam("token")
        token: String,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<Boolean>

    @Operation(summary = "获取mr信息")
    @GET
    @Path("/getMergeRequestInfo")
    fun getMergeRequestInfo(
        @Parameter(description = "项目唯一标识或NAMESPACE_PATH/PROJECT_PATH", required = true)
        @QueryParam("repoName")
        repoName: String,
        @Parameter(description = "合并请求的 id", required = true)
        @QueryParam("mrId")
        mrId: Long,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum,
        @Parameter(description = "token", required = true)
        @QueryParam("token")
        token: String,
        @Parameter(description = "仓库url", required = true)
        @QueryParam("repoUrl")
        repoUrl: String? = null
    ): Result<GitMrInfo>

    @Operation(summary = "下载git仓库文件")
    @GET
    @Path("/downloadGitRepoFile")
    fun downloadGitRepoFile(
        @Parameter(description = "仓库id")
        @QueryParam("repoId")
        repoId: String,
        @Parameter(description = "代码库请求类型", required = true)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?,
        @Parameter(description = "commit hash值、分支名或tag", required = false)
        @QueryParam("sha")
        sha: String?,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum,
        @Parameter(description = "限定为下载指定路径的文件", required = false)
        @QueryParam("filePath")
        filePath: String?,
        @Parameter(description = "支持的 format 格式有:zip、tar、tar.gz、tar.xz、tar.bz2(默认为.zip 格式)", required = false)
        @QueryParam("format")
        format: String?,
        @Parameter(description = "将项目名作为目录打包进去 (默认：false)", required = false)
        @QueryParam("isProjectPathWrapped")
        isProjectPathWrapped: Boolean?,
        @Parameter(description = "项目ID", required = false)
        @QueryParam("projectId")
        projectId: String?,
        @Context
        response: HttpServletResponse
    )

    @Operation(summary = "获取mr信息")
    @GET
    @Path("/getMergeRequestReviewersInfo")
    fun getMergeRequestReviewersInfo(
        @Parameter(description = "项目唯一标识或NAMESPACE_PATH/PROJECT_PATH", required = true)
        @QueryParam("repoName")
        repoName: String,
        @Parameter(description = "合并请求的 id", required = true)
        @QueryParam("mrId")
        mrId: Long,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum,
        @Parameter(description = "token", required = true)
        @QueryParam("token")
        token: String,
        @Parameter(description = "仓库url", required = true)
        @QueryParam("repoUrl")
        repoUrl: String? = null
    ): Result<GitMrReviewInfo>

    @Operation(summary = "获取mr信息")
    @GET
    @Path("/getMergeRequestChangeInfo")
    fun getMergeRequestChangeInfo(
        @Parameter(description = "项目唯一标识或NAMESPACE_PATH/PROJECT_PATH", required = true)
        @QueryParam("repoName")
        repoName: String,
        @Parameter(description = "合并请求的 id", required = true)
        @QueryParam("mrId")
        mrId: Long,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum,
        @Parameter(description = "token", required = true)
        @QueryParam("token")
        token: String,
        @Parameter(description = "仓库url", required = true)
        @QueryParam("repoUrl")
        repoUrl: String? = null
    ): Result<GitMrChangeInfo>

    @Operation(summary = "获取仓库最近一次提交信息")
    @GET
    @Path("/getRepoRecentCommitInfo")
    fun getRepoRecentCommitInfo(
        @Parameter(description = "项目唯一标识或NAMESPACE_PATH/PROJECT_PATH", required = true)
        @QueryParam("repoName")
        repoName: String,
        @Parameter(description = "hash值、分支名或tag", required = true)
        @QueryParam("sha")
        sha: String,
        @Parameter(description = "token", required = true)
        @QueryParam("token")
        token: String,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<GitCommit?>

    @Operation(summary = "解锁hook锁")
    @POST
    @Path("/unLockHookLock")
    fun unLockHookLock(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String? = "",
        @Parameter(description = "项目唯一标识或NAMESPACE_PATH/PROJECT_PATH", required = true)
        @QueryParam("repoName")
        repoName: String,
        @Parameter(description = "合并请求的 id", required = true)
        @QueryParam("mrId")
        mrId: Long
    ): Result<Boolean>

    @Operation(summary = "获取git项目组的详细信息")
    @GET
    @Path("/getProjectGroupInfo")
    fun getProjectGroupInfo(
        @Parameter(description = "git项目组id", required = true)
        @QueryParam("id")
        id: String,
        @Parameter(description = "是否包含subgroup项目", required = false)
        @QueryParam("includeSubgroups")
        includeSubgroups: Boolean?,
        @Parameter(description = "token", required = true)
        @QueryParam("token")
        token: String,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<GitProjectGroupInfo>

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
        pageSize: Int
    ): Result<List<ChangeFileInfo>>

    @Operation(summary = "获取指定项目详细信息")
    @GET
    @Path("/getProjectInfo")
    fun getProjectInfo(
        @Parameter(description = "token")
        @QueryParam("token")
        token: String,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum,
        @Parameter(description = "gitProjectId")
        @QueryParam("gitProjectId")
        gitProjectId: String
    ): Result<GitProjectInfo?>

    @Operation(summary = "获取某仓库某成员的信息")
    @GET
    @Path("/checkUserGitAuth")
    fun getProjectUserInfo(
        @Parameter(description = "token")
        @QueryParam("token")
        token: String,
        @Parameter(description = "userId", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "gitProjectId", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<GitMember>

    @Operation(summary = "获取用户所有项目组列表，分页获取")
    @GET
    @Path("/getProjectGroupsList")
    fun getProjectGroupsList(
        @Parameter(description = "oauth accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @Parameter(description = "第几页", required = true)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数据条数,最大值100", required = true)
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(description = "若为true则只返回owner为当前用户的group")
        @QueryParam("owned")
        owned: Boolean?,
        @Parameter(description = "指定最小访问级别，返回的group列表中，当前用户的group访问级别大于或者等于指定值")
        @QueryParam("minAccessLevel")
        minAccessLevel: GitAccessLevelEnum?,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<List<GitCodeGroup>>

    @Operation(summary = "获取GitCode项目成员信息")
    @GET
    @Path("/getMembers")
    fun getMembers(
        @Parameter(description = "token", required = true)
        @QueryParam("token")
        token: String,
        @Parameter(description = "项目ID或者全路径", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @Parameter(description = "page", required = true)
        @QueryParam("page")
        page: Int = 1,
        @Parameter(description = "pageSize", required = true)
        @QueryParam("pageSize")
        pageSize: Int = 20,
        @Parameter(description = "搜索用户关键字", required = false)
        @QueryParam("search")
        search: String?,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<List<GitMember>>

    @Operation(summary = "校验用户git项目权限")
    @GET
    @Path("/getUserId")
    fun getGitUserId(
        @Parameter(description = "userId", required = true)
        @QueryParam("userId")
        rtxUserId: String,
        @Parameter(description = "gitProjectId", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum,
        @Parameter(description = "token", required = true)
        @QueryParam("token")
        token: String
    ): Result<String?>

    @Operation(summary = "获取项目下具有权限的成员信息")
    @GET
    @Path("/projects/members/all")
    fun getProjectMembersAll(
        @Parameter(description = "gitProjectId")
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @Parameter(description = "page", required = true)
        @QueryParam("page")
        page: Int = 1,
        @Parameter(description = "pageSize", required = true)
        @QueryParam("pageSize")
        pageSize: Int = 20,
        @Parameter(description = "搜索用户关键字", required = false)
        @QueryParam("search")
        search: String?,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum,
        @Parameter(description = "token", required = true)
        @QueryParam("token")
        token: String
    ): Result<List<GitMember>>

    @Operation(summary = "文件内容和一些文件信息")
    @GET
    @Path("/getGitFileInfo")
    fun getGitFileInfo(
        @Parameter(description = "gitProjectId")
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @Parameter(description = "文件路径")
        @QueryParam("filePath")
        filePath: String?,
        @Parameter(description = "token")
        @QueryParam("token")
        token: String,
        @Parameter(description = "提交id 或者 分支")
        @QueryParam("ref")
        ref: String?,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<GitCodeFileInfo>

    @Operation(summary = "添加mr评论")
    @POST
    @Path("/addMrComment")
    fun addMrComment(
        @Parameter(description = "token")
        @QueryParam("token")
        token: String,
        @Parameter(description = "gitProjectId")
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @Parameter(description = "mrId")
        @QueryParam("mrId")
        mrId: Long,
        @Parameter(description = "mr评论请求体")
        mrBody: String,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    )

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

    @Operation(summary = "获取仓库的所有提交记录")
    @GET
    @Path("/stream/commits")
    fun getCommits(
        @Parameter(description = "gitProjectId")
        @QueryParam("gitProjectId")
        gitProjectId: Long,
        @Parameter(description = "filePath")
        @QueryParam("filePath")
        filePath: String?,
        @Parameter(description = "branch")
        @QueryParam("branch")
        branch: String?,
        @Parameter(description = "token")
        @QueryParam("token")
        token: String,
        @Parameter(description = "在这之后的时间的提交")
        @QueryParam("since")
        since: String?,
        @Parameter(description = "在这之前的时间的提交")
        @QueryParam("until")
        until: String?,
        @Parameter(description = "页码", example = "1")
        @QueryParam("page")
        page: Int,
        @Parameter(description = "每页数量,最大100", example = "20")
        @QueryParam("perPage")
        perPage: Int,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<List<Commit>>

    @Operation(summary = "开启git仓库ci")
    @GET
    @Path("/stream/gitEnableCi")
    fun enableCi(
        @Parameter(description = "仓库id或编码过的仓库path")
        @QueryParam("projectName")
        projectName: String,
        @QueryParam("token")
        token: String,
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum,
        @QueryParam("enable")
        enable: Boolean? = true
    ): Result<Boolean>

    @Operation(summary = "工蜂创建文件")
    @POST
    @Path("/gitcode/create/file")
    fun gitCreateFile(
        @Parameter(description = "gitProjectId")
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @Parameter(description = "token")
        @QueryParam("token")
        token: String,
        @Parameter(description = "创建文件内容")
        gitOperationFile: GitOperationFile,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<Boolean>

    @Operation(summary = "工蜂修改文件")
    @POST
    @Path("/gitcode/update/file")
    fun gitUpdateFile(
        @Parameter(description = "gitProjectId")
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @Parameter(description = "token")
        @QueryParam("token")
        token: String,
        @Parameter(description = "创建文件内容")
        gitOperationFile: GitOperationFile,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<Boolean>

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

    @Operation(summary = "获取用户的基本信息")
    @GET
    @Path("/getUserInfoById")
    fun getUserInfoById(
        @Parameter(description = "用户id", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "token", required = true)
        @QueryParam("token")
        token: String,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum = TokenTypeEnum.OAUTH
    ): Result<GitUserInfo>

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

    @Operation(summary = "获取mr关联的tapd单")
    @GET
    @Path("/getTapdWorkItems")
    fun getTapdWorkItems(
        @Parameter(description = "accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum = TokenTypeEnum.OAUTH,
        @Parameter(description = "gitProjectId")
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @Parameter(description = "类型,可选mr,cr,issue")
        @QueryParam("type")
        type: String,
        @Parameter(description = "iid,类型对应的iid")
        @QueryParam("iid")
        iid: Long
    ): Result<List<TapdWorkItem>>

    @Operation(summary = "获得某次commit的文件变更信息")
    @GET
    @Path("/get_commit_diff")
    fun getCommitDiff(
        @Parameter(description = "accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum = TokenTypeEnum.OAUTH,
        @Parameter(description = "项目 ID 或 项目全路径 project_full_path")
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @Parameter(description = "commit hash 值、分支名或 tag")
        @QueryParam("sha")
        sha: String,
        @Parameter(description = "文件路径")
        @QueryParam("path")
        path: String?,
        @Parameter(description = "有差异的内容是否忽略空白符，默认不忽略")
        @QueryParam("ignore_white_space")
        ignoreWhiteSpace: Boolean?
    ): Result<List<GitDiff>>

    @Operation(summary = "创建分支")
    @POST
    @Path("/createBranch")
    fun createBranch(
        @Parameter(description = "token", required = true)
        @QueryParam("token")
        token: String,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum = TokenTypeEnum.OAUTH,
        @Parameter(description = "项目 ID 或 项目全路径 project_full_path")
        @QueryParam("gitProjectId")
        gitProjectId: String,
        gitCreateBranch: GitCreateBranch
    ): Result<Boolean>

    @Operation(summary = "获取合并请求列表")
    @POST
    @Path("/listMergeRequest")
    fun listMergeRequest(
        @Parameter(description = "token", required = true)
        @QueryParam("token")
        token: String,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum = TokenTypeEnum.OAUTH,
        @Parameter(description = "项目 ID 或 项目全路径 project_full_path")
        @QueryParam("gitProjectId")
        gitProjectId: String,
        gitListMergeRequest: GitListMergeRequest
    ): Result<List<GitMrInfo>>

    @Operation(summary = "创建合并请求")
    @POST
    @Path("/createMergeRequest")
    fun createMergeRequest(
        @Parameter(description = "token", required = true)
        @QueryParam("token")
        token: String,
        @Parameter(description = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum = TokenTypeEnum.OAUTH,
        @Parameter(description = "项目 ID 或 项目全路径 project_full_path")
        @QueryParam("gitProjectId")
        gitProjectId: String,
        gitCreateMergeRequest: GitCreateMergeRequest
    ): Result<GitMrInfo>
}
