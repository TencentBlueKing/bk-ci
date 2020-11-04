package com.tencent.devops.experience.dao

import com.tencent.devops.model.experience.tables.TExperiencePublic
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExperiencePublicDao {
    fun create(
        dslContext: DSLContext,
        recordId: Long,
        projectId: String,
        experienceName: String,
        category: Int,
        platform: String,
        bundleIdentifier: String,
        endDate: LocalDateTime,
        size: Long
    ) {
        val now = LocalDateTime.now()
        with(TExperiencePublic.T_EXPERIENCE_PUBLIC) {
            dslContext.insertInto(
                this,
                RECORD_ID,
                PROJECT_ID,
                EXPERIENCE_NAME,
                CATEGORY,
                PLATFORM,
                BUNDLE_IDENTIFIER,
                END_DATE,
                ONLINE,
                CREATE_TIME,
                UPDATE_TIME,
                DOWNLOAD_TIME,
                SIZE
            ).values(
                recordId,
                projectId,
                experienceName,
                category,
                platform,
                bundleIdentifier,
                endDate,
                true,
                now,
                now,
                0,
                size
            ).onDuplicateKeyUpdate()
                .set(RECORD_ID, recordId)
                .set(EXPERIENCE_NAME, experienceName)
                .set(CATEGORY, category)
                .set(END_DATE, endDate)
                .set(ONLINE, true)
                .set(UPDATE_TIME, now)
                .set(SIZE, size)
                .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        projectId: String,
        platform: String,
        bundleIdentifier: String,
        online: Boolean?
    ) {
        val now = LocalDateTime.now()
        with(TExperiencePublic.T_EXPERIENCE_PUBLIC) {
            dslContext.update(this)
                .set(UPDATE_TIME, now)
                .let { if (null == online) it else it.set(ONLINE, online) }
                .where(PROJECT_ID.eq(projectId).and(PLATFORM.eq(platform)).and(BUNDLE_IDENTIFIER.eq(bundleIdentifier)))
                .execute()
        }
    }
}