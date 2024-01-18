package com.tencent.devops.auth.service.oauth2.grant

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.constant.AuthMessageCode.ERROR_REFRESH_TOKEN_EXPIRED
import com.tencent.devops.auth.pojo.ClientDetailsInfo
import com.tencent.devops.auth.pojo.Oauth2AccessTokenRequest
import com.tencent.devops.auth.pojo.dto.Oauth2AccessTokenDTO
import com.tencent.devops.auth.pojo.enum.Oauth2GrantType
import com.tencent.devops.auth.service.oauth2.Oauth2AccessTokenService
import com.tencent.devops.auth.service.oauth2.Oauth2RefreshTokenService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.utils.AuthUtils
import org.springframework.stereotype.Service

@Service
class RefreshTokenGranter(
    private val accessTokenService: Oauth2AccessTokenService,
    private val refreshTokenService: Oauth2RefreshTokenService
) : AbstractTokenGranter(
    grantType = Oauth2GrantType.REFRESH_TOKEN.grantType,
    accessTokenService = accessTokenService
) {
    override fun getAccessToken(
        accessTokenRequest: Oauth2AccessTokenRequest,
        clientDetails: ClientDetailsInfo
    ): Oauth2AccessTokenDTO {
        val refreshToken = accessTokenRequest.refreshToken
        // 1.校验refresh_token是否存在
        val refreshTokenInfo = refreshTokenService.get(
            refreshToken = refreshToken
        )!!
        // 2.校验refresh_token是否跟client_id匹配
        if (refreshTokenInfo.clientId != clientDetails.clientId) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.INVALID_REFRESH_TOKEN,
                defaultMessage = "The refresh token invalid"
            )
        }
        // 3.根据refresh_token获取access_token，获取access_token中的user_name
        val accessTokenInfo = accessTokenService.get(
            clientId = clientDetails.clientId,
            refreshToken = accessTokenRequest.refreshToken
        ) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.INVALID_REFRESH_TOKEN,
            defaultMessage = "The refresh token invalid"
        )
        // 4.清除跟该refresh_token授权码相关的access_token
        accessTokenService.delete(
            accessToken = accessTokenInfo.accessToken
        )
        // 5.校验refresh_token是否过期
        if (AuthUtils.isExpired(refreshTokenInfo.expiredTime)) {
            // 6.删除掉refresh_token
            refreshTokenService.delete(refreshToken = refreshToken)
            throw ErrorCodeException(
                errorCode = ERROR_REFRESH_TOKEN_EXPIRED,
                defaultMessage = "The refresh token has expired!"
            )
        }
        return Oauth2AccessTokenDTO(
            userName = accessTokenInfo.userName,
            refreshToken = refreshToken,
            scopeId = accessTokenInfo.scopeId
        )
    }
}
