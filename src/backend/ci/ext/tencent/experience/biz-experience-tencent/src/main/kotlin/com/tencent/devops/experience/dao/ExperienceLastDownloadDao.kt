package com.tencent.devops.experience.dao

import com.tencent.devops.model.experience.tables.TExperienceLastDownload
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExperienceLastDownloadDao {
    fun upset(
        dslContext: DSLContext,
        userId: String,
        bundleId: String,
        projectId: String,
        platform: String,
        recordId: Long
    ) {
        with(TExperienceLastDownload.T_EXPERIENCE_LAST_DOWNLOAD) {
            val now = LocalDateTime.now()
            dslContext.insertInto(this)
                .columns(
                    USER_ID,
                    BUNDLE_IDENTIFIER,
                    PROJECT_ID,
                    PLATFORM,
                    LAST_DONWLOAD_RECORD_ID,
                    CREATE_TIME,
                    UPDATE_TIME
                ).values(
                    userId,
                    bundleId,
                    projectId,
                    platform,
                    recordId,
                    now,
                    now
                ).onDuplicateKeyUpdate()
                .set(LAST_DONWLOAD_RECORD_ID, recordId)
                .set(UPDATE_TIME, now)
                .where(USER_ID.eq(userId))
                .and(BUNDLE_IDENTIFIER.eq(bundleId))
                .and(PROJECT_ID.eq(projectId))
                .and(PLATFORM.eq(platform))
                .execute()
        }
    }
}
