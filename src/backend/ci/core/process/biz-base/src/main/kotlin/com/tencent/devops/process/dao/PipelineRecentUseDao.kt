package com.tencent.devops.process.dao

import com.tencent.devops.model.process.tables.TPipelineRecentUse
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 最近使用的流水线
 */
@Repository
class PipelineRecentUseDao {
    /**
     * 新增记录
     */
    fun add(
        dslContext: DSLContext,
        projectId: String,
        userId: String,
        pipelineId: String
    ) {
        val now = LocalDateTime.now()
        with(TPipelineRecentUse.T_PIPELINE_RECENT_USE) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                USER_ID,
                PIPELINE_ID,
                USE_TIME
            ).values(
                projectId,
                userId,
                pipelineId,
                now
            ).onDuplicateKeyUpdate().set(USE_TIME, now).execute()
        }
    }

    /**
     * 流水线列表
     */
    fun listRecentPipelineIds(
        dslContext: DSLContext,
        projectId: String,
        userId: String,
        limit: Int
    ): List<String> {
        return with(TPipelineRecentUse.T_PIPELINE_RECENT_USE) {
            dslContext.select(PIPELINE_ID)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(USER_ID.eq(userId))
                .orderBy(USE_TIME.desc())
                .limit(limit)
                .fetch(0, String::class.java)
        }
    }
}
