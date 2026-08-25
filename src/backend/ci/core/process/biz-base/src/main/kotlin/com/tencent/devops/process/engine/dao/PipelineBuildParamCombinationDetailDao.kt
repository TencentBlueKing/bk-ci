package com.tencent.devops.process.engine.dao

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.model.process.tables.TPipelineBuildParamCombinationDetail
import com.tencent.devops.model.process.tables.records.TPipelineBuildParamCombinationDetailRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineBuildParamCombinationDetailDao {
    fun save(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        userId: String,
        combinationId: Long,
        params: List<BuildFormProperty>
    ) {
        val now = LocalDateTime.now()
        with(TPipelineBuildParamCombinationDetail.T_PIPELINE_BUILD_PARAM_COMBINATION_DETAIL) {
            val addStep = params.mapIndexed { index, it ->
                dslContext.insertInto(this)
                    .set(PROJECT_ID, projectId)
                    .set(PIPELINE_ID, pipelineId)
                    .set(COMBINATION_ID, combinationId)
                    .set(VAR_NAME, it.id)
                    .set(VAR_INDEX, index)
                    .set(BUILD_FORM_PROPERTY, JsonUtil.toJson(it))
                    .set(CREATOR, userId)
                    .set(MODIFIER, userId)
                    .set(CREATE_TIME, now)
                    .set(UPDATE_TIME, now)
                    .onDuplicateKeyIgnore()
            }
            dslContext.batch(addStep).execute()
        }
    }

    fun delete(dslContext: DSLContext, projectId: String, pipelineId: String, combinationId: Long) {
        with(TPipelineBuildParamCombinationDetail.T_PIPELINE_BUILD_PARAM_COMBINATION_DETAIL) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(COMBINATION_ID.eq(combinationId))
                .execute()
        }
    }

    fun getParams(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        combinationId: Long
    ): List<BuildFormProperty> {
        with(TPipelineBuildParamCombinationDetail.T_PIPELINE_BUILD_PARAM_COMBINATION_DETAIL) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(COMBINATION_ID.eq(combinationId))
                .orderBy(VAR_INDEX.asc())
                .fetch().map {
                    JsonUtil.to(it.get(BUILD_FORM_PROPERTY), BuildFormProperty::class.java)
                }
        }
    }

    fun listCombinationDetail(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        combinationIds: List<Long>
    ): Result<TPipelineBuildParamCombinationDetailRecord> {
        with(TPipelineBuildParamCombinationDetail.T_PIPELINE_BUILD_PARAM_COMBINATION_DETAIL) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(COMBINATION_ID.`in`(combinationIds))
                .fetch()
        }
    }
}
