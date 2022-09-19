package com.tencent.devops.repository.api.github

import com.tencent.devops.common.api.auth.AUTH_HEADER_GITHUB_TOKEN
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.sdk.github.request.CreateCheckRunRequest
import com.tencent.devops.common.sdk.github.request.UpdateCheckRunRequest
import com.tencent.devops.common.sdk.github.response.CheckRunResponse
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_CHECK_GITHUB"], description = "服务-github-check")
@Path("/service/github/check")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceGithubCheckResource {

    @ApiOperation("创建检查任务")
    @POST
    @Path("/createCheckRun")
    fun createCheckRun(
        @ApiParam("授权token", required = true)
        @HeaderParam(AUTH_HEADER_GITHUB_TOKEN)
        token: String,
        request: CreateCheckRunRequest
    ): Result<CheckRunResponse>

    @ApiOperation("创建检查任务")
    @POST
    @Path("/createCheckRunByToken")
    fun createCheckRunByToken(
        @ApiParam("授权token", required = true)
        @HeaderParam(AUTH_HEADER_GITHUB_TOKEN)
        token: String,
        request: CreateCheckRunRequest
    ): Result<CheckRunResponse>

    @ApiOperation("更新检查任务")
    @POST
    @Path("/updateCheckRun")
    fun updateCheckRun(
        @ApiParam("授权token", required = true)
        @HeaderParam(AUTH_HEADER_GITHUB_TOKEN)
        token: String,
        request: UpdateCheckRunRequest
    ): Result<CheckRunResponse>
}
