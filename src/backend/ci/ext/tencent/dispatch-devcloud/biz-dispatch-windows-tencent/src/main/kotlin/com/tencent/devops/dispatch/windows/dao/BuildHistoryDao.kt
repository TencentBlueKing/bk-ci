package com.tencent.devops.dispatch.windows.dao

import com.tencent.devops.dispatch.windows.enums.DevCloudCreateWindowsStatus
import com.tencent.devops.model.dispatch.windows.tables.TBuildHistory
import com.tencent.devops.model.dispatch.windows.tables.records.TBuildHistoryRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class BuildHistoryDao {

    fun saveBuildHistory(dslContext: DSLContext, rec: TBuildHistoryRecord): Int {
        with(TBuildHistory.T_BUILD_HISTORY) {
            return dslContext.insertInto(this).set(rec).set(START_TIME, LocalDateTime.now())
                .set(VM_IP, rec.vmIp)
                .execute()
        }
    }

    fun findByBuildIdAndVmSeqId(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String?
    ): Result<TBuildHistoryRecord>? {
        with(TBuildHistory.T_BUILD_HISTORY) {
            val conditions = mutableListOf<Condition>()
            conditions.add(BUILD_ID.eq(buildId))
            // 当vmSeqId=null的时候，说明是整条流水线停止，只关掉正在运行中的。
            if (!vmSeqId.isNullOrBlank()) {
                conditions.add(VM_SEQ_ID.eq(vmSeqId))
            }
            return dslContext.selectFrom(this).where(conditions).fetch()
        }
    }

    fun findRunningBuildsByBuildIdAndVmSeqId(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String?
    ): Result<TBuildHistoryRecord>? {
        with(TBuildHistory.T_BUILD_HISTORY) {
            val conditions = mutableListOf<Condition>()
            conditions.add(BUILD_ID.eq(buildId))
            // 当vmSeqId=null的时候，说明是整条流水线停止，只关掉正在运行中的。
            if (!vmSeqId.isNullOrBlank()) {
                conditions.add(VM_SEQ_ID.eq(vmSeqId))
            }
            conditions.add(STATUS.eq(DevCloudCreateWindowsStatus.Running.name))
            return dslContext.selectFrom(this).where(conditions).fetch()
        }
    }
    fun endStatus(dslContext: DSLContext, status: String, buildHistoryId: Long): Boolean {
        with(TBuildHistory.T_BUILD_HISTORY) {
            return dslContext.update(this).set(STATUS, status).set(END_TIME, LocalDateTime.now())
                .where(ID.eq(buildHistoryId)).execute() > 0
        }
    }

    fun findRunningBuilds(dslContext: DSLContext, vmIp: String): Result<TBuildHistoryRecord> {
        with(TBuildHistory.T_BUILD_HISTORY) {
            return dslContext.selectFrom(this)
                .where(VM_IP.eq(vmIp))
                .and(STATUS.eq(DevCloudCreateWindowsStatus.Running.name))
                .fetch()
        }
    }

    fun getByBuildIdAndVmSeqId(dslContext: DSLContext, buildId: String, vmSeqId: String): TBuildHistoryRecord {
        with(TBuildHistory.T_BUILD_HISTORY) {
            val conditions = mutableListOf<Condition>()
            conditions.add(BUILD_ID.eq(buildId))
            conditions.add(VM_SEQ_ID.eq(vmSeqId))
            return dslContext.selectFrom(this).where(conditions).orderBy(START_TIME.desc()).fetchAny()!!
        }
    }

    fun listCancelHistory(dslContext: DSLContext): Result<TBuildHistoryRecord> {
        with(TBuildHistory.T_BUILD_HISTORY) {
            val conditions = mutableListOf<Condition>()
            conditions.add(STATUS.eq("RUNNING"))
            conditions.add(START_TIME.lessThan(LocalDateTime.now().minusHours(8)))
            conditions.add(START_TIME.greaterThan(LocalDateTime.now().minusDays(7)))
//            conditions.add(START_TIME.lessThan(LocalDateTime.now().minusMinutes(5)))
            conditions.add(PROJECT_ID.notEqual("").and(PROJECT_ID.isNotNull))
            conditions.add(PIPELINE_ID.notEqual("").and(PIPELINE_ID.isNotNull))
            conditions.add(BUILD_ID.notEqual("").and(BUILD_ID.isNotNull))
            return dslContext.selectFrom(this).where(conditions).orderBy(START_TIME.desc()).fetch()
        }
    }
}
