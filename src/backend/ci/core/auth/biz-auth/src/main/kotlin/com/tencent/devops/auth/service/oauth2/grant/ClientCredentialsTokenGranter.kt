package com.tencent.devops.auth.service.oauth2.grant

import com.tencent.devops.auth.pojo.Oauth2AccessTokenRequest
import com.tencent.devops.auth.pojo.dto.Oauth2AccessTokenDTO
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo
import com.tencent.devops.auth.service.oauth2.Oauth2AccessTokenService
import com.tencent.devops.auth.service.oauth2.Oauth2ClientService
import com.tencent.devops.model.auth.tables.records.TAuthOauth2ClientDetailsRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ClientCredentialsTokenGranter constructor(
    private val clientService: Oauth2ClientService,
    private val accessTokenService: Oauth2AccessTokenService
) : AbstractTokenGranter(
    grantType = GRANT_TYPE,
    oauth2ClientService = clientService,
    accessTokenService = accessTokenService
) {
    companion object {
        private const val GRANT_TYPE = "client_credentials"
        private val logger = LoggerFactory.getLogger(ClientCredentialsTokenGranter::class.java)
    }

    override fun getAccessToken(
        accessTokenRequest: Oauth2AccessTokenRequest,
        clientDetail: TAuthOauth2ClientDetailsRecord
    ): Oauth2AccessTokenDTO {
        logger.info("client_credentials getAccessToken")
        // 1、根据appcode获取accessToken
        val accessTokenRecord = accessTokenService.get(
            clientId = accessTokenRequest.clientId
        )
        return Oauth2AccessTokenDTO(
            accessToken = accessTokenRecord?.accessToken,
            expiredTime = accessTokenRecord?.expiredTime
        )
    }
}
