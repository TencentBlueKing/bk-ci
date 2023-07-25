package com.tencent.devops.auth.service.oauth2

import com.tencent.devops.auth.pojo.Oauth2AccessTokenRequest
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo
import com.tencent.devops.auth.service.oauth2.grant.TokenGranter
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class Oauth2EndpointService constructor(
    private val tokenGranter: TokenGranter,
    private val clientService: Oauth2ClientService,
    private val codeService: Oauth2CodeService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(Oauth2EndpointService::class.java)
        private const val AUTHORIZATION_CODE_TYPE = "authorization_code"
    }

    fun getAuthorizationCode(
        userId: String,
        clientId: String,
        redirectUri: String
    ): String {
        logger.info("Oauth2EndpointService|getAuthorizationCode: $userId $clientId, $redirectUri")
        // 1、校验用户是否登录
        // 2、校验clientId是否存在
        val clientDetail = clientService.getClientDetail(clientId = clientId)
        // 3、校验客户端信息是否正确
        clientService.verifyClientInformation(
            clientId = clientId,
            redirectUri = redirectUri,
            grantType = AUTHORIZATION_CODE_TYPE,
            clientDetail = clientDetail
        )
        // 4、生成授权码并存储数据库，授权码有效期为5分钟
        val code = UUID.randomUUID().toString()
        codeService.create(
            code = code,
            clientId = clientId,
        )
        // 5、返回授权码
        return code
    }

    fun getAccessToken(
        userId: String,
        accessTokenRequest: Oauth2AccessTokenRequest
    ): Oauth2AccessTokenVo? {
        // 1、校验用户是否登录
        val grantType = accessTokenRequest.grantType
        return tokenGranter.grant(grantType = grantType, accessTokenRequest = accessTokenRequest)
    }
}
