package com.tencent.devops.auth.service.oauth2.grant

import com.tencent.devops.auth.pojo.ClientDetailsInfo
import com.tencent.devops.auth.pojo.Oauth2AccessTokenRequest
import com.tencent.devops.auth.pojo.dto.Oauth2AccessTokenDTO
import com.tencent.devops.auth.service.oauth2.Oauth2AccessTokenService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ClientCredentialsTokenGranter constructor(
    private val accessTokenService: Oauth2AccessTokenService
) : AbstractTokenGranter(
    grantType = GRANT_TYPE,
    accessTokenService = accessTokenService
) {
    companion object {
        private const val GRANT_TYPE = "client_credentials"
    }

    override fun getAccessToken(
        accessTokenRequest: Oauth2AccessTokenRequest,
        clientDetails: ClientDetailsInfo
    ): Oauth2AccessTokenDTO {
        val accessTokenInfo = accessTokenService.get(
            clientId = accessTokenRequest.clientId,
            grantType = GRANT_TYPE
        )
        return Oauth2AccessTokenDTO(
            accessToken = accessTokenInfo?.accessToken,
            expiredTime = accessTokenInfo?.expiredTime
        )
    }
}
