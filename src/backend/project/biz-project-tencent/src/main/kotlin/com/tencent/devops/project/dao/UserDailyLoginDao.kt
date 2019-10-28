package com.tencent.devops.project.dao

import com.tencent.devops.model.project.tables.TUserDailyLogin
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
class UserDailyLoginDao {
    fun create(
        dslContext: DSLContext,
        userId: String,
        os: String,
        ip: String
    ) {
        with(TUserDailyLogin.T_USER_DAILY_LOGIN) {
            dslContext.insertInto(this,
                USER_ID,
                DATE,
                LOGIN_TIME,
                OS,
                IP
            ).values(
                userId,
                LocalDate.now(),
                LocalDateTime.now(),
                os,
                ip
            ).execute()
        }
    }
}