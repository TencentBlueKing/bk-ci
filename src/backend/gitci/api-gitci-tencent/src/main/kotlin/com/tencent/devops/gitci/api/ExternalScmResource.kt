package com.tencent.devops.gitci.api

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["EXTERNAL_GIT_HOOKS"], description = "GIT WebHooks触发")
@Path("/service/scm")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ExternalScmResource {

    @ApiOperation("Code平台Git仓库提交")
    @POST
    @Path("/codegit/commit")
    fun webHookCodeGitCommit(
        @HeaderParam("X-Token")
        token: String,
        event: String
    ): Result<Boolean>
}