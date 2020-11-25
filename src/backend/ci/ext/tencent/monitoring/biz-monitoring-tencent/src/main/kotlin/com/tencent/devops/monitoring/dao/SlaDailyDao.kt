package com.tencent.devops.monitoring.dao

import com.tencent.devops.model.monitoring.tables.TSlaDaily
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class SlaDailyDao {
    fun insert(
        dslContext: DSLContext,
        module: String,
        name: String,
        percent: Double,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ) {
        with(TSlaDaily.T_SLA_DAILY) {
            dslContext.insertInto(
                this,
                MODULE,
                NAME,
                SUCCESS_PERCENT,
                START_TIME,
                END_TIME
            ).values(
                module,
                name,
                percent,
                startTime,
                endTime
            ).execute()
        }
    }
}