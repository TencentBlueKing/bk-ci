package com.tencent.devops.auth.dao

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.auth.tables.TAuthGroupPerssion
import com.tencent.devops.model.auth.tables.TAuthGroupUser
import com.tencent.devops.model.auth.tables.records.TAuthGroupPerssionRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
class AuthGroupPermissionDao {

    fun create(dslContext: DSLContext, groupCode:String, userId: String, authAction: String): Int {
        with(TAuthGroupPerssion.T_AUTH_GROUP_PERSSION) {
            return dslContext.insertInto(
                this,
                ID,
                GROUP_CODE,
                AUTH_ACTION,
                CREATE_USER,
                CREATE_TIME,
                UPDATE_USER,
                UPDATE_TIME
            ).values(
                UUIDUtil.generate(),
                groupCode,
                authAction,
                userId,
                LocalDateTime.now(),
                null,
                null
            ).execute()
        }
    }

    fun batchCreateAction(dslContext: DSLContext, groupCode:String, userId: String, authActions: List<String>) {
        if (authActions.isEmpty()) {
            return
        }
        dslContext.batch(authActions.map {
            with(TAuthGroupPerssion.T_AUTH_GROUP_PERSSION) {
                dslContext.insertInto(
                    this,
                    ID,
                    GROUP_CODE,
                    AUTH_ACTION,
                    CREATE_USER,
                    CREATE_TIME,
                    UPDATE_USER,
                    UPDATE_TIME
                ).values(
                    UUID.randomUUID().toString(),
                    groupCode,
                    it,
                    userId,
                    LocalDateTime.now(),
                    null,
                    null
                )
            }
        }).execute()
    }

    fun getByGroupCode(dslContext: DSLContext, groupCode:String): Result<TAuthGroupPerssionRecord>? {
        with(TAuthGroupPerssion.T_AUTH_GROUP_PERSSION) {
            return dslContext.selectFrom(this).where(GROUP_CODE.eq(groupCode)).fetch()
        }
    }
}