package com.tencent.devops.auth.service.oauth2

import com.tencent.devops.auth.pojo.dto.Oauth2AccessTokenDTO
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo
import com.tencent.devops.auth.service.oauth2.grant.TokenGranter
import org.springframework.stereotype.Service

@Service
class Oauth2EndpointService constructor(
    private val tokenGranter: TokenGranter
) {
    fun getAuthorizationCode(
        userId: String,
        clientId: String,
        redirectUri: String
    ): String {
        // 1、校验用户是否登录
        // 2、校验clientId是否存在
        // 3、校验client是否为授权码模式
        // 4、校验redirectUri是否和数据库一致
        // 5、生成授权码并存储数据库，授权码有效期为5分钟
        // 6、返回授权码
        return "authorizationCode"
    }

    fun getAccessToken(
        userId: String,
        oauth2AccessTokenDTO: Oauth2AccessTokenDTO
    ): Oauth2AccessTokenVo? {
        // 1、校验用户是否登录
        // 2、校验client_id和client_secret是否正确
        val grantType = oauth2AccessTokenDTO.grantType
        return tokenGranter.grant(grantType, oauth2AccessTokenDTO)
    }
}
