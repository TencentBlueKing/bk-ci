package com.tencent.devops.lambda.dao

import com.tencent.devops.model.process.tables.TPipelineBuildTask
import com.tencent.devops.model.process.tables.records.TPipelineBuildTaskRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class BuildTaskDao {

    fun getTask(
        dslContext: DSLContext,
        buildId: String,
        taskId: String
    ): TPipelineBuildTaskRecord? {
        with(TPipelineBuildTask.T_PIPELINE_BUILD_TASK) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(TASK_ID.eq(taskId))
                .fetchOne()
        }
    }
}