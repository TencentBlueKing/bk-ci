package com.tencent.devops.auth.service.oauth2

import com.tencent.devops.auth.dao.AuthOauth2ScopeDao
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class Oauth2ScopeService constructor(
    private val dslContext: DSLContext,
    private val authOauth2ScopeDao: AuthOauth2ScopeDao
) {
    fun create(
        scope: String
    ): Int {
        return authOauth2ScopeDao.create(
            dslContext = dslContext,
            scope = scope
        )
    }
}
