package com.tencent.devops.auth.dao

import com.tencent.devops.model.auth.tables.TAuthOauth2RefreshToken
import com.tencent.devops.model.auth.tables.records.TAuthOauth2RefreshTokenRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class AuthOauth2RefreshTokenDao {
    fun get(
        dslContext: DSLContext,
        refreshToken: String
    ): TAuthOauth2RefreshTokenRecord? {
        return with(TAuthOauth2RefreshToken.T_AUTH_OAUTH2_REFRESH_TOKEN) {
            dslContext.selectFrom(this)
                .where(REFRESH_TOKEN.eq(refreshToken))
                .fetchOne()
        }
    }

    fun delete(
        dslContext: DSLContext,
        refreshToken: String
    ): Int {
        return with(TAuthOauth2RefreshToken.T_AUTH_OAUTH2_REFRESH_TOKEN) {
            dslContext.deleteFrom(this)
                .where(REFRESH_TOKEN.eq(refreshToken))
                .execute()
        }
    }

    fun create(
        dslContext: DSLContext,
        refreshToken: String,
        clientId: String,
        expiredTime: Long
    ): Int {
        return with(TAuthOauth2RefreshToken.T_AUTH_OAUTH2_REFRESH_TOKEN) {
            dslContext.insertInto(
                this,
                CLIENT_ID,
                REFRESH_TOKEN,
                EXPIRED_TIME
            ).values(
                clientId,
                refreshToken,
                expiredTime
            ).execute()
        }
    }
}
