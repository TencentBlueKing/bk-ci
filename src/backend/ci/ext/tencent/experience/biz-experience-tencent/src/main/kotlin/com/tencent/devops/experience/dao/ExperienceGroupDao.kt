package com.tencent.devops.experience.dao

import com.tencent.devops.model.experience.tables.TExperienceGroup
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Result
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

    fun listRecordIdByGroupIds(
        dslContext: DSLContext,
        groupIds: Set<Long>
    ): Result<Record1<Long>> {
        return with(TExperienceGroup.T_EXPERIENCE_GROUP) {
            dslContext.select(RECORD_ID)
                .from(this)
                .where(GROUP_ID.`in`(groupIds))
                .fetch()
        }
    }

    fun count(dslContext: DSLContext, recordId: Long, groupId: Long): Int {
        return with(TExperienceGroup.T_EXPERIENCE_GROUP) {
            dslContext.selectCount()
                .from(this)
                .where(RECORD_ID.eq(recordId))
                .and(GROUP_ID.eq(groupId))
                .fetchOne().value1()
        }
    }
}