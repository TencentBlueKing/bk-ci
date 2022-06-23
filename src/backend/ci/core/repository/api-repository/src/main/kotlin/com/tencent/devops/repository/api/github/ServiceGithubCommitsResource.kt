package com.tencent.devops.repository.api.github

import com.tencent.devops.common.sdk.github.request.GetCommitRequest
import com.tencent.devops.common.sdk.github.request.ListCommitRequest
import com.tencent.devops.common.sdk.github.response.CommitResponse
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.Consumes
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
        request: ListCommitRequest,
        userId: String
    ): List<CommitResponse>

    @ApiOperation("获取某个提交记录")
    @POST
    @Path("/getCommit")
    fun getCommit(
        request: GetCommitRequest,
        userId: String
    ): CommitResponse
}
