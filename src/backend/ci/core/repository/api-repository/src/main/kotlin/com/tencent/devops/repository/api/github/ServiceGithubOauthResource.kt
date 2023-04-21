package com.tencent.devops.repository.api.github

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.github.GithubOauthCallback
import com.tencent.devops.repository.pojo.oauth.GithubTokenType
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DefaultValue
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
        redirectUrl: String,
        @ApiParam(value = "userId, 如果指定将以该userId入库，否则会以github login name 入库")
        @QueryParam("userId")
        userId: String?,
        @ApiParam("token 类型", required = false)
        @QueryParam("tokenType")
        @DefaultValue("GITHUB_APP")
        tokenType: GithubTokenType? = GithubTokenType.GITHUB_APP
    ): Result<String>
}
