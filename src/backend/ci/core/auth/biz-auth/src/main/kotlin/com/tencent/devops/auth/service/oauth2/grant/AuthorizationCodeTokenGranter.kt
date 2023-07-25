package com.tencent.devops.auth.service.oauth2.grant

import com.tencent.devops.auth.pojo.Oauth2AccessTokenRequest
import com.tencent.devops.auth.pojo.dto.Oauth2AccessTokenDTO
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo
import com.tencent.devops.auth.service.oauth2.Oauth2AccessTokenService
import com.tencent.devops.auth.service.oauth2.Oauth2ClientService
import com.tencent.devops.auth.service.oauth2.Oauth2CodeService
import com.tencent.devops.auth.service.oauth2.Oauth2RefreshTokenService
import com.tencent.devops.model.auth.tables.records.TAuthOauth2ClientDetailsRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AuthorizationCodeTokenGranter constructor(
    private val clientService: Oauth2ClientService,
    private val codeService: Oauth2CodeService,
    private val accessTokenService: Oauth2AccessTokenService,
    private val refreshTokenService: Oauth2RefreshTokenService
) : AbstractTokenGranter(
    grantType = GRANT_TYPE,
    oauth2ClientService = clientService,
    accessTokenService = accessTokenService
) {
    companion object {
        private const val GRANT_TYPE = "authorization_code"
        private val logger = LoggerFactory.getLogger(AuthorizationCodeTokenGranter::class.java)
    }

    override fun getAccessToken(
        accessTokenRequest: Oauth2AccessTokenRequest,
        clientDetail: TAuthOauth2ClientDetailsRecord
    ): Oauth2AccessTokenDTO {
        logger.info("authorization_code getAccessToken|$accessTokenRequest|$clientDetail")
        val clientId = accessTokenRequest.clientId
        val code = accessTokenRequest.code
        val codeDetails = codeService.get(
            code = code
        )
        codeService.verifyCode(
            clientId = clientId,
            codeDetails = codeDetails

        )
        // 若授权码没有问题，则直接消费授权码，授权码单次有效
        codeService.consume(code = code!!)

        val accessTokenRecord = accessTokenService.get(
            clientId = clientId,
            userName = accessTokenRequest.userName
        )
        val isAccessTokenValid = accessTokenRecord != null && accessTokenRecord.expiredTime < System.currentTimeMillis()
        val refreshToken = if (isAccessTokenValid) {
            // 若accessToken未过期，refreshToken不变
            accessTokenRecord!!.refreshToken
        } else {
            val refreshToken = UUID.randomUUID().toString()
            // 若accessToken过期，refreshToken重新生成
            refreshTokenService.delete(
                refreshToken = accessTokenRecord?.refreshToken ?: ""
            )
            val refreshTokenValidity = clientDetail.refreshTokenValidity
            refreshTokenService.create(
                refreshToken = refreshToken,
                clientId = clientId,
                expiredTime = System.currentTimeMillis() + refreshTokenValidity * 1000L
            )
            refreshToken
        }
        return Oauth2AccessTokenDTO(
            accessToken = accessTokenRecord?.accessToken,
            refreshToken = refreshToken,
            expiredTime = accessTokenRecord?.expiredTime
        )
    }
}
