package com.tencent.devops.auth.service.oauth2.grant

import com.tencent.devops.auth.pojo.dto.Oauth2AccessTokenDTO
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo

class CompositeTokenGranter constructor(
    private val tokenGranters: List<TokenGranter>
) : TokenGranter {
    override fun grant(
        grantType: String,
        oauth2AccessTokenDTO: Oauth2AccessTokenDTO
    ): Oauth2AccessTokenVo? {
        for (granter in tokenGranters) {
            val grant = granter.grant(grantType, oauth2AccessTokenDTO)
            if (grant != null) {
                return grant
            }
        }
        return null
    }
}
