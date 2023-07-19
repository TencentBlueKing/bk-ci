package com.tencent.devops.auth.service.oauth2

import com.tencent.devops.auth.service.oauth2.grant.TokenGranter

class TokenService constructor(
    private val tokenGranter: TokenGranter
) {
    fun getAccessToken(grantType: String): String? {
        //1.校验是否登录
        //2.校验客户端ID和秘钥
        return tokenGranter.grant(grantType)
    }
}
