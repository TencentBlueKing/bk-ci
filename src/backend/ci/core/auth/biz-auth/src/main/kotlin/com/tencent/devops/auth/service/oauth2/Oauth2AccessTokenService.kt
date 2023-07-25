package com.tencent.devops.auth.service.oauth2

import com.tencent.devops.auth.dao.AuthOauth2AccessTokenDao
import com.tencent.devops.model.auth.tables.records.TAuthOauth2AccessTokenRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class Oauth2AccessTokenService constructor(
    private val oauth2AccessTokenDao: AuthOauth2AccessTokenDao,
    private val dslContext: DSLContext
) {
    fun get(
        clientId: String,
        userName: String? = null
    ): TAuthOauth2AccessTokenRecord? {
        return oauth2AccessTokenDao.get(
            dslContext = dslContext,
            clientId = clientId,
            userName = userName
        )
    }

    fun create(
        clientId: String,
        userName: String?,
        accessToken: String,
        refreshToken: String?,
        expiredTime: Long
    ) {
        oauth2AccessTokenDao.create(
            dslContext = dslContext,
            clientId = clientId,
            userName = userName,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiredTime = expiredTime
        )
    }

    fun delete(accessToken: String) {
        oauth2AccessTokenDao.delete(
            dslContext = dslContext,
            accessToken = accessToken
        )
    }

    fun deleteByRefreshToken(refreshToken: String) {
        oauth2AccessTokenDao.deleteByRefreshToken(
            dslContext = dslContext,
            refreshToken = refreshToken
        )
    }
}
