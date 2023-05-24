package com.tencent.devops.auth.dao

import com.tencent.devops.model.auth.tables.TAuthAction
import com.tencent.devops.model.auth.tables.records.TAuthActionRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class AuthActionDao {
    fun list(
        dslContext: DSLContext,
        resourceType: String
    ): Result<TAuthActionRecord> {
        return with(TAuthAction.T_AUTH_ACTION) {
            dslContext.selectFrom(this).where(RESOURCE_TYPE.eq(resourceType))
                .fetch()
        }
    }

    fun get(
        dslContext: DSLContext,
        action: String
    ): TAuthActionRecord? {
        return with(TAuthAction.T_AUTH_ACTION) {
            dslContext.selectFrom(this).where(ACTION.eq(action))
                .fetchAny()
        }
    }
}
