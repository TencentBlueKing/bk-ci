package com.tencent.devops.auth.service.oauth2.grant

import com.tencent.devops.auth.pojo.ClientDetailsInfo
import com.tencent.devops.auth.pojo.Oauth2AccessTokenRequest
import com.tencent.devops.auth.pojo.dto.Oauth2AccessTokenDTO
import com.tencent.devops.auth.service.oauth2.Oauth2AccessTokenService
import com.tencent.devops.auth.service.oauth2.Oauth2CodeService
import com.tencent.devops.auth.service.oauth2.Oauth2RefreshTokenService
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.auth.utils.AuthUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AuthorizationCodeTokenGranter constructor(
    private val codeService: Oauth2CodeService,
    private val accessTokenService: Oauth2AccessTokenService,
    private val refreshTokenService: Oauth2RefreshTokenService
) : AbstractTokenGranter(
    grantType = GRANT_TYPE,
    accessTokenService = accessTokenService
) {
    companion object {
        private const val GRANT_TYPE = "authorization_code"
        private val logger = LoggerFactory.getLogger(AuthorizationCodeTokenGranter::class.java)
    }

    override fun getAccessToken(
        accessTokenRequest: Oauth2AccessTokenRequest,
        clientDetails: ClientDetailsInfo
    ): Oauth2AccessTokenDTO {
        logger.info("authorization code getAccessToken|$accessTokenRequest|$clientDetails")
        val clientId = accessTokenRequest.clientId
        val code = accessTokenRequest.code
        val codeDetails = codeService.get(
            code = code
        )
        codeService.verifyCode(
            clientId = clientId,
            codeDetails = codeDetails
        )
        val userName = codeDetails.userName
        // 若授权码没有问题，则直接消费授权码，授权码单次有效
        codeService.consume(code = code!!)

        val accessTokenInfo = accessTokenService.get(
            clientId = clientId,
            userName = codeDetails.userName
        )
        val isAccessTokenValid = accessTokenInfo != null && !AuthUtils.isExpired(accessTokenInfo.expiredTime)
        val refreshToken = if (isAccessTokenValid) {
            // 若accessToken未过期，refreshToken不变
            accessTokenInfo!!.refreshToken
        } else {
            val newRefreshToken = UUIDUtil.generate()
            // 若accessToken过期，refreshToken重新生成
            refreshTokenService.delete(
                refreshToken = accessTokenInfo?.refreshToken
            )
            val refreshTokenValidity = clientDetails.refreshTokenValidity
            refreshTokenService.create(
                refreshToken = newRefreshToken,
                clientId = clientId,
                expiredTime = DateTimeUtil.getFutureTimestamp(refreshTokenValidity)
            )
            newRefreshToken
        }
        return Oauth2AccessTokenDTO(
            accessToken = accessTokenInfo?.accessToken,
            refreshToken = refreshToken,
            expiredTime = accessTokenInfo?.expiredTime,
            userName = userName
        )
    }
}
