package com.tencent.devops.process.dao

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.model.process.tables.TPipelineBuildCommits
import com.tencent.devops.scm.pojo.GitCommit
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit


@Suppress("ALL")
@Repository
class PipelineBuildCommitsDao {

    fun create(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        webhookCommits: List<GitCommit>,
        mrId: String
    ) {
        with(TPipelineBuildCommits.T_PIPELINE_BUILD_COMMITS) {
            webhookCommits.map {
                val commitTime =
                    DateTimeUtil.convertDateToLocalDateTime(Date(DateTimeUtil.zoneDateToTimestamp(it.committed_date)))
                dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    PIPELINE_ID,
                    BUILD_ID,
                    COMMIT_ID,
                    MESSAGE,
                    AUTHOR_NAME,
                    MERGE_REQUEST_ID,
                    COMMIT_TIME,
                    CREATE_TIME
                )
                    .values(
                        projectId,
                        pipelineId,
                        buildId,
                        it.id,
                        it.message,
                        it.author_name,
                        mrId,
                        commitTime,
                        LocalDateTime.now()
                    ).execute()
            }
        }
    }
}