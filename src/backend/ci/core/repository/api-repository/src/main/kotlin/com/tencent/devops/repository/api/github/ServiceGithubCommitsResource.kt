package com.tencent.devops.repository.api.github

import com.tencent.devops.common.api.auth.AUTH_HEADER_GITHUB_TOKEN
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.sdk.github.request.CompareTwoCommitsRequest
import com.tencent.devops.common.sdk.github.request.GetCommitRequest
import com.tencent.devops.common.sdk.github.request.ListCommitRequest
import com.tencent.devops.common.sdk.github.response.CommitResponse
import com.tencent.devops.common.sdk.github.response.CompareTwoCommitsResponse
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_COMMITS_GITHUB"], description = "服务-github-commits")
@Path("/service/github/commits")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceGithubCommitsResource {

    @ApiOperation("提交记录列表")
    @POST
    @Path("/listCommits")
    fun listCommits(
        @ApiParam("授权token", required = true)
        @HeaderParam(AUTH_HEADER_GITHUB_TOKEN)
        token: String,
        request: ListCommitRequest
    ): Result<List<CommitResponse>>

    @ApiOperation("获取某个提交记录")
    @POST
    @Path("/getCommit")
    fun getCommit(
        @ApiParam("授权token", required = true)
        @HeaderParam(AUTH_HEADER_GITHUB_TOKEN)
        token: String,
        request: GetCommitRequest
    ): Result<CommitResponse?>

    @ApiOperation("对比两个commit变更记录")
    @POST
    @Path("/compareTwoCommits")
    fun compareTwoCommits(
        @ApiParam("授权token", required = true)
        @HeaderParam(AUTH_HEADER_GITHUB_TOKEN)
        token: String,
        request: CompareTwoCommitsRequest
    ): Result<CompareTwoCommitsResponse?>
}
