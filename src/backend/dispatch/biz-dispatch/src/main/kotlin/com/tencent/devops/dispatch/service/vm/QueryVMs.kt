/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.dispatch.service.vm

import com.tencent.devops.dispatch.dao.MachineDao
import com.tencent.devops.dispatch.utils.VMUtils.getService
import com.tencent.devops.dispatch.utils.VMUtils.invalid
import com.vmware.vim25.VirtualMachinePowerState
import com.vmware.vim25.mo.InventoryNavigator
import com.vmware.vim25.mo.VirtualMachine
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class QueryVMs @Autowired constructor(
    private val dslContext: DSLContext,
    private val machineDao: MachineDao
) {

    fun queryAllPowerOffVM(): Set<VirtualMachine> {
        val vms = HashSet<VirtualMachine>()

        val results = machineDao.findAllMachine(dslContext)
        if (results.isEmpty()) {
            return vms
        }

        results.forEach {
            val m = machineDao.parseMachine(it)
            if (m != null) {
                try {
                    val maxRunningVM = it.maxVmRun
                    val startEpoch = System.currentTimeMillis()
                    logger.info("Start to get the vm [${m.name}|${m.id}|${m.ip}]")
                    val si = getService(m) ?: return@forEach
                    logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to get the mv [${m.ip}]")

                    val allVMS = InventoryNavigator(si.rootFolder)
                        .searchManagedEntities(VIRTUAL_MACHINE)
                    var runningVMCnt = 0
                    run lit@{
                        allVMS.forEach {
                            if ((it as VirtualMachine).runtime.powerState == VirtualMachinePowerState.poweredOff) {
                                vms.add(it)
                            } else {
                                runningVMCnt++
                                if (maxRunningVM > 0 && runningVMCnt >= maxRunningVM) {
                                    logger.warn("There are too many running vm($runningVMCnt) which is more than the setter one($maxRunningVM) of ESX(${m.ip})")
                                    return@lit
                                }
                            }
                        }
                    }
                } catch (t: Throwable) {
                    logger.warn("Fail to list power off vms of vm($m) because of ${t.message}")
                    invalid(m)
                    return@forEach
                }
            }
        }

        return vms
    }

    fun queryAllPowerOnVM(): Set<VirtualMachine> {
        val vms = HashSet<VirtualMachine>()

        val results = machineDao.findAllMachine(dslContext)
        if (results.isEmpty()) {
            return vms
        }

        results.forEach {
            val m = machineDao.parseMachine(it)
            if (m != null) {
                try {
                    val si = getService(m) ?: return@forEach

                    val allVMS = InventoryNavigator(si.rootFolder)
                            .searchManagedEntities(VIRTUAL_MACHINE)
                    allVMS.forEach {
                        if ((it as VirtualMachine).runtime.powerState == VirtualMachinePowerState.poweredOn) {
                            vms.add(it)
                        }
                    }
                } catch (t: Throwable) {
                    logger.warn("Fail to list power off vms of vm($m) because of ${t.message}")
                    invalid(m)
                    return@forEach
                }
            }
        }

        return vms
    }

    companion object {
        private val logger = LoggerFactory.getLogger(QueryVMs::class.java)
    }
}