package com.tencent.devops.auth.dao

import com.tencent.devops.model.auth.tables.TAuthOauth2ScopeOperationInformation
import com.tencent.devops.model.auth.tables.records.TAuthOauth2ScopeOperationInformationRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class AuthOauth2ScopeOperationDao {
    fun get(
        dslContext: DSLContext,
        operationId: String
    ): TAuthOauth2ScopeOperationInformationRecord? {
        return with(TAuthOauth2ScopeOperationInformation.T_AUTH_OAUTH2_SCOPE_OPERATION_INFORMATION) {
            dslContext.selectFrom(this).where(OPERATION_ID.eq(operationId))
                .fetchOne()
        }
    }
}
