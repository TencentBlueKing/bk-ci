package com.tencent.devops.dispatch.dao

import com.tencent.devops.model.dispatch.tables.TDispatchPrivateVm
import com.tencent.devops.model.dispatch.tables.records.TDispatchPrivateVmRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class PrivateVMDao {

    fun findVMByProject(dslContext: DSLContext, projectId: String): Result<TDispatchPrivateVmRecord> {
        with(TDispatchPrivateVm.T_DISPATCH_PRIVATE_VM) {
            return dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId))
                    .fetch()
        }
    }

    fun list(dslContext: DSLContext): Result<TDispatchPrivateVmRecord> {
        with(TDispatchPrivateVm.T_DISPATCH_PRIVATE_VM) {
            return dslContext.selectFrom(this)
                    .fetch()
        }
    }

    fun add(dslContext: DSLContext, vmId: Int, projectId: String) {
        with(TDispatchPrivateVm.T_DISPATCH_PRIVATE_VM) {
            dslContext.insertInto(this,
                    VM_ID,
                    PROJECT_ID)
                    .values(vmId, projectId)
                    .execute()
        }
    }

    fun delete(dslContext: DSLContext, vmId: Int, projectId: String) {
        with(TDispatchPrivateVm.T_DISPATCH_PRIVATE_VM) {
            dslContext.deleteFrom(this)
                    .where(VM_ID.eq(vmId))
                    .and(PROJECT_ID.eq(projectId))
                    .execute()
        }
    }
}