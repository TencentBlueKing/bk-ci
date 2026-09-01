package com.tencent.devops.auth.dao

import com.tencent.devops.common.db.utils.skipCheck
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
            dslContext.selectFrom(this)
                .where(RESOURCE_TYPE.eq(resourceType))
                .and(DELETE.eq(false))
                .fetch()
        }
    }

    fun listAll(
        dslContext: DSLContext
    ): Result<TAuthActionRecord> {
        return with(TAuthAction.T_AUTH_ACTION) {
            dslContext.selectFrom(this)
                .where(DELETE.eq(false))
                .orderBy(RESOURCE_TYPE, ACTION)
                .skipCheck()
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
                .skipCheck()
                .fetch()
        }
    }

    fun create(
        dslContext: DSLContext,
        action: String,
        resourceType: String,
        relatedResourceType: String,
        actionName: String,
        englishName: String,
        actionType: String,
        createUser: String
    ): Boolean {
        val now = LocalDateTime.now()
        return with(TAuthAction.T_AUTH_ACTION) {
            dslContext.insertInto(
                this,
                ACTION,
                RESOURCE_TYPE,
                RELATED_RESOURCE_TYPE,
                ACTION_NAME,
                ENGLISH_NAME,
                ACTION_TYPE,
                CREATE_USER,
                CREATE_TIME,
                UPDATE_TIME,
                DELETE
            ).values(
                action,
                resourceType,
                relatedResourceType,
                actionName,
                englishName,
                actionType,
                createUser,
                now,
                now,
                false
            ).execute() > 0
        }
    }

    fun batchCreate(
        dslContext: DSLContext,
        actions: List<TAuthActionRecord>
    ): Int {
        if (actions.isEmpty()) {
            return 0
        }
        val now = LocalDateTime.now()
        return with(TAuthAction.T_AUTH_ACTION) {
            var count = 0
            actions.forEach { record ->
                val result = dslContext.insertInto(
                    this,
                    ACTION,
                    RESOURCE_TYPE,
                    RELATED_RESOURCE_TYPE,
                    ACTION_NAME,
                    ENGLISH_NAME,
                    ACTION_TYPE,
                    CREATE_USER,
                    CREATE_TIME,
                    UPDATE_TIME,
                    DELETE
                ).values(
                    record.action,
                    record.resourceType,
                    record.relatedResourceType,
                    record.actionName,
                    record.englishName,
                    record.actionType,
                    record.createUser,
                    now,
                    now,
                    false
                ).onDuplicateKeyUpdate()
                    .set(ACTION_NAME, record.actionName)
                    .set(ENGLISH_NAME, record.englishName)
                    .set(UPDATE_TIME, now)
                    .execute()
                count += result
            }
            count
        }
    }

    fun delete(
        dslContext: DSLContext,
        action: String
    ): Boolean {
        return with(TAuthAction.T_AUTH_ACTION) {
            dslContext.update(this)
                .set(DELETE, true)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ACTION.eq(action))
                .execute() > 0
        }
    }

    fun updateActionName(
        dslContext: DSLContext,
        authActionI18nMap: Map<String, String>
    ) {
        authActionI18nMap.forEach {
            with(TAuthAction.T_AUTH_ACTION) {
                dslContext.update(this)
                    .set(ACTION_NAME, it.value)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .where(ACTION.eq(it.key))
                    .execute()
            }
        }
    }
}
