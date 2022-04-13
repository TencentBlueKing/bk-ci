package com.tencent.devops.experience.dao

import com.tencent.devops.model.experience.tables.TExperienceDownloadDetail
import com.tencent.devops.model.experience.tables.TExperiencePublic
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExperienceDownloadDetailDao {
    @SuppressWarnings("LongParameterList")
    fun create(
        dslContext: DSLContext,
        userId: String,
        recordId: Long,
        projectId: String,
        bundleIdentifier: String,
        platform: String
    ) {
        with(TExperienceDownloadDetail.T_EXPERIENCE_DOWNLOAD_DETAIL) {
            val now = LocalDateTime.now()
            dslContext.insertInto(
                this,
                USER_ID,
                RECORD_ID,
                PROJECT_ID,
                BUNDLE_IDENTIFIER,
                PLATFORM,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                userId,
                recordId,
                projectId,
                bundleIdentifier,
                platform,
                now,
                now
            ).execute()
        }
    }

    fun countForHot(
        dslContext: DSLContext,
        projectId: String,
        bundleIdentifier: String,
        platform: String,
        hotDaysAgo: LocalDateTime
    ): Int {
        with(TExperienceDownloadDetail.T_EXPERIENCE_DOWNLOAD_DETAIL) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(BUNDLE_IDENTIFIER.eq(bundleIdentifier))
                .and(PLATFORM.eq(platform))
                .and(CREATE_TIME.gt(hotDaysAgo))
                .fetchAny()?.value1() ?: 0
        }
    }

    fun listIdsForPublic(dslContext: DSLContext, userId: String, platform: String?, limit: Int): List<Long> {
        val p = TExperiencePublic.T_EXPERIENCE_PUBLIC.`as`("p")
        val d = TExperienceDownloadDetail.T_EXPERIENCE_DOWNLOAD_DETAIL.`as`("d")
        val join = p.leftJoin(d).on(
            p.PLATFORM.eq(d.PLATFORM)
                .and(p.PROJECT_ID.eq(d.PROJECT_ID))
                .and(p.BUNDLE_IDENTIFIER.eq(d.BUNDLE_IDENTIFIER))
        )
        return dslContext.selectDistinct(p.RECORD_ID).from(join)
            .where(p.ONLINE.eq(true))
            .and(p.END_DATE.gt(LocalDateTime.now()))
            .and(d.USER_ID.eq(userId))
            .let { if (null == platform) it else it.and(p.PLATFORM.eq(platform)) }
            .orderBy(p.UPDATE_TIME.desc()).limit(limit)
            .fetch(p.RECORD_ID)
    }

    fun countDownloadHistory(
        dslContext: DSLContext,
        projectId: String,
        bundleIdentifier: String,
        platform: String,
        userId: String
    ): Int {
        with(TExperienceDownloadDetail.T_EXPERIENCE_DOWNLOAD_DETAIL) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(BUNDLE_IDENTIFIER.eq(bundleIdentifier))
                .and(USER_ID.eq(userId))
                .and(PLATFORM.eq(platform))
                .fetchAny()?.value1() ?: 0
        }
    }
}
