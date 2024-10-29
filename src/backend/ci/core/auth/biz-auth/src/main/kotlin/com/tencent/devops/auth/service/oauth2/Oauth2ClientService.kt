package com.tencent.devops.auth.service.oauth2

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthOauth2ClientDetailsDao
import com.tencent.devops.auth.pojo.ClientDetailsInfo
import com.tencent.devops.auth.pojo.dto.ClientDetailsDTO
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.model.auth.tables.records.TAuthOauth2ClientDetailsRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class Oauth2ClientService constructor(
    private val dslContext: DSLContext,
    private val authOauth2ClientDetailsDao: AuthOauth2ClientDetailsDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(Oauth2ClientService::class.java)
    }

    fun getClientDetails(clientId: String): ClientDetailsInfo {
        return authOauth2ClientDetailsDao.get(
            dslContext = dslContext,
            clientId = clientId
        )?.convert() ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.ERROR_CLIENT_NOT_EXIST,
            params = arrayOf(clientId),
            defaultMessage = "the client $clientId not exists"
        )
    }

    fun TAuthOauth2ClientDetailsRecord.convert(): ClientDetailsInfo {
        return ClientDetailsInfo(
            clientId = clientId,
            clientSecret = clientSecret,
            clientName = clientName,
            scope = scope,
            authorizedGrantTypes = authorizedGrantTypes,
            redirectUri = webServerRedirectUri,
            accessTokenValidity = accessTokenValidity,
            refreshTokenValidity = refreshTokenValidity,
            icon = icon
        )
    }

    fun createClientDetails(clientDetailsDTO: ClientDetailsDTO): Boolean {
        authOauth2ClientDetailsDao.create(
            dslContext = dslContext,
            clientDetailsDTO = clientDetailsDTO
        )
        return true
    }

    fun deleteClientDetails(clientId: String): Boolean {
        authOauth2ClientDetailsDao.delete(
            dslContext = dslContext,
            clientId = clientId
        )
        return true
    }

    @Suppress("ThrowsCount", "LongParameterList")
    fun verifyClientInformation(
        clientId: String,
        clientDetails: ClientDetailsInfo,
        clientSecret: String? = null,
        redirectUri: String? = null,
        grantType: String? = null,
        scope: List<String>? = null
    ): Boolean {
        val authorizedGrantTypes = clientDetails.authorizedGrantTypes.split(",")
        if (grantType != null && !authorizedGrantTypes.contains(grantType)) {
            logger.warn("The client($clientId) does not support $grantType type")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.INVALID_AUTHORIZATION_TYPE,
                params = arrayOf(clientId),
                defaultMessage = "The client($clientId) does not support $grantType type"
            )
        }
        if (redirectUri != null && !clientDetails.redirectUri.split(",").contains(redirectUri)) {
            logger.warn("The redirectUri is invalid|$clientId|$redirectUri")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.INVALID_REDIRECT_URI,
                params = arrayOf(redirectUri),
                defaultMessage = "The redirect uri($redirectUri) is invalid"
            )
        }
        if (clientSecret != null && clientSecret != clientDetails.clientSecret) {
            logger.warn("The client($clientId) secret is invalid")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.INVALID_CLIENT_SECRET,
                params = arrayOf(clientId),
                defaultMessage = "The client($clientId) secret is invalid"
            )
        }
        if (scope != null && !clientDetails.scope.split(",").containsAll(scope)) {
            logger.warn("The client($clientId) scope is invalid")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.INVALID_SCOPE,
                params = arrayOf(clientId),
                defaultMessage = "The client($clientId) scope is invalid"
            )
        }
        return true
    }
}
