package com.tencent.devops.process.dao

import com.tencent.devops.model.process.Tables.T_PIPELINE_RESOURCE
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class TxPipelineResourceDao {

    fun deleteResourceVersion(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int
    ): Int {
        return with(T_PIPELINE_RESOURCE) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .and(VERSION.eq(version))
                .execute()
        }
    }

    fun deleteResourceExceptVersion(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int
    ): Int {
        return with(T_PIPELINE_RESOURCE) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .and(VERSION.ne(version))
                .execute()
        }
    }
}
