package com.tencent.devops.process.dao

import com.tencent.devops.model.process.Tables.T_PIPELINE_WEBHOOK_SVN_REVISION
import org.jooq.DSLContext
import org.jooq.Record2
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class PipelineWebhookSvnRevisionDao {
    fun saveOrUpdateSvnRevision(
        dslContext: DSLContext,
        projectName: String,
        revision: String
    ) {
        with(T_PIPELINE_WEBHOOK_SVN_REVISION) {
            dslContext.insertInto(
                this,
                PROJECT_NAME,
                REVISION,
                CREATE_TIME,
                UPDATE_TIME
            )
                .values(
                    projectName,
                    revision,
                    LocalDateTime.now(),
                    LocalDateTime.now()
                )
                .onDuplicateKeyUpdate()
                .set(REVISION, revision)
                .set(UPDATE_TIME, LocalDateTime.now())
                .execute()
        }
    }

    fun getSvnRevisonByProjectNames(
        dslContext: DSLContext,
        projectName: List<String>
    ): Result<Record2<String, String>> {
        return with(T_PIPELINE_WEBHOOK_SVN_REVISION) {
            dslContext.select(PROJECT_NAME, REVISION).from(this).where(PROJECT_NAME.`in`(projectName)).fetch()
        }
    }
}
