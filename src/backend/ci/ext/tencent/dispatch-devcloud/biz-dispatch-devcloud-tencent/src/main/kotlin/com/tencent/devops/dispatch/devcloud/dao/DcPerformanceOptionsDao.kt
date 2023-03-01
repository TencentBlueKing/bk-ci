package com.tencent.devops.dispatch.devcloud.dao

import com.tencent.devops.model.dispatch_devcloud.tables.TDevcloudPerformanceOptions
import com.tencent.devops.model.dispatch_devcloud.tables.records.TDevcloudPerformanceOptionsRecord
import com.tencent.devops.dispatch.devcloud.pojo.performance.PerformanceOptionsVO
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class DcPerformanceOptionsDao {

    fun create(
        dslContext: DSLContext,
        cpu: Int,
        memory: Int,
        disk: Int,
        description: String
    ) {
        with(TDevcloudPerformanceOptions.T_DEVCLOUD_PERFORMANCE_OPTIONS) {
            dslContext.insertInto(
                this,
                CPU,
                MEMORY,
                DISK,
                DESCRIPTION,
                GMT_CREATE,
                GMT_MODIFIED
            ).values(
                cpu,
                memory,
                disk,
                description,
                LocalDateTime.now(),
                LocalDateTime.now()
            ).execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        id: Long,
        performanceOptionsVO: PerformanceOptionsVO
    ) {
        with(TDevcloudPerformanceOptions.T_DEVCLOUD_PERFORMANCE_OPTIONS) {
            dslContext.update(this)
                .set(CPU, performanceOptionsVO.cpu)
                .set(MEMORY, performanceOptionsVO.memory)
                .set(DISK, performanceOptionsVO.disk)
                .set(DESCRIPTION, performanceOptionsVO.description)
                .set(GMT_MODIFIED, LocalDateTime.now())
                .where(ID.eq(id))
                .execute()
        }
    }

    fun get(dslContext: DSLContext, id: Long): TDevcloudPerformanceOptionsRecord? {
        with(TDevcloudPerformanceOptions.T_DEVCLOUD_PERFORMANCE_OPTIONS) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }

    fun getList(dslContext: DSLContext): Result<TDevcloudPerformanceOptionsRecord> {
        with(TDevcloudPerformanceOptions.T_DEVCLOUD_PERFORMANCE_OPTIONS) {
            return dslContext.selectFrom(this)
                .fetch()
        }
    }

    fun getOptionsList(
        dslContext: DSLContext,
        cpu: Int,
        memory: Int,
        disk: Int
    ): Result<TDevcloudPerformanceOptionsRecord> {
        with(TDevcloudPerformanceOptions.T_DEVCLOUD_PERFORMANCE_OPTIONS) {
            return dslContext.selectFrom(this)
                .where(CPU.lessOrEqual(cpu))
                .and(MEMORY.lessOrEqual(memory))
                .and(DISK.lessOrEqual(disk))
                .fetch()
        }
    }

    fun delete(
        dslContext: DSLContext,
        id: Long
    ): Int {
        return with(TDevcloudPerformanceOptions.T_DEVCLOUD_PERFORMANCE_OPTIONS) {
            dslContext.delete(this)
                    .where(ID.eq(id))
                    .execute()
        }
    }
}
