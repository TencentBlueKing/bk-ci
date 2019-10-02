package com.tencent.devops.scm.api

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.enums.CodeSvnRegion
import com.tencent.devops.scm.pojo.RevisionInfo
import com.tencent.devops.scm.pojo.TokenCheckResult
import com.tencent.devops.scm.pojo.request.CommitCheckRequest
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

@Api(tags = ["SERVICE_SCM_CODE_OAUTH"], description = "Service Code Svn resource")
@Path("/service/scm/oauth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceScmOauthResource {

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
        userName: String?
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
        userName: String
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
        @ApiParam("token", required = false)
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
        event: String?
    ): Result<Boolean>

    @ApiOperation("添加Git Commit Check")
    @POST
    @Path("addCommitCheck")
    fun addCommitCheck(
        request: CommitCheckRequest
    ): Result<Boolean>
}