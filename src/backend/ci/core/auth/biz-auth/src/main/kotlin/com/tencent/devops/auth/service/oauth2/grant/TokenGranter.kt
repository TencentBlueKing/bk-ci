package com.tencent.devops.auth.service.oauth2.grant

import com.tencent.devops.auth.pojo.ClientDetailsInfo
import com.tencent.devops.auth.pojo.Oauth2AccessTokenRequest
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo

interface TokenGranter {
    fun grant(
        grantType: String,
        clientDetails: ClientDetailsInfo,
        accessTokenRequest: Oauth2AccessTokenRequest
    ): Oauth2AccessTokenVo?
}
