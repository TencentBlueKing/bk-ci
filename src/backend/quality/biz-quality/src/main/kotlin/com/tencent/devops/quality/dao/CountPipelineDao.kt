package com.tencent.devops.quality.dao

import com.tencent.devops.model.quality.tables.TCountPipeline
import com.tencent.devops.model.quality.tables.records.TCountPipelineRecord
import org.jooq.DSLContext
import org.jooq.Record2
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
class CountPipelineDao {
    fun list(dslContext: DSLContext, projectId: String, pipelineIds: Set<String>): Result<TCountPipelineRecord>? {
        return with(TCountPipeline.T_COUNT_PIPELINE) {
            dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.`in`(pipelineIds)))
                    .fetch()
        }
    }

    fun listByCount(dslContext: DSLContext, projectId: String, offset: Int, limit: Int, isDesc: Boolean = true): Result<Record2<String, BigDecimal>> {
        with(TCountPipeline.T_COUNT_PIPELINE) {
            val sql = dslContext.select(PIPELINE_ID, COUNT.sum().`as`("allCount"))
                    .from(this)
                    .where(PROJECT_ID.eq(projectId))
                    .groupBy(PROJECT_ID, PIPELINE_ID)
            if (isDesc) sql.orderBy(DSL.field("allCount").desc()) else sql.orderBy(DSL.field("allCount"))
            return sql.offset(offset)
                    .limit(limit)
                    .fetch()
        }
    }

    fun listByInterceptCount(dslContext: DSLContext, projectId: String, isDesc: Boolean = true): Result<Record2<String, BigDecimal>> {
        with(TCountPipeline.T_COUNT_PIPELINE) {
            val sql = dslContext.select(PIPELINE_ID, INTERCEPT_COUNT.sum().`as`("allCount"))
                    .from(this)
                    .where(PROJECT_ID.eq(projectId))
                    .groupBy(PROJECT_ID, PIPELINE_ID)
            if (isDesc) sql.orderBy(DSL.field("allCount").desc()) else sql.orderBy(DSL.field("allCount"))
            return sql.fetch()
        }
    }

    fun getOrNull(dslContext: DSLContext, projectId: String, pipelineId: String, date: LocalDate): TCountPipelineRecord? {
        with(TCountPipeline.T_COUNT_PIPELINE) {
            return dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
                    .and(DATE.eq(date))
                    .fetchOne()
        }
    }

    fun create(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        date: LocalDate,
        count: Int,
        lastInterceptTime: LocalDateTime
    ): Long {
        val now = LocalDateTime.now()
        with(TCountPipeline.T_COUNT_PIPELINE) {
            val record = dslContext.insertInto(this,
                    PROJECT_ID,
                    PIPELINE_ID,
                    DATE,
                    COUNT,
                    LAST_INTERCEPT_TIME,
                    CREATE_TIME,
                    UPDATE_TIME
            ).values(
                    projectId,
                    pipelineId,
                    date,
                    count,
                    lastInterceptTime,
                    now,
                    now)
                    .returning(ID)
                    .fetchOne()
            return record.id
        }
    }

    fun plusCount(dslContext: DSLContext, id: Long, lastInterceptTime: LocalDateTime) {
        with(TCountPipeline.T_COUNT_PIPELINE) {
            dslContext.update(this)
                    .set(COUNT, COUNT + 1)
                    .set(LAST_INTERCEPT_TIME, lastInterceptTime)
                    .where(ID.eq(id))
                    .execute()
        }
    }

    fun count(dslContext: DSLContext, projectId: String): Long {
        with(TCountPipeline.T_COUNT_PIPELINE) {
            return dslContext.select(PIPELINE_ID.countDistinct())
                    .from(this)
                    .where(PROJECT_ID.eq(projectId))
                    .fetchOne(0, Long::class.java)
        }
    }

    fun plusInterceptCount(dslContext: DSLContext, id: Long) {
        with(TCountPipeline.T_COUNT_PIPELINE) {
            dslContext.update(this)
                    .set(INTERCEPT_COUNT, INTERCEPT_COUNT + 1)
                    .where(ID.eq(id))
                    .execute()
        }
    }
}