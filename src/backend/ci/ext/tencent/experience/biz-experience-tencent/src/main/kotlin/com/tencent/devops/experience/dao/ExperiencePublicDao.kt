package com.tencent.devops.experience.dao

import com.tencent.devops.model.experience.tables.TExperiencePublic
import com.tencent.devops.model.experience.tables.records.TExperiencePublicRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExperiencePublicDao {
    fun listHot(
        dslContext: DSLContext,
        offset: Int,
        limit: Int,
        category: Int? = null,
        platform: String?
    ): Result<TExperiencePublicRecord> {
        val now = LocalDateTime.now()
        return with(TExperiencePublic.T_EXPERIENCE_PUBLIC) {
            dslContext.selectFrom(this)
                .where(END_DATE.gt(now))
                .and(ONLINE.eq(true))
                .let {
                    if (null == category) it else it.and(CATEGORY.eq(category))
                }
                .let {
                    if (null == platform) it else it.and(PLATFORM.eq(platform))
                }
                .orderBy(DOWNLOAD_TIME.desc())
                .limit(offset, limit)
                .fetch()
        }
    }

    fun listNew(
        dslContext: DSLContext,
        offset: Int,
        limit: Int,
        category: Int? = null,
        platform: String?
    ): Result<TExperiencePublicRecord> {
        val now = LocalDateTime.now()
        return with(TExperiencePublic.T_EXPERIENCE_PUBLIC) {
            dslContext.selectFrom(this)
                .where(END_DATE.gt(now))
                .and(ONLINE.eq(true))
                .let {
                    if (null == category) it else it.and(CATEGORY.eq(category))
                }
                .let {
                    if (null == platform) it else it.and(PLATFORM.eq(platform))
                }
                .orderBy(UPDATE_TIME.desc())
                .limit(offset, limit)
                .fetch()
        }
    }

    fun listLikeExperienceName(
        dslContext: DSLContext,
        experienceName: String,
        platform: String?
    ): Result<TExperiencePublicRecord> {
        val now = LocalDateTime.now()
        return with(TExperiencePublic.T_EXPERIENCE_PUBLIC) {
            dslContext.selectFrom(this)
                .where(END_DATE.gt(now))
                .and(ONLINE.eq(true))
                .and(EXPERIENCE_NAME.like("%$experienceName%"))
                .let {
                    if (null == platform) it else it.and(PLATFORM.eq(platform))
                }
                .orderBy(UPDATE_TIME.desc())
                .limit(100)
                .fetch()
        }
    }

    fun create(
        dslContext: DSLContext,
        recordId: Long,
        projectId: String,
        experienceName: String,
        category: Int,
        platform: String,
        bundleIdentifier: String,
        endDate: LocalDateTime,
        size: Long,
        iconUrl: String
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
                SIZE,
                ICON_URL
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
                size,
                iconUrl
            ).onDuplicateKeyUpdate()
                .set(RECORD_ID, recordId)
                .set(EXPERIENCE_NAME, experienceName)
                .set(CATEGORY, category)
                .set(END_DATE, endDate)
                .set(ONLINE, true)
                .set(UPDATE_TIME, now)
                .set(SIZE, size)
                .set(ICON_URL, iconUrl)
                .execute()
        }
    }

    fun updateByBundleId(
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

    fun updateByRecordId(
        dslContext: DSLContext,
        recordId: Long,
        online: Boolean? = null,
        endDate: LocalDateTime? = null
    ) {
        val now = LocalDateTime.now()
        with(TExperiencePublic.T_EXPERIENCE_PUBLIC) {
            dslContext.update(this)
                .set(UPDATE_TIME, now)
                .let { if (null == online) it else it.set(ONLINE, online) }
                .let { if (null == endDate) it else it.set(END_DATE, endDate) }
                .where(RECORD_ID.eq(recordId))
                .execute()
        }
    }
}