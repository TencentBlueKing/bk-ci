package com.tencent.devops.experience.dao

import com.tencent.devops.model.experience.tables.TExperienceGroupInner
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExperienceGroupInnerDao {
    fun create(
        dslContext: DSLContext,
        groupId: Long,
        username: String
    ) {
        val now = LocalDateTime.now()
        with(TExperienceGroupInner.T_EXPERIENCE_GROUP_INNER) {
            dslContext.insertInto(this)
                .set(GROUP_ID, groupId)
                .set(USERNAME, username)
                .set(CREATE_TIME, now)
                .set(UPDATE_TIME, now)
                .onConflictDoNothing()
                .execute()
        }
    }
}