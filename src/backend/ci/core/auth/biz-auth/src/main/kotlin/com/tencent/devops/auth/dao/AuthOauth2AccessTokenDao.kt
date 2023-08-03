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
        refreshToken: String?,
        userName: String?,
        grantType: String?
    ): TAuthOauth2AccessTokenRecord? {
        return with(TAuthOauth2AccessToken.T_AUTH_OAUTH2_ACCESS_TOKEN) {
            dslContext.selectFrom(this).where(
                CLIENT_ID.eq(clientId)
            ).let {
                if (userName != null) { it.and(USER_NAME.eq(userName)) } else { it }
            }.let {
                if (grantType != null) { it.and(GRANT_TYPE.eq(grantType)) } else { it }
            }.let {
                if (refreshToken != null) { it.and(REFRESH_TOKEN.eq(refreshToken)) } else { it }
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

    @Suppress("LongParameterList")
    fun create(
        dslContext: DSLContext,
        clientId: String,
        userName: String?,
        grantType: String,
        accessToken: String,
        refreshToken: String? = null,
        expiredTime: Long,
        scopeId: Int
    ): Int {
        return with(TAuthOauth2AccessToken.T_AUTH_OAUTH2_ACCESS_TOKEN) {
            dslContext.insertInto(
                this,
                CLIENT_ID,
                USER_NAME,
                GRANT_TYPE,
                ACCESS_TOKEN,
                REFRESH_TOKEN,
                EXPIRED_TIME,
                SCOPE_ID
            ).values(
                clientId,
                userName,
                grantType,
                accessToken,
                refreshToken,
                expiredTime,
                scopeId
            ).execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        accessToken: String,
        scopeId: Int
    ){
        return with(TAuthOauth2AccessToken.T_AUTH_OAUTH2_ACCESS_TOKEN) {
            dslContext.update(this)
                .set(SCOPE_ID, scopeId)
                .where(ACCESS_TOKEN.eq(accessToken))
                .execute()
        }
    }
}
