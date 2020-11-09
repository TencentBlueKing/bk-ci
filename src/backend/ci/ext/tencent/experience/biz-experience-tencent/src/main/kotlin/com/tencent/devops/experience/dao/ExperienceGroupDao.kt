package com.tencent.devops.experience.dao

import com.tencent.devops.model.experience.tables.TExperienceGroup
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExperienceGroupDao {
    fun create(
        dslContext: DSLContext,
        recordId: Long,
        groupId: Long
    ) {
        val now = LocalDateTime.now()
        with(TExperienceGroup.T_EXPERIENCE_GROUP) {
            dslContext.insertInto(this)
                .set(RECORD_ID, recordId)
                .set(GROUP_ID, groupId)
                .set(CREATE_TIME, now)
                .set(UPDATE_TIME, now)
                .onConflictDoNothing()
                .execute()
        }
    }
}