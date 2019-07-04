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

package com.tencent.devops.dispatch.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.dispatch.dao.MachineDao
import com.tencent.devops.dispatch.pojo.Machine
import com.tencent.devops.dispatch.pojo.MachineCreate
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class MachineService @Autowired constructor(
    private val machineDao: MachineDao,
    private val dslContext: DSLContext
) {

    private val logger = LoggerFactory.getLogger(MachineService::class.java)

    fun queryAllMachine(ip: String?, name: String?, username: String?): List<Machine> {
        val results = machineDao.findAllMachine(dslContext, ip, name, username)
        val machines = ArrayList<Machine>()
        results.forEach {
            val m = machineDao.parseMachine(it)
            if (m != null) {
                machines.add(m)
            }
        }
        return machines
    }

    fun queryMachineById(id: Int): Machine? {
        return machineDao.parseMachine(machineDao.findMachineById(dslContext, id))
    }

    fun queryMachineByIp(ip: String): Machine? {
        return machineDao.parseMachine(machineDao.findMachineByIp(dslContext, ip))
    }

    fun addMachine(machine: MachineCreate): Result<Boolean> {
        logger.info("the machine request is {}", machine)
        // 判断ip是否重复
        val ipCount = machineDao.countByIp(dslContext, machine.ip)
        if (ipCount > 0) {
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_EXIST,
                arrayOf(machine.ip),
                false
            )
        }
        // 判断名字是否重复
        val nameCount = machineDao.countByName(dslContext, machine.name)
        if (nameCount > 0) {
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_EXIST,
                arrayOf(machine.name),
                false
            )
        }
        machineDao.addMachine(
            dslContext,
            machine.ip,
            machine.name,
            machine.username,
            machine.password,
            machine.maxVMRun
        )
        return Result(true)
    }

    fun updateMachine(machine: MachineCreate): Result<Boolean> {
        logger.info("the machine request is {}", machine)
        val ipCount = machineDao.countByIp(dslContext, machine.ip)
        if (ipCount > 0) {
            val machineObj = machineDao.findMachineById(dslContext, machine.id)
            if (null != machineObj && machineObj.machineIp != machine.ip) {
                return MessageCodeUtil.generateResponseDataObject(
                    CommonMessageCode.PARAMETER_IS_EXIST,
                    arrayOf(machine.ip),
                    false
                )
            }
        }
        val nameCount = machineDao.countByName(dslContext, machine.name)
        if (nameCount > 0) {
            val machineObj = machineDao.findMachineById(dslContext, machine.id)
            if (null != machineObj && machineObj.machineName != machine.name) {
                return MessageCodeUtil.generateResponseDataObject(
                    CommonMessageCode.PARAMETER_IS_EXIST,
                    arrayOf(machine.name),
                    false
                )
            }
        }
        machineDao.updateMachine(
            dslContext,
            machine.id,
            machine.ip,
            machine.name,
            machine.username,
            machine.password, machine.maxVMRun
        )
        return Result(true)
    }

    fun deleteMachine(id: Int) {
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            machineDao.deleteMachine(transactionContext, id)
        }
    }
}
