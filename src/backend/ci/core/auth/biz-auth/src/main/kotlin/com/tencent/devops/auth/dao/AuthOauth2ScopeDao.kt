package com.tencent.devops.auth.dao

import com.tencent.devops.model.auth.tables.TAuthOauth2Scope
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class AuthOauth2ScopeDao {
    fun create(
        dslContext: DSLContext,
        scope: String
    ): Int {
        return with(TAuthOauth2Scope.T_AUTH_OAUTH2_SCOPE) {
            dslContext.insertInto(
                this,
                SCOPE
            ).values(
                scope
            ).returning(ID)
                .fetchOne()!!.id
        }
    }
}
