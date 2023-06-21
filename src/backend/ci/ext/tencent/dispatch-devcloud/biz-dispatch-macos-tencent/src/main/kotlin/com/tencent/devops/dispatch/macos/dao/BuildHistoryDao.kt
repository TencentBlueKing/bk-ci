package com.tencent.devops.dispatch.macos.dao

import com.tencent.devops.dispatch.macos.enums.MacJobStatus
import com.tencent.devops.model.dispatch.macos.tables.TBuildHistory
import com.tencent.devops.model.dispatch.macos.tables.records.TBuildHistoryRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class BuildHistoryDao {

    fun saveBuildHistory(dslContext: DSLContext, rec: TBuildHistoryRecord): Long {
        with(TBuildHistory.T_BUILD_HISTORY) {
            return dslContext.insertInto(this).set(rec).set(START_TIME, LocalDateTime.now())
                .set(VM_IP, rec.vmIp)
                .returning(ID)
                .fetchOne()!!.id
        }
    }

    fun getBuildHistory(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String,
        executeCount: Int
    ): Result<TBuildHistoryRecord>? {
        with(TBuildHistory.T_BUILD_HISTORY) {
            val conditions = mutableListOf<Condition>()
            conditions.add(BUILD_ID.eq(buildId))
            conditions.add(VM_SEQ_ID.eq(vmSeqId))
            conditions.add(EXECUTE_COUNT.eq(executeCount))
            return dslContext.selectFrom(this).where(conditions).fetch()
        }
    }

    fun updateVmIP(
        vmIp: String,
        vmId: Int,
        buildHistoryId: Long,
        dslContext: DSLContext
    ) {
        with(TBuildHistory.T_BUILD_HISTORY) {
            dslContext.update(this)
                .set(STATUS, MacJobStatus.Running.title)
                .set(VM_IP, vmIp)
                .set(VM_ID, vmId)
                .where(ID.eq(buildHistoryId))
                .execute()
        }
    }

    fun updateBuildHistoryStatus(
        dslContext: DSLContext,
        status: String,
        buildHistoryId: Long
    ): Boolean {
        with(TBuildHistory.T_BUILD_HISTORY) {
            return dslContext.update(this)
                .set(STATUS, status)
                .set(END_TIME, LocalDateTime.now())
                .where(ID.eq(buildHistoryId))
                .execute() > 0
        }
    }

    fun findRunningBuilds(dslContext: DSLContext, vmIp: String): Result<TBuildHistoryRecord> {
        with(TBuildHistory.T_BUILD_HISTORY) {
            return dslContext.selectFrom(this)
                .where(VM_IP.eq(vmIp))
                .and(STATUS.eq(MacJobStatus.Running.name))
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
