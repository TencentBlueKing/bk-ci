package com.tencent.devops.auth.dao

import com.tencent.devops.model.auth.tables.TAuthOauth2ClientDetails
import com.tencent.devops.model.auth.tables.records.TAuthOauth2ClientDetailsRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class AuthOauth2ClientDetailsDao {
    fun get(
        dslContext: DSLContext,
        clientId: String
    ): TAuthOauth2ClientDetailsRecord? {
        return with(TAuthOauth2ClientDetails.T_AUTH_OAUTH2_CLIENT_DETAILS) {
            dslContext.selectFrom(this).where(
                CLIENT_ID.eq(clientId)
            )
        }.fetchOne()
    }
}
