package com.tencent.devops.auth.dao

import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.auth.tables.TAuthResourceType
import com.tencent.devops.model.auth.tables.records.TAuthResourceTypeRecord
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class AuthResourceTypeDao {
    fun list(dslContext: DSLContext): Result<TAuthResourceTypeRecord> {
        return with(TAuthResourceType.T_AUTH_RESOURCE_TYPE) {
            dslContext.selectFrom(this)
                .where(DELETE.eq(false))
                .orderBy(ID.asc())
                .skipCheck()
                .fetch()
        }
    }

    fun list(
        dslContext: DSLContext,
        page: Int,
        pageSize: Int
    ): Result<TAuthResourceTypeRecord> {
        return with(TAuthResourceType.T_AUTH_RESOURCE_TYPE) {
            dslContext.selectFrom(this)
                .orderBy(CREATE_TIME.desc(), RESOURCE_TYPE)
                .limit(pageSize).offset((page - 1) * pageSize)
                .skipCheck()
                .fetch()
        }
    }

    fun get(
        dslContext: DSLContext,
        resourceType: String
    ): TAuthResourceTypeRecord? {
        return with(TAuthResourceType.T_AUTH_RESOURCE_TYPE) {
            dslContext.selectFrom(this)
                .where(RESOURCE_TYPE.eq(resourceType))
                .and(DELETE.eq(false))
                .fetchOne()
        }
    }

    fun getMaxId(dslContext: DSLContext): Int {
        return with(TAuthResourceType.T_AUTH_RESOURCE_TYPE) {
            dslContext.select(ID.max())
                .from(this)
                .fetchOne(0, Int::class.java) ?: 0
        }
    }

    fun create(
        dslContext: DSLContext,
        resourceType: String,
        name: String,
        englishName: String,
        desc: String?,
        englishDesc: String?,
        parent: String?,
        system: String?,
        createUser: String
    ): Int {
        val now = LocalDateTime.now()
        val newId = getMaxId(dslContext) + 1
        with(TAuthResourceType.T_AUTH_RESOURCE_TYPE) {
            dslContext.insertInto(
                this,
                ID,
                RESOURCE_TYPE,
                NAME,
                ENGLISH_NAME,
                DESC,
                ENGLISH_DESC,
                PARENT,
                SYSTEM,
                CREATE_USER,
                CREATE_TIME,
                UPDATE_USER,
                UPDATE_TIME,
                DELETE
            ).values(
                newId,
                resourceType,
                name,
                englishName,
                desc ?: name,
                englishDesc ?: englishName,
                parent,
                system,
                createUser,
                now,
                createUser,
                now,
                false
            ).execute()
        }
        return newId
    }

    fun delete(
        dslContext: DSLContext,
        resourceType: String
    ): Boolean {
        return with(TAuthResourceType.T_AUTH_RESOURCE_TYPE) {
            dslContext.update(this)
                .set(DELETE, true)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(RESOURCE_TYPE.eq(resourceType))
                .execute() > 0
        }
    }

    fun batchUpdateAuthResourceType(
        dslContext: DSLContext,
        authActionResourceTypes: List<TAuthResourceTypeRecord>
    ) {
        if (authActionResourceTypes.isEmpty()) {
            return
        }
        with(TAuthResourceType.T_AUTH_RESOURCE_TYPE) {
            authActionResourceTypes.forEach {
                dslContext.update(this)
                    .set(NAME, it.name)
                    .set(DESC, it.desc)
                    .where(ID.eq(it.id))
                    .execute()
            }
        }
    }
}
