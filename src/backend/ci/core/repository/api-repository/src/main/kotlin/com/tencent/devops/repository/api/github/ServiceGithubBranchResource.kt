package com.tencent.devops.repository.api.github

import com.tencent.devops.common.sdk.github.request.GHGetBranchRequest
import com.tencent.devops.common.sdk.github.request.GHListBranchesRequest
import com.tencent.devops.common.sdk.github.response.GHBranchResponse
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_BRANCH_GITHUB"], description = "服务-github-branch")
@Path("/service/github/branch")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceGithubBranchResource {

    @ApiOperation("仓库分支列表")
    @POST
    @Path("/listBranch")
    fun listBranch(
        request: GHListBranchesRequest,
        userId: String
    ): List<GHBranchResponse>

    @ApiOperation("获取仓库分支")
    @POST
    @Path("/getBranch")
    fun getBranch(
        request: GHGetBranchRequest,
        userId: String
    ): GHBranchResponse
}
