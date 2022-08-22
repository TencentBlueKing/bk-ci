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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.scm.pojo.ChangeFileInfo
import com.tencent.devops.scm.pojo.GitCIProjectInfo
import com.tencent.devops.scm.pojo.GitCodeBranchesOrder
import com.tencent.devops.scm.pojo.GitCodeBranchesSort
import com.tencent.devops.scm.pojo.GitCodeFileInfo
import com.tencent.devops.scm.pojo.GitCodeGroup
import com.tencent.devops.scm.pojo.GitCodeProjectInfo
import com.tencent.devops.scm.pojo.GitCodeProjectsOrder
import com.tencent.devops.scm.pojo.GitMember
import com.tencent.devops.scm.pojo.GitMrChangeInfo
import com.tencent.devops.scm.pojo.MrCommentBody
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_SCM_GIT_CI"], description = "Service Code GIT CI resource")
@Path("/service/gitci/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceGitCiResource {

    @ApiOperation("获取项目的超级token")
    @GET
    @Path("/getToken")
    fun getToken(
        @ApiParam("gitProjectId", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: String
    ): Result<GitToken>

    @ApiOperation("校验用户git项目权限")
    @GET
    @Path("/checkUserGitAuth")
    fun checkUserGitAuth(
        @ApiParam("userId", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam("gitProjectId", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @ApiParam("accessLevel", required = true)
        @QueryParam("accessLevel")
        accessLevel: Int,
        @ApiParam("privateToken", required = false)
        @QueryParam("privateToken")
        privateToken: String? = null,
        @ApiParam("useAccessToken", required = false)
        @QueryParam("useAccessToken")
        useAccessToken: Boolean = true
    ): Result<Boolean>

    @ApiOperation("销毁项目的超级token")
    @DELETE
    @Path("/clearToken")
    fun clearToken(
        @ApiParam("token", required = true)
        @QueryParam("token")
        token: String
    ): Result<Boolean>

    @ApiOperation("刷新项目的token")
    @GET
    @Path("/refreshToken")
    fun refreshToken(
        @ApiParam(value = "项目ID或者全路径", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @QueryParam("refreshToken")
        @ApiParam("refreshToken", required = true)
        token: String
    ): Result<GitToken>

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
        search: String?
    ): Result<List<GitMember>>

    @ApiOperation("获取项目分支信息")
    @GET
    @Path("/getBranches")
    fun getBranches(
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
        @ApiParam(value = "搜索用户关键字", required = true)
        @QueryParam("search")
        search: String?,
        @ApiParam(value = "返回列表的排序字段,可选可选字段:name、updated")
        @QueryParam("orderBy")
        orderBy: GitCodeBranchesOrder?,
        @ApiParam(value = "返回列表的排序字段,可选可选字段:name、updated")
        @QueryParam("sort")
        sort: GitCodeBranchesSort?
    ): Result<List<String>>

    @ApiOperation("校验用户git项目权限")
    @GET
    @Path("/getUserId")
    fun getGitUserId(
        @ApiParam("userId", required = true)
        @QueryParam("userId")
        rtxUserId: String,
        @ApiParam("gitProjectId", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: String
    ): Result<String?>

    @ApiOperation("获取指定项目详细信息(简略)")
    @GET
    @Path("/getProjectInfo")
    fun getProjectInfo(
        @ApiParam("accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @ApiParam("工蜂项目id", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @ApiParam("是否使用accessToken", required = true)
        @QueryParam("useAccessToken")
        useAccessToken: Boolean
    ): Result<GitCIProjectInfo?>

    @ApiOperation("获取git文件内容")
    @GET
    @Path("/gitci/getGitCIFileContent")
    fun getGitCIFileContent(
        @ApiParam(value = "gitProjectId")
        @QueryParam("gitProjectId")
        gitProjectId: String,
        @ApiParam(value = "文件路径")
        @QueryParam("filePath")
        filePath: String,
        @ApiParam(value = "token")
        @QueryParam("token")
        token: String,
        @ApiParam(value = "提交id 或者 分支")
        @QueryParam("ref")
        ref: String,
        @ApiParam("是否使用accessToken", required = true)
        @QueryParam("useAccessToken")
        useAccessToken: Boolean
    ): Result<String>

    @ApiOperation("获取工蜂项目详细信息(使用超级token)")
    @GET
    @Path("/getGitCodeProjectInfo")
    fun getGitCodeProjectInfo(
        @ApiParam("工蜂项目id", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: String
    ): Result<GitCodeProjectInfo?>

    @ApiOperation("查询合并请求的代码变更")
    @GET
    @Path("getMergeRequestChangeInfo")
    fun getMergeRequestChangeInfo(
        @ApiParam("工蜂项目id", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam("token", required = true)
        @QueryParam("token")
        token: String?,
        @ApiParam("mrId", required = true)
        @QueryParam("mrId")
        mrId: Long
    ): Result<GitMrChangeInfo?>

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
        search: String?
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
        @ApiParam("是否使用accessToken", required = true)
        @QueryParam("useAccessToken")
        useAccessToken: Boolean
    ): Result<GitCodeFileInfo>

    @ApiOperation("获取两次提交的差异文件列表")
    @GET
    @Path("/getCommitChangeFileList")
    fun getCommitChangeFileList(
        @ApiParam(value = "token")
        @QueryParam("token")
        token: String,
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
        pageSize: Int,
        @ApiParam("是否使用accessToken", required = true)
        @QueryParam("useAccessToken")
        @DefaultValue("true")
        useAccessToken: Boolean
    ): Result<List<ChangeFileInfo>>

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
        mrBody: MrCommentBody
    )

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
        minAccessLevel: GitAccessLevelEnum?
    ): Result<List<GitCodeGroup>>
}
