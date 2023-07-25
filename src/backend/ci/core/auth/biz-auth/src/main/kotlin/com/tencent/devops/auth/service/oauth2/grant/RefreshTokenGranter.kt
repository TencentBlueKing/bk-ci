package com.tencent.devops.auth.service.oauth2.grant

import com.tencent.devops.auth.dao.AuthOauth2AccessTokenDao
import com.tencent.devops.auth.dao.AuthOauth2CodeDao
import com.tencent.devops.auth.dao.AuthOauth2RefreshTokenDao
import com.tencent.devops.auth.pojo.Oauth2AccessTokenInfo
import com.tencent.devops.auth.pojo.dto.Oauth2AccessTokenDTO
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo
import com.tencent.devops.auth.service.oauth2.ClientService
import com.tencent.devops.common.api.exception.ErrorCodeException
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RefreshTokenGranter(
    private val clientService: ClientService,
    private val authOauth2RefreshTokenDao: AuthOauth2RefreshTokenDao,
    private val authOauth2AccessTokenDao: AuthOauth2AccessTokenDao,
    private val dslContext: DSLContext
) : AbstractTokenGranter(
    grantType = GRANT_TYPE,
    clientService = clientService,
    authOauth2AccessTokenDao = authOauth2AccessTokenDao,
    dslContext = dslContext
) {
    companion object {
        private const val GRANT_TYPE = "refresh_token"
        private val logger = LoggerFactory.getLogger(RefreshTokenGranter::class.java)
    }

    override fun getAccessToken(
        oauth2AccessTokenDTO: Oauth2AccessTokenDTO
    ): Oauth2AccessTokenVo {
        logger.info("refresh_token getAccessToken")
        //1.校验refresh_token是否为空
        if (oauth2AccessTokenDTO.refreshToken == null) {
            throw ErrorCodeException(
                errorCode = "111",
                defaultMessage = "The authorization code invalid"
            )
        }
        // 2.校验refresh_token是否存在
        val refreshTokenRecord = authOauth2RefreshTokenDao.get(
            dslContext = dslContext,
            refreshToken = oauth2AccessTokenDTO.refreshToken!!
        ) ?: throw ErrorCodeException(
            errorCode = "111",
            defaultMessage = "The authorization code invalid"
        )
        if (refreshTokenRecord.clientId != oauth2AccessTokenDTO.clientId) {
            throw ErrorCodeException(
                errorCode = "111",
                defaultMessage = "The authorization code invalid"
            )
        }
        //2.清除跟该refresh_token授权码相关的access_token
        authOauth2AccessTokenDao.deleteByRefreshToken(
            dslContext = dslContext,
            refreshToken = oauth2AccessTokenDTO.refreshToken!!
        )
        //3.校验refresh_token是否过期
        if (refreshTokenRecord.expiredTime < System.currentTimeMillis()) {
            throw ErrorCodeException(
                errorCode = "111",
                defaultMessage = "The authorization code invalid"
            )
        } else {
            return super.handleAccessToken(
                oauth2AccessTokenDTO, Oauth2AccessTokenInfo()
            )
        }
    }
}
