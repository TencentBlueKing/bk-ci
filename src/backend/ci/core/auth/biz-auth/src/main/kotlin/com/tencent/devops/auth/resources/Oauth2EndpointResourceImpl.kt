package com.tencent.devops.auth.resources

import com.tencent.devops.auth.api.oauth2.Oauth2EndpointResource
import com.tencent.devops.auth.pojo.Oauth2AccessTokenRequest
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo
import com.tencent.devops.auth.service.oauth2.Oauth2EndpointService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource

@RestResource
class Oauth2EndpointResourceImpl constructor(
    private val endpointService: Oauth2EndpointService
) : Oauth2EndpointResource {
    override fun getHtml(userId: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <title>OAuth2授权</title>
            </head>
            <body>
              <h1>授权信息：是否授权“行云”访问您在蓝盾的制品库</h1>
              <button onclick="authorize()">授权</button>

              <script>
                function authorize() {
                  window.location.href = "https://dev.devops.woa.com/ms/auth/api/user/oauth2/endpoint/getAuthorizationCode?clientId=test1&redirectUri=https://www.baidu.com/";
                }
              </script>
            </body>
            </html>
        """.trimIndent()
    }

    override fun getAuthorizationCode(
        userId: String,
        clientId: String,
        redirectUri: String
    ): Result<String> {
        return Result(
            endpointService.getAuthorizationCode(
                userId = userId,
                clientId = clientId,
                redirectUri = redirectUri
            )
        )
    }

    override fun getAccessToken(
        accessTokenRequest: Oauth2AccessTokenRequest
    ): Result<Oauth2AccessTokenVo?> {
        return Result(
            endpointService.getAccessToken(
                accessTokenRequest = accessTokenRequest
            )
        )
    }
}
