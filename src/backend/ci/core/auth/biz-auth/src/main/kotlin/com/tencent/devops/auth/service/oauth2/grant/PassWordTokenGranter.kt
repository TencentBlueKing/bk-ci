package com.tencent.devops.auth.service.oauth2.grant

import com.tencent.devops.auth.pojo.ClientDetailsInfo
import com.tencent.devops.auth.pojo.Oauth2PassWordRequest
import com.tencent.devops.auth.pojo.dto.Oauth2AccessTokenDTO
import com.tencent.devops.auth.pojo.enum.Oauth2GrantType
import com.tencent.devops.auth.service.oauth2.Oauth2AccessTokenService
import com.tencent.devops.auth.service.oauth2.Oauth2ScopeService
import org.springframework.stereotype.Service

@Service
class PassWordTokenGranter(
    private val oauth2ScopeService: Oauth2ScopeService,
    accessTokenService: Oauth2AccessTokenService
) : AbstractTokenGranter<Oauth2PassWordRequest>(
    accessTokenService = accessTokenService
) {
    override fun getAccessToken(
        accessTokenRequest: Oauth2PassWordRequest,
        clientDetails: ClientDetailsInfo
    ): Oauth2AccessTokenDTO {
        val accessTokenInfo = accessTokenService.get(
            clientId = clientDetails.clientId,
            userName = accessTokenRequest.userName,
            passWord = accessTokenRequest.passWord,
            grantType = type().grantType
        )
        val scopeId = oauth2ScopeService.create(
            scope = clientDetails.scope
        )

        return Oauth2AccessTokenDTO(
            userName = accessTokenRequest.userName,
            passWord = accessTokenRequest.passWord,
            accessToken = accessTokenInfo?.accessToken,
            expiredTime = accessTokenInfo?.expiredTime,
            scopeId = scopeId
        )
    }

    override fun type(): Oauth2GrantType = Oauth2GrantType.PASS_WORD
}
