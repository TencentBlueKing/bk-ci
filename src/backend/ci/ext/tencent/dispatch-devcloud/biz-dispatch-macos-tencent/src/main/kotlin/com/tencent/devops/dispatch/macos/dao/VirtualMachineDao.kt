package com.tencent.devops.dispatcher.macos.dao

import com.tencent.devops.dispatch.macos.enums.MacVMStatus
import com.tencent.devops.dispatcher.macos.pojo.VirtualMachineInfo
import com.tencent.devops.model.dispatcher.macos.tables.TVirtualMachine
import com.tencent.devops.model.dispatcher.macos.tables.records.TVirtualMachineRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import java.net.URLDecoder

@Repository
open class VirtualMachineDao {

    fun findByIp(dslContext: DSLContext, ip: String): TVirtualMachineRecord? {
        with(TVirtualMachine.T_VIRTUAL_MACHINE) {
            return dslContext.selectFrom(this).where(IP.eq(ip)).fetchOne()
        }
    }

    fun findByName(dslContext: DSLContext, name: String): TVirtualMachineRecord? {
        with(TVirtualMachine.T_VIRTUAL_MACHINE) {
            return dslContext.selectFrom(this).where(NAME.eq(name)).fetchOne()
        }
    }

    // 查询到一台空闲的(开机最久的，避免正在开机init的vm),lock it，修改状态到使用中
    fun findOneIdleAndUseIt(
        dslContext: DSLContext,
        idList: List<Int>?
    ): TVirtualMachineRecord? {
        with(TVirtualMachine.T_VIRTUAL_MACHINE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(STATUS.eq(MacVMStatus.Idle.name))
            if (idList != null && idList.isNotEmpty()) {
                conditions.add(ID.`in`(idList))
            }
            val recordList =
                dslContext.selectFrom(this).where(conditions).orderBy(START_TIME.asc()).fetch()
            var resultRecord: TVirtualMachineRecord? = null
            if (recordList == null) {
                logger.info("Get empty idle vms")
                resultRecord = null
            } else {
                run lit@{
                    recordList.forEach { record ->
                        // 尝试获取(锁住)并修改一个空闲的vm的状态，当没有找到合适的空闲vm，resultRecord依旧为null
                        val idleRecord = dslContext.selectFrom(this)
                            .where(ID.eq(record.id))
                            .and(STATUS.eq(MacVMStatus.Idle.name))
                            .forUpdate()
                            .fetchOne()
                        // 单状态依旧为空闲的时候，获取vm成功。
                        if (idleRecord != null) {
                            val count = dslContext.update(this)
                                .set(STATUS, MacVMStatus.BeUsed.name)
                                .set(STATUS_CHANGE_TIME, LocalDateTime.now())
                                .where(ID.eq(record.id))
                                .execute()
                            if (count != 1) {
                                logger.warn("Fail to update the vm status with response count: $count")
                                return@forEach
                            }
                            idleRecord.status = MacVMStatus.BeUsed.name
                            // 返回vm最新的状态。
                            resultRecord = idleRecord
                            return@lit
                        }
                    }
                }
            }
            return resultRecord
        }
    }

    fun updateStatusById(dslContext: DSLContext, status: MacVMStatus, id: Int): Boolean {
        with(TVirtualMachine.T_VIRTUAL_MACHINE) {
            return dslContext.update(this).set(STATUS, status.name).set(STATUS_CHANGE_TIME, LocalDateTime.now())
                .where(ID.eq(id)).execute() > 0
        }
    }

    fun casUpdateStatusById(dslContext: DSLContext, status: MacVMStatus, expect: MacVMStatus, id: Int): Boolean {
        with(TVirtualMachine.T_VIRTUAL_MACHINE) {
            val res = dslContext.update(this).set(STATUS, status.name).set(STATUS_CHANGE_TIME, LocalDateTime.now())
                .where(ID.eq(id).and(STATUS.eq(expect.name))).execute()
            return res > 0
        }
    }

    fun updateStatusAndStartTimeById(dslContext: DSLContext, status: MacVMStatus, startTime: LocalDateTime, id: Int) {
        with(TVirtualMachine.T_VIRTUAL_MACHINE) {
            dslContext.update(this).set(STATUS, status.name).set(STATUS_CHANGE_TIME, LocalDateTime.now())
                .set(START_TIME, startTime)
                .where(ID.eq(id)).execute()
        }
    }

    fun findByStatus(dslContext: DSLContext, status: MacVMStatus?): Result<TVirtualMachineRecord>? {
        with(TVirtualMachine.T_VIRTUAL_MACHINE) {
            val conditions = mutableListOf<Condition>()
            if (status != null) {
                conditions.add(STATUS.eq(status.name))
            }
            return dslContext.selectFrom(this).where(conditions).fetch()
        }
    }

    fun search(
        dslContext: DSLContext,
        name: String?,
        status: MacVMStatus?,
        ip: String?,
        motherMachineIp: String?,
        vmTypeId: Int?
    ): Result<TVirtualMachineRecord>? {
        with(TVirtualMachine.T_VIRTUAL_MACHINE) {
            val conditions = mutableListOf<Condition>()
            if (status != null) {
                conditions.add(STATUS.eq(status.name))
            }
            if (name != null) {
                conditions.add(NAME.like(
                    "%" + URLDecoder.decode(
                        name.trim(),
                        "UTF-8"
                    ) + "%"
                ))
            }
            if (ip != null) {
                conditions.add(IP.like(
                    "%" + URLDecoder.decode(
                        ip.trim(),
                        "UTF-8"
                    ) + "%"
                ))
            }
            if (motherMachineIp != null) {
                conditions.add(MOTHER_MACHINE_IP.like(
                    "%" + URLDecoder.decode(
                        motherMachineIp.trim(),
                        "UTF-8"
                    ) + "%"
                ))
            }
            if (vmTypeId != null) {
                conditions.add(VM_TYPE_ID.eq(vmTypeId))
            }
            return dslContext.selectFrom(this).where(conditions).fetch()
        }
    }

    fun findAll(dslContext: DSLContext): Result<TVirtualMachineRecord>? {
        with(TVirtualMachine.T_VIRTUAL_MACHINE) {
            return dslContext.selectFrom(this).fetch()
        }
    }

    fun findNotUpdate(dslContext: DSLContext): Result<TVirtualMachineRecord>? {
        with(TVirtualMachine.T_VIRTUAL_MACHINE) {
            return dslContext.selectFrom(this).where(STATUS.notEqual(MacVMStatus.Updating.name)).fetch()
        }
    }

    fun saveVm(dslContext: DSLContext, info: VirtualMachineInfo): Int {
        var rec = TVirtualMachineRecord()
        rec.name = info.name
        rec.ip = info.ip
        rec.userName = info.userName
        rec.password = info.password
        rec.motherMachineIp = info.motherMachineIp
        rec.status = info.status
        rec.vmTypeId = info.vmTypeId
        rec.createTime = LocalDateTime.now()
        rec.updateTime = LocalDateTime.now()
        with(TVirtualMachine.T_VIRTUAL_MACHINE) {
            return dslContext.insertInto(this).set(rec).execute()
        }
    }

    fun updateVm(dslContext: DSLContext, info: VirtualMachineInfo): Int {
        with(TVirtualMachine.T_VIRTUAL_MACHINE) {
            val rec = TVirtualMachineRecord()
            if (!info.name.isNullOrBlank()) {
                rec.name = info.name
            }
            if (!info.ip.isNullOrBlank()) {
                rec.ip = info.ip
            }
            if (!info.userName.isNullOrBlank()) {
                rec.userName = info.userName
            }
            if (!info.password.isNullOrBlank()) {
                rec.password = info.password
            }
            if (!info.motherMachineIp.isNullOrBlank()) {
                rec.motherMachineIp = info.motherMachineIp
            }
            if (!info.status.isNullOrBlank()) {
                rec.status = info.status
                rec.statusChangeTime = LocalDateTime.now()
            }
            if (info.vmTypeId != 0) {
                rec.vmTypeId = info.vmTypeId
            }

            rec.updateTime = LocalDateTime.now()
            return dslContext.update(this).set(rec).where(ID.eq(info.id)).execute()
        }
    }

    fun deleteVm(dslContext: DSLContext, mvId: Int): Int {
        with(TVirtualMachine.T_VIRTUAL_MACHINE) {
            return dslContext.deleteFrom(this).where(ID.eq(mvId)).execute()
        }
    }

    fun search(dslContext: DSLContext, status: MacVMStatus?, vmTypeIdList: List<Int>?): Result<TVirtualMachineRecord>? {
        with(TVirtualMachine.T_VIRTUAL_MACHINE) {
            val conditions = mutableListOf<Condition>()
            if (status != null) {
                conditions.add(STATUS.eq(status.name))
            }
            if (vmTypeIdList != null && vmTypeIdList.isNotEmpty()) {
                conditions.add(VM_TYPE_ID.`in`(vmTypeIdList))
            }
            return dslContext.selectFrom(this).where(conditions).fetch()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(VirtualMachineDao::class.java)
    }
}
