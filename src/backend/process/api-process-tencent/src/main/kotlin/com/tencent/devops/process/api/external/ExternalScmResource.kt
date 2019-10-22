package com.tencent.devops.process.api.external

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["EXTERNAL_CODE_SVN"], description = "外部-CODE-SVN-资源")
@Path("/external/scm")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ExternalScmResource {

    @ApiOperation("Code平台SVN仓库提交")
    @POST
    @Path("/codesvn/commit")
    fun webHookCodeSvnCommit(event: String): Result<Boolean>

    @ApiOperation("Code平台Git仓库提交")
    @POST
    @Path("/codegit/commit")
    fun webHookCodeGitCommit(
        @HeaderParam("X-Token")
        token: String,
        event: String
    ): Result<Boolean>

    @ApiOperation("Gitlab仓库提交")
    @POST
    @Path("/gitlab/commit")
    fun webHookGitlabCommit(event: String): Result<Boolean>
}