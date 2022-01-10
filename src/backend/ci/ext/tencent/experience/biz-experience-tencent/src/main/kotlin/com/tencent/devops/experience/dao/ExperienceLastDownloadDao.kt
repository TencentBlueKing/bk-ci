package com.tencent.devops.experience.dao

import com.google.common.cache.CacheBuilder
import com.tencent.devops.model.experience.tables.TExperienceLastDownload
import com.tencent.devops.model.experience.tables.records.TExperienceLastDownloadRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Repository
class ExperienceLastDownloadDao {
    private val userDownloadCache =
        CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).maximumSize(50000)
            .build<String, Result<TExperienceLastDownloadRecord>?>()

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

    fun get(
        dslContext: DSLContext,
        userId: String,
        bundleId: String,
        projectId: String,
        platform: String
    ): TExperienceLastDownloadRecord? {
        with(TExperienceLastDownload.T_EXPERIENCE_LAST_DOWNLOAD) {
            return dslContext.selectFrom(this)
                .where(USER_ID.eq(userId))
                .and(BUNDLE_IDENTIFIER.eq(bundleId))
                .and(PROJECT_ID.eq(projectId))
                .and(PLATFORM.eq(platform))
                .fetchOne()
        }
    }

    fun listByUserId(
        dslContext: DSLContext,
        userId: String
    ): Result<TExperienceLastDownloadRecord>? {
        return userDownloadCache.get(userId) {
            with(TExperienceLastDownload.T_EXPERIENCE_LAST_DOWNLOAD) {
                dslContext.selectFrom(this)
                    .where(USER_ID.eq(userId))
                    .fetch()
            }
        }
    }
}
