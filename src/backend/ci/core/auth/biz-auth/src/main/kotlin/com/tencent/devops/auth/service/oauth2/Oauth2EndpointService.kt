package com.tencent.devops.auth.service.oauth2

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthActionDao
import com.tencent.devops.auth.pojo.Oauth2AccessTokenRequest
import com.tencent.devops.auth.pojo.enum.Oauth2GrantType
import com.tencent.devops.auth.pojo.vo.ActionInfoVo
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo
import com.tencent.devops.auth.pojo.vo.Oauth2AuthorizationInfoVo
import com.tencent.devops.auth.service.oauth2.grant.TokenGranter
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.UUIDUtil
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class Oauth2EndpointService constructor(
    private val tokenGranter: TokenGranter,
    private val clientService: Oauth2ClientService,
    private val codeService: Oauth2CodeService,
    private val dslContext: DSLContext,
    private val authActionDao: AuthActionDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(Oauth2EndpointService::class.java)
        private const val codeValiditySeconds = 300L
    }

    private val actionCache = Caffeine.newBuilder()
        .maximumSize(500)
        .expireAfterWrite(7L, TimeUnit.DAYS)
        .build<String/*action*/, ActionInfoVo>()

    fun getAuthorizationInformation(
        userId: String,
        clientId: String
    ): Oauth2AuthorizationInfoVo {
        logger.info("get authorization information:$clientId")
        // 1、校验clientId是否存在
        val clientDetails = clientService.getClientDetails(clientId = clientId)
        // 2、校验客户端信息是否正确
        clientService.verifyClientInformation(
            clientId = clientId,
            grantType = Oauth2GrantType.AUTHORIZATION_CODE.grantType,
            clientDetails = clientDetails
        )
        val scope = clientDetails.scope.split(",")
            .associate { getActionInfo(it).actionName.let { actionName -> it to actionName } }

        return Oauth2AuthorizationInfoVo(
            userName = userId,
            clientName = clientDetails.clientName,
            scope = scope
        )
    }

    fun getAuthorizationCode(
        userId: String,
        clientId: String,
        redirectUri: String
    ): String {
        logger.info("get authorization code:$userId|$clientId|$redirectUri")
        // 1、校验clientId是否存在
        val clientDetails = clientService.getClientDetails(clientId = clientId)
        // 2、校验客户端信息是否正确
        clientService.verifyClientInformation(
            clientId = clientId,
            redirectUri = redirectUri,
            grantType = Oauth2GrantType.AUTHORIZATION_CODE.grantType,
            clientDetails = clientDetails
        )
        // 3、生成授权码并存储数据库，授权码有效期为5分钟
        val code = UUIDUtil.generate()
        codeService.create(
            userId = userId,
            code = code,
            clientId = clientId,
            codeValiditySeconds = codeValiditySeconds
        )
        // 4、返回跳转链接及授权码
        return "$redirectUri?code=$code"
    }

    fun getAccessToken(
        accessTokenRequest: Oauth2AccessTokenRequest
    ): Oauth2AccessTokenVo? {
        val grantType = accessTokenRequest.grantType
        val clientId = accessTokenRequest.clientId
        logger.info("get access token:$clientId|$grantType|$accessTokenRequest")
        val clientDetails = clientService.getClientDetails(
            clientId = clientId
        )
        clientService.verifyClientInformation(
            clientId = clientId,
            clientSecret = accessTokenRequest.clientSecret,
            grantType = grantType,
            clientDetails = clientDetails
        )
        return tokenGranter.grant(
            grantType = grantType,
            clientDetails = clientDetails,
            accessTokenRequest = accessTokenRequest
        )
    }

    private fun getActionInfo(action: String): ActionInfoVo {
        if (actionCache.getIfPresent(action) == null) {
            val actionRecord = authActionDao.get(dslContext, action)
                ?: throw ErrorCodeException(
                    errorCode = AuthMessageCode.ACTION_NOT_EXIST,
                    params = arrayOf(action),
                    defaultMessage = "the action($action) does not exist"
                )
            val actionInfo = ActionInfoVo(
                action = actionRecord.action,
                actionName = actionRecord.actionName,
                resourceType = actionRecord.resourceType,
                relatedResourceType = actionRecord.relatedResourceType
            )
            actionCache.put(action, actionInfo)
        }
        return actionCache.getIfPresent(action)!!
    }
}
