package com.tencent.devops.auth.api.oauth2

import com.tencent.devops.auth.pojo.Oauth2AccessTokenRequest
import com.tencent.devops.auth.pojo.dto.Oauth2AuthorizationCodeDTO
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo
import com.tencent.devops.auth.pojo.vo.Oauth2AuthorizationInfoVo
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_OAUTH2_AUTHORIZATION
import com.tencent.devops.common.api.auth.AUTH_HEADER_OAUTH2_CLIENT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_OAUTH2_CLIENT_SECRET
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

@Api(tags = ["OAUTH2_ENDPOINT"], description = "oauth2相关")
@Path("/service/oauth2/endpoint")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface Oauth2ServiceEndpointResource {
    @GET
    @Path("/getAuthorizationInformation")
    @ApiOperation("获取授权信息")
    fun getAuthorizationInformation(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @ApiParam("待校验用户ID", required = true)
        userId: String,
        @QueryParam("clientId")
        @ApiParam("客户端ID", required = true)
        clientId: String,
        @QueryParam("redirectUri")
        @ApiParam("跳转链接", required = true)
        redirectUri: String
    ): Result<Oauth2AuthorizationInfoVo>

    @POST
    @Path("/getAuthorizationCode")
    @ApiOperation("获取授权码")
    fun getAuthorizationCode(
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @ApiParam("待校验用户ID", required = true)
        userId: String,
        @QueryParam("clientId")
        @ApiParam("客户端ID", required = true)
        clientId: String,
        @QueryParam("redirectUri")
        @ApiParam("跳转链接", required = true)
        redirectUri: String,
        @ApiParam("oauth2获取授权码请求报文体", required = true)
        authorizationCodeDTO: Oauth2AuthorizationCodeDTO
    ): Result<String>

    @POST
    @Path("/getAccessToken")
    @ApiOperation("获取accessToken")
    fun getAccessToken(
        @HeaderParam(AUTH_HEADER_OAUTH2_CLIENT_ID)
        @ApiParam("客户端id", required = true)
        clientId: String,
        @HeaderParam(AUTH_HEADER_OAUTH2_CLIENT_SECRET)
        @ApiParam("客户端秘钥", required = true)
        clientSecret: String,
        @ApiParam("oauth2获取token请求报文体", required = true)
        accessTokenRequest: Oauth2AccessTokenRequest
    ): Result<Oauth2AccessTokenVo?>

    @POST
    @Path("/verifyAccessToken")
    @ApiOperation("校验accessToken")
    fun verifyAccessToken(
        @HeaderParam(AUTH_HEADER_OAUTH2_CLIENT_ID)
        @ApiParam("客户端id", required = true)
        clientId: String,
        @HeaderParam(AUTH_HEADER_OAUTH2_CLIENT_SECRET)
        @ApiParam("客户端秘钥", required = true)
        clientSecret: String,
        @HeaderParam(AUTH_HEADER_OAUTH2_AUTHORIZATION)
        @ApiParam("access token", required = true)
        accessToken: String
    ): Result<String>
}
