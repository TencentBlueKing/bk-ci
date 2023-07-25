package com.tencent.devops.auth.service.oauth2.grant

import com.tencent.devops.auth.dao.AuthOauth2AccessTokenDao
import com.tencent.devops.auth.dao.AuthOauth2CodeDao
import com.tencent.devops.auth.dao.AuthOauth2RefreshTokenDao
import com.tencent.devops.auth.pojo.Oauth2AccessTokenInfo
import com.tencent.devops.auth.pojo.dto.Oauth2AccessTokenDTO
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo
import com.tencent.devops.auth.service.oauth2.ClientService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ClientCredentialsTokenGranter constructor(
    private val clientService: ClientService,
    private val authOauth2RefreshTokenDao: AuthOauth2RefreshTokenDao,
    private val authOauth2AccessTokenDao: AuthOauth2AccessTokenDao,
    private val authOauth2CodeDao: AuthOauth2CodeDao,
    private val dslContext: DSLContext
) : AbstractTokenGranter(
    grantType = GRANT_TYPE,
    clientService = clientService,
    authOauth2AccessTokenDao = authOauth2AccessTokenDao,
    dslContext = dslContext
) {
    companion object {
        private const val GRANT_TYPE = "client_credentials"
        private val logger = LoggerFactory.getLogger(ClientCredentialsTokenGranter::class.java)
    }

    override fun getAccessToken(
        oauth2AccessTokenDTO: Oauth2AccessTokenDTO
    ): Oauth2AccessTokenVo {
        logger.info("client_credentials getAccessToken")
        // 1、根据appcode获取accessToken
        val accessTokenRecord = authOauth2AccessTokenDao.get(
            dslContext = dslContext,
            clientId = oauth2AccessTokenDTO.clientId
        )
        return super.handleAccessToken(
            oauth2AccessTokenDTO,
            Oauth2AccessTokenInfo(
                accessToken = accessTokenRecord?.accessToken,
                expiredTime = accessTokenRecord?.expiredTime
            )
        )
    }
}
