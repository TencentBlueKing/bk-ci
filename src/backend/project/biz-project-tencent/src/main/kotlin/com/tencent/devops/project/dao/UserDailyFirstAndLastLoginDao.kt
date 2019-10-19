package com.tencent.devops.project.dao

import com.tencent.devops.model.project.tables.TUserDailyFirstAndLastLogin
import com.tencent.devops.model.project.tables.records.TUserDailyFirstAndLastLoginRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
class UserDailyFirstAndLastLoginDao {
    fun get(dslContext: DSLContext, userId: String, date: LocalDate): TUserDailyFirstAndLastLoginRecord? {
        with(TUserDailyFirstAndLastLogin.T_USER_DAILY_FIRST_AND_LAST_LOGIN) {
            return dslContext.selectFrom(this)
                .where(USER_ID.eq(userId))
                .and(DATE.eq(date))
                .fetchOne()
        }
    }

    fun create(
        dslContext: DSLContext,
        userId: String,
        date: LocalDate,
        firstLoginTime: LocalDateTime,
        lastLoginTime: LocalDateTime
    ) {
        with(TUserDailyFirstAndLastLogin.T_USER_DAILY_FIRST_AND_LAST_LOGIN) {
            dslContext.insertInto(this,
                USER_ID,
                DATE,
                FIRST_LOGIN_TIME,
                LAST_LOGIN_TIME
            ).values(
                userId,
                date,
                firstLoginTime,
                lastLoginTime
            ).execute()
        }
    }

    fun update(dslContext: DSLContext, id: Long, lastLoginTime: LocalDateTime) {
        with(TUserDailyFirstAndLastLogin.T_USER_DAILY_FIRST_AND_LAST_LOGIN) {
            dslContext.update(this)
                .set(LAST_LOGIN_TIME, lastLoginTime)
                .where(ID.eq(id))
                .execute()
        }
    }
}