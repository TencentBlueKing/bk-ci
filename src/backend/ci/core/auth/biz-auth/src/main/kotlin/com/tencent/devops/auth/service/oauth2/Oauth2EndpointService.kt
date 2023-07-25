package com.tencent.devops.auth.service.oauth2

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthOauth2CodeDao
import com.tencent.devops.auth.pojo.dto.Oauth2AccessTokenDTO
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo
import com.tencent.devops.auth.service.oauth2.grant.TokenGranter
import com.tencent.devops.common.api.exception.ErrorCodeException
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class Oauth2EndpointService constructor(
    private val tokenGranter: TokenGranter,
    private val clientService: ClientService,
    private val authOauth2CodeDao: AuthOauth2CodeDao,
    private val dslContext: DSLContext,
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
        // 3、校验client是否为授权码模式
        val grantType = clientDetail.authorizedGrantTypes.split(",")
        if (!grantType.contains(AUTHORIZATION_CODE_TYPE)) {
            logger.warn("The client($clientId) does not support the authorization code type")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.INVALID_AUTHORIZATION_TYPE,
                params = arrayOf(clientId),
                defaultMessage = "The client($clientId) does not support the authorization code type"
            )
        }
        // 4、校验redirectUri是否和数据库一致
        if (redirectUri != clientDetail.webServerRedirectUri) {
            logger.warn("The redirectUri is invalid|$clientId|$redirectUri")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.INVALID_REDIRECT_URI,
                params = arrayOf(redirectUri),
                defaultMessage = "The redirectUri($redirectUri) is invalid"
            )
        }
        // 5、生成授权码并存储数据库，授权码有效期为5分钟
        // todo
        val code = "authorizationCode"
        authOauth2CodeDao.create(
            dslContext = dslContext,
            code = code,
            clientId = clientId,
            //todo
            expiredTime = 111
        )
        // 6、返回授权码
        return code
    }

    fun getAccessToken(
        userId: String,
        oauth2AccessTokenDTO: Oauth2AccessTokenDTO
    ): Oauth2AccessTokenVo? {
        // 1、校验用户是否登录
        val grantType = oauth2AccessTokenDTO.grantType
        return tokenGranter.grant(grantType, oauth2AccessTokenDTO)
    }
}
