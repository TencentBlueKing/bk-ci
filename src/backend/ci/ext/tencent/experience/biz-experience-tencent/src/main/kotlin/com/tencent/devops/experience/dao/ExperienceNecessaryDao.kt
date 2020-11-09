package com.tencent.devops.experience.dao

import com.tencent.devops.model.experience.tables.TExperienceNecessary
import com.tencent.devops.model.experience.tables.records.TExperienceNecessaryRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class ExperienceNecessaryDao {
    fun list(
        dslContext: DSLContext,
        offset: Int,
        limit: Int
    ): Result<TExperienceNecessaryRecord> {
        return with(TExperienceNecessary.T_EXPERIENCE_NECESSARY) {
            dslContext.selectFrom(this)
                .where(ONLINE.eq(true))
                .orderBy(UPDATE_TIME.desc())
                .limit(offset, limit)
                .fetch()
        }
    }
}