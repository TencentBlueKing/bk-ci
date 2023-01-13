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

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.scm.enums.CodeSvnRegion
import com.tencent.devops.scm.pojo.CommitCheckRequest
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitMrChangeInfo
import com.tencent.devops.scm.pojo.GitMrInfo
import com.tencent.devops.scm.pojo.GitMrReviewInfo
import com.tencent.devops.scm.pojo.RepositoryProjectInfo
import com.tencent.devops.scm.pojo.RevisionInfo
import com.tencent.devops.scm.pojo.TokenCheckResult
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_SCM_CODE"], description = "仓库分支、tag、hook相关")
@Path("/service/scm/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceScmResource {

    @ApiOperation("Get the repo latest revision")
    @GET
    @Path("/latestRevision")
    fun getLatestRevision(
        @ApiParam("项目名称", required = true)
        @QueryParam("projectName")
        projectName: String,
        @ApiParam("仓库地址", required = true)
        @QueryParam("url")
        url: String,
        @ApiParam("仓库类型", required = true)
        @QueryParam("type")
        type: ScmType,
        @ApiParam("branch name", required = false)
        @QueryParam("branchName")
        branchName: String? = null,
        @ApiParam("SVN additional path", required = false)
        @QueryParam("additionalPath")
        additionalPath: String? = null,
        @ApiParam("privateKey", required = false)
        @QueryParam("privateKey")
        privateKey: String?,
        @ApiParam("passPhrase", required = false)
        @QueryParam("passPhrase")
        passPhrase: String?,
        @ApiParam("token", required = false)
        @QueryParam("token")
        token: String?,
        @ApiParam("仓库区域前缀（只有svn用到）", required = false)
        @QueryParam("region")
        region: CodeSvnRegion?,
        @ApiParam("仓库对应的用户名", required = false)
        @QueryParam("userName")
        userName: String?
    ): Result<RevisionInfo>

    @ApiOperation("List all the branches of repo")
    @GET
    @Path("/branches")
    fun listBranches(
        @ApiParam("项目名称", required = true)
        @QueryParam("projectName")
        projectName: String,
        @ApiParam("仓库地址", required = true)
        @QueryParam("url")
        url: String,
        @ApiParam("仓库类型", required = true)
        @QueryParam("type")
        type: ScmType,
        @ApiParam("privateKey", required = true)
        @QueryParam("privateKey")
        privateKey: String?,
        @ApiParam("passPhrase", required = false)
        @QueryParam("passPhrase")
        passPhrase: String?,
        @ApiParam("token", required = false)
        @QueryParam("token")
        token: String?,
        @ApiParam("仓库区域前缀（只有svn用到）", required = false)
        @QueryParam("region")
        region: CodeSvnRegion?,
        @ApiParam("仓库对应的用户名", required = false)
        @QueryParam("userName")
        userName: String?,
        @ApiParam("搜索条件", required = false)
        @QueryParam("search")
        search: String? = null,
        @ApiParam(value = "page", required = true)
        @QueryParam("page")
        page: Int = 1,
        @ApiParam(value = "pageSize", required = true)
        @QueryParam("pageSize")
        pageSize: Int = 20
    ): Result<List<String>>

    @ApiOperation("List all the branches of repo")
    @GET
    @Path("/tags")
    fun listTags(
        @ApiParam("项目名称", required = true)
        @QueryParam("projectName")
        projectName: String,
        @ApiParam("仓库地址", required = true)
        @QueryParam("url")
        url: String,
        @ApiParam("仓库类型", required = true)
        @QueryParam("type")
        type: ScmType,
        @ApiParam("token", required = true)
        @QueryParam("token")
        token: String,
        @ApiParam("仓库对应的用户名", required = true)
        @QueryParam("userName")
        userName: String,
        @ApiParam("搜索条件", required = false)
        @QueryParam("search")
        search: String? = null
    ): Result<List<String>>

    @ApiOperation("Check if the svn private key and passphrase legal")
    @GET
    @Path("tokenCheck")
    fun checkPrivateKeyAndToken(
        @ApiParam("项目名称", required = true)
        @QueryParam("projectName")
        projectName: String,
        @ApiParam("仓库地址", required = true)
        @QueryParam("url")
        url: String,
        @ApiParam("仓库类型", required = true)
        @QueryParam("type")
        type: ScmType,
        @ApiParam("privateKey", required = true)
        @QueryParam("privateKey")
        privateKey: String?,
        @ApiParam("passPhrase", required = false)
        @QueryParam("passPhrase")
        passPhrase: String?,
        @ApiParam("token", required = false)
        @QueryParam("token")
        token: String?,
        @ApiParam("仓库区域前缀（只有svn用到）", required = false)
        @QueryParam("region")
        region: CodeSvnRegion?,
        @ApiParam("仓库对应的用户名", required = false)
        @QueryParam("userName")
        userName: String
    ): Result<TokenCheckResult>

    @ApiOperation("Check if the svn private key and passphrase legal")
    @GET
    @Path("usernameAndPasswordCheck")
    fun checkUsernameAndPassword(
        @ApiParam("项目名称", required = true)
        @QueryParam("projectName")
        projectName: String,
        @ApiParam("仓库地址", required = true)
        @QueryParam("url")
        url: String,
        @ApiParam("仓库类型", required = true)
        @QueryParam("type")
        type: ScmType,
        @ApiParam("username", required = true)
        @QueryParam("username")
        username: String,
        @ApiParam("password", required = false)
        @QueryParam("password")
        password: String,
        @ApiParam("token", required = false)
        @QueryParam("token")
        token: String,
        @ApiParam("仓库区域前缀（只有svn用到）", required = false)
        @QueryParam("region")
        region: CodeSvnRegion?,
        @ApiParam("仓库对应的用户名", required = false)
        @QueryParam("repoUsername")
        repoUsername: String
    ): Result<TokenCheckResult>

    @ApiOperation("添加Git或者Gitlab WEB hook")
    @POST
    @Path("addWebHook")
    fun addWebHook(
        @ApiParam("项目名称", required = true)
        @QueryParam("projectName")
        projectName: String,
        @ApiParam("仓库地址", required = true)
        @QueryParam("url")
        url: String,
        @ApiParam("仓库类型", required = true)
        @QueryParam("type")
        type: ScmType,
        @ApiParam("privateKey", required = true)
        @QueryParam("privateKey")
        privateKey: String?,
        @ApiParam("passPhrase", required = false)
        @QueryParam("passPhrase")
        passPhrase: String?,
        @ApiParam("token", required = true)
        @QueryParam("token")
        token: String?,
        @ApiParam("仓库区域前缀（只有svn用到）", required = false)
        @QueryParam("region")
        region: CodeSvnRegion?,
        @ApiParam("仓库对应的用户名", required = true)
        @QueryParam("userName")
        userName: String,
        @ApiParam("事件类型", required = false)
        @QueryParam("event")
        event: String?,
        @ApiParam("回调url", required = false)
        @QueryParam("hookUrl")
        hookUrl: String? = null
    ): Result<Boolean>

    @ApiOperation("添加Git Commit Check")
    @POST
    @Path("addCommitCheck")
    fun addCommitCheck(
        request: CommitCheckRequest
    ): Result<Boolean>

    @ApiOperation("lock svn")
    @POST
    @Path("lock")
    fun lock(
        @ApiParam("项目ID", required = true)
              @QueryParam("projectId")
              projectName: String,
        @ApiParam("仓库地址", required = true)
              @QueryParam("url")
              url: String,
        @ApiParam("仓库类型", required = true)
              @QueryParam("type")
              type: ScmType,
        @ApiParam("仓库区域前缀（只有svn用到）", required = false)
              @QueryParam("region")
              region: CodeSvnRegion?,
        @ApiParam("仓库对应的用户名", required = true)
              @QueryParam("userName")
              userName: String
    ): Result<Boolean>

    @ApiOperation("lock svn")
    @POST
    @Path("unlock")
    fun unlock(
        @ApiParam("项目名称", required = true)
        @QueryParam("projectName")
        projectName: String,
        @ApiParam("仓库地址", required = true)
        @QueryParam("url")
        url: String,
        @ApiParam("仓库类型", required = true)
        @QueryParam("type")
        type: ScmType,
        @ApiParam("仓库区域前缀（只有svn用到）", required = false)
        @QueryParam("region")
        region: CodeSvnRegion?,
        @ApiParam("仓库对应的用户名", required = true)
        @QueryParam("userName")
        userName: String
    ): Result<Boolean>

    @ApiOperation("查询合并请求的代码变更")
    @GET
    @Path("getMergeRequestChangeInfo")
    fun getMergeRequestChangeInfo(
        @ApiParam("项目名称", required = true)
        @QueryParam("projectName")
        projectName: String,
        @ApiParam("仓库地址", required = true)
        @QueryParam("url")
        url: String,
        @ApiParam("仓库类型", required = true)
        @QueryParam("type")
        type: ScmType,
        @ApiParam("token", required = true)
        @QueryParam("token")
        token: String?,
        @ApiParam("mrId", required = true)
        @QueryParam("mrId")
        mrId: Long
    ): Result<GitMrChangeInfo?>

    @ApiOperation("查询合并请求的代码变更")
    @GET
    @Path("getMrInfo")
    fun getMrInfo(
        @ApiParam("项目名称", required = true)
        @QueryParam("projectName")
        projectName: String,
        @ApiParam("仓库地址", required = true)
        @QueryParam("url")
        url: String,
        @ApiParam("仓库类型", required = true)
        @QueryParam("type")
        type: ScmType,
        @ApiParam("token", required = true)
        @QueryParam("token")
        token: String?,
        @ApiParam("mrId", required = true)
        @QueryParam("mrId")
        mrId: Long
    ): Result<GitMrInfo?>

    @ApiOperation("查询合并请求的代码变更")
    @GET
    @Path("getMrReviewInfo")
    fun getMrReviewInfo(
        @ApiParam("项目名称", required = true)
        @QueryParam("projectName")
        projectName: String,
        @ApiParam("仓库地址", required = true)
        @QueryParam("url")
        url: String,
        @ApiParam("仓库类型", required = true)
        @QueryParam("type")
        type: ScmType,
        @ApiParam("token", required = true)
        @QueryParam("token")
        token: String?,
        @ApiParam("mrId", required = true)
        @QueryParam("mrId")
        mrId: Long
    ): Result<GitMrReviewInfo?>

    @ApiOperation("查询合并请求的提交记录")
    @GET
    @Path("getMrCommitList")
    fun getMrCommitList(
        @ApiParam("项目名称", required = true)
        @QueryParam("projectName")
        projectName: String,
        @ApiParam("仓库地址", required = true)
        @QueryParam("url")
        url: String,
        @ApiParam("仓库类型", required = true)
        @QueryParam("type")
        type: ScmType,
        @ApiParam("token", required = true)
        @QueryParam("token")
        token: String?,
        @ApiParam("mrId", required = true)
        @QueryParam("mrId")
        mrId: Long,
        @ApiParam("page", required = true)
        @QueryParam("page")
        page: Int,
        @ApiParam("size", required = true)
        @QueryParam("size")
        size: Int
    ): Result<List<GitCommit>>

    @ApiOperation("获取项目详情")
    @GET
    @Path("getProjectInfo")
    fun getProjectInfo(
        @ApiParam("项目名称", required = true)
        @QueryParam("projectName")
        projectName: String,
        @ApiParam("仓库地址", required = true)
        @QueryParam("url")
        url: String,
        @ApiParam("仓库类型", required = true)
        @QueryParam("type")
        type: ScmType,
        @ApiParam("token", required = true)
        @QueryParam("token")
        token: String?
    ): Result<RepositoryProjectInfo>
}
