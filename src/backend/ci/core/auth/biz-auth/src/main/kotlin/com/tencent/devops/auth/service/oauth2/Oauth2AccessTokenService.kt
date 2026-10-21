package com.tencent.devops.auth.service.oauth2

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.crypto.Oauth2AccessTokenCryptoHelper
import com.tencent.devops.auth.dao.AuthOauth2AccessTokenDao
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.model.auth.tables.records.TAuthOauth2AccessTokenRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class Oauth2AccessTokenService(
    private val oauth2AccessTokenDao: AuthOauth2AccessTokenDao,
    private val dslContext: DSLContext,
    private val oauth2AccessTokenCryptoHelper: Oauth2AccessTokenCryptoHelper
) {

    fun get(
        clientId: String,
        accessToken: String
    ): TAuthOauth2AccessTokenRecord {
        return oauth2AccessTokenDao.get(
            dslContext = dslContext,
            clientId = clientId,
            accessToken = accessToken
        ) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.INVALID_ACCESS_TOKEN,
            params = arrayOf(clientId),
            defaultMessage = "The access token invalid"
        )
    }

    fun get(
        clientId: String,
        refreshToken: String? = null,
        userName: String? = null,
        passWord: String? = null,
        grantType: String? = null
    ): TAuthOauth2AccessTokenRecord? {
        return oauth2AccessTokenDao.get(
            dslContext = dslContext,
            clientId = clientId,
            refreshToken = refreshToken,
            userName = userName,
            passWord = passWord?.let(oauth2AccessTokenCryptoHelper::encryptSm4ButAes),
            grantType = grantType
        )
    }

    @Suppress("LongParameterList")
    fun create(
        clientId: String,
        userName: String?,
        passWord: String?,
        grantType: String,
        accessToken: String,
        refreshToken: String?,
        expiredTime: Long,
        scopeId: Int
    ) {
        oauth2AccessTokenDao.create(
            dslContext = dslContext,
            clientId = clientId,
            userName = userName,
            passWord = passWord?.let(oauth2AccessTokenCryptoHelper::encryptSm4ButAes),
            grantType = grantType,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiredTime = expiredTime,
            scopeId = scopeId,
            aesKeySha = oauth2AccessTokenCryptoHelper.currentKeySha()
        )
    }

    fun delete(accessToken: String) {
        oauth2AccessTokenDao.delete(
            dslContext = dslContext,
            accessToken = accessToken
        )
    }

    fun update(
        accessToken: String,
        scopeId: Int
    ) {
        oauth2AccessTokenDao.update(
            dslContext = dslContext,
            accessToken = accessToken,
            scopeId = scopeId
        )
    }
}
