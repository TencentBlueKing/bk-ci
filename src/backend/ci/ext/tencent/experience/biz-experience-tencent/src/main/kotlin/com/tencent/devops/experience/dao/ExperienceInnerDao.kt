package com.tencent.devops.experience.dao

import com.tencent.devops.model.experience.tables.TExperienceInner
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExperienceInnerDao {
    fun create(
        dslContext: DSLContext,
        recordId: Long,
        userId: String
    ) {
        val now = LocalDateTime.now()
        with(TExperienceInner.T_EXPERIENCE_INNER) {
            dslContext.insertInto(this)
                .set(RECORD_ID, recordId)
                .set(USER_ID, userId)
                .set(CREATE_TIME, now)
                .set(UPDATE_TIME, now)
                .onConflictDoNothing()
                .execute()
        }
    }

    fun listRecordIdsByUserId(
        dslContext: DSLContext,
        userId: String
    ): Result<Record1<Long>> {
        return with(TExperienceInner.T_EXPERIENCE_INNER) {
            dslContext.select(RECORD_ID)
                .from(this)
                .where(USER_ID.eq(userId))
                .fetch()
        }
    }

    fun listUserIdsByRecordId(dslContext: DSLContext, experienceId: Long): Result<Record1<String>> {
        return with(TExperienceInner.T_EXPERIENCE_INNER) {
            dslContext.select(USER_ID)
                .from(this)
                .where(RECORD_ID.eq(experienceId))
                .fetch()
        }
    }
}