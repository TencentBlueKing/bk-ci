/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.dispatch.dao

import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.dispatch.pojo.Machine
import com.tencent.devops.model.dispatch.tables.TDispatchMachine
import com.tencent.devops.model.dispatch.tables.records.TDispatchMachineRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import org.springframework.util.StringUtils
import java.time.LocalDateTime

@Repository
class MachineDao {

    fun findAllMachine(dslContext: DSLContext): Result<TDispatchMachineRecord> {
        return findAllMachine(dslContext, null, null, null)
    }

    fun findAllMachine(
        dslContext: DSLContext,
        ip: String?,
        name: String?,
        username: String?
    ): Result<TDispatchMachineRecord> {
        with(TDispatchMachine.T_DISPATCH_MACHINE) {
            val conditions = mutableListOf<Condition>()
            if (!StringUtils.isEmpty(ip)) conditions.add(MACHINE_IP.like("%" + ip + "%"))
            if (!StringUtils.isEmpty(name)) conditions.add(MACHINE_NAME.like("%" + name + "%"))
            if (!StringUtils.isEmpty(username)) conditions.add(MACHINE_USERNAME.like("%" + username + "%"))
            return dslContext.selectFrom(this)
                .where(conditions)
                .orderBy(MACHINE_ID.asc())
                .fetch()
        }
    }

    fun findMachineById(dslContext: DSLContext, id: Int): TDispatchMachineRecord? {
        return dslContext.selectFrom(TDispatchMachine.T_DISPATCH_MACHINE)
            .where(TDispatchMachine.T_DISPATCH_MACHINE.MACHINE_ID.eq(id))
            .fetchAny()
    }

    fun findMachineByIp(dslContext: DSLContext, ip: String): TDispatchMachineRecord? {
        return dslContext.selectFrom(TDispatchMachine.T_DISPATCH_MACHINE)
            .where(TDispatchMachine.T_DISPATCH_MACHINE.MACHINE_IP.eq(ip))
            .fetchAny()
    }

    fun findMachineByName(dslContext: DSLContext, vmName: String): TDispatchMachineRecord? {
        with(TDispatchMachine.T_DISPATCH_MACHINE) {
            return dslContext.selectFrom(this)
                .where(MACHINE_NAME.eq(vmName))
                .fetchAny()
        }
    }

    fun countByIp(dslContext: DSLContext, ip: String): Int {
        with(TDispatchMachine.T_DISPATCH_MACHINE) {
            return dslContext.selectCount().from(this).where(MACHINE_IP.eq(ip)).fetchOne(0, Int::class.java)
        }
    }

    fun countByName(dslContext: DSLContext, name: String): Int {
        with(TDispatchMachine.T_DISPATCH_MACHINE) {
            return dslContext.selectCount().from(this).where(MACHINE_NAME.eq(name)).fetchOne(0, Int::class.java)
        }
    }

    fun addMachine(
        dslContext: DSLContext,
        ip: String,
        name: String,
        username: String,
        password: String,
        maxVMRun: Int
    ) {
        with(TDispatchMachine.T_DISPATCH_MACHINE) {
            val now = LocalDateTime.now()
            dslContext.insertInto(
                this,
                MACHINE_IP,
                MACHINE_NAME,
                MACHINE_USERNAME,
                MACHINE_PASSWORD,
                MAX_VM_RUN,
                MACHINE_CREATED_TIME,
                MACHINE_UPDATED_TIME
            )
                .values(
                    ip,
                    name,
                    username,
                    SecurityUtil.encrypt(password),
                    maxVMRun,
                    now,
                    now
                )
                .execute()
        }
    }

    fun updateMachine(
        dslContext: DSLContext,
        id: Int,
        ip: String,
        name: String,
        username: String,
        password: String,
        maxVMRun: Int
    ) {
        with(TDispatchMachine.T_DISPATCH_MACHINE) {
            dslContext.update(this)
                .set(MACHINE_IP, ip)
                .set(MACHINE_NAME, name)
                .set(MACHINE_USERNAME, username)
                .set(MACHINE_PASSWORD, SecurityUtil.encrypt(password))
                .set(MAX_VM_RUN, maxVMRun)
                .set(MACHINE_UPDATED_TIME, LocalDateTime.now())
                .where(MACHINE_ID.eq(id))
                .execute()
        }
    }

    fun deleteMachine(dslContext: DSLContext, id: Int) {
        with(TDispatchMachine.T_DISPATCH_MACHINE) {
            dslContext.deleteFrom(this)
                .where(MACHINE_ID.eq(id))
                .execute()
        }
    }

    fun parseMachine(record: TDispatchMachineRecord?): Machine? {
        return if (record == null) {
            null
        } else Machine(
            record.machineId,
            record.machineIp,
            record.machineName,
            record.machineUsername,
            SecurityUtil.decrypt(record.machinePassword),
            record.currentVmRun,
            record.maxVmRun,
            record.machineCreatedTime.timestamp(),
            record.machineUpdatedTime.timestamp()
        )
    }
}
