package com.tencent.devops.process.engine.dao

import com.tencent.devops.model.process.tables.TPipelineWebhookBuildParameter
import com.tencent.devops.model.process.tables.records.TPipelineWebhookBuildParameterRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class WebhookBuildParameterDao {

    fun save(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        buildParameters: String
    ) {
        with(TPipelineWebhookBuildParameter.T_PIPELINE_WEBHOOK_BUILD_PARAMETER) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                BUILD_ID,
                BUILD_PARAMETERS
            ).values(
                projectId,
                pipelineId,
                buildId,
                buildParameters
            ).execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        buildId: String
    ): TPipelineWebhookBuildParameterRecord? {
        return with(TPipelineWebhookBuildParameter.T_PIPELINE_WEBHOOK_BUILD_PARAMETER) {
            dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .fetchOne()
        }
    }
}
