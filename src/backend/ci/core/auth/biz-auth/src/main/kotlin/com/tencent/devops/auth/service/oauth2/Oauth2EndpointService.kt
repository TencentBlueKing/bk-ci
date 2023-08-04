package com.tencent.devops.auth.service.oauth2

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.pojo.Oauth2AccessTokenRequest
import com.tencent.devops.auth.pojo.dto.Oauth2AuthorizationCodeDTO
import com.tencent.devops.auth.pojo.enum.Oauth2GrantType
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo
import com.tencent.devops.auth.pojo.vo.Oauth2AuthorizationInfoVo
import com.tencent.devops.auth.service.oauth2.grant.TokenGranter
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.auth.utils.AuthUtils
import org.slf4j.LoggerFactory

class Oauth2EndpointService constructor(
    private val tokenGranter: TokenGranter,
    private val clientService: Oauth2ClientService,
    private val codeService: Oauth2CodeService,
    private val scopeService: Oauth2ScopeService,
    private val accessTokenService: Oauth2AccessTokenService
) {
    fun getAuthorizationInformation(
        userId: String,
        clientId: String
    ): Oauth2AuthorizationInfoVo {
        logger.info("get authorization information:$userId|$clientId")
        // 1、校验clientId是否存在
        val clientDetails = clientService.getClientDetails(clientId = clientId)
        // 2、校验客户端信息是否正确
        clientService.verifyClientInformation(
            clientId = clientId,
            grantType = Oauth2GrantType.AUTHORIZATION_CODE.grantType,
            clientDetails = clientDetails
        )
        return Oauth2AuthorizationInfoVo(
            userName = userId,
            clientName = clientDetails.clientName,
            scope = SCOPE
        )
    }

    fun getAuthorizationCode(
        userId: String,
        clientId: String,
        redirectUri: String,
        authorizationCodeDTO: Oauth2AuthorizationCodeDTO
    ): String {
        logger.info("get authorization code:$userId|$clientId|$redirectUri")
        // 1、校验clientId是否存在
        val clientDetails = clientService.getClientDetails(clientId = clientId)
        // 2、校验客户端信息是否正确
        clientService.verifyClientInformation(
            clientId = clientId,
            redirectUri = redirectUri,
            grantType = Oauth2GrantType.AUTHORIZATION_CODE.grantType,
            clientDetails = clientDetails,
            scope = authorizationCodeDTO.scope
        )
        // 3、存储scope信息
        val scopeId = scopeService.create(scope = authorizationCodeDTO.scope.joinToString(","))
        // 4、生成授权码并存储数据库，授权码有效期为5分钟
        val code = UUIDUtil.generate()
        codeService.create(
            userId = userId,
            code = code,
            clientId = clientId,
            scopeId = scopeId,
            codeValiditySeconds = codeValiditySeconds
        )
        // 4、返回跳转链接及授权码
        return "$redirectUri?code=$code"
    }

    fun getAccessToken(
        clientId: String,
        clientSecret: String,
        accessTokenRequest: Oauth2AccessTokenRequest
    ): Oauth2AccessTokenVo? {
        val grantType = accessTokenRequest.grantType
        logger.info("get access token:$clientId|$grantType|$accessTokenRequest")
        val clientDetails = clientService.getClientDetails(
            clientId = clientId
        )
        clientService.verifyClientInformation(
            clientId = clientId,
            clientSecret = clientSecret,
            grantType = grantType,
            clientDetails = clientDetails
        )
        return tokenGranter.grant(
            grantType = grantType,
            clientDetails = clientDetails,
            accessTokenRequest = accessTokenRequest
        )
    }

    fun verifyAccessToken(
        clientId: String,
        clientSecret: String,
        accessToken: String
    ): String {
        val clientDetails = clientService.getClientDetails(
            clientId = clientId
        )
        clientService.verifyClientInformation(
            clientId = clientId,
            clientSecret = clientSecret,
            clientDetails = clientDetails
        )
        val accessTokenInfo = accessTokenService.get(
            clientId = clientId,
            accessToken = accessToken.substringAfter(OAUTH2_SCHEME).trim()
        )
        if (AuthUtils.isExpired(accessTokenInfo.expiredTime)) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.ERROR_ACCESS_TOKEN_EXPIRED,
                params = arrayOf(clientId),
                defaultMessage = "The access token has expired!"
            )
        }
        return accessTokenInfo.userName
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Oauth2EndpointService::class.java)
        private const val codeValiditySeconds = 600L
        private val SCOPE = mutableMapOf(
            "project_visit" to "获取你有权限的项目列表",
            "pipeline_list" to "获取你有权限的流水线列表",
            "pipeline_download" to "下载你有权限的制品",
        )
        private const val OAUTH2_SCHEME = "Bearer "
    }
}
