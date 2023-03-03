package com.tencent.devops.dispatch.macos.dao

import com.tencent.devops.model.dispatch.macos.tables.TBuildTask
import com.tencent.devops.model.dispatch.macos.tables.records.TBuildTaskRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class BuildTaskDao {

    fun save(dslContext: DSLContext, record: TBuildTaskRecord): Int {
        with(TBuildTask.T_BUILD_TASK) {
            val exist = exist(dslContext, record)
            return if (exist) {
                dslContext.update(this).set(record).where(BUILD_ID.eq(record.buildId).and(VM_SEQ_ID.eq(record.vmSeqId)))
                    .execute()
            } else {
                dslContext.insertInto(this).set(record).execute()
            }
        }
    }

    fun exist(dslContext: DSLContext, record: TBuildTaskRecord): Boolean {
        with(TBuildTask.T_BUILD_TASK) {
            val record =
                dslContext.selectFrom(this).where(BUILD_ID.eq(record.buildId).and(VM_SEQ_ID.eq(record.vmSeqId)))
                    .fetchOne()
            return record != null
        }
    }

    fun deleteById(
        dslContext: DSLContext,
        id: Long
    ): Boolean {
        with(TBuildTask.T_BUILD_TASK) {
            return dslContext.deleteFrom(this).where(ID.eq(id)).execute() > 0
        }
    }

    fun deleteByBuildId(
        dslContext: DSLContext,
        buildId: String
    ): Boolean {
        with(TBuildTask.T_BUILD_TASK) {
            return dslContext.deleteFrom(this).where(BUILD_ID.eq(buildId)).execute() > 0
        }
    }

    fun deleteByBuildIdAndVmSeqId(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String
    ): Boolean {
        with(TBuildTask.T_BUILD_TASK) {
            return dslContext.deleteFrom(this).where(BUILD_ID.eq(buildId).and(VM_SEQ_ID.eq(vmSeqId))).execute() > 0
        }
    }

    fun listByBuildIdAndVmSeqId(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String?,
        executeCount: Int?
    ): Result<TBuildTaskRecord> {
        with(TBuildTask.T_BUILD_TASK) {
            val conditions = mutableListOf<Condition>()
            conditions.add(BUILD_ID.eq(buildId))
            if (vmSeqId != null) {
                conditions.add(VM_SEQ_ID.eq(vmSeqId))
            }

            if (executeCount != null) {
                conditions.add(EXECUTE_COUNT.eq(executeCount))
            }

            return dslContext.selectFrom(this).where(conditions).fetch()
        }
    }

    fun getByVmIp(
        dslContext: DSLContext,
        vmIp: String
    ): TBuildTaskRecord? {
        with(TBuildTask.T_BUILD_TASK) {
            val conditions = mutableListOf<Condition>()
            conditions.add(VM_IP.eq(vmIp))
            return dslContext.selectFrom(this).where(conditions).orderBy(ID.desc()).limit(1).fetchOne()
        }
    }
}
