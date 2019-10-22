package com.tencent.devops.lambda.dao

import com.tencent.devops.model.process.tables.TTemplatePipeline
import com.tencent.devops.model.process.tables.records.TTemplatePipelineRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PipelineTemplateDao {

    fun getTemplate(
        dslContext: DSLContext,
        pipelineId: String
    ): TTemplatePipelineRecord? {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .fetchOne()
        }
    }
}