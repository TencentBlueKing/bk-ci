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
