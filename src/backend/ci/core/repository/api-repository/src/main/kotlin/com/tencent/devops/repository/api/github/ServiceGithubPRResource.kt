package com.tencent.devops.repository.api.github

import com.tencent.devops.common.api.auth.AUTH_HEADER_GITHUB_TOKEN
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.sdk.github.request.GetPullRequestRequest
import com.tencent.devops.repository.sdk.github.request.ListPullRequestFileRequest
import com.tencent.devops.repository.sdk.github.response.PullRequestFileResponse
import com.tencent.devops.repository.sdk.github.response.PullRequestResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Tag(name = "SERVICE_PULL_REQUEST_GITHUB", description = "服务-github-pull-request")
@Path("/service/github/pullRequest")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceGithubPRResource {

    @Operation(summary = "获取PR")
    @POST
    @Path("/getPullRequest")
    fun getPullRequest(
        @Parameter(description = "授权token", required = true)
        @HeaderParam(AUTH_HEADER_GITHUB_TOKEN)
        token: String,
        request: GetPullRequestRequest
    ): Result<PullRequestResponse?>

    @Operation(summary = "PR文件列表")
    @POST
    @Path("/listPullRequestFiles")
    fun listPullRequestFiles(
        @Parameter(description = "授权token", required = true)
        @HeaderParam(AUTH_HEADER_GITHUB_TOKEN)
        token: String,
        request: ListPullRequestFileRequest
    ): Result<List<PullRequestFileResponse>>
}
