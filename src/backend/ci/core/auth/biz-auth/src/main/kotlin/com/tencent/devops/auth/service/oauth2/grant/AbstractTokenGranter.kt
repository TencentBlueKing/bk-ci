package com.tencent.devops.auth.service.oauth2.grant

import com.tencent.devops.auth.pojo.ClientDetailsInfo
import com.tencent.devops.auth.pojo.Oauth2AccessTokenRequest
import com.tencent.devops.auth.pojo.dto.Oauth2AccessTokenDTO
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo
import com.tencent.devops.auth.service.oauth2.Oauth2AccessTokenService
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.auth.utils.AuthUtils

abstract class AbstractTokenGranter(
    private val grantType: String,
    private val accessTokenService: Oauth2AccessTokenService
) : TokenGranter {
    override fun grant(
        grantType: String,
        clientDetails: ClientDetailsInfo,
        accessTokenRequest: Oauth2AccessTokenRequest
    ): Oauth2AccessTokenVo? {
        if (this.grantType != grantType) {
            return null
        }
        val accessTokenDTO = getAccessToken(
            accessTokenRequest = accessTokenRequest,
            clientDetails = clientDetails
        )
        return handleAccessToken(
            accessTokenRequest = accessTokenRequest,
            accessTokenDTO = accessTokenDTO,
            clientDetails = clientDetails
        )
    }

    private fun handleAccessToken(
        accessTokenRequest: Oauth2AccessTokenRequest,
        accessTokenDTO: Oauth2AccessTokenDTO,
        clientDetails: ClientDetailsInfo
    ): Oauth2AccessTokenVo {
        val clientId = clientDetails.clientId
        val accessToken = accessTokenDTO.accessToken
        val refreshToken = accessTokenDTO.refreshToken
        // 若access_token为空或者已过期，则重新生成access_token
        if (accessToken == null || AuthUtils.isExpired(accessTokenDTO.expiredTime!!)) {
            val newAccessToken = UUIDUtil.generate()
            val accessTokenValidity = clientDetails.accessTokenValidity
            val accessTokenExpiredTime = DateTimeUtil.getFutureTimestamp(accessTokenValidity)
            // 删除过期的access_token记录
            if (accessToken != null) {
                accessTokenService.delete(accessToken)
            }
            // 创建新的 access_token记录
            accessTokenService.create(
                clientId = clientId,
                userName = accessTokenDTO.userName,
                grantType = grantType,
                accessToken = newAccessToken,
                refreshToken = refreshToken,
                expiredTime = accessTokenExpiredTime,
                scopeId = accessTokenDTO.scopeId
            )
            return Oauth2AccessTokenVo(newAccessToken, accessTokenExpiredTime, refreshToken)
        } else {
            // scope可能会变化，需要更新
            accessTokenService.update(
                accessToken = accessToken,
                scopeId = accessTokenDTO.scopeId
            )
            // 返回未过期的 access_token
            return Oauth2AccessTokenVo(accessToken, accessTokenDTO.expiredTime!!, refreshToken)
        }
    }

    abstract fun getAccessToken(
        accessTokenRequest: Oauth2AccessTokenRequest,
        clientDetails: ClientDetailsInfo
    ): Oauth2AccessTokenDTO
}
