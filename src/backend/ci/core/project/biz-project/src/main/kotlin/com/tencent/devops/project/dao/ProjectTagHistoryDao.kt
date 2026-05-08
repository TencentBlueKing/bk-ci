package com.tencent.devops.project.dao

import com.tencent.devops.model.project.Tables
import com.tencent.devops.model.project.tables.records.TProjectTagHistoryRecord
import com.tencent.devops.project.pojo.ProjectReleaseBatchCreateDTO
import com.tencent.devops.project.pojo.enums.ProjectReleaseBatchStatus
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class ProjectTagHistoryDao {

    fun batchCreate(
        dslContext: DSLContext,
        records: List<ProjectReleaseBatchCreateDTO>
    ) {
        if (records.isEmpty()) {
            return
        }
        with(Tables.T_PROJECT_TAG_HISTORY) {
            val queries = records.map {
                dslContext.insertInto(
                    this,
                    VERSION,
                    CHANNEL,
                    PROJECT_ID,
                    BATCH_PERCENT,
                    SOURCE_TAG,
                    TARGET_TAG,
                    STATUS
                ).values(
                    it.version,
                    it.channel,
                    it.projectId,
                    it.batchPercent,
                    it.sourceTag,
                    it.targetTag,
                    it.status.name
                ).onDuplicateKeyIgnore()
            }
            dslContext.batch(queries).execute()
        }
    }

    fun listHistoryRecords(
        dslContext: DSLContext,
        version: String,
        channel: String,
        batchPercent: Int
    ): List<TProjectTagHistoryRecord> {
        return with(Tables.T_PROJECT_TAG_HISTORY) {
            dslContext.selectFrom(this)
                .where(VERSION.eq(version))
                .and(CHANNEL.eq(channel))
                .and(BATCH_PERCENT.eq(batchPercent))
                .fetch()
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        version: String,
        channel: String,
        batchPercent: Int,
        status: ProjectReleaseBatchStatus
    ): Int {
        return with(Tables.T_PROJECT_TAG_HISTORY) {
            dslContext.update(this)
                .set(STATUS, status.name)
                .where(VERSION.eq(version))
                .and(CHANNEL.eq(channel))
                .and(BATCH_PERCENT.eq(batchPercent))
                .execute()
        }
    }
}
