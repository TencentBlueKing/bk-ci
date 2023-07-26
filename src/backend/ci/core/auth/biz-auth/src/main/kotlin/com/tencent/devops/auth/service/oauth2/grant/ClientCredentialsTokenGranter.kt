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
        private val logger = LoggerFactory.getLogger(ClientCredentialsTokenGranter::class.java)
    }

    override fun getAccessToken(
        accessTokenRequest: Oauth2AccessTokenRequest,
        clientDetails: ClientDetailsInfo
    ): Oauth2AccessTokenDTO {
        logger.info("client credentials getAccessToken|$accessTokenRequest|$clientDetails")
        // 1、根据appcode获取accessToken
        val accessTokenInfo = accessTokenService.get(
            clientId = accessTokenRequest.clientId
        )
        return Oauth2AccessTokenDTO(
            accessToken = accessTokenInfo?.accessToken,
            expiredTime = accessTokenInfo?.expiredTime
        )
    }
}
