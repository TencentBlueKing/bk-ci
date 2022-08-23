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

package com.tencent.devops.scm.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.enums.GitCodeBranchesSort
import com.tencent.devops.repository.pojo.enums.GitCodeProjectsOrder
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.repository.pojo.git.GitCodeFileInfo
import com.tencent.devops.repository.pojo.git.GitCodeProjectInfo
import com.tencent.devops.repository.pojo.git.GitCreateFile
import com.tencent.devops.repository.pojo.git.GitMrChangeInfo
import com.tencent.devops.repository.pojo.git.GitMrInfo
import com.tencent.devops.repository.pojo.git.GitMrReviewInfo
import com.tencent.devops.repository.pojo.git.GitProjectInfo
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
import com.tencent.devops.scm.pojo.CommitCheckRequest
import com.tencent.devops.scm.pojo.GitCICommitRef
import com.tencent.devops.scm.pojo.GitCICreateFile
import com.tencent.devops.scm.pojo.GitCIFileCommit
import com.tencent.devops.scm.pojo.GitCIMrInfo
import com.tencent.devops.scm.pojo.GitCIProjectInfo
import com.tencent.devops.scm.pojo.GitCodeGroup
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitFileInfo
import com.tencent.devops.scm.pojo.GitMember
import com.tencent.devops.scm.pojo.GitProjectGroupInfo
import com.tencent.devops.scm.pojo.GitRepositoryDirItem
import com.tencent.devops.scm.pojo.GitRepositoryResp
import com.tencent.devops.scm.pojo.Project
import com.tencent.devops.scm.pojo.TapdWorkItem
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_SCM_GIT"], description = "Service Code GIT resource")
@Path("/service/git/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceGitResource {

    @ApiOperation("获取用户所有git项目")
    @GET
    @Path("/getProject")
    fun getProject(
        @ApiParam("accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @ApiParam("用户id", required = true)
        @QueryParam("userId")
        userId: String
    ): Result<List<Project>>

    @ApiOperation("获取指定项目详细信息")
    @GET
    @Path("/getProjectInfo")
    fun getProjectInfo(
        @ApiParam("accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @ApiParam("工蜂项目id", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: Long
    ): Result<GitCIProjectInfo?>

    @ApiOperation("获取用户所有git项目，分页方式获取")
    @GET
    @Path("/getProjectList")
    fun getProjectList(
        @ApiParam("accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @ApiParam("用户id", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam("第几页", required = true)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数据条数", required = true)
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam("搜索条件，模糊匹配path,name")
        @QueryParam("search")
        search: String? = null,
        @ApiParam("排序字段")
        @QueryParam("orderBy")
        orderBy: GitProjectsOrderBy? = null,
        @ApiParam("排序方式")
        @QueryParam("sort")
        sort: GitSortAscOrDesc? = null,
        @ApiParam("若为true，返回的是当前用户个人namespace下的project，以及owner为当前用户的group下的所有project")
        @QueryParam("owned")
        owned: Boolean? = null,
        @ApiParam("指定最小访问级别，返回的project列表中，当前用户的project访问级别大于或者等于指定值")
        @QueryParam("minAccessLevel")
        minAccessLevel: GitAccessLevelEnum? = null
    ): Result<List<Project>>

    @ApiOperation("获取用户所有git分支")
    @GET
    @Path("/getBranch")
    fun getBranch(
        @ApiParam("accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @ApiParam("用户id", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam("仓库ID", required = true)
        @QueryParam("repository")
        repository: String,
        @ApiParam("第几页", required = true)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数据条数", required = true)
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam("搜索条件", required = true)
        @QueryParam("search")
        search: String?
    ): Result<List<GitBranch>>

    @ApiOperation("获取用户所有git TAG")
    @GET
    @Path("/getTag")
    fun getTag(
        @ApiParam("accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @ApiParam("用户id", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam("仓库ID", required = true)
        @QueryParam("repository")
        repository: String,
        @ApiParam("第几页", required = true)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数据条数", required = true)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<GitTag>>

    @ApiOperation("刷新用户的token")
    @PUT
    @Path("/refreshToken")
    fun refreshToken(
        @ApiParam("用户id", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam("accessToken", required = true)
        accessToken: GitToken
    ): Result<GitToken>

    @ApiOperation("获取授权路径")
    @GET
    @Path("/getAuthUrl")
    fun getAuthUrl(
        @ApiParam("参数json串", required = true)
        @QueryParam("authParamJsonStr")
        authParamJsonStr: String
    ): Result<String>

    @ApiOperation("获取用户的token")
    @GET
    @Path("/getToken")
    fun getToken(
        @ApiParam("用户id", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam("code", required = true)
        @QueryParam("code")
        code: String
    ): Result<GitToken>

    @ApiOperation("获取用户的基本信息")
    @GET
    @Path("/getUserInfoByToken")
    fun getUserInfoByToken(
        @ApiParam("用户id", required = true)
        @QueryParam("token")
        token: String,
        @ApiParam("是否使用access token", required = true)
        @QueryParam("useAccessToken")
        useAccessToken: Boolean = true
    ): Result<GitUserInfo>

    @ApiOperation("获取项目的token")
    @GET
    @Path("/stream/getToken")
    fun getToken(
        @ApiParam("gitProjectId", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: Long
    ): Result<GitToken>

//    @ApiOperation("销毁项目的token")
//    @DELETE
//    @Path("/stream/clearToken")
//    fun clearToken(
//        @ApiParam("token", required = true)
//        @QueryParam("token")
//        token: String
//    ): Result<Boolean>

    @ApiOperation("获取git文件内容")
    @GET
    @Path("/stream/getGitCIFileContent")
    fun getGitCIFileContent(
        @ApiParam(value = "gitProjectId")
        @QueryParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam(value = "文件路径")
        @QueryParam("filePath")
        filePath: String,
        @ApiParam(value = "token")
        @QueryParam("token")
        token: String,
        @ApiParam(value = "提交id 或者 分支")
        @QueryParam("ref")
        ref: String
    ): Result<String>

    @ApiOperation("获取git文件目录列表")
    @GET
    @Path("/stream/getGitCIFileTree")
    fun getGitCIFileTree(
        @ApiParam(value = "gitProjectId")
        @QueryParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam(value = "目录路径")
        @QueryParam("path")
        path: String,
        @ApiParam(value = "token")
        @QueryParam("token")
        token: String,
        @ApiParam(value = "提交id 或者 分支")
        @QueryParam("ref")
        ref: String?,
        @ApiParam(value = "是否支持递归目录结构")
        @QueryParam("recursive")
        recursive: Boolean? = false,
        @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum = TokenTypeEnum.OAUTH
    ): Result<List<GitFileInfo>>

    @ApiOperation("获取mr请求的代码变更")
    @GET
    @Path("/stream/getGitCIMrChanges")
    fun getGitCIMrChanges(
        @ApiParam(value = "gitProjectId")
        @QueryParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam(value = "mergeRequestId")
        @QueryParam("mergeRequestId")
        mergeRequestId: Long,
        @ApiParam(value = "token")
        @QueryParam("token")
        token: String
    ): Result<GitMrChangeInfo>

    @ApiOperation("获取mr请求的信息")
    @GET
    @Path("/stream/getGitCIMrInfo")
    fun getGitCIMrInfo(
        @ApiParam(value = "gitProjectId")
        @QueryParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam(value = "mergeRequestId")
        @QueryParam("mergeRequestId")
        mergeRequestId: Long,
        @ApiParam(value = "token")
        @QueryParam("token")
        token: String
    ): Result<GitCIMrInfo>

    @ApiOperation("获取当前文件的commit记录(用于差异比较)")
    @GET
    @Path("/stream/getFileCommits")
    fun getFileCommits(
        @ApiParam(value = "gitProjectId")
        @QueryParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam(value = "filePath")
        @QueryParam("filePath")
        filePath: String,
        @ApiParam(value = "branch")
        @QueryParam("branch")
        branch: String,
        @ApiParam(value = "token")
        @QueryParam("token")
        token: String
    ): Result<List<GitCIFileCommit>>

    @ApiOperation("获取仓库的所有提交记录")
    @GET
    @Path("/stream/commits")
    fun getCommits(
        @ApiParam(value = "gitProjectId")
        @QueryParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam(value = "filePath")
        @QueryParam("filePath")
        filePath: String?,
        @ApiParam(value = "branch")
        @QueryParam("branch")
        branch: String?,
        @ApiParam(value = "token")
        @QueryParam("token")
        token: String,
        @ApiParam(value = "在这之后的时间的提交")
        @QueryParam("since")
        since: String?,
        @ApiParam(value = "在这之前的时间的提交")
        @QueryParam("until")
        until: String?,
        @ApiParam(value = "页码", defaultValue = "1")
        @QueryParam("page")
        page: Int,
        @ApiParam(value = "每页数量,最大100", defaultValue = "20")
        @QueryParam("perPage")
        perPage: Int,
        @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum = TokenTypeEnum.OAUTH
    ): Result<List<Commit>>

    @ApiOperation("工蜂创建文件")
    @POST
    @Path("/stream/create/file")
    fun gitCICreateFile(
        @ApiParam(value = "gitProjectId")
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @ApiParam(value = "token")
        @QueryParam("token")
        token: String,
        @ApiParam(value = "创建文件内容")
        gitCreateFile: GitCICreateFile
    ): Result<Boolean>

    @ApiOperation("获取当前commit记录所属")
    @GET
    @Path("/stream/commitRefs")
    fun getCommitRefs(
        @ApiParam(value = "gitProjectId")
        @QueryParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam(value = "commitId")
        @QueryParam("commitId")
        commitId: String,
        @ApiParam(value = "branch/tag/all")
        @QueryParam("type")
        type: String,
        @ApiParam(value = "token")
        @QueryParam("token")
        token: String
    ): Result<List<GitCICommitRef>>

    @ApiOperation("获取转发地址")
    @GET
    @Path("/getRedirectUrl")
    fun getRedirectUrl(
        @ApiParam("参数json串", required = true)
        @QueryParam("authParamJsonStr")
        authParamJsonStr: String
    ): Result<String>

    @ApiOperation("获取git文件内容")
    @GET
    @Path("/getGitFileContent")
    fun getGitFileContent(
        @ApiParam(value = "仓库url")
        @QueryParam("repoUrl")
        repoUrl: String? = null,
        @ApiParam(value = "仓库名字")
        @QueryParam("repoName")
        repoName: String,
        @ApiParam(value = "文件路径")
        @QueryParam("filePath")
        filePath: String,
        @ApiParam(value = "认证方式")
        @QueryParam("authType")
        authType: RepoAuthType?,
        @ApiParam(value = "token")
        @QueryParam("token")
        token: String,
        @ApiParam(value = "提交id 或者 分支")
        @QueryParam("ref")
        ref: String
    ): Result<String>

    @ApiOperation("获取gitlab文件内容")
    @GET
    @Path("/getGitlabFileContent")
    fun getGitlabFileContent(
        @ApiParam(value = "仓库url")
        @QueryParam("repoUrl")
        repoUrl: String? = null,
        @ApiParam(value = "仓库名字")
        @QueryParam("repoName")
        repoName: String,
        @ApiParam(value = "文件路径")
        @QueryParam("filePath")
        filePath: String,
        @ApiParam(value = "提交id 或者 分支")
        @QueryParam("ref")
        ref: String,
        @ApiParam(value = "accessToken")
        @QueryParam("accessToken")
        accessToken: String
    ): Result<String>

    @ApiOperation("创建git代码库")
    @POST
    @Path("/createGitCodeRepository")
    fun createGitCodeRepository(
        @ApiParam("用户id", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam("token", required = true)
        @QueryParam("token")
        token: String,
        @ApiParam(value = "代码库名称", required = true)
        @QueryParam("repositoryName")
        repositoryName: String,
        @ApiParam("样例工程路径", required = false)
        @QueryParam("sampleProjectPath")
        sampleProjectPath: String?,
        @ApiParam(value = "命名空间ID", required = false)
        @QueryParam("namespaceId")
        namespaceId: Int?,
        @ApiParam(value = "项目可视范围", required = false)
        @QueryParam("visibilityLevel")
        visibilityLevel: VisibilityLevelEnum?,
        @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum,
        @ApiParam(value = "前端UI渲染方式", required = false)
        @QueryParam("frontendType")
        frontendType: FrontendTypeEnum?
    ): Result<GitRepositoryResp?>

    @ApiOperation("更新git代码库信息")
    @PUT
    @Path("/updateGitCodeRepository")
    fun updateGitCodeRepository(
        @ApiParam("token", required = true)
        @QueryParam("token")
        token: String,
        @ApiParam(value = "项目名称", required = true)
        @QueryParam("projectName")
        projectName: String,
        @ApiParam(value = "更新git项目信息", required = true)
        updateGitProjectInfo: UpdateGitProjectInfo,
        @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<Boolean>

    @ApiOperation("获取版本库文件和目录列表")
    @GET
    @Path("/getGitRepositoryTreeInfo")
    fun getGitRepositoryTreeInfo(
        @ApiParam("用户id", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam(value = "仓库名字", required = true)
        @QueryParam("repoName")
        repoName: String,
        @ApiParam(value = "commit hash值、分支 或 tag，默认：默认分支", required = false)
        @QueryParam("refName")
        refName: String?,
        @ApiParam(value = "文件路径", required = false)
        @QueryParam("path")
        path: String?,
        @ApiParam("token", required = true)
        @QueryParam("token")
        token: String,
        @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<List<GitRepositoryDirItem>?>

    @ApiOperation("把项目迁移到指定项目组下")
    @POST
    @Path("/moveProjectToGroup")
    fun moveProjectToGroup(
        @ApiParam("token", required = true)
        @QueryParam("token")
        token: String,
        @ApiParam(value = "项目组代码", required = true)
        @QueryParam("groupCode")
        groupCode: String,
        @ApiParam(value = "代码库名称", required = true)
        @QueryParam("repositoryName")
        repositoryName: String,
        @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<GitProjectInfo?>

    @ApiOperation("为项目成员赋予代码库权限")
    @POST
    @Path("/addGitProjectMember")
    fun addGitProjectMember(
        @ApiParam("增加的用户列表", required = true)
        @QueryParam("userIdList")
        userIdList: List<String>,
        @ApiParam(value = "代码库命名空间名称", required = true)
        @QueryParam("repositorySpaceName")
        repositorySpaceName: String,
        @ApiParam(value = "git访问权限", required = true)
        @QueryParam("gitAccessLevel")
        gitAccessLevel: GitAccessLevelEnum,
        @ApiParam("token", required = true)
        @QueryParam("token")
        token: String,
        @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<Boolean>

    @ApiOperation("删除项目成员的代码库权限")
    @DELETE
    @Path("/deleteGitProjectMember")
    fun deleteGitProjectMember(
        @ApiParam("删除的用户列表", required = true)
        @QueryParam("userIdList")
        userIdList: List<String>,
        @ApiParam(value = "代码库命名空间名称", required = true)
        @QueryParam("repositorySpaceName")
        repositorySpaceName: String,
        @ApiParam("token", required = true)
        @QueryParam("token")
        token: String,
        @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<Boolean>

    @ApiOperation("删除项目")
    @DELETE
    @Path("/deleteGitProject")
    fun deleteGitProject(
        @ApiParam(value = "代码库命名空间名称", required = true)
        @QueryParam("repositorySpaceName")
        repositorySpaceName: String,
        @ApiParam("token", required = true)
        @QueryParam("token")
        token: String,
        @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<Boolean>

    @ApiOperation("获取mr信息")
    @GET
    @Path("/getMergeRequestInfo")
    fun getMergeRequestInfo(
        @ApiParam(value = "项目唯一标识或NAMESPACE_PATH/PROJECT_PATH", required = true)
        @QueryParam("repoName")
        repoName: String,
        @ApiParam(value = "合并请求的 id", required = true)
        @QueryParam("mrId")
        mrId: Long,
        @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum,
        @ApiParam(value = "token", required = true)
        @QueryParam("token")
        token: String,
        @ApiParam(value = "仓库url", required = true)
        @QueryParam("repoUrl")
        repoUrl: String? = null
    ): Result<GitMrInfo>

    @ApiOperation("下载git仓库")
    @GET
    @Path("/downloadGitRepoFile")
    fun downloadGitRepoFile(
        @ApiParam(value = "项目唯一标识或NAMESPACE_PATH/PROJECT_PATH", required = true)
        @QueryParam("repoName")
        repoName: String,
        @ApiParam("commit hash值、分支名或tag", required = false)
        @QueryParam("sha")
        sha: String?,
        @ApiParam("token", required = true)
        @QueryParam("token")
        token: String,
        @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum,
        @Context
        response: HttpServletResponse
    )

    @ApiOperation("获取mr信息")
    @GET
    @Path("/getMergeRequestReviewersInfo")
    fun getMergeRequestReviewersInfo(
        @ApiParam(value = "项目唯一标识或NAMESPACE_PATH/PROJECT_PATH", required = true)
        @QueryParam("repoName")
        repoName: String,
        @ApiParam(value = "合并请求的 id", required = true)
        @QueryParam("mrId")
        mrId: Long,
        @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum,
        @ApiParam(value = "token", required = true)
        @QueryParam("token")
        token: String,
        @ApiParam(value = "仓库url", required = true)
        @QueryParam("repoUrl")
        repoUrl: String? = null
    ): Result<GitMrReviewInfo>

    @ApiOperation("获取mr信息")
    @GET
    @Path("/getMergeRequestChangeInfo")
    fun getMergeRequestChangeInfo(
        @ApiParam(value = "项目唯一标识或NAMESPACE_PATH/PROJECT_PATH", required = true)
        @QueryParam("repoName")
        repoName: String,
        @ApiParam(value = "合并请求的 id", required = true)
        @QueryParam("mrId")
        mrId: Long,
        @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum,
        @ApiParam(value = "token", required = true)
        @QueryParam("token")
        token: String,
        @ApiParam(value = "仓库url", required = true)
        @QueryParam("repoUrl")
        repoUrl: String? = null
    ): Result<GitMrChangeInfo>

    @ApiOperation("获取项目成员信息")
    @GET
    @Path("/getRepoMembers")
    fun getRepoMembers(
        @ApiParam(value = "项目唯一标识或NAMESPACE_PATH/PROJECT_PATH", required = true)
        @QueryParam("repoName")
        repoName: String,
        @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum,
        @ApiParam(value = "token", required = true)
        @QueryParam("token")
        token: String
    ): Result<List<GitMember>>

    @ApiOperation("获取项目某成员信息")
    @GET
    @Path("/getRepoMemberInfo")
    fun getRepoMemberInfo(
        @ApiParam(value = "token")
        @QueryParam("token")
        token: String,
        @ApiParam("userId", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam("gitProjectId", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<GitMember>

    @ApiOperation("获取所有项目成员信息")
    @GET
    @Path("/getRepoMembers/all")
    fun getRepoAllMembers(
        @ApiParam(value = "项目唯一标识或NAMESPACE_PATH/PROJECT_PATH", required = true)
        @QueryParam("repoName")
        repoName: String,
        @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum,
        @ApiParam(value = "token", required = true)
        @QueryParam("token")
        token: String
    ): Result<List<GitMember>>

    @ApiOperation("添加Git Commit Check")
    @POST
    @Path("/addCommitCheck")
    fun addCommitCheck(
        request: CommitCheckRequest
    ): Result<Boolean>

    @ApiOperation("获取仓库最近一次提交信息")
    @GET
    @Path("/getRepoRecentCommitInfo")
    fun getRepoRecentCommitInfo(
        @ApiParam(value = "项目唯一标识或NAMESPACE_PATH/PROJECT_PATH", required = true)
        @QueryParam("repoName")
        repoName: String,
        @ApiParam(value = "hash值、分支名或tag", required = true)
        @QueryParam("sha")
        sha: String,
        @ApiParam(value = "token", required = true)
        @QueryParam("token")
        token: String,
        @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<GitCommit?>

    @ApiOperation("解锁hook锁")
    @POST
    @Path("/unLockHookLock")
    fun unLockHookLock(
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String? = "",
        @ApiParam(value = "项目唯一标识或NAMESPACE_PATH/PROJECT_PATH", required = true)
        @QueryParam("repoName")
        repoName: String,
        @ApiParam(value = "合并请求的 id", required = true)
        @QueryParam("mrId")
        mrId: Long
    ): Result<Boolean>

    @ApiOperation("创建tag")
    @POST
    @Path("/createGitTag")
    fun createGitTag(
        @ApiParam(value = "项目唯一标识或NAMESPACE_PATH/PROJECT_PATH", required = true)
        @QueryParam("repoName")
        repoName: String,
        @ApiParam(value = "tag名称", required = true)
        @QueryParam("tagName")
        tagName: String,
        @ApiParam(value = "关联项", required = true)
        @QueryParam("ref")
        ref: String,
        @ApiParam("token", required = true)
        @QueryParam("token")
        token: String,
        @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<Boolean>

    @ApiOperation("获取git项目组的详细信息")
    @GET
    @Path("/getProjectGroupInfo")
    fun getProjectGroupInfo(
        @ApiParam(value = "git项目组id", required = true)
        @QueryParam("id")
        id: String,
        @ApiParam(value = "是否包含subgroup项目", required = false)
        @QueryParam("includeSubgroups")
        includeSubgroups: Boolean?,
        @ApiParam(value = "token", required = true)
        @QueryParam("token")
        token: String,
        @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<GitProjectGroupInfo>

    @ApiOperation("获取两次提交的差异文件列表")
    @GET
    @Path("/getChangeFileList")
    fun getChangeFileList(
        @ApiParam(value = "token")
        @QueryParam("token")
        token: String,
        @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum,
        @ApiParam(value = "gitProjectId")
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @ApiParam(value = "旧commit")
        @QueryParam("from")
        from: String,
        @ApiParam(value = "新commit")
        @QueryParam("to")
        to: String,
        @ApiParam(value = "true：两个点比较差异，false：三个点比较差异。默认是 false")
        @QueryParam("straight")
        straight: Boolean? = false,
        @ApiParam(value = "页码")
        @QueryParam("page")
        page: Int,
        @ApiParam(value = "每页大小")
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<List<ChangeFileInfo>>

    @ApiOperation("获取指定项目详细信息")
    @GET
    @Path("/getGitProjectInfo")
    fun getGitProjectInfo(
        @ApiParam(value = "token")
        @QueryParam("token")
        token: String,
        @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum,
        @ApiParam(value = "gitProjectId")
        @QueryParam("gitProjectId")
        gitProjectId: String
    ): Result<GitProjectInfo?>

    @ApiOperation("获取用户所有项目组列表，分页获取")
    @GET
    @Path("/getProjectGroupsList")
    fun getProjectGroupsList(
        @ApiParam("oauth accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @ApiParam("第几页", required = true)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数据条数,最大值100", required = true)
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam("若为true则只返回owner为当前用户的group")
        @QueryParam("owned")
        owned: Boolean?,
        @ApiParam("指定最小访问级别，返回的group列表中，当前用户的group访问级别大于或者等于指定值")
        @QueryParam("minAccessLevel")
        minAccessLevel: GitAccessLevelEnum?,
        @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<List<GitCodeGroup>>

    @ApiOperation("获取GitCode项目成员信息")
    @GET
    @Path("/getMembers")
    fun getMembers(
        @ApiParam("token", required = true)
        @QueryParam("token")
        token: String,
        @ApiParam(value = "项目ID或者全路径", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @ApiParam(value = "page", required = true)
        @QueryParam("page")
        page: Int = 1,
        @ApiParam(value = "pageSize", required = true)
        @QueryParam("pageSize")
        pageSize: Int = 20,
        @ApiParam(value = "搜索用户关键字", required = false)
        @QueryParam("search")
        search: String?,
        @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<List<GitMember>>

    @ApiOperation("校验用户git项目权限")
    @GET
    @Path("/getUserId")
    fun getGitUserId(
        @ApiParam("userId", required = true)
        @QueryParam("userId")
        rtxUserId: String,
        @ApiParam("gitProjectId", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum,
        @ApiParam("token", required = true)
        @QueryParam("token")
        token: String
    ): Result<String?>

    @ApiOperation("获取项目下具有权限的成员信息")
    @GET
    @Path("/projects/members/all")
    fun getProjectMembersAll(
        @ApiParam(value = "gitProjectId")
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @ApiParam(value = "page", required = true)
        @QueryParam("page")
        page: Int = 1,
        @ApiParam(value = "pageSize", required = true)
        @QueryParam("pageSize")
        pageSize: Int = 20,
        @ApiParam(value = "搜索用户关键字", required = true)
        @QueryParam("search")
        search: String?,
        @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum,
        @ApiParam("token", required = true)
        @QueryParam("token")
        token: String
    ): Result<List<GitMember>>

    @ApiOperation("文件内容和一些文件信息")
    @GET
    @Path("/getGitFileInfo")
    fun getGitFileInfo(
        @ApiParam(value = "gitProjectId")
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @ApiParam(value = "文件路径")
        @QueryParam("filePath")
        filePath: String?,
        @ApiParam(value = "token")
        @QueryParam("token")
        token: String,
        @ApiParam(value = "提交id 或者 分支")
        @QueryParam("ref")
        ref: String?,
        @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<GitCodeFileInfo>

    @ApiOperation("添加mr评论")
    @POST
    @Path("/addMrComment")
    fun addMrComment(
        @ApiParam(value = "token")
        @QueryParam("token")
        token: String,
        @ApiParam(value = "gitProjectId")
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @ApiParam(value = "mrId")
        @QueryParam("mrId")
        mrId: Long,
        @ApiParam(value = "mr评论请求体")
        mrBody: String,
        @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    )

    @ApiOperation("获取用户所有git项目，分页方式获取")
    @GET
    @Path("/getGitCodeProjectList")
    fun getGitCodeProjectList(
        @ApiParam("accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @ApiParam("第几页", required = true)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数据条数", required = true)
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam("搜索条件，模糊匹配path,name")
        @QueryParam("search")
        search: String?,
        @ApiParam("排序字段")
        @QueryParam("orderBy")
        orderBy: GitCodeProjectsOrder?,
        @ApiParam("排序方式")
        @QueryParam("sort")
        sort: GitCodeBranchesSort?,
        @ApiParam("若为true，返回的是当前用户个人namespace下的project，以及owner为当前用户的group下的所有project")
        @QueryParam("owned")
        owned: Boolean?,
        @ApiParam("指定最小访问级别，返回的project列表中，当前用户的project访问级别大于或者等于指定值")
        @QueryParam("minAccessLevel")
        minAccessLevel: GitAccessLevelEnum?
    ): Result<List<GitCodeProjectInfo>>

    @ApiOperation("工蜂创建文件")
    @POST
    @Path("/gitcode/create/file")
    fun gitCreateFile(
        @ApiParam(value = "gitProjectId")
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @ApiParam(value = "token")
        @QueryParam("token")
        token: String,
        @ApiParam(value = "创建文件内容")
        gitCreateFile: GitCreateFile,
        @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum
    ): Result<Boolean>

    @ApiOperation("获取mr关联的tapd单")
    @GET
    @Path("/getTapdWorkItems")
    fun getTapdWorkItems(
        @ApiParam("accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @ApiParam(value = "token类型 0：oauth 1:privateKey", required = true)
        @QueryParam("tokenType")
        tokenType: TokenTypeEnum = TokenTypeEnum.OAUTH,
        @ApiParam(value = "gitProjectId")
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @ApiParam(value = "类型,可选mr,cr,issue")
        @QueryParam("type")
        type: String,
        @ApiParam(value = "iid,类型对应的iid")
        @QueryParam("iid")
        iid: Long
    ): Result<List<TapdWorkItem>>
}
