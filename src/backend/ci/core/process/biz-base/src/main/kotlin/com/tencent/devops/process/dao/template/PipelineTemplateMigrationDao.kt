package com.tencent.devops.process.dao.template

import com.tencent.devops.common.pipeline.template.MigrationStatus
import com.tencent.devops.model.process.tables.TPipelineTemplateMigration
import com.tencent.devops.model.process.tables.records.TPipelineTemplateMigrationRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineTemplateMigrationDao {
    fun create(
        dslContext: DSLContext,
        projectId: String,
        status: MigrationStatus
    ) {
        val now = LocalDateTime.now()
        with(TPipelineTemplateMigration.T_PIPELINE_TEMPLATE_MIGRATION) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                STATUS,
                START_TIME,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                projectId,
                status.name,
                now,
                now,
                now
            ).onDuplicateKeyUpdate()
                .set(STATUS, status.name)
                .set(ERROR_MESSAGE, "")
                .set(START_TIME, now)
                .set(UPDATE_TIME, now)
                .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        projectId: String,
        status: MigrationStatus,
        errorMessage: String? = null,
        totalTime: Long? = null,
        beforeTemplateCount: Int? = null,
        afterTemplateCount: Int? = null
    ) {
        with(TPipelineTemplateMigration.T_PIPELINE_TEMPLATE_MIGRATION) {
            val update = dslContext.update(this)
                .set(STATUS, status.name)
                .set(END_TIME, LocalDateTime.now())
            errorMessage?.let { update.set(ERROR_MESSAGE, errorMessage) }
            totalTime?.let { update.set(TOTAL_TIME, totalTime) }
            beforeTemplateCount?.let { update.set(BEFORE_TEMPLATE_COUNT, beforeTemplateCount) }
            afterTemplateCount?.let { update.set(AFTER_TEMPLATE_COUNT, afterTemplateCount) }
            update.where(PROJECT_ID.eq(projectId)).execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        projectId: String
    ): TPipelineTemplateMigrationRecord? {
        return with(TPipelineTemplateMigration.T_PIPELINE_TEMPLATE_MIGRATION) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .fetchOne()
        }
    }
}
