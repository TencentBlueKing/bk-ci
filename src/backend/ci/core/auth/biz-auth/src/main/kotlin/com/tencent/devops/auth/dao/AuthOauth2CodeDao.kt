package com.tencent.devops.auth.dao

import com.tencent.devops.model.auth.tables.TAuthOauth2Code
import com.tencent.devops.model.auth.tables.records.TAuthOauth2CodeRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class AuthOauth2CodeDao {
    fun get(
        dslContext: DSLContext,
        code: String
    ): TAuthOauth2CodeRecord? {
        return with(TAuthOauth2Code.T_AUTH_OAUTH2_CODE) {
            dslContext.selectFrom(this).where(CODE.eq(code))
                .fetchOne()
        }
    }

    @Suppress("LongParameterList")
    fun create(
        dslContext: DSLContext,
        code: String,
        userId: String,
        clientId: String,
        scopeId: Int,
        expiredTime: Long
    ): Int {
        return with(TAuthOauth2Code.T_AUTH_OAUTH2_CODE) {
            dslContext.insertInto(
                this,
                CLIENT_ID,
                CODE,
                SCOPE_ID,
                USER_NAME,
                EXPIRED_TIME
            ).values(
                clientId,
                code,
                scopeId,
                userId,
                expiredTime
            ).execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        code: String
    ): Int {
        return with(TAuthOauth2Code.T_AUTH_OAUTH2_CODE) {
            dslContext.deleteFrom(this)
                .where(CODE.eq(code))
                .execute()
        }
    }
}
