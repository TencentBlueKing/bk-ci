package com.tencent.devops.auth.service.oauth2.grant

import com.tencent.devops.auth.pojo.dto.Oauth2AccessTokenDTO
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo

interface TokenGranter {
    fun grant(
        grantType: String,
        oauth2AccessTokenDTO: Oauth2AccessTokenDTO
    ): Oauth2AccessTokenVo?
}
