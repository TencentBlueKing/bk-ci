package com.tencent.devops.experience.dao

import com.tencent.devops.model.experience.Tables.T_EXPERIENCE_BANNER
import com.tencent.devops.model.experience.tables.records.TExperienceBannerRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class ExperienceBannerDao {

    fun listAvailable(
        dslContext: DSLContext,
        offset: Int,
        limit: Int
    ): Result<TExperienceBannerRecord> {
        return with(T_EXPERIENCE_BANNER) {
            dslContext.selectFrom(this)
                .where(ONLINE.eq(true))
                .limit(offset, limit)
                .fetch()
        }
    }
}