package com.tencent.devops.auth.service.oauth2

import com.tencent.devops.auth.dao.AuthOauth2ScopeOperationDao
import com.tencent.devops.auth.pojo.dto.ScopeOperationDTO
import com.tencent.devops.model.auth.tables.records.TAuthOauth2ScopeOperationRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class Oauth2ScopeOperationService constructor(
    private val dslContext: DSLContext,
    private val oauth2ScopeOperationDao: AuthOauth2ScopeOperationDao
) {
    fun get(
        operationId: String
    ): TAuthOauth2ScopeOperationRecord? {
        return oauth2ScopeOperationDao.get(
            dslContext = dslContext, operationId = operationId
        )
    }

    fun create(
        scopeOperationDTO: ScopeOperationDTO
    ): Boolean {
        oauth2ScopeOperationDao.create(
            dslContext = dslContext, scopeOperationDTO = scopeOperationDTO
        )
        return true
    }

    fun delete(
        operationId: String
    ): Boolean {
        oauth2ScopeOperationDao.delete(
            dslContext = dslContext, operationId = operationId
        )
        return true
    }
}
