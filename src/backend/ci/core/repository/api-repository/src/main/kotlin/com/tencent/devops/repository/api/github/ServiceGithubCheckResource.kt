package com.tencent.devops.repository.api.github

import com.tencent.devops.common.sdk.github.request.CreateCheckRunRequest
import com.tencent.devops.common.sdk.github.request.UpdateCheckRunRequest
import com.tencent.devops.common.sdk.github.response.CheckRunResponse
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.Consumes
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
        request: CreateCheckRunRequest,
        userId: String
    ): CheckRunResponse

    @ApiOperation("更新检查任务")
    @POST
    @Path("/updateCheckRun")
    fun updateCheckRun(
        request: UpdateCheckRunRequest,
        userId: String
    ): CheckRunResponse
}
