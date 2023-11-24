package com.tencent.devops.auth.dao

import com.tencent.devops.auth.pojo.dto.ScopeOperationDTO
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

    fun create(
        dslContext: DSLContext,
        scopeOperationDTO: ScopeOperationDTO
    ) {
        with(TAuthOauth2ScopeOperation.T_AUTH_OAUTH2_SCOPE_OPERATION) {
            dslContext.insertInto(
                this,
                ID,
                OPERATION_ID,
                OPERATION_NAME_CN,
                OPERATION_NAME_EN
            ).values(
                scopeOperationDTO.id,
                scopeOperationDTO.operationId,
                scopeOperationDTO.operationNameCn,
                scopeOperationDTO.operationNameEn
            ).execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        operationId: String
    ) {
        with(TAuthOauth2ScopeOperation.T_AUTH_OAUTH2_SCOPE_OPERATION) {
            dslContext.deleteFrom(this).where(OPERATION_ID.eq(operationId)).execute()
        }
    }
}
