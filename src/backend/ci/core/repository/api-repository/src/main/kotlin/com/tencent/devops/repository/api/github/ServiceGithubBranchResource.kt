package com.tencent.devops.repository.api.github

import com.tencent.devops.common.api.auth.AUTH_HEADER_GITHUB_TOKEN
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.sdk.github.request.GetBranchRequest
import com.tencent.devops.repository.sdk.github.request.ListBranchesRequest
import com.tencent.devops.repository.sdk.github.response.BranchResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Tag(name = "SERVICE_BRANCH_GITHUB", description = "服务-github-branch")
@Path("/service/github/branch")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceGithubBranchResource {

    @Operation(summary = "仓库分支列表")
    @POST
    @Path("/listBranch")
    fun listBranch(
        @Parameter(description = "授权token", required = true)
        @HeaderParam(AUTH_HEADER_GITHUB_TOKEN)
        token: String,
        request: ListBranchesRequest
    ): Result<List<BranchResponse>>

    @Operation(summary = "获取仓库分支")
    @POST
    @Path("/getBranch")
    fun getBranch(
        @Parameter(description = "授权token", required = true)
        @HeaderParam(AUTH_HEADER_GITHUB_TOKEN)
        token: String,
        request: GetBranchRequest
    ): Result<BranchResponse>
}
