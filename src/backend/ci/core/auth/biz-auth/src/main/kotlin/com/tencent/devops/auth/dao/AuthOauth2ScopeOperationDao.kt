package com.tencent.devops.auth.dao

import com.tencent.devops.model.auth.tables.TAuthOauth2ScopeOperation
import com.tencent.devops.model.auth.tables.records.TAuthOauth2ScopeOperationRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class AuthOauth2ScopeOperationDao {
    fun get(
        dslContext: DSLContext,
        operationId: String
    ): TAuthOauth2ScopeOperationRecord? {
        return with(TAuthOauth2ScopeOperation.T_AUTH_OAUTH2_SCOPE_OPERATION) {
            dslContext.selectFrom(this).where(OPERATION_ID.eq(operationId))
                .fetchOne()
        }
    }
}
