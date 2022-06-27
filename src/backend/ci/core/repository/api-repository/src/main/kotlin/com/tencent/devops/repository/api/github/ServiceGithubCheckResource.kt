package com.tencent.devops.repository.api.github

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.sdk.github.request.CreateCheckRunRequest
import com.tencent.devops.common.sdk.github.request.UpdateCheckRunRequest
import com.tencent.devops.common.sdk.github.response.CheckRunResponse
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
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
        @ApiParam("用户id", required = true)
        @QueryParam("userId")
        userId: String,
        request: CreateCheckRunRequest
    ): Result<CheckRunResponse>

    @ApiOperation("更新检查任务")
    @POST
    @Path("/updateCheckRun")
    fun updateCheckRun(
        @ApiParam("用户id", required = true)
        @QueryParam("userId")
        userId: String,
        request: UpdateCheckRunRequest
    ): Result<CheckRunResponse>
}
