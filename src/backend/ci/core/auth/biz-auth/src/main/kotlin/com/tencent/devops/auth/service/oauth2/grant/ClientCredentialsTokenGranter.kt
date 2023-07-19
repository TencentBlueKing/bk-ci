package com.tencent.devops.auth.service.oauth2.grant

import org.slf4j.LoggerFactory

class ClientCredentialsTokenGranter : AbstractTokenGranter(GRANT_TYPE) {
    companion object {
        private const val GRANT_TYPE = "client_credentials"
        private val logger = LoggerFactory.getLogger(ClientCredentialsTokenGranter::class.java)
    }

    override fun getAccessToken(grantType: String): String {
        logger.info("client_credentials getAccessToken")
        // 1、根据appcode获取accessToken
        return super.getAccessToken(grantType)
    }
}
