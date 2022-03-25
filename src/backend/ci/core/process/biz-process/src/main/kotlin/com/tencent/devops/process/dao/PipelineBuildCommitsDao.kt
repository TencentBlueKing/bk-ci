package com.tencent.devops.process.dao

import com.tencent.devops.model.process.tables.TPipelineBuildCommits
import com.tencent.devops.process.service.builds.PipelineBuildCommitsService
import com.tencent.devops.scm.pojo.WebhookCommit
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
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
            val num = webhookCommits.map {
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
            }.size
            logger.info("save commit success | save $num commitss")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildCommitsDao::class.java)
    }
}