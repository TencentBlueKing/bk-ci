package com.tencent.devops.external.api

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Api(tags = ["EXTERNAL_GITHUB"], description = "External-Github")
@Path("/external/github/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ExternalGithubResource {

    @ApiOperation("Github仓库提交")
    @POST
    @Path("/webhook/commit")
    fun webhookCommit(
        @ApiParam(value = "事件类型", required = true)
        @HeaderParam("X-GitHub-Event")
        event: String,
        @ApiParam(value = "事件ID", required = true)
        @HeaderParam("X-Github-Delivery")
        guid: String,
        @ApiParam(value = "secretKey签名(sha1)", required = true)
        @HeaderParam("X-Hub-Signature")
        signature: String,
        body: String
    ): Result<Boolean>

    @ApiOperation("Github Oauth回调")
    @GET
    @Path("/oauth/callback")
    fun oauthCallback(
        @ApiParam(value = "code")
        @QueryParam("code")
        code: String,
        @ApiParam(value = "state")
        @QueryParam("state")
        state: String
    ): Response
}