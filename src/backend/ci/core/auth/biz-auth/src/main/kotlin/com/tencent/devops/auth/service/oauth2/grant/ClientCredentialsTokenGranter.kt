package com.tencent.devops.auth.service.oauth2.grant

import com.tencent.devops.auth.pojo.dto.Oauth2AccessTokenDTO
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo
import org.slf4j.LoggerFactory

class ClientCredentialsTokenGranter : AbstractTokenGranter(GRANT_TYPE) {
    companion object {
        private const val GRANT_TYPE = "client_credentials"
        private val logger = LoggerFactory.getLogger(ClientCredentialsTokenGranter::class.java)
    }

    override fun getAccessToken(
        oauth2AccessTokenDTO: Oauth2AccessTokenDTO,
        accessToken: String?
    ): Oauth2AccessTokenVo {
        logger.info("client_credentials getAccessToken")
        // 1、根据appcode获取accessToken
        return super.getAccessToken(oauth2AccessTokenDTO, accessToken)
    }
}
