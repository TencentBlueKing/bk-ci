package com.tencent.devops.experience.dao

import com.tencent.devops.model.experience.tables.TExperienceDownloadDetail
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
