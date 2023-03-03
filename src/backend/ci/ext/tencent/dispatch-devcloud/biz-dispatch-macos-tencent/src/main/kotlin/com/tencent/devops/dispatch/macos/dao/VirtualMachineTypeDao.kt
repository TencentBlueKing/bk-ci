package com.tencent.devops.dispatcher.macos.dao

import com.tencent.devops.dispatcher.macos.pojo.VMTypeCreate
import com.tencent.devops.dispatcher.macos.pojo.VMTypeUpdate
import com.tencent.devops.model.dispatcher.macos.tables.TVirtualMachineType
import com.tencent.devops.model.dispatcher.macos.tables.records.TVirtualMachineTypeRecord
import org.apache.commons.lang3.StringUtils
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.net.URLDecoder
import java.time.LocalDateTime

@Repository
open class VirtualMachineTypeDao {
    fun getNameById(dslContext: DSLContext, vmTypeId: Int): String? {
        with(TVirtualMachineType.T_VIRTUAL_MACHINE_TYPE) {
            return dslContext.selectFrom(this).where(ID.eq(vmTypeId)).fetchOne()!!.name
        }
    }

    // 根据英文短名获取系统版本
    fun getSystemVersionByVersion(dslContext: DSLContext, version: String?): String? {
        if (version.isNullOrEmpty()) return null
        with(TVirtualMachineType.T_VIRTUAL_MACHINE_TYPE) {
            return if (version != "latest") {
                dslContext.selectFrom(this).where(VERSION.eq(version)).fetchOne()?.systemVersion
            } else {
                dslContext.selectFrom(this).orderBy(VERSION.desc()).limit(1).fetchOne()!!.systemVersion
            }
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
            rec.xcodeVersion = ";" + StringUtils.join(vmType.xcodeVersionList, ";") + ";"
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
            rec.version = vmType.version
            rec.xcodeVersion = ";" + StringUtils.join(vmType.xcodeVersionList, ";") + ";"
            rec.updateTime = LocalDateTime.now()
            return dslContext.update(this).set(rec).where(ID.eq(vmType.id)).execute()
        }
    }

    fun search(dslContext: DSLContext, systemVersion: String?, xcodeVersion: String?): Result<TVirtualMachineTypeRecord>? {
        with(TVirtualMachineType.T_VIRTUAL_MACHINE_TYPE) {
            val conditions = mutableListOf<Condition>()
            if (!systemVersion.isNullOrBlank()) {
                conditions.add(SYSTEM_VERSION.eq(systemVersion!!.trim()))
            }
            if (!xcodeVersion.isNullOrBlank()) {
                conditions.add(
                    XCODE_VERSION.like(
                        "%" + URLDecoder.decode(
                            ";" + xcodeVersion!!.trim() + ";",
                            "UTF-8"
                        ) + "%"
                    )
                )
            }
            return dslContext.selectFrom(this).where(conditions).fetch()
        }
    }
}
