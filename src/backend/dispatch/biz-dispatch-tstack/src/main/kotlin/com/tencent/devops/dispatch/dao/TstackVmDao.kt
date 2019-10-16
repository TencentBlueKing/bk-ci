package com.tencent.devops.dispatch.dao

import com.tencent.devops.model.dispatch.tables.TDispatchTstackVm
import com.tencent.devops.model.dispatch.tables.records.TDispatchTstackVmRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class TstackVmDao {
    fun insertVm(
        dslContext: DSLContext,
        tstackVmId: String,
        vmIp: String,
        vmName: String,
        vmOs: String,
        vmOsVersion: String,
        vmCpu: String,
        vmMemory: String,
        status: String
    ): Long {
        with(TDispatchTstackVm.T_DISPATCH_TSTACK_VM) {
            val now = LocalDateTime.now()
            return dslContext.insertInto(this,
                    TSTACK_VM_ID,
                    VM_IP,
                    VM_NAME,
                    VM_OS,
                    VM_OS_VERSION,
                    VM_CPU,
                    VM_MEMORY,
                    STATUS,
                    CREATED_TIME,
                    UPDATED_TIME
            )
                    .values(
                            tstackVmId,
                            vmIp,
                            vmName,
                            vmOs,
                            vmOsVersion,
                            vmCpu,
                            vmMemory,
                            status,
                            now,
                            now
                    )
                    .returning(ID)
                    .fetchOne().id.toLong()
        }
    }

    fun deleteVm(dslContext: DSLContext, id: Long) {
        with(TDispatchTstackVm.T_DISPATCH_TSTACK_VM) {
            dslContext.deleteFrom(this)
                    .where(ID.eq(id))
                    .execute()
        }
    }

    fun getOrNull(dslContext: DSLContext, id: Long): TDispatchTstackVmRecord? {
        with(TDispatchTstackVm.T_DISPATCH_TSTACK_VM) {
            return dslContext.selectFrom(this)
                    .where(ID.eq(id))
                    .fetchOne()
        }
    }

    fun updateStatus(dslContext: DSLContext, id: Long, status: String) {
        with(TDispatchTstackVm.T_DISPATCH_TSTACK_VM) {
            dslContext.update(this)
                    .set(STATUS, status)
                    .set(UPDATED_TIME, LocalDateTime.now())
                    .where(ID.eq(id))
                    .execute()
        }
    }

    fun listVmByStatus(dslContext: DSLContext, status: String): Result<TDispatchTstackVmRecord> {
        with(TDispatchTstackVm.T_DISPATCH_TSTACK_VM) {
            return dslContext.selectFrom(this)
                    .where(STATUS.eq(status))
                    .fetch()
        }
    }
}