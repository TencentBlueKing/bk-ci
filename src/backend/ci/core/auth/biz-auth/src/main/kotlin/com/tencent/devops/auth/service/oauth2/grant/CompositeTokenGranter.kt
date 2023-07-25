package com.tencent.devops.auth.service.oauth2.grant

import com.tencent.devops.auth.pojo.Oauth2AccessTokenRequest
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo

class CompositeTokenGranter constructor(
    private val tokenGranters: List<TokenGranter>
) : TokenGranter {
    override fun grant(
        grantType: String,
        accessTokenRequest: Oauth2AccessTokenRequest
    ): Oauth2AccessTokenVo? {
        for (granter in tokenGranters) {
            val grant = granter.grant(grantType, accessTokenRequest)
            if (grant != null) {
                return grant
            }
        }
        return null
    }
}
