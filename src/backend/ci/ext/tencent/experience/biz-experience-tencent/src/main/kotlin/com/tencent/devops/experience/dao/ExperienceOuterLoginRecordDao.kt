package com.tencent.devops.experience.dao

import com.tencent.devops.model.experience.tables.TExperienceOuterLoginHistory
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExperienceOuterLoginRecordDao {
    fun add(
        dslContext: DSLContext,
        username: String,
        realIp: String,
        loginTime: LocalDateTime,
        appVersion: String,
        platform: Int
    ) {
        with(TExperienceOuterLoginHistory.T_EXPERIENCE_OUTER_LOGIN_HISTORY) {
            dslContext.insertInto(this)
                .set(USERNAME, username)
                .set(REAL_IP, realIp)
                .set(LOGIN_TIME, loginTime)
                .set(APP_VERSION, appVersion)
                .set(PLATFORM, platform)
                .execute()
        }
    }
}
