package com.tencent.devops.quality.dao

import com.tencent.devops.model.quality.tables.TCountIntercept
import com.tencent.devops.model.quality.tables.records.TCountInterceptRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
class CountInterceptDao {
    fun list(dslContext: DSLContext, projectId: String, start: LocalDate, end: LocalDate): Result<TCountInterceptRecord> {
        with(TCountIntercept.T_COUNT_INTERCEPT) {
            return dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(DATE.ge(start))
                    .and(DATE.le(end))
                    .orderBy(DATE.desc())
                    .fetch()
        }
    }

    fun getOrNull(dslContext: DSLContext, projectId: String, date: LocalDate): TCountInterceptRecord? {
        with(TCountIntercept.T_COUNT_INTERCEPT) {
            return dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(DATE.eq(date))
                    .fetchOne()
        }
    }

    fun create(dslContext: DSLContext, projectId: String, date: LocalDate, count: Int): Long {
        val now = LocalDateTime.now()
        with(TCountIntercept.T_COUNT_INTERCEPT) {
            val record = dslContext.insertInto(this,
                    PROJECT_ID,
                    DATE,
                    COUNT,
                    CREATE_TIME,
                    UPDATE_TIME
            ).values(
                    projectId,
                    date,
                    count,
                    now,
                    now)
                    .returning(ID)
                    .fetchOne()
            return record.id
        }
    }

    fun plusCount(dslContext: DSLContext, id: Long) {
        with(TCountIntercept.T_COUNT_INTERCEPT) {
            dslContext.update(this)
                    .set(COUNT, COUNT + 1)
                    .where(ID.eq(id))
                    .execute()
        }
    }

    fun plusRuleInterceptCount(dslContext: DSLContext, id: Long, count: Int = 1) {
        with(TCountIntercept.T_COUNT_INTERCEPT) {
            dslContext.update(this)
                    .set(RULE_INTERCEPT_COUNT, RULE_INTERCEPT_COUNT + count)
                    .where(ID.eq(id))
                    .execute()
        }
    }

    fun count(dslContext: DSLContext, projectId: String): Long {
        with(TCountIntercept.T_COUNT_INTERCEPT) {
            return dslContext.select(COUNT.sum())
                    .from(this)
                    .where(PROJECT_ID.eq(projectId))
                    .fetchOne(0, Long::class.java) ?: 0L
        }
    }

    fun countRuleIntercept(dslContext: DSLContext, projectId: String): Long {
        with(TCountIntercept.T_COUNT_INTERCEPT) {
            return dslContext.select(RULE_INTERCEPT_COUNT.sum())
                    .from(this)
                    .where(PROJECT_ID.eq(projectId))
                    .fetchOne(0, Long::class.java) ?: 0L
        }
    }
}