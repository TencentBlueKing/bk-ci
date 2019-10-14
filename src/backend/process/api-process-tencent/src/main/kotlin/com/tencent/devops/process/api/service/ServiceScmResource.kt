package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.code.WebhookCommit
import com.tencent.devops.process.pojo.code.github.GithubWebhook
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_SCM"], description = "服务-SCM")
@Path("/service/scm")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceScmResource {

    @ApiOperation("Code平台SVN仓库提交")
    @POST
    @Path("/github/commit")
    fun webHookCodeGithubCommit(
        webhook: GithubWebhook
    ): Result<Boolean>

    @ApiOperation("Webhook代码库提交")
    @POST
    @Path("/webhook/commit")
    fun webhookCommit(
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        webhookCommit: WebhookCommit
    ): Result<String>
}