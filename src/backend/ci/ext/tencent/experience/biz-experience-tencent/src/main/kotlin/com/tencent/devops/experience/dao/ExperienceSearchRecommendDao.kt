package com.tencent.devops.experience.dao

import com.tencent.devops.model.experience.tables.TExperienceSrchRecommend
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class ExperienceSearchRecommendDao {
    fun listContent(
        dslContext: DSLContext,
        platform: String?
    ): Result<Record1<String>>? {
        return with(TExperienceSrchRecommend.T_EXPERIENCE_SRCH_RECOMMEND) {
            dslContext.select(CONTENT)
                .from(this)
                .where(ONLINE.eq(true))
                .let {
                    if (null == platform) it else it.and(PLATFORM.eq(platform))
                }
                .orderBy(UPDATE_TIME.desc())
                .limit(100)
                .fetch()
        }
    }
}