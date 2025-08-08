package com.tencent.devops.auth.service.oauth2.grant

import com.tencent.devops.auth.pojo.ClientDetailsInfo
import com.tencent.devops.auth.pojo.Oauth2AccessTokenRequest
import com.tencent.devops.auth.pojo.enum.Oauth2GrantType
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo

interface TokenGranter<T : Oauth2AccessTokenRequest> {
    fun grant(
        clientDetails: ClientDetailsInfo,
        accessTokenRequest: T
    ): Oauth2AccessTokenVo

    /**
     * 支持类型
     */
    fun type(): Oauth2GrantType
}
