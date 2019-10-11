package com.tencent.devops.quality.dao

import com.tencent.devops.model.quality.tables.THistory
import com.tencent.devops.model.quality.tables.records.THistoryRecord
import com.tencent.devops.quality.pojo.enum.RuleInterceptResult
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class HistoryDao {
    fun create(
        dslContext: DSLContext,
        projectId: String,
        ruleId: Long,
        pipelineId: String,
        buildId: String,
        result: String,
        interceptList: String,
        createTime: LocalDateTime,
        updateTime: LocalDateTime
    ): Long {
        with(THistory.T_HISTORY) {
            val record = dslContext.insertInto(this,
                    PROJECT_ID,
                    RULE_ID,
                    PIPELINE_ID,
                    BUILD_ID,
                    RESULT,
                    INTERCEPT_LIST,
                    CREATE_TIME,
                    UPDATE_TIME
            ).values(
                    projectId,
                    ruleId,
                    pipelineId,
                    buildId,
                    result,
                    interceptList,
                    createTime,
                    updateTime)
                    .returning(ID)
                    .fetchOne()

            // 更新projectNum
            val projectNum = dslContext.selectCount()
                    .from(this)
                    .where(PROJECT_ID.eq(projectId).and(ID.lt(record.id)))
                    .fetchOne(0, Long::class.java) + 1
            dslContext.update(this)
                    .set(PROJECT_NUM, projectNum)
                    .where(ID.eq(record.id))
                    .execute()
            return record.id
        }
    }

    fun listByRuleId(
        dslContext: DSLContext,
        projectId: String,
        ruleId: Long,
        offset: Int,
        limit: Int
    ): Result<THistoryRecord> {
        with(THistory.T_HISTORY) {
            return dslContext.selectFrom(this)
                    .where(RULE_ID.eq(ruleId).and(PROJECT_ID.eq(projectId)))
                    .orderBy(PROJECT_NUM.desc())
                    .offset(offset)
                    .limit(limit)
                    .fetch()
        }
    }

    fun list(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String?,
        ruleId: Long?,
        result: String?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
        offset: Int,
        limit: Int
    ): Result<THistoryRecord> {
        with(THistory.T_HISTORY) {
            val step1 = dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId))
            val step2 = if (pipelineId == null) step1 else step1.and(PIPELINE_ID.eq(pipelineId))
            val step3 = if (ruleId == null) step2 else step2.and(RULE_ID.eq(ruleId))
            val step4 = if (result == null) step3 else step3.and(RESULT.eq(result))
            val step5 = if (startTime == null) step4 else step4.and(CREATE_TIME.gt(startTime))
            val step6 = if (endTime == null) step5 else step5.and(CREATE_TIME.lt(endTime))
            return step6.orderBy(PROJECT_NUM.desc())
                    .offset(offset)
                    .limit(limit)
                    .fetch()
        }
    }

    fun listIntercept(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String?,
        ruleId: Long?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
        offset: Int,
        limit: Int
    ): Result<THistoryRecord> {
        return list(dslContext, projectId, pipelineId, ruleId, RuleInterceptResult.FAIL.name, startTime, endTime, offset, limit)
    }

    fun listByBuildIdAndResult(dslContext: DSLContext, projectId: String, pipelineId: String, buildId: String, result: String): Result<THistoryRecord> {
        with(THistory.T_HISTORY) {
            return dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
                    .and(BUILD_ID.eq(buildId))
                    .and(RESULT.eq(result))
                    .fetch()
        }
    }

    fun listByBuildId(dslContext: DSLContext, projectId: String, pipelineId: String, buildId: String): Result<THistoryRecord> {
        with(THistory.T_HISTORY) {
            return dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
                    .and(BUILD_ID.eq(buildId))
                    .fetch()
        }
    }

    fun count(dslContext: DSLContext, ruleId: Long): Long {
        with(THistory.T_HISTORY) {
            return dslContext.selectCount()
                    .from(this)
                    .where(RULE_ID.eq(ruleId))
                    .fetchOne(0, Long::class.java)
        }
    }

    fun count(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String?,
        ruleId: Long?,
        result: String?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?
    ): Long {
        with(THistory.T_HISTORY) {
            val step1 = dslContext.selectCount().from(this).where(PROJECT_ID.eq(projectId))
            val step2 = if (pipelineId == null) step1 else step1.and(PIPELINE_ID.eq(pipelineId))
            val step3 = if (ruleId == null) step2 else step2.and(RULE_ID.eq(ruleId))
            val step4 = if (result == null) step3 else step3.and(RESULT.eq(result))
            val step5 = if (startTime == null) step4 else step4.and(CREATE_TIME.gt(startTime))
            val step6 = if (endTime == null) step5 else step5.and(CREATE_TIME.lt(endTime))
            return step6.fetchOne(0, Long::class.java)
        }
    }

    fun countIntercept(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String?,
        ruleId: Long?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?
    ): Long {
        return count(dslContext, projectId, pipelineId, ruleId, RuleInterceptResult.FAIL.name, startTime, endTime)
    }
}