package com.tencent.devops.repository.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_OAUTH_GIT"], description = "用户-git的oauth")
@Path("/user/git/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserGitResource {

    @ApiOperation("根据用户ID, 通过oauth方式获取项目")
    @GET
    @Path("/getProject")
    fun getProject(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "用户ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @ApiParam(value = "repo hash iD", required = false)
        @QueryParam("repoHashId")
        repoHashId: String?
    ): Result<AuthorizeResult>

    @ApiOperation("删除用户的token ID")
    @DELETE
    @Path("/deleteToken")
    fun deleteToken(
        @ApiParam(value = "用户ID", required = true)
        @QueryParam("userId")
        userId: String
    ): Result<Int>

    @ApiOperation("根据用户ID判断用户是否已经oauth认证")
    @GET
    @Path("/isOauth")
    fun isOAuth(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("重定向url类型", required = false)
        @QueryParam("redirectUrlType")
        redirectUrlType: RedirectUrlTypeEnum?,
        @ApiParam(value = "插件代码", required = false)
        @QueryParam("atomCode")
        atomCode: String?
    ): Result<AuthorizeResult>
}