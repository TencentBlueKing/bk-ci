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

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.devops.dispatch.dao.MachineDao
import com.tencent.devops.dispatch.dao.VMDao
import com.tencent.devops.dispatch.pojo.VM
import com.tencent.devops.dispatch.utils.VMUtils.getService
import com.tencent.devops.dispatch.utils.VMUtils.invalid
import com.vmware.vim25.mo.InventoryNavigator
import com.vmware.vim25.mo.VirtualMachine
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Created by rdeng on 2017/8/31.
 */
const val VIRTUAL_MACHINE = "VirtualMachine"
@Component
class VMCache @Autowired constructor(
    private val machineDao: MachineDao,
    private val vmDao: VMDao,
    private val dslContext: DSLContext
) {

    private val cache = CacheBuilder.newBuilder()
            .maximumSize(3000)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build<Int/*vmId*/, VirtualMachine>(
                    object : CacheLoader<Int, VirtualMachine>() {
                        @Throws(Exception::class)
                        override fun load(vmId: Int) = queryMachine(
                                vmDao.parseVM(vmDao.findVMById(dslContext, vmId)))
                    }
            )

    private val machine2VMMap = ConcurrentHashMap<Int/*machineId*/, ArrayList<Int>/*vmIds*/>()

    fun getVM(vmId: Int): VirtualMachine? {
        return cache.get(vmId)
    }

    fun expire(vmId: Int) {
        cache.invalidate(vmId)
    }

    fun expireByMachineId(machineId: Int) {
        val list = machine2VMMap.remove(machineId)
        list?.forEach(
                {
                    expire(it)
                }
        )
    }

    private fun queryMachine(vm: VM?): VirtualMachine? {
        return if (vm == null) {
            null
        } else {
            val machine = machineDao.parseMachine(machineDao.findMachineById(dslContext, vm.machineId))

            return if (machine == null) {
                null
            } else {
                try {
                    val si = getService(machine) ?: return null
                    val rootFolder = si.rootFolder
                    val virtualMachine = InventoryNavigator(
                            rootFolder).searchManagedEntity(VIRTUAL_MACHINE, vm.name)
                    if (virtualMachine != null) {
                        val v = virtualMachine as VirtualMachine
                        var list = machine2VMMap[vm.machineId]
                        if (list == null) {
                            list = ArrayList()
                            machine2VMMap.put(vm.machineId, list)
                        }

                        list.add(vm.id)
                        return v
                    }

                    null
                } catch (e: Exception) {
                    logger.warn("Fail to get the vm service($machine)", e)
                    invalid(machine)
                    null
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(VMCache::class.java)
    }
}