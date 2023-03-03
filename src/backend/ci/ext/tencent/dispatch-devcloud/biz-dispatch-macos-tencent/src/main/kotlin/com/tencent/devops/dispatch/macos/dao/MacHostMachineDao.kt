package com.tencent.devops.dispatch.macos.dao

import com.tencent.devops.dispatch.macos.pojo.HostMachineInfo
import com.tencent.devops.model.dispatch.macos.tables.TMacHostMachine
import com.tencent.devops.model.dispatch.macos.tables.records.TMacHostMachineRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.net.URLDecoder
import java.time.LocalDateTime

@Repository
open class MacHostMachineDao {

    fun findByIp(dslContext: DSLContext, ip: String): TMacHostMachineRecord? {
        with(TMacHostMachine.T_MAC_HOST_MACHINE) {
            return dslContext.selectFrom(this).where(IP.eq(ip)).fetchOne()
        }
    }

    fun findAll(dslContext: DSLContext): Result<TMacHostMachineRecord>? {
        with(TMacHostMachine.T_MAC_HOST_MACHINE) {
            return dslContext.selectFrom(this).fetch()
        }
    }

    fun search(dslContext: DSLContext, name: String?, ip: String?): Result<TMacHostMachineRecord>? {
        with(TMacHostMachine.T_MAC_HOST_MACHINE) {
            val conditions = mutableListOf<Condition>()
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
            return dslContext.selectFrom(this).where(conditions).fetch()
        }
    }

    fun get(dslContext: DSLContext, hostId: Int): TMacHostMachineRecord? {
        with(TMacHostMachine.T_MAC_HOST_MACHINE) {
            return dslContext.selectFrom(this).where(ID.eq(hostId)).fetchOne()
        }
    }

    fun delete(dslContext: DSLContext, hostId: Int): Int {
        with(TMacHostMachine.T_MAC_HOST_MACHINE) {
            return dslContext.deleteFrom(this).where(ID.eq(hostId)).execute()
        }
    }

    fun saveHostInfo(dslContext: DSLContext, info: HostMachineInfo): Int {
        val rec = TMacHostMachineRecord()
        rec.name = info.name
        rec.ip = info.ip
        rec.userName = info.userName
        rec.password = info.password
        rec.createTime = LocalDateTime.now()
        rec.updateTime = LocalDateTime.now()
        with(TMacHostMachine.T_MAC_HOST_MACHINE) {
            return dslContext.insertInto(this).set(rec).execute()
        }
    }

    fun updateHostInfo(dslContext: DSLContext, info: HostMachineInfo): Int {
        with(TMacHostMachine.T_MAC_HOST_MACHINE) {
            val rec = TMacHostMachineRecord()
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
            rec.updateTime = LocalDateTime.now()
            return dslContext.update(this).set(rec).where(ID.eq(info.id)).execute()
        }
    }
}
