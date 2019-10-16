package com.tencent.devops.experience.dao

import com.tencent.devops.model.experience.tables.TExperienceDownload
import com.tencent.devops.model.experience.tables.records.TExperienceDownloadRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExperienceDownloadDao {
    fun create(dslContext: DSLContext, experienceId: Long, userId: String): Long {
        val now = LocalDateTime.now()
        with(TExperienceDownload.T_EXPERIENCE_DOWNLOAD) {
            val record = dslContext.insertInto(this,
                    EXPERIENCE_ID,
                    USER_ID,
                    TIMES,
                    CREATE_TIME,
                    UPDATE_TIME
            ).values(
                    experienceId,
                    userId,
                    1,
                    now,
                    now)
                    .returning(ID)
                    .fetchOne()
            return record.id
        }
    }

    fun plusTimes(dslContext: DSLContext, id: Long) {
        with(TExperienceDownload.T_EXPERIENCE_DOWNLOAD) {
            dslContext.update(this)
                    .set(TIMES, TIMES + 1)
                    .where(ID.eq(id))
                    .execute()
        }
    }

    fun getOrNull(dslContext: DSLContext, experienceId: Long, userId: String): TExperienceDownloadRecord? {
        with(TExperienceDownload.T_EXPERIENCE_DOWNLOAD) {
            return dslContext.selectFrom(this)
                    .where(EXPERIENCE_ID.eq(experienceId))
                    .and(USER_ID.eq(userId))
                    .fetchOne()
        }
    }

    fun list(dslContext: DSLContext, experienceId: Long, offset: Int, limit: Int): Result<TExperienceDownloadRecord> {
        with(TExperienceDownload.T_EXPERIENCE_DOWNLOAD) {
            return dslContext.selectFrom(this)
                    .where(EXPERIENCE_ID.eq(experienceId))
                    .orderBy(UPDATE_TIME.desc())
                    .offset(offset)
                    .limit(limit)
                    .fetch()
        }
    }

    fun count(dslContext: DSLContext, experienceId: Long): Long {
        with(TExperienceDownload.T_EXPERIENCE_DOWNLOAD) {
            return dslContext.selectCount()
                    .from(this)
                    .where(EXPERIENCE_ID.eq(experienceId))
                    .fetchOne(0, Long::class.java)
        }
    }

    fun sumTimes(dslContext: DSLContext, experienceId: Long): Long {
        with(TExperienceDownload.T_EXPERIENCE_DOWNLOAD) {
            return dslContext.select(TIMES.sum())
                    .from(this)
                    .where(EXPERIENCE_ID.eq(experienceId))
                    .fetchOne(0, Long::class.java)
        }
    }
}