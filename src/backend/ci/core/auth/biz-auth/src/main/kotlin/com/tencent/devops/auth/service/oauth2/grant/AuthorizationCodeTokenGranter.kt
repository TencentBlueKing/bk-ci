package com.tencent.devops.auth.service.oauth2.grant

import com.tencent.devops.auth.pojo.ClientDetailsInfo
import com.tencent.devops.auth.pojo.Oauth2AccessTokenRequest
import com.tencent.devops.auth.pojo.dto.Oauth2AccessTokenDTO
import com.tencent.devops.auth.pojo.enum.Oauth2GrantType
import com.tencent.devops.auth.service.oauth2.Oauth2AccessTokenService
import com.tencent.devops.auth.service.oauth2.Oauth2CodeService
import com.tencent.devops.auth.service.oauth2.Oauth2RefreshTokenService
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.auth.utils.AuthUtils
import com.tencent.devops.model.auth.tables.records.TAuthOauth2AccessTokenRecord
import com.tencent.devops.model.auth.tables.records.TAuthOauth2CodeRecord
import org.springframework.stereotype.Service

@Service
class AuthorizationCodeTokenGranter constructor(
    private val codeService: Oauth2CodeService,
    private val accessTokenService: Oauth2AccessTokenService,
    private val refreshTokenService: Oauth2RefreshTokenService
) : AbstractTokenGranter(
    grantType = Oauth2GrantType.AUTHORIZATION_CODE.grantType,
    accessTokenService = accessTokenService
) {
    override fun getAccessToken(
        accessTokenRequest: Oauth2AccessTokenRequest,
        clientDetails: ClientDetailsInfo
    ): Oauth2AccessTokenDTO {
        val clientId = clientDetails.clientId
        val code = accessTokenRequest.code
        val codeDetails = handleAuthorizationCode(
            code = code,
            clientId = clientId
        )
        val userName = codeDetails.userName
        val accessTokenInfo = accessTokenService.get(
            clientId = clientId,
            userName = codeDetails.userName
        )
        val refreshToken = generateRefreshToken(
            clientId = clientId,
            clientDetails = clientDetails,
            accessTokenInfo = accessTokenInfo
        )
        return Oauth2AccessTokenDTO(
            accessToken = accessTokenInfo?.accessToken,
            refreshToken = refreshToken,
            expiredTime = accessTokenInfo?.expiredTime,
            userName = userName,
            scopeId = codeDetails.scopeId
        )
    }

    private fun handleAuthorizationCode(
        code: String?,
        clientId: String
    ): TAuthOauth2CodeRecord {
        val codeDetails = codeService.get(
            code = code
        )
        codeService.verifyCode(
            clientId = clientId,
            codeDetails = codeDetails
        )
        // 若授权码没有问题，则直接消费授权码，授权码单次有效
        codeService.consume(code = code!!)
        return codeDetails
    }

    private fun generateRefreshToken(
        clientId: String,
        clientDetails: ClientDetailsInfo,
        accessTokenInfo: TAuthOauth2AccessTokenRecord?
    ): String {
        val isAccessTokenValid = accessTokenInfo != null && !AuthUtils.isExpired(accessTokenInfo.expiredTime)
        return if (isAccessTokenValid) {
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
    }
}
