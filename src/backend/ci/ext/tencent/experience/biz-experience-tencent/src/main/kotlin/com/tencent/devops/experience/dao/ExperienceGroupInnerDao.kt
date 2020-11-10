package com.tencent.devops.experience.dao

import com.tencent.devops.model.experience.tables.TExperienceGroupInner
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExperienceGroupInnerDao {
    fun create(
        dslContext: DSLContext,
        groupId: Long,
        userId: String
    ) {
        val now = LocalDateTime.now()
        with(TExperienceGroupInner.T_EXPERIENCE_GROUP_INNER) {
            dslContext.insertInto(this)
                .set(GROUP_ID, groupId)
                .set(USER_ID, userId)
                .set(CREATE_TIME, now)
                .set(UPDATE_TIME, now)
                .onConflictDoNothing()
                .execute()
        }
    }

    fun listGroupIdsByUserId(
        dslContext: DSLContext,
        userId: String
    ): Result<Record1<Long>> {
        return with(TExperienceGroupInner.T_EXPERIENCE_GROUP_INNER) {
            dslContext.select(GROUP_ID)
                .from(this)
                .where(USER_ID.eq(userId))
                .fetch()
        }
    }
}