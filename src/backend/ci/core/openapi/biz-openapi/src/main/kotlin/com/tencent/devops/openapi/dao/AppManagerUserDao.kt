package com.tencent.devops.openapi.dao

import com.tencent.devops.model.openapi.tables.TAppUserInfo
import com.tencent.devops.model.openapi.tables.records.TAppUserInfoRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class AppManagerUserDao {
    fun set(
        dslContext: DSLContext,
        managerUser: String,
        appCode: String,
        createUser: String,
        exist: Boolean
    ): Boolean {
        val now = LocalDateTime.now()
        with(TAppUserInfo.T_APP_USER_INFO) {
            return if (exist) {
                dslContext.update(this)
                    .set(APP_CODE, appCode)
                    .set(MANAGER_ID, managerUser)
                    .set(IS_DELETE, false)
                    .set(CREATE_USER, createUser)
                    .set(CREATE_TIME, now)
                    .where(APP_CODE.eq(appCode))
                    .execute() > 0
            } else {
                dslContext.insertInto(this,
                    APP_CODE,
                    MANAGER_ID,
                    IS_DELETE,
                    CREATE_USER,
                    CREATE_TIME
                ).values(
                    appCode,
                    managerUser,
                    false,
                    createUser,
                    now
                ).execute() > 0
            }
        }
    }

    fun list(
        dslContext: DSLContext
    ): List<TAppUserInfoRecord>? {
        with(TAppUserInfo.T_APP_USER_INFO) {
            return dslContext.selectFrom(this)
                .where(IS_DELETE.eq(false))
                .fetch()
        }
    }

    fun get(
        dslContext: DSLContext,
        appCode: String
    ): TAppUserInfoRecord? {
        with(TAppUserInfo.T_APP_USER_INFO) {
            return dslContext.selectFrom(this)
                .where(APP_CODE.eq(appCode)
                    .and(IS_DELETE.eq(false))).fetchAny()
        }
    }

    fun getById(
        dslContext: DSLContext,
        id: Int
    ): TAppUserInfoRecord? {
        with(TAppUserInfo.T_APP_USER_INFO) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id)).fetchAny()
        }
    }

    fun delete(
        dslContext: DSLContext,
        id: Int
    ) {
        with(TAppUserInfo.T_APP_USER_INFO) {
            dslContext.delete(this).where(ID.eq(id)).execute()
        }
    }
}
