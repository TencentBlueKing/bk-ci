package com.tencent.devops.auth.service.oauth2

import com.tencent.devops.auth.constant.AuthMessageCode.ERROR_REFRESH_TOKEN_NOT_FOUND
import com.tencent.devops.auth.constant.AuthMessageCode.INVALID_REFRESH_TOKEN
import com.tencent.devops.auth.dao.AuthOauth2RefreshTokenDao
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.model.auth.tables.records.TAuthOauth2RefreshTokenRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class Oauth2RefreshTokenService constructor(
    private val authOauth2RefreshTokenDao: AuthOauth2RefreshTokenDao,
    private val dslContext: DSLContext
) {
    fun get(refreshToken: String?): TAuthOauth2RefreshTokenRecord? {
        if (refreshToken == null)
            throw ErrorCodeException(
                errorCode = ERROR_REFRESH_TOKEN_NOT_FOUND,
                defaultMessage = "The refresh token must be provided"
            )
        return authOauth2RefreshTokenDao.get(
            dslContext = dslContext,
            refreshToken = refreshToken
        ) ?: throw ErrorCodeException(
            errorCode = INVALID_REFRESH_TOKEN,
            defaultMessage = "The refresh token invalid"
        )
    }

    fun delete(refreshToken: String?) {
        refreshToken ?: return
        authOauth2RefreshTokenDao.delete(
            dslContext = dslContext,
            refreshToken = refreshToken
        )
    }

    fun create(
        refreshToken: String,
        clientId: String,
        expiredTime: Long
    ) {
        authOauth2RefreshTokenDao.create(
            dslContext = dslContext,
            refreshToken = refreshToken,
            clientId = clientId,
            expiredTime = expiredTime
        )
    }
}
