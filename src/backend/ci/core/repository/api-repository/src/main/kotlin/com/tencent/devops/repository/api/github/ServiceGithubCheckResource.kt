package com.tencent.devops.repository.api.github

import com.tencent.devops.common.api.auth.AUTH_HEADER_GITHUB_TOKEN
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.sdk.github.request.CreateCheckRunRequest
import com.tencent.devops.common.sdk.github.request.UpdateCheckRunRequest
import com.tencent.devops.common.sdk.github.response.CheckRunResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Tag(name = "SERVICE_CHECK_GITHUB", description = "服务-github-check")
@Path("/service/github/check")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceGithubCheckResource {

    @Operation(summary = "创建检查任务")
    @POST
    @Path("/createCheckRun")
    fun createCheckRun(
        @Parameter(description = "授权token", required = true)
        @HeaderParam(AUTH_HEADER_GITHUB_TOKEN)
        token: String,
        request: CreateCheckRunRequest
    ): Result<CheckRunResponse>

    @Operation(summary = "创建检查任务")
    @POST
    @Path("/createCheckRunByToken")
    fun createCheckRunByToken(
        @Parameter(description = "授权token", required = true)
        @HeaderParam(AUTH_HEADER_GITHUB_TOKEN)
        token: String,
        request: CreateCheckRunRequest
    ): Result<CheckRunResponse>

    @Operation(summary = "更新检查任务")
    @POST
    @Path("/updateCheckRun")
    fun updateCheckRun(
        @Parameter(description = "授权token", required = true)
        @HeaderParam(AUTH_HEADER_GITHUB_TOKEN)
        token: String,
        request: UpdateCheckRunRequest
    ): Result<CheckRunResponse>
}
