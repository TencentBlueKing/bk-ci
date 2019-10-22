package com.tencent.devops.lambda.dao

import com.tencent.devops.model.process.Tables
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PipelineResDao {

    fun getModel(
        dslContext: DSLContext,
        pipelineId: String,
        version: Int
    ): String? {
        return with(Tables.T_PIPELINE_RESOURCE) {
            dslContext.select(MODEL)
                .from(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VERSION.eq(version))
                .fetchAny(0, String::class.java)
        }
    }
}