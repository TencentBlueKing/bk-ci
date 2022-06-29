package com.tencent.devops.repository.api.github

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.github.GithubOauthCallback
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_GITHUB_OAUTH"], description = "service github oauth")
@Path("/service/github/oauth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceGithubOauthResource {

    @ApiOperation("github回调请求")
    @GET
    @Path("/callback")
    fun githubCallback(
        @ApiParam(value = "code")
        @QueryParam("code")
        code: String,
        @ApiParam(value = "state")
        @QueryParam("state")
        state: String?,
        @ApiParam(value = "channelCode")
        @QueryParam("channelCode")
        channelCode: String? = null
    ): Result<GithubOauthCallback>

    @ApiOperation("github授权url")
    @GET
    @Path("/oauthUrl")
    fun oauthUrl(
        @ApiParam(value = "redirectUrl")
        @QueryParam("redirectUrl")
        redirectUrl: String
    ): Result<String>
}
