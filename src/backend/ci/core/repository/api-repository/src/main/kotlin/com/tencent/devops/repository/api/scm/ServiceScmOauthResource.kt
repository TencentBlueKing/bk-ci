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

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.scm.enums.CodeSvnRegion
import com.tencent.devops.scm.pojo.CommitCheckRequest
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitCommitReviewInfo
import com.tencent.devops.scm.pojo.GitMrChangeInfo
import com.tencent.devops.scm.pojo.GitMrInfo
import com.tencent.devops.scm.pojo.GitMrReviewInfo
import com.tencent.devops.scm.pojo.GitTagInfo
import com.tencent.devops.scm.pojo.RevisionInfo
import com.tencent.devops.scm.pojo.TapdWorkItem
import com.tencent.devops.scm.pojo.TokenCheckResult
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_SCM_CODE_OAUTH", description = "Service Code Svn resource")
@Path("/service/scm/oauth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface ServiceScmOauthResource {

    @Operation(summary = "Get the repo latest revision")
    @GET
    @Path("/latestRevision")
    fun getLatestRevision(
        @Parameter(description = "项目名称", required = true)
        @QueryParam("projectName")
        projectName: String,
        @Parameter(description = "仓库地址", required = true)
        @QueryParam("url")
        url: String,
        @Parameter(description = "仓库类型", required = true)
        @QueryParam("type")
        type: ScmType,
        @Parameter(description = "branch name", required = false)
        @QueryParam("branchName")
        branchName: String? = null,
        @Parameter(description = "SVN additional path", required = false)
        @QueryParam("additionalPath")
        additionalPath: String? = null,
        @Parameter(description = "privateKey", required = false)
        @QueryParam("privateKey")
        privateKey: String?,
        @Parameter(description = "passPhrase", required = false)
        @QueryParam("passPhrase")
        passPhrase: String?,
        @Parameter(description = "token", required = false)
        @QueryParam("token")
        token: String?,
        @Parameter(description = "仓库区域前缀（只有svn用到）", required = false)
        @QueryParam("region")
        region: CodeSvnRegion?,
        @Parameter(description = "仓库对应的用户名", required = false)
        @QueryParam("userName")
        userName: String?
    ): Result<RevisionInfo>

    @Operation(summary = "List all the branches of repo")
    @GET
    @Path("/branches")
    fun listBranches(
        @Parameter(description = "项目名称", required = true)
        @QueryParam("projectName")
        projectName: String,
        @Parameter(description = "仓库地址", required = true)
        @QueryParam("url")
        url: String,
        @Parameter(description = "仓库类型", required = true)
        @QueryParam("type")
        type: ScmType,
        @Parameter(description = "privateKey", required = true)
        @QueryParam("privateKey")
        privateKey: String?,
        @Parameter(description = "passPhrase", required = false)
        @QueryParam("passPhrase")
        passPhrase: String?,
        @Parameter(description = "token", required = false)
        @QueryParam("token")
        token: String?,
        @Parameter(description = "仓库区域前缀（只有svn用到）", required = false)
        @QueryParam("region")
        region: CodeSvnRegion?,
        @Parameter(description = "仓库对应的用户名", required = false)
        @QueryParam("userName")
        userName: String?,
        @Parameter(description = "搜索条件", required = false)
        @QueryParam("search")
        search: String? = null,
        @Parameter(description = "page", required = true)
        @QueryParam("page")
        page: Int = 1,
        @Parameter(description = "pageSize", required = true)
        @QueryParam("pageSize")
        pageSize: Int = 20
    ): Result<List<String>>

    @Operation(summary = "List all the branches of repo")
    @GET
    @Path("/tags")
    fun listTags(
        @Parameter(description = "项目名称", required = true)
        @QueryParam("projectName")
        projectName: String,
        @Parameter(description = "仓库地址", required = true)
        @QueryParam("url")
        url: String,
        @Parameter(description = "仓库类型", required = true)
        @QueryParam("type")
        type: ScmType,
        @Parameter(description = "token", required = true)
        @QueryParam("token")
        token: String,
        @Parameter(description = "仓库对应的用户名", required = true)
        @QueryParam("userName")
        userName: String,
        @Parameter(description = "搜索条件", required = false)
        @QueryParam("search")
        search: String? = null
    ): Result<List<String>>

    @Operation(summary = "Check if the svn private key and passphrase legal")
    @GET
    @Path("tokenCheck")
    fun checkPrivateKeyAndToken(
        @Parameter(description = "项目名称", required = true)
        @QueryParam("projectName")
        projectName: String,
        @Parameter(description = "仓库地址", required = true)
        @QueryParam("url")
        url: String,
        @Parameter(description = "仓库类型", required = true)
        @QueryParam("type")
        type: ScmType,
        @Parameter(description = "privateKey", required = true)
        @QueryParam("privateKey")
        privateKey: String?,
        @Parameter(description = "passPhrase", required = false)
        @QueryParam("passPhrase")
        passPhrase: String?,
        @Parameter(description = "token", required = false)
        @QueryParam("token")
        token: String?,
        @Parameter(description = "仓库区域前缀（只有svn用到）", required = false)
        @QueryParam("region")
        region: CodeSvnRegion?,
        @Parameter(description = "仓库对应的用户名", required = false)
        @QueryParam("userName")
        userName: String
    ): Result<TokenCheckResult>

    @Operation(summary = "添加Git或者Gitlab WEB hook")
    @POST
    @Path("addWebHook")
    fun addWebHook(
        @Parameter(description = "项目名称", required = true)
        @QueryParam("projectName")
        projectName: String,
        @Parameter(description = "仓库地址", required = true)
        @QueryParam("url")
        url: String,
        @Parameter(description = "仓库类型", required = true)
        @QueryParam("type")
        type: ScmType,
        @Parameter(description = "privateKey", required = true)
        @QueryParam("privateKey")
        privateKey: String?,
        @Parameter(description = "passPhrase", required = false)
        @QueryParam("passPhrase")
        passPhrase: String?,
        @Parameter(description = "token", required = false)
        @QueryParam("token")
        token: String?,
        @Parameter(description = "仓库区域前缀（只有svn用到）", required = false)
        @QueryParam("region")
        region: CodeSvnRegion?,
        @Parameter(description = "仓库对应的用户名", required = true)
        @QueryParam("userName")
        userName: String,
        @Parameter(description = "事件类型", required = false)
        @QueryParam("event")
        event: String?
    ): Result<Boolean>

    @Operation(summary = "添加Git Commit Check")
    @POST
    @Path("addCommitCheck")
    fun addCommitCheck(
        request: CommitCheckRequest
    ): Result<Boolean>

    @Operation(summary = "查询合并请求的代码变更")
    @GET
    @Path("getMergeRequestChangeInfo")
    fun getMergeRequestChangeInfo(
        @Parameter(description = "项目名称", required = true)
        @QueryParam("projectName")
        projectName: String,
        @Parameter(description = "仓库地址", required = true)
        @QueryParam("url")
        url: String,
        @Parameter(description = "仓库类型", required = true)
        @QueryParam("type")
        type: ScmType,
        @Parameter(description = "token", required = true)
        @QueryParam("token")
        token: String?,
        @Parameter(description = "mrId", required = true)
        @QueryParam("mrId")
        mrId: Long
    ): Result<GitMrChangeInfo?>

    @Operation(summary = "查询合并请求的代码变更")
    @GET
    @Path("getMrInfo")
    fun getMrInfo(
        @Parameter(description = "项目名称", required = true)
        @QueryParam("projectName")
        projectName: String,
        @Parameter(description = "仓库地址", required = true)
        @QueryParam("url")
        url: String,
        @Parameter(description = "仓库类型", required = true)
        @QueryParam("type")
        type: ScmType,
        @Parameter(description = "token", required = true)
        @QueryParam("token")
        token: String?,
        @Parameter(description = "mrId", required = true)
        @QueryParam("mrId")
        mrId: Long
    ): Result<GitMrInfo?>

    @Operation(summary = "查询合并请求的代码变更")
    @GET
    @Path("getMrReviewInfo")
    fun getMrReviewInfo(
        @Parameter(description = "项目名称", required = true)
        @QueryParam("projectName")
        projectName: String,
        @Parameter(description = "仓库地址", required = true)
        @QueryParam("url")
        url: String,
        @Parameter(description = "仓库类型", required = true)
        @QueryParam("type")
        type: ScmType,
        @Parameter(description = "token", required = true)
        @QueryParam("token")
        token: String?,
        @Parameter(description = "mrId", required = true)
        @QueryParam("mrId")
        mrId: Long
    ): Result<GitMrReviewInfo?>

    @Operation(summary = "查询合并请求的commit记录")
    @GET
    @Path("getMrCommitList")
    fun getMrCommitList(
        @Parameter(description = "项目名称", required = true)
        @QueryParam("projectName")
        projectName: String,
        @Parameter(description = "仓库地址", required = true)
        @QueryParam("url")
        url: String,
        @Parameter(description = "仓库类型", required = true)
        @QueryParam("type")
        type: ScmType,
        @Parameter(description = "token", required = true)
        @QueryParam("token")
        token: String?,
        @Parameter(description = "mrId", required = true)
        @QueryParam("mrId")
        mrId: Long,
        @Parameter(description = "page", required = true)
        @QueryParam("page")
        page: Int,
        @Parameter(description = "size", required = true)
        @QueryParam("size")
        size: Int
    ): Result<List<GitCommit>>

    @Operation(summary = "查询日常评审的信息")
    @GET
    @Path("getCommitReviewInfo")
    fun getCommitReviewInfo(
        @Parameter(description = "项目名称", required = true)
        @QueryParam("projectName")
        projectName: String,
        @Parameter(description = "仓库地址", required = true)
        @QueryParam("url")
        url: String,
        @Parameter(description = "仓库类型", required = true)
        @QueryParam("type")
        type: ScmType,
        @Parameter(description = "token", required = true)
        @QueryParam("token")
        token: String?,
        @Parameter(description = "commitReviewId", required = true)
        @QueryParam("crId")
        crId: Long
    ): Result<GitCommitReviewInfo?>

    @Operation(summary = "获取指定 TAG")
    @GET
    @Path("getTagInfo")
    fun getTagInfo(
        @Parameter(description = "项目名称", required = true)
        @QueryParam("projectName")
        projectName: String,
        @Parameter(description = "仓库地址", required = true)
        @QueryParam("url")
        url: String,
        @Parameter(description = "仓库类型", required = true)
        @QueryParam("type")
        type: ScmType,
        @Parameter(description = "token", required = true)
        @QueryParam("token")
        token: String?,
        @Parameter(description = "tagName", required = true)
        @QueryParam("tagName")
        tagName: String
    ): Result<GitTagInfo?>

    @Operation(summary = "获取mr关联的tapd单")
    @GET
    @Path("getTapdWorkItems")
    fun getTapdWorkItems(
        @Parameter(description = "项目名称", required = true)
        @QueryParam("projectName")
        projectName: String,
        @Parameter(description = "仓库地址", required = true)
        @QueryParam("url")
        url: String,
        @Parameter(description = "仓库类型", required = true)
        @QueryParam("type")
        type: ScmType,
        @Parameter(description = "token", required = true)
        @QueryParam("token")
        token: String?,
        @Parameter(description = "类型,可选mr,cr,issue")
        @QueryParam("refType")
        refType: String,
        @Parameter(description = "iid,类型对应的iid")
        @QueryParam("iid")
        iid: Long
    ): Result<List<TapdWorkItem>>
}
