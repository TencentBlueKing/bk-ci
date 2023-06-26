package com.tencent.devops.auth.dao

import com.tencent.devops.model.auth.tables.TAuthAction
import com.tencent.devops.model.auth.tables.records.TAuthActionRecord
import java.time.LocalDateTime
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

    fun list(
        dslContext: DSLContext,
        page: Int,
        pageSize: Int
    ): Result<TAuthActionRecord> {
        return with(TAuthAction.T_AUTH_ACTION) {
            dslContext.selectFrom(this)
                .orderBy(CREATE_TIME.desc(), ACTION)
                .limit(pageSize).offset((page - 1) * pageSize)
                .fetch()
        }
    }

    fun updateActionName(
        dslContext: DSLContext,
        authActionI18nMap: Map<String, String>
    ) {
        dslContext.batch(
            authActionI18nMap.map {
                with(TAuthAction.T_AUTH_ACTION) {
                    dslContext.update(this)
                        .set(ACTION_NAME, it.value)
                        .set(UPDATE_TIME, LocalDateTime.now())
                        .where(ACTION.eq(it.key))
                }
            }
        ).execute()
    }
}
