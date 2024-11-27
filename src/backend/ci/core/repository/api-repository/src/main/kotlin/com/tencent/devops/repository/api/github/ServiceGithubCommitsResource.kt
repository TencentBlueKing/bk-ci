package com.tencent.devops.repository.api.github

import com.tencent.devops.common.api.auth.AUTH_HEADER_GITHUB_TOKEN
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.sdk.github.request.CompareTwoCommitsRequest
import com.tencent.devops.repository.sdk.github.request.GetCommitRequest
import com.tencent.devops.repository.sdk.github.request.ListCommitRequest
import com.tencent.devops.repository.sdk.github.response.CommitResponse
import com.tencent.devops.repository.sdk.github.response.CompareTwoCommitsResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Tag(name = "SERVICE_COMMITS_GITHUB", description = "服务-github-commits")
@Path("/service/github/commits")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceGithubCommitsResource {

    @Operation(summary = "提交记录列表")
    @POST
    @Path("/listCommits")
    fun listCommits(
        @Parameter(description = "授权token", required = true)
        @HeaderParam(AUTH_HEADER_GITHUB_TOKEN)
        token: String,
        request: ListCommitRequest
    ): Result<List<CommitResponse>>

    @Operation(summary = "获取某个提交记录")
    @POST
    @Path("/getCommit")
    fun getCommit(
        @Parameter(description = "授权token", required = true)
        @HeaderParam(AUTH_HEADER_GITHUB_TOKEN)
        token: String,
        request: GetCommitRequest
    ): Result<CommitResponse?>

    @Operation(summary = "对比两个commit变更记录")
    @POST
    @Path("/compareTwoCommits")
    fun compareTwoCommits(
        @Parameter(description = "授权token", required = true)
        @HeaderParam(AUTH_HEADER_GITHUB_TOKEN)
        token: String,
        request: CompareTwoCommitsRequest
    ): Result<CompareTwoCommitsResponse?>
}
