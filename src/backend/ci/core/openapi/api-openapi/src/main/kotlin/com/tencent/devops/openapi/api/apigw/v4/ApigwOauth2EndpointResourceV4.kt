package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.auth.pojo.Oauth2AccessTokenRequest
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_OAUTH2_CLIENT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_OAUTH2_CLIENT_SECRET
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Tag(name = "OPENAPI_OAUTH2_V4", description = "OPENAPI-OAUTH2相关")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/auth/oauth2/endpoint/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwOauth2EndpointResourceV4 {
    @POST
    @Path("/getAccessToken")
    @Operation(
        summary = "oauth2获取accessToken",
        tags = ["v4_app_oauth2_access_token"]
    )
    fun getAccessToken(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @HeaderParam(AUTH_HEADER_OAUTH2_CLIENT_ID)
        @Parameter(description = "客户端id", required = true)
        clientId: String,
        @HeaderParam(AUTH_HEADER_OAUTH2_CLIENT_SECRET)
        @Parameter(description = "客户端秘钥", required = true)
        clientSecret: String,
        @Parameter(description = "oauth2获取token请求报文体", required = true)
        accessTokenRequest: Oauth2AccessTokenRequest
    ): Result<Any?>
}
