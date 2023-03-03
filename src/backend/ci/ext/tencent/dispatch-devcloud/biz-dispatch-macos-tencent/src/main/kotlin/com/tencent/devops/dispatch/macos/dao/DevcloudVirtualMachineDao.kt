package com.tencent.devops.dispatcher.macos.dao

import com.tencent.devops.dispatcher.macos.pojo.devcloud.DevCloudMacosVmCreateInfo
import com.tencent.devops.model.dispatcher.macos.tables.TDevcloudVirtualMachine
import com.tencent.devops.model.dispatcher.macos.tables.records.TDevcloudVirtualMachineRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import org.slf4j.LoggerFactory

@Repository
open class DevcloudVirtualMachineDao {

    fun getById(dslContext: DSLContext, id: Int): TDevcloudVirtualMachineRecord? {
        with(TDevcloudVirtualMachine.T_DEVCLOUD_VIRTUAL_MACHINE) {
            return dslContext.selectFrom(this).where(ID.eq(id)).fetchOne()
        }
    }

    fun deleteById(dslContext: DSLContext, id: Int): Boolean {
        with(TDevcloudVirtualMachine.T_DEVCLOUD_VIRTUAL_MACHINE) {
            return dslContext.deleteFrom(this).where(ID.eq(id)).execute() > 0
        }
    }

    fun getByVmId(dslContext: DSLContext, vmId: Int): TDevcloudVirtualMachineRecord? {
        with(TDevcloudVirtualMachine.T_DEVCLOUD_VIRTUAL_MACHINE) {
            return dslContext.selectFrom(this).where(VM_ID.eq(vmId)).fetchOne()
        }
    }

    fun deleteByVmId(dslContext: DSLContext, vmId: Int): Boolean {
        with(TDevcloudVirtualMachine.T_DEVCLOUD_VIRTUAL_MACHINE) {
            return dslContext.deleteFrom(this).where(VM_ID.eq(vmId)).execute() > 0
        }
    }

    fun exist(dslContext: DSLContext, vmId: Int): Boolean {
        with(TDevcloudVirtualMachine.T_DEVCLOUD_VIRTUAL_MACHINE) {
            val record =
                dslContext.selectFrom(this).where(VM_ID.eq(vmId)).fetchOne()
            return record != null
        }
    }

    fun create(dslContext: DSLContext, vmCreateInfo: DevCloudMacosVmCreateInfo): Boolean {
        with(TDevcloudVirtualMachine.T_DEVCLOUD_VIRTUAL_MACHINE) {
            val exist = exist(dslContext, vmCreateInfo.id)
            return if (exist) {
                true
            } else {
                dslContext.insertInto(this,
                    VM_ID,
                    IP,
                    NAME,
                    ASSET_ID,
                    USER,
                    PASSWORD,
                    CREATOR,
                    CREATE_AT,
                    MEMORY,
                    DISK,
                    OS,
                    CPU
                ).values(
                    vmCreateInfo.id,
                    vmCreateInfo.ip,
                    vmCreateInfo.name,
                    vmCreateInfo.assetId,
                    vmCreateInfo.user,
                    vmCreateInfo.password,
                    vmCreateInfo.creator,
                    vmCreateInfo.createdAt,
                    vmCreateInfo.memory,
                    vmCreateInfo.disk,
                    vmCreateInfo.os,
                    vmCreateInfo.cpu
                ).execute() > 0
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DevcloudVirtualMachineDao::class.java)
    }
}