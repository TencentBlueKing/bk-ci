package com.tencent.devops.auth.service.oauth2.grant

import com.tencent.devops.auth.pojo.Oauth2AccessTokenRequest
import com.tencent.devops.auth.pojo.dto.Oauth2AccessTokenDTO
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo
import com.tencent.devops.auth.service.oauth2.Oauth2AccessTokenService
import com.tencent.devops.auth.service.oauth2.Oauth2ClientService
import com.tencent.devops.auth.service.oauth2.Oauth2EndpointService
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.auth.utils.AuthUtils
import com.tencent.devops.model.auth.tables.records.TAuthOauth2ClientDetailsRecord
import org.slf4j.LoggerFactory

abstract class AbstractTokenGranter(
    private val grantType: String,
    private val oauth2ClientService: Oauth2ClientService,
    private val accessTokenService: Oauth2AccessTokenService,
) : TokenGranter {
    override fun grant(
        grantType: String,
        accessTokenRequest: Oauth2AccessTokenRequest
    ): Oauth2AccessTokenVo? {
        if (this.grantType != grantType) {
            return null
        }
        // todo 记得要去掉改行日志，内包含敏感信息
        logger.info("AbstractTokenGranter|grant:$grantType|$accessTokenRequest")
        val clientId = accessTokenRequest.clientId
        val clientDetail = oauth2ClientService.getClientDetail(
            clientId = clientId
        )
        oauth2ClientService.verifyClientInformation(
            clientId = clientId,
            clientSecret = accessTokenRequest.clientSecret,
            grantType = grantType,
            clientDetail = clientDetail
        )
        val accessTokenDTO = getAccessToken(
            accessTokenRequest = accessTokenRequest,
            clientDetail = clientDetail
        )
        return handleAccessToken(
            accessTokenRequest = accessTokenRequest,
            accessTokenDTO = accessTokenDTO,
            clientDetail = clientDetail
        )
    }

    private fun handleAccessToken(
        accessTokenRequest: Oauth2AccessTokenRequest,
        accessTokenDTO: Oauth2AccessTokenDTO,
        clientDetail: TAuthOauth2ClientDetailsRecord
    ): Oauth2AccessTokenVo {
        // todo 记得要去掉改行日志，内包含敏感信息
        logger.info("AbstractTokenGranter|handleAccessToken:$accessTokenRequest|$accessTokenDTO|$clientDetail")
        val clientId = accessTokenRequest.clientId
        val accessToken = accessTokenDTO.accessToken
        val refreshToken = accessTokenDTO.refreshToken

        if (accessToken == null || AuthUtils.isExpired(accessTokenDTO.expiredTime!!)) {
            // 生成新的 access_token
            val newAccessToken = UUIDUtil.generate()
            val accessTokenValidity = clientDetail.accessTokenValidity
            val accessTokenExpiredTime = DateTimeUtil.getFutureTimestamp(accessTokenValidity)
            // 删除旧的 access_token 记录
            if (accessToken != null) {
                accessTokenService.delete(accessToken)
            }
            // 创建新的 access_token 记录
            accessTokenService.create(
                clientId = clientId,
                userName = accessTokenDTO.userName,
                accessToken = newAccessToken,
                refreshToken = refreshToken,
                expiredTime = accessTokenExpiredTime
            )
            return Oauth2AccessTokenVo(newAccessToken, accessTokenExpiredTime, refreshToken)
        } else {
            // 返回未过期的 access_token
            return Oauth2AccessTokenVo(accessToken, accessTokenDTO.expiredTime!!, refreshToken)
        }
    }

    abstract fun getAccessToken(
        accessTokenRequest: Oauth2AccessTokenRequest,
        clientDetail: TAuthOauth2ClientDetailsRecord
    ): Oauth2AccessTokenDTO

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractTokenGranter::class.java)
    }
}
