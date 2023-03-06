package com.tencent.devops.dispatch.windows.dao

import com.tencent.devops.dispatch.windows.pojo.VMTypeCreate
import com.tencent.devops.dispatch.windows.pojo.VMTypeUpdate
import com.tencent.devops.model.dispatch.windows.tables.TVirtualMachineType
import com.tencent.devops.model.dispatch.windows.tables.records.TVirtualMachineTypeRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.net.URLDecoder
import java.time.LocalDateTime

@Repository
class VirtualMachineTypeDao {
    fun getNameById(dslContext: DSLContext, vmTypeId: Int): String? {
        with(TVirtualMachineType.T_VIRTUAL_MACHINE_TYPE) {
            return dslContext.selectFrom(this).where(ID.eq(vmTypeId)).fetchOne()!!.name
        }
    }

    fun getVmType(dslContext: DSLContext, vmTypeId: Int): TVirtualMachineTypeRecord? {
        with(TVirtualMachineType.T_VIRTUAL_MACHINE_TYPE) {
            return dslContext.selectFrom(this).where(ID.eq(vmTypeId)).fetchOne()
        }
    }

    fun listVmType(dslContext: DSLContext): Result<TVirtualMachineTypeRecord>? {
        with(TVirtualMachineType.T_VIRTUAL_MACHINE_TYPE) {
            return dslContext.selectFrom(this).fetch()
        }
    }

    fun create(dslContext: DSLContext, vmType: VMTypeCreate): Boolean {
        with(TVirtualMachineType.T_VIRTUAL_MACHINE_TYPE) {
            val rec = TVirtualMachineTypeRecord()
            rec.name = vmType.name
            rec.systemVersion = vmType.systemVersion
            rec.createTime = LocalDateTime.now()
            rec.updateTime = LocalDateTime.now()
            return dslContext.insertInto(this).set(rec).execute() > 0
        }
    }

    fun delete(dslContext: DSLContext, vmTypeId: Int): Int {
        with(TVirtualMachineType.T_VIRTUAL_MACHINE_TYPE) {
            return dslContext.deleteFrom(this).where(ID.eq(vmTypeId)).execute()
        }
    }

    fun update(dslContext: DSLContext, vmType: VMTypeUpdate): Int {
        with(TVirtualMachineType.T_VIRTUAL_MACHINE_TYPE) {
            val rec = TVirtualMachineTypeRecord()
            rec.name = vmType.name
            rec.systemVersion = vmType.systemVersion
            rec.updateTime = LocalDateTime.now()
            return dslContext.update(this).set(rec).where(ID.eq(vmType.id)).execute()
        }
    }

    fun search(dslContext: DSLContext, systemVersion: String?): Result<TVirtualMachineTypeRecord>? {
        with(TVirtualMachineType.T_VIRTUAL_MACHINE_TYPE) {
            val conditions = mutableListOf<Condition>()
            if (!systemVersion.isNullOrBlank()) {
                conditions.add(SYSTEM_VERSION.eq(systemVersion!!.trim()))
            }
            return dslContext.selectFrom(this).where(conditions).fetch()
        }
    }
}
