package com.tencent.devops.external.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.external.pojo.GithubBranch
import com.tencent.devops.external.pojo.GithubCheckRuns
import com.tencent.devops.external.pojo.GithubCheckRunsResponse
import com.tencent.devops.external.pojo.GithubOauth
import com.tencent.devops.external.pojo.GithubRepository
import com.tencent.devops.external.pojo.GithubTag
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_GITHUB"], description = "Service-Github接口")
@Path("/service/github/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceGithubResource {

    @ApiOperation("获取Github Oauth信息")
    @GET
    @Path("/oauth")
    fun getOauth(
        @ApiParam(value = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @ApiParam(value = "用户ID", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam(value = "仓库ID", required = true)
        @QueryParam("repoHashId")
        repoHashId: String?
    ): Result<GithubOauth>

    @ApiOperation("获取Github Apps Url")
    @GET
    @Path("/githubAppUrl")
    fun getGithubAppUrl(): Result<String>

    @ApiOperation("获取Github仓库列表")
    @GET
    @Path("/project")
    fun getProject(
        @ApiParam("accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @ApiParam("用户id", required = true)
        @QueryParam("userId")
        userId: String
    ): Result<List<GithubRepository>>

    @ApiOperation("创建github checkRuns")
    @POST
    @Path("/checkRuns")
    fun addCheckRuns(
        @ApiParam("accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @ApiParam("projectName", required = true)
        @QueryParam("projectName")
        projectName: String,
        checkRuns: GithubCheckRuns
    ): Result<GithubCheckRunsResponse>

    @ApiOperation("更新github checkRuns")
    @PUT
    @Path("/checkRuns")
    fun updateCheckRuns(
        @ApiParam("accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @ApiParam("projectName", required = true)
        @QueryParam("projectName")
        projectName: String,
        @ApiParam("checkRunId", required = true)
        @QueryParam("checkRunId")
        checkRunId: Int,
        checkRuns: GithubCheckRuns
    ): Result<Boolean>

    @ApiOperation("获取github文件内容")
    @GET
    @Path("/getFileContent")
    fun getFileContent(
        @ApiParam("projectName", required = true)
        @QueryParam("projectName")
        projectName: String,
        @ApiParam("ref", required = true)
        @QueryParam("ref")
        ref: String,
        @ApiParam("filePath", required = true)
        @QueryParam("filePath")
        filePath: String
    ): Result<String>

    @ApiOperation("获取github指定分支")
    @GET
    @Path("/getGithubBranch")
    fun getGithubBranch(
        @ApiParam("accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @ApiParam("projectName", required = true)
        @QueryParam("projectName")
        projectName: String,
        @ApiParam("branch", required = false)
        @QueryParam("branch")
        branch: String?
    ): Result<GithubBranch?>

    @ApiOperation("获取github指定tag")
    @GET
    @Path("/getGithubTag")
    fun getGithubTag(
        @ApiParam("accessToken", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @ApiParam("projectName", required = true)
        @QueryParam("projectName")
        projectName: String,
        @ApiParam("tag", required = true)
        @QueryParam("tag")
        tag: String
    ): Result<GithubTag?>
}
