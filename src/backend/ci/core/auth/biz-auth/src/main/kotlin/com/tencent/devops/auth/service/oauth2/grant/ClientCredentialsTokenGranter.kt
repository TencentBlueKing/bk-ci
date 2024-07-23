package com.tencent.devops.auth.service.oauth2.grant

import com.tencent.devops.auth.pojo.ClientDetailsInfo
import com.tencent.devops.auth.pojo.Oauth2AccessTokenRequest
import com.tencent.devops.auth.pojo.dto.Oauth2AccessTokenDTO
import com.tencent.devops.auth.pojo.enum.Oauth2GrantType
import com.tencent.devops.auth.service.oauth2.Oauth2AccessTokenService
import com.tencent.devops.auth.service.oauth2.Oauth2ScopeService
import org.springframework.stereotype.Service

@Service
class ClientCredentialsTokenGranter(
    private val oauth2ScopeService: Oauth2ScopeService,
    accessTokenService: Oauth2AccessTokenService
) : AbstractTokenGranter<Oauth2AccessTokenRequest>(
    accessTokenService = accessTokenService
) {

    override fun getAccessToken(
        accessTokenRequest: Oauth2AccessTokenRequest,
        clientDetails: ClientDetailsInfo
    ): Oauth2AccessTokenDTO {
        val accessTokenInfo = accessTokenService.get(
            clientId = clientDetails.clientId,
            grantType = type().grantType
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

    override fun type(): Oauth2GrantType = Oauth2GrantType.CLIENT_CREDENTIALS
}
