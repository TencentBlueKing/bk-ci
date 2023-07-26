package com.tencent.devops.auth.service.oauth2.grant

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.constant.AuthMessageCode.ERROR_REFRESH_TOKEN_EXPIRED
import com.tencent.devops.auth.constant.AuthMessageCode.ERROR_REFRESH_TOKEN_NOT_FOUND
import com.tencent.devops.auth.pojo.dto.Oauth2AccessTokenDTO
import com.tencent.devops.auth.pojo.Oauth2AccessTokenRequest
import com.tencent.devops.auth.service.oauth2.Oauth2AccessTokenService
import com.tencent.devops.auth.service.oauth2.Oauth2ClientService
import com.tencent.devops.auth.service.oauth2.Oauth2RefreshTokenService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.utils.AuthUtils
import com.tencent.devops.model.auth.tables.records.TAuthOauth2ClientDetailsRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RefreshTokenGranter(
    private val clientService: Oauth2ClientService,
    private val accessTokenService: Oauth2AccessTokenService,
    private val refreshTokenService: Oauth2RefreshTokenService
) : AbstractTokenGranter(
    grantType = GRANT_TYPE,
    oauth2ClientService = clientService,
    accessTokenService = accessTokenService
) {
    companion object {
        private const val GRANT_TYPE = "refresh_token"
        private val logger = LoggerFactory.getLogger(RefreshTokenGranter::class.java)
    }

    override fun getAccessToken(
        accessTokenRequest: Oauth2AccessTokenRequest,
        clientDetail: TAuthOauth2ClientDetailsRecord
    ): Oauth2AccessTokenDTO {
        logger.info("refresh token getAccessToken|$accessTokenRequest|$clientDetail")
        //1.校验refresh_token是否为空
        val refreshToken = accessTokenRequest.refreshToken
            ?: throw ErrorCodeException(
                errorCode = ERROR_REFRESH_TOKEN_NOT_FOUND,
                defaultMessage = "The refresh token must be provided"
            )
        // 2.校验refresh_token是否存在
        val refreshTokenInfo = refreshTokenService.get(
            refreshToken = refreshToken
        )!!
        // 3.校验refresh_token是否跟client_id匹配
        if (refreshTokenInfo.clientId != accessTokenRequest.clientId) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.INVALID_REFRESH_TOKEN,
                defaultMessage = "The authorization code invalid"
            )
        }
        //4.清除跟该refresh_token授权码相关的access_token
        accessTokenService.deleteByRefreshToken(
            refreshToken = accessTokenRequest.refreshToken!!
        )
        //5.校验refresh_token是否过期
        if (AuthUtils.isExpired(refreshTokenInfo.expiredTime)) {
            throw ErrorCodeException(
                errorCode = ERROR_REFRESH_TOKEN_EXPIRED,
                defaultMessage = "The refresh token has expired!"
            )
        }
        return Oauth2AccessTokenDTO()
    }
}
