package com.tencent.devops.lambda.dao

import com.tencent.devops.model.process.Tables.T_PIPELINE_BUILD_HISTORY
import com.tencent.devops.model.process.tables.records.TPipelineBuildHistoryRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PipelineBuildDao {

    fun getBuildInfo(
        dslContext: DSLContext,
        buildId: String
    ): TPipelineBuildHistoryRecord? {
        return with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .fetchOne()
        }
    }
}