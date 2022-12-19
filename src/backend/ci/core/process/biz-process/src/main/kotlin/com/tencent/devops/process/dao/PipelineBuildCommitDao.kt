package com.tencent.devops.process.dao

import com.tencent.devops.model.process.tables.TPipelineBuildCommits
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class PipelineBuildCommitDao {

    fun create(
        dslContext: DSLContext,
        id: Long? = null,
        projectId: String,
        pipelineId: String,
        buildId: String,
        commitId: String,
        authorName: String,
        message: String,
        repoType: String,
        url: String,
        eventType: String,
        commitTime: LocalDateTime,
        mrId: String,
        channel: String,
        action: String
    ) {
        with(TPipelineBuildCommits.T_PIPELINE_BUILD_COMMITS) {
            dslContext.insertInto(
                this,
                ID,
                PROJECT_ID,
                PIPELINE_ID,
                BUILD_ID,
                COMMIT_ID,
                MESSAGE,
                AUTHOR_NAME,
                MERGE_REQUEST_ID,
                REPOSITORY_TYPE,
                URL,
                EVENT_TYPE,
                COMMIT_TIME,
                CREATE_TIME,
                CHANNEL,
                ACTION
            )
                .values(
                    id,
                    projectId,
                    pipelineId,
                    buildId,
                    commitId,
                    message,
                    authorName,
                    mrId,
                    repoType,
                    url,
                    eventType,
                    commitTime,
                    LocalDateTime.now(),
                    channel,
                    action
                ).execute()
        }
    }
}
