package com.tencent.devops.gitci.dao

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.gitci.pojo.GitProjectPipeline
import com.tencent.devops.model.gitci.tables.TGitProjectPipeline
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class GitProjectPipelineDao {

    fun save(
        dslContext: DSLContext,
        gitProjectId: Long,
        projectCode: String,
        pipelineId: String
    ) {
        with(TGitProjectPipeline.T_GIT_PROJECT_PIPELINE) {
            dslContext.insertInto(this,
                    ID,
                    PROJECT_CODE,
                    PIPELINE_ID,
                    CREATE_TIME,
                    UPDATE_TIME
                    )
                    .values(
                        gitProjectId,
                        projectCode,
                        pipelineId,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                    ).execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        gitProjectId: Long,
        projectCode: String,
        pipelineId: String
    ) {
        with(TGitProjectPipeline.T_GIT_PROJECT_PIPELINE) {
            dslContext.update(this)
                    .set(PROJECT_CODE, projectCode)
                    .set(PIPELINE_ID, pipelineId)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .where(ID.eq(gitProjectId))
                    .execute()
        }
    }

    fun get(dslContext: DSLContext, gitProjectId: Long): GitProjectPipeline? {
        with(TGitProjectPipeline.T_GIT_PROJECT_PIPELINE) {
            val record = dslContext.selectFrom(this)
                .where(ID.eq(gitProjectId))
                .fetchOne()
            return if (record == null) {
                null
            } else {
                GitProjectPipeline(
                        record.id,
                        record.projectCode,
                        record.pipelineId,
                        record.createTime.timestampmilli(),
                        record.updateTime.timestampmilli()
                )
            }
        }
    }
}
