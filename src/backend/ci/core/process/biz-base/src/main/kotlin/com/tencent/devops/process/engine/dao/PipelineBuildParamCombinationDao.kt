package com.tencent.devops.process.engine.dao

import com.tencent.devops.model.process.tables.TPipelineBuildParamCombination
import com.tencent.devops.model.process.tables.TPipelineBuildParamCombinationDetail
import com.tencent.devops.model.process.tables.records.TPipelineBuildParamCombinationRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineBuildParamCombinationDao {
    fun save(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        userId: String,
        combinationId: Long,
        combinationName: String
    ) {
        val now = LocalDateTime.now()
        with(TPipelineBuildParamCombination.T_PIPELINE_BUILD_PARAM_COMBINATION) {
            dslContext.insertInto(
                this,
                ID,
                PROJECT_ID,
                PIPELINE_ID,
                COMBINATION_NAME,
                CREATOR,
                MODIFIER,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                combinationId,
                projectId,
                pipelineId,
                combinationName,
                userId,
                userId,
                now,
                now
            ).execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        userId: String,
        combinationId: Long,
        combinationName: String
    ) {
        val now = LocalDateTime.now()
        with(TPipelineBuildParamCombination.T_PIPELINE_BUILD_PARAM_COMBINATION) {
            dslContext.update(this)
                .set(COMBINATION_NAME, combinationName)
                .set(MODIFIER, userId)
                .set(UPDATE_TIME, now)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(ID.eq(combinationId))
                .execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        combinationId: Long
    ): TPipelineBuildParamCombinationRecord? {
        with(TPipelineBuildParamCombination.T_PIPELINE_BUILD_PARAM_COMBINATION) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(ID.eq(combinationId))
                .fetchOne()
        }
    }

    fun getByName(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        combinationName: String
    ): TPipelineBuildParamCombinationRecord? {
        with(TPipelineBuildParamCombination.T_PIPELINE_BUILD_PARAM_COMBINATION) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(COMBINATION_NAME.eq(combinationName))
                .fetchOne()
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        combinationId: Long
    ) {
        with(TPipelineBuildParamCombination.T_PIPELINE_BUILD_PARAM_COMBINATION) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(ID.eq(combinationId))
                .execute()
        }
    }

    fun listCombination(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        combinationName: String?,
        varName: String?,
        offset: Int,
        limit: Int
    ): Result<TPipelineBuildParamCombinationRecord> {
        val combination = TPipelineBuildParamCombination.T_PIPELINE_BUILD_PARAM_COMBINATION
        val detail = TPipelineBuildParamCombinationDetail.T_PIPELINE_BUILD_PARAM_COMBINATION_DETAIL

        val query = dslContext.selectFrom(combination)
            .where(combination.PROJECT_ID.eq(projectId))
            .and(combination.PIPELINE_ID.eq(pipelineId))

        // 组合名称条件
        combinationName?.takeIf { it.isNotBlank() }?.let {
            query.and(combination.COMBINATION_NAME.like("%$it%"))
        }

        // 变量名称条件（需要联表查询）
        varName?.takeIf { it.isNotBlank() }?.let {
            query.andExists(
                dslContext.selectOne()
                    .from(detail)
                    .where(detail.COMBINATION_ID.eq(combination.ID))
                    .and(detail.VAR_NAME.like("%$it%"))
            )
        }

        return query.orderBy(combination.COMBINATION_NAME.desc())
            .offset(offset)
            .limit(limit)
            .fetch()
    }

    fun countCombination(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        combinationName: String?,
        varName: String?,
    ): Long {
        val combination = TPipelineBuildParamCombination.T_PIPELINE_BUILD_PARAM_COMBINATION
        val detail = TPipelineBuildParamCombinationDetail.T_PIPELINE_BUILD_PARAM_COMBINATION_DETAIL

        val query = dslContext.selectCount().from(combination)
            .where(combination.PROJECT_ID.eq(projectId))
            .and(combination.PIPELINE_ID.eq(pipelineId))

        // 组合名称条件
        combinationName?.takeIf { it.isNotBlank() }?.let {
            query.and(combination.COMBINATION_NAME.like("%$it%"))
        }

        // 变量名称条件（需要联表查询）
        varName?.takeIf { it.isNotBlank() }?.let {
            query.andExists(
                dslContext.selectOne()
                    .from(detail)
                    .where(detail.COMBINATION_ID.eq(combination.ID))
                    .and(detail.VAR_NAME.like("%$it%"))
            )
        }

        return query.fetchOne(0, Long::class.java) ?: 0L
    }
}
