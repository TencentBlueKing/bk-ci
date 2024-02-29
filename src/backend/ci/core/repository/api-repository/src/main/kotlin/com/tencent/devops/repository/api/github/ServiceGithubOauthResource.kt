package com.tencent.devops.repository.api.github

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.github.GithubOauthCallback
import com.tencent.devops.repository.pojo.oauth.GithubTokenType
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "SERVICE_GITHUB_OAUTH", description = "service github oauth")
@Path("/service/github/oauth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceGithubOauthResource {

    @Operation(summary = "github回调请求")
    @GET
    @Path("/callback")
    fun githubCallback(
        @Parameter(description = "code")
        @QueryParam("code")
        code: String,
        @Parameter(description = "state")
        @QueryParam("state")
        state: String?,
        @Parameter(description = "channelCode")
        @QueryParam("channelCode")
        channelCode: String? = null
    ): Result<GithubOauthCallback>

    @Operation(summary = "github授权url")
    @GET
    @Path("/oauthUrl")
    fun oauthUrl(
        @Parameter(description = "redirectUrl")
        @QueryParam("redirectUrl")
        redirectUrl: String,
        @Parameter(description = "userId, 如果指定将以该userId入库，否则会以github login name 入库")
        @QueryParam("userId")
        userId: String?,
        @Parameter(description = "token 类型", required = false)
        @QueryParam("tokenType")
        @DefaultValue("GITHUB_APP")
        tokenType: GithubTokenType? = GithubTokenType.GITHUB_APP
    ): Result<String>
}
