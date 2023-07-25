package com.tencent.devops.auth.service.oauth2

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthOauth2ClientDetailsDao
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.model.auth.tables.records.TAuthOauth2ClientDetailsRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ClientService constructor(
    private val dslContext: DSLContext,
    private val authOauth2ClientDetailsDao: AuthOauth2ClientDetailsDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ClientService::class.java)
    }

    fun getClientDetail(clientId: String): TAuthOauth2ClientDetailsRecord {
        return authOauth2ClientDetailsDao.get(
            dslContext = dslContext,
            clientId = clientId
        ) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.ERROR_CLIENT_NOT_EXIST,
            params = arrayOf(clientId),
            defaultMessage = "the client $clientId not exists"
        )
    }

    fun verifyClientInformation(
        clientId: String,
        redirectUri: String,
        grantType: String,
        clientDetail: TAuthOauth2ClientDetailsRecord,
    ): Boolean {
        val authorizedGrantTypes = clientDetail.authorizedGrantTypes.split(",")
        if (!authorizedGrantTypes.contains(grantType)) {
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
        return true
    }
}
