package com.tencent.devops.auth.service.oauth2.grant

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.constant.AuthMessageCode.ERROR_REFRESH_TOKEN_EXPIRED
import com.tencent.devops.auth.pojo.ClientDetailsInfo
import com.tencent.devops.auth.pojo.Oauth2AccessTokenRequest
import com.tencent.devops.auth.pojo.dto.Oauth2AccessTokenDTO
import com.tencent.devops.auth.service.oauth2.Oauth2AccessTokenService
import com.tencent.devops.auth.service.oauth2.Oauth2RefreshTokenService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.utils.AuthUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RefreshTokenGranter(
    private val accessTokenService: Oauth2AccessTokenService,
    private val refreshTokenService: Oauth2RefreshTokenService
) : AbstractTokenGranter(
    grantType = GRANT_TYPE,
    accessTokenService = accessTokenService
) {
    companion object {
        private const val GRANT_TYPE = "refresh_token"
        private val logger = LoggerFactory.getLogger(RefreshTokenGranter::class.java)
    }

    override fun getAccessToken(
        accessTokenRequest: Oauth2AccessTokenRequest,
        clientDetails: ClientDetailsInfo
    ): Oauth2AccessTokenDTO {
        logger.info("refresh token getAccessToken|$accessTokenRequest|$clientDetails")
        val refreshToken = accessTokenRequest.refreshToken
        // 1.校验refresh_token是否存在
        val refreshTokenInfo = refreshTokenService.get(
            refreshToken = refreshToken
        )!!
        // 2.校验refresh_token是否跟client_id匹配
        if (refreshTokenInfo.clientId != accessTokenRequest.clientId) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.INVALID_REFRESH_TOKEN,
                defaultMessage = "The refresh token invalid"
            )
        }
        // 3.根据refresh_token获取access_token，获取access_token中的user_name
        val accessTokenInfo = accessTokenService.get(
            clientId = accessTokenRequest.clientId,
            refreshToken = accessTokenRequest.refreshToken
        ) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.INVALID_REFRESH_TOKEN,
            defaultMessage = "The refresh token invalid"
        )
        // 4.校验refresh_token是否过期
        if (AuthUtils.isExpired(refreshTokenInfo.expiredTime)) {
            // 5.删除掉refresh_token
            refreshTokenService.delete(refreshToken = refreshToken)
            throw ErrorCodeException(
                errorCode = ERROR_REFRESH_TOKEN_EXPIRED,
                defaultMessage = "The refresh token has expired!"
            )
        }
        return Oauth2AccessTokenDTO(
            accessToken = accessTokenInfo.accessToken,
            userName = accessTokenInfo.userName,
            expiredTime = 0L,
            refreshToken = refreshToken
        )
    }
}
