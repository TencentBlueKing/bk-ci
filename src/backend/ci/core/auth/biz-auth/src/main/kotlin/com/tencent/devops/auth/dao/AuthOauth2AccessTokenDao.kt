package com.tencent.devops.auth.dao

import com.tencent.devops.model.auth.tables.TAuthOauth2AccessToken
import com.tencent.devops.model.auth.tables.records.TAuthOauth2AccessTokenRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class AuthOauth2AccessTokenDao {
    fun get(
        dslContext: DSLContext,
        clientId: String,
        userName: String? = null
    ): TAuthOauth2AccessTokenRecord? {
        return with(TAuthOauth2AccessToken.T_AUTH_OAUTH2_ACCESS_TOKEN) {
            dslContext.selectFrom(this).where(
                CLIENT_ID.eq(clientId)
            ).let {
                if (userName != null) {
                    it.and(USER_NAME.eq(userName))
                } else {
                    it
                }
            }.fetchOne()
        }
    }

    fun delete(
        dslContext: DSLContext,
        accessToken: String
    ): Int {
        return with(TAuthOauth2AccessToken.T_AUTH_OAUTH2_ACCESS_TOKEN) {
            dslContext.deleteFrom(this)
                .where(ACCESS_TOKEN.eq(accessToken))
                .execute()
        }
    }

    fun deleteByRefreshToken(
        dslContext: DSLContext,
        refreshToken: String
    ): Int {
        return with(TAuthOauth2AccessToken.T_AUTH_OAUTH2_ACCESS_TOKEN) {
            dslContext.deleteFrom(this)
                .where(REFRESH_TOKEN.eq(refreshToken))
                .execute()
        }
    }

    @Suppress("LongParameterList")
    fun create(
        dslContext: DSLContext,
        clientId: String,
        userName: String?,
        accessToken: String,
        refreshToken: String? = null,
        expiredTime: Long
    ): Int {
        return with(TAuthOauth2AccessToken.T_AUTH_OAUTH2_ACCESS_TOKEN) {
            dslContext.insertInto(
                this,
                CLIENT_ID,
                USER_NAME,
                ACCESS_TOKEN,
                REFRESH_TOKEN,
                EXPIRED_TIME,
            ).values(
                clientId,
                userName,
                accessToken,
                refreshToken,
                expiredTime
            ).execute()
        }
    }
}
