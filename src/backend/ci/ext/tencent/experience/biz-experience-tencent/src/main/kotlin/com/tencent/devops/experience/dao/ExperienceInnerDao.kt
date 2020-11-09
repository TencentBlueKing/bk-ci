package com.tencent.devops.experience.dao

import com.tencent.devops.model.experience.tables.TExperienceInner
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExperienceInnerDao {
    fun create(
        dslContext: DSLContext,
        recordId: Long,
        username: String
    ) {
        val now = LocalDateTime.now()
        with(TExperienceInner.T_EXPERIENCE_INNER) {
            dslContext.insertInto(this)
                .set(RECORD_ID, recordId)
                .set(USERNAME, username)
                .set(CREATE_TIME, now)
                .set(UPDATE_TIME, now)
                .onConflictDoNothing()
                .execute()
        }
    }
}