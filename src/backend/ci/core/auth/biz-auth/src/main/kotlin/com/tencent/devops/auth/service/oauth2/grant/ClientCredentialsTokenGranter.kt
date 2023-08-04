package com.tencent.devops.auth.service.oauth2.grant

import com.tencent.devops.auth.pojo.ClientDetailsInfo
import com.tencent.devops.auth.pojo.Oauth2AccessTokenRequest
import com.tencent.devops.auth.pojo.dto.Oauth2AccessTokenDTO
import com.tencent.devops.auth.pojo.enum.Oauth2GrantType
import com.tencent.devops.auth.service.oauth2.Oauth2AccessTokenService
import com.tencent.devops.auth.service.oauth2.Oauth2ScopeService
import org.springframework.stereotype.Service

@Service
class ClientCredentialsTokenGranter constructor(
    private val accessTokenService: Oauth2AccessTokenService,
    private val oauth2ScopeService: Oauth2ScopeService
) : AbstractTokenGranter(
    grantType = GRANT_TYPE,
    accessTokenService = accessTokenService
) {
    companion object {
        private val GRANT_TYPE = Oauth2GrantType.CLIENT_CREDENTIALS.grantType
    }

    override fun getAccessToken(
        accessTokenRequest: Oauth2AccessTokenRequest,
        clientDetails: ClientDetailsInfo
    ): Oauth2AccessTokenDTO {
        val accessTokenInfo = accessTokenService.get(
            clientId = clientDetails.clientId,
            grantType = GRANT_TYPE
        )
        val scopeId = oauth2ScopeService.create(
            scope = clientDetails.scope
        )

        return Oauth2AccessTokenDTO(
            accessToken = accessTokenInfo?.accessToken,
            expiredTime = accessTokenInfo?.expiredTime,
            scopeId = scopeId
        )
    }
}
