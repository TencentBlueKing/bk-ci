package com.tencent.devops.quality.dao

import com.tencent.devops.model.quality.tables.TCountRule
import com.tencent.devops.model.quality.tables.records.TCountRuleRecord
import org.jooq.DSLContext
import org.jooq.Record3
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
class CountRuleDao {
    fun listRuleCountDesc(
        dslContext: DSLContext,
        projectId: String,
        start: LocalDate,
        end: LocalDate,
        offset: Int,
        limit: Int
    ): Result<Record3<Long, BigDecimal, LocalDateTime>> {
        with(TCountRule.T_COUNT_RULE) {
            return dslContext.select(RULE_ID, COUNT.sum().`as`("allCount"), LAST_INTERCEPT_TIME.max().`as`("lastInterceptTime"))
                    .from(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(DATE.ge(start))
                    .and(DATE.le(end))
                    .groupBy(PROJECT_ID, RULE_ID)
                    .orderBy(DSL.field("allCount").desc())
                    .offset(offset)
                    .limit(limit)
                    .fetch()
        }
    }

    fun getOrNull(
        dslContext: DSLContext,
        projectId: String,
        ruleId: Long,
        date: LocalDate
    ): TCountRuleRecord? {
        with(TCountRule.T_COUNT_RULE) {
            return dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(RULE_ID.eq(ruleId))
                    .and(DATE.eq(date))
                    .fetchOne()
        }
    }

    fun countRule(
        dslContext: DSLContext,
        projectId: String,
        start: LocalDate,
        end: LocalDate
    ): Long {
        with(TCountRule.T_COUNT_RULE) {
            return dslContext.select(RULE_ID.countDistinct())
                    .from(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(DATE.ge(start))
                    .and(DATE.le(end))
                    .fetchOne(0, Long::class.java)
        }
    }

    fun create(
        dslContext: DSLContext,
        projectId: String,
        ruleId: Long,
        date: LocalDate,
        count: Int,
        lastInterceptTime: LocalDateTime
    ): Long {
        val now = LocalDateTime.now()
        with(TCountRule.T_COUNT_RULE) {
            val record = dslContext.insertInto(this,
                    PROJECT_ID,
                    RULE_ID,
                    DATE,
                    COUNT,
                    LAST_INTERCEPT_TIME,
                    CREATE_TIME,
                    UPDATE_TIME
            ).values(
                    projectId,
                    ruleId,
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

    fun plusCount(dslContext: DSLContext, id: Long) {
        with(TCountRule.T_COUNT_RULE) {
            dslContext.update(this)
                    .set(COUNT, COUNT + 1)
                    .where(ID.eq(id))
                    .execute()
        }
    }

    fun plusInterceptCount(dslContext: DSLContext, id: Long) {
        with(TCountRule.T_COUNT_RULE) {
            dslContext.update(this)
                    .set(INTERCEPT_COUNT, INTERCEPT_COUNT + 1)
                    .where(ID.eq(id))
                    .execute()
        }
    }

    fun list(dslContext: DSLContext, projectId: String, ruleIds: Collection<Long>): Result<TCountRuleRecord>? {
        return with(TCountRule.T_COUNT_RULE) {
            dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId).and(RULE_ID.`in`(ruleIds)))
                    .fetch()
        }
    }
}