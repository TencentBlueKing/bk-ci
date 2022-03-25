package com.tencent.devops.process.dao

import com.tencent.devops.model.process.tables.TPipelineBuildCommits
import com.tencent.devops.scm.pojo.WebhookCommit
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class PipelineBuildCommitsDao {

    fun create(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        webhookCommits: List<WebhookCommit>,
        mrId: String
    ) {
        with(TPipelineBuildCommits.T_PIPELINE_BUILD_COMMITS) {
            webhookCommits.map {
                dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    PIPELINE_ID,
                    BUILD_ID,
                    COMMIT_ID,
                    MESSAGE,
                    AUTHOR_NAME,
                    MERGE_REQUEST_ID,
                    REPOSITORY_TYPE,
                    COMMIT_TIME,
                    CREATE_TIME
                )
                    .values(
                        projectId,
                        pipelineId,
                        buildId,
                        it.commitId,
                        it.message,
                        it.authorName,
                        mrId,
                        it.repoType,
                        it.commitTime,
                        LocalDateTime.now()
                    ).execute()
            }
        }
    }
}
