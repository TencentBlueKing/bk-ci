package com.tencent.devops.lambda.dao.process

import com.tencent.devops.model.process.tables.TPipelineBuildCommits
import com.tencent.devops.model.process.tables.records.TPipelineBuildCommitsRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class LambdaBuildCommitDao {

    fun getCommits(
        dslContext: DSLContext,
        projectId: String,
        buildId: String
    ): Result<TPipelineBuildCommitsRecord> {
        with(TPipelineBuildCommits.T_PIPELINE_BUILD_COMMITS) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(PROJECT_ID.eq(projectId))
                .fetch()
        }
    }
}
