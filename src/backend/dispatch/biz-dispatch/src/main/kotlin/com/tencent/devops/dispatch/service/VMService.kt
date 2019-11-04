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

package com.tencent.devops.dispatch.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.service.utils.ByteUtils
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.dispatch.dao.MachineDao
import com.tencent.devops.dispatch.dao.PrivateVMDao
import com.tencent.devops.dispatch.dao.VMDao
import com.tencent.devops.dispatch.dao.VMTypeDao
import com.tencent.devops.dispatch.exception.MachineNotExistException
import com.tencent.devops.dispatch.exception.VMNotExistException
import com.tencent.devops.dispatch.exception.VMTypeNotExistException
import com.tencent.devops.dispatch.pojo.VM
import com.tencent.devops.dispatch.pojo.VMCreate
import com.tencent.devops.dispatch.pojo.VMResponse
import com.tencent.devops.dispatch.pojo.VMWithPage
import com.tencent.devops.dispatch.service.vm.PowerOffVM
import com.tencent.devops.dispatch.service.vm.PowerOnVM
import com.tencent.devops.dispatch.service.vm.QueryVMs
import com.tencent.devops.dispatch.service.vm.VMCache
import com.vmware.vim25.mo.VirtualMachine
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Collections
import java.util.stream.Collectors
import javax.ws.rs.NotFoundException

/**
 * Created by rdeng on 2017/9/1.
 */
@Service
class VMService @Autowired constructor(
    private val vmDao: VMDao,
    private val machineDao: MachineDao,
    private val vmTypeDao: VMTypeDao,
    private val privateVMDao: PrivateVMDao,
    private val queryVMs: QueryVMs,
    private val powerOnVM: PowerOnVM,
    private val powerOffVM: PowerOffVM,
    private val vmCache: VMCache,
    private val dslContext: DSLContext
) {

    private val logger = LoggerFactory.getLogger(VMService::class.java)

    fun queryVMs(ip: String?, name: String?, typeId: Int?, os: String?, osVersion: String?, offset: Int?, limit: Int?): VMWithPage {
        val vms = ArrayList<VMResponse>()
        vmDao.findVms(dslContext, ip, name, typeId, os, osVersion, offset, limit)?.forEach {
            val vm = VMResponse(
                    id = it["id"] as Int,
                machineId = it["machineId"] as Int,
                machineName = it["machineName"] as String,
                typeId = it["typeId"] as Int,
                typeName = it["typeName"] as String,
                ip = it["ip"] as String,
                name = it["name"] as String,
                os = it["os"] as String,
                osVersion = it["osVersion"] as String,
                cpu = it["cpu"] as String,
                memory = it["memory"] as String,
                inMaintain = ByteUtils.byte2Bool(it["inMaintain"] as Byte),
                vmManagerUsername = it["vmManagerUsername"] as String,
                vmManagerPassword = it["vmManagerPassword"] as String,
                vmUsername = it["vmUsername"] as String,
                vmPassword = it["vmPassword"] as String,
                createdTime = (it["createdTime"] as LocalDateTime).timestamp(),
                updatedTime = (it["updatedTime"] as LocalDateTime).timestamp()
            )
            vms.add(vm)
        }
        return VMWithPage(vms.size, vms)
    }

    fun addVM(vm: VMCreate): Result<Boolean> {
        logger.info("the VMCreate request is {}", vm)
        // 判断ip是否重复
        val ipCount = vmDao.countByIp(dslContext, vm.ip)
        if (ipCount > 0) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_EXIST, arrayOf(vm.ip), false)
        }
        // 判断名字是否重复
        val nameCount = vmDao.countByName(dslContext, vm.name)
        if (nameCount > 0) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_EXIST, arrayOf(vm.name), false)
        }
        // Check the machine exist
        machineDao.findMachineById(dslContext, vm.machineId)
            ?: throw MachineNotExistException("The machine(${vm.machineId}) is not exist")
        vmTypeDao.findVMTypeById(dslContext, vm.typeId)
            ?: throw VMTypeNotExistException("The VMType(${vm.typeId}) is not exist")
        vmDao.addVM(dslContext, vm)
        return Result(true)
    }

    fun deleteVM(id: Int) {
        vmDao.deleteVM(dslContext, id)
        vmCache.expire(id)
    }

    fun updateVM(vm: VMCreate): Result<Boolean> {
        logger.info("the VMCreate request is {}", vm)
        val ipCount = vmDao.countByIp(dslContext, vm.ip)
        if (ipCount > 0) {
            val vmObj = vmDao.findVMById(dslContext, vm.id)
            if (null != vmObj && vmObj.vmIp != vm.ip) {
                return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_EXIST, arrayOf(vm.ip), false)
            }
        }
        val nameCount = vmDao.countByName(dslContext, vm.name)
        if (nameCount > 0) {
            val vmObj = vmDao.findVMById(dslContext, vm.id)
            if (null != vmObj && vmObj.vmName != vm.name) {
                return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_EXIST, arrayOf(vm.name), false)
            }
        }
        machineDao.findMachineById(dslContext, vm.machineId)
            ?: throw MachineNotExistException("The machine(${vm.machineId}) is not exist")
        vmTypeDao.findVMTypeById(dslContext, vm.typeId)
            ?: throw VMTypeNotExistException("The VMType(${vm.typeId}) is not exist")
        vmDao.updateVM(dslContext, vm)
        vmCache.expire(vm.id)
        return Result(true)
    }

    fun queryVMById(id: Int): VM {
        return vmDao.parseVM(vmDao.findVMById(dslContext, id)) ?: throw NotFoundException("VM $id is not exist")
    }

    fun queryVMByName(name: String): VM? {
        return vmDao.parseVM(vmDao.findVMByName(dslContext, name))
    }

    fun queryVMByIp(ip: String): VM {
        return vmDao.parseVM(vmDao.findVMByIp(dslContext, ip)) ?: throw NotFoundException("VM($ip) 不存在")
    }

    fun shutdownVM(vmId: Int, snapshotKey: String): Boolean {
        return powerOffVM.shutdown(vmId, snapshotKey)
    }

    fun directShutdownVM(vmId: Int): Boolean {
        return powerOffVM.directShutdown(vmId)
    }

    fun startUpVM(projectId: String, vmId: Int, snapshotKey: String): Boolean {
        return powerOnVM.powerOn(projectId, vmId, snapshotKey)
    }

    fun startUpVM(vmId: Int) = powerOnVM.powerOn(vmId)

    fun shutdownVM(vmId: Int) = powerOffVM.shutdown(vmId)

    fun queryVMStatus(vmHashId: String): String {
        val vmId = HashUtil.decodeOtherIdToInt(vmHashId)
        val vm = vmCache.getVM(vmId)
        return vm?.runtime?.powerState?.toString() ?: throw VMNotExistException("VM($vmId) NOT EXIST")
    }

    fun queryVMMaintainStatus(vmId: Int): Boolean {
        val vm = vmDao.parseVM(vmDao.findVMById(dslContext, vmId))
        return vm?.inMaintain ?: throw VMNotExistException("VM($vmId) is not exist")
    }

    fun maintainVM(vmId: Int, maintain: Boolean) {
        vmDao.maintainVM(dslContext, vmId, maintain)
    }

    @Synchronized
    fun findVM(projectId: String, preferVMName: String?, from: String, preVMs: List<Int>): VM? {
        if (from.isBlank()) {
            return null
        }
        if (!(from.equals("macos", ignoreCase = true) ||
            from.equals("WINDOWS", ignoreCase = true) ||
            from.equals("LINUX", ignoreCase = true))) {
            logger.warn("The os is illegal($from)")
            return null
        }
        val vms = queryVMs.queryAllPowerOffVM()
        if (vms.isEmpty()) {
            logger.warn("All vms are power on")
            return null
        }

        val filterVms = vms.stream().filter { vmName ->
            try {
                vmName.name.startsWith("bkdevops-$from", true)
            } catch (t: Throwable) {
                logger.warn("Fail to get the VM names of the vm - $vmName", t)
                false
            }
        }.collect(Collectors.toList())
        return getIdleVM(projectId, preferVMName, from, filterVms, preVMs)
    }

    private fun getIdleVM(projectId: String, preferVMName: String?, from: String, vms: List<VirtualMachine>, preVMs: List<Int>): VM? {
        val vmNames = if (preferVMName.isNullOrBlank()) {
            emptyArray()
        } else {
            preferVMName!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        }
        val privateVMs = getPrivateVMs(projectId)

        if (privateVMs.isNotEmpty()) {
            val postVMs = vms.filter { vm ->
                var filter = false
                run lit@{
                    privateVMs.forEach { m ->
                        if (m.name == vm.name) {
                            filter = true
                            return@lit
                        }
                    }
                }
                filter
            }

            /**
             * 如果设置了 VM 的名称
             */
            if (vmNames.isNotEmpty()) {
                val vm = getPreferVMs(postVMs, vmNames, true)
                if (vm != null) {
                    return vm
                }
                return getPublicVM(vms, vmNames, preVMs)
            } else {
                val vm = getNonePreferVMs(postVMs, preVMs, true)
                if (vm != null) {
                    return vm
                }
                return getPublicVM(vms, vmNames, preVMs)
            }
        }
        return getPublicVM(vms, vmNames, preVMs)
    }

    private fun getPublicVM(vms: List<VirtualMachine>, preferVMs: Array<String>, preVMs: List<Int>): VM? {
        if (preferVMs.isNotEmpty()) {
            return getPreferVMs(vms, preferVMs, false)
        } else {
            return getNonePreferVMs(vms, preVMs, false)
        }
    }

    private fun getNonePreferVMs(vms: List<VirtualMachine>, preVMs: List<Int>, includePrivateVM: Boolean): VM? {
        val filterVM = mutableListOf<VM>()
        vms.forEach {
            filterVM.add(getVM(it.name, includePrivateVM) ?: return@forEach)
        }

        if (filterVM.isEmpty()) {
            logger.warn("The filter vm is empty")
            return null
        }
        Collections.shuffle(filterVM)

        preVMs.forEach { p ->
            filterVM.forEach { vm ->
                if (p == vm.id) {
                    return vm
                }
            }
        }
        return filterVM[0]
    }

    private fun getPrivateVMs(projectId: String): Set<VM> {
        return dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)
            val privateRecords = privateVMDao.findVMByProject(context, projectId)
            val set = mutableSetOf<VM>()
            if (privateRecords.isNotEmpty) {
                privateRecords.forEach {
                    set.add(vmDao.parseVM(vmDao.findVMById(context, it.vmId)) ?: return@forEach)
                }
            }
            set
        }
    }

    private fun getPreferVMs(vms: List<VirtualMachine>, preferVMs: Array<String>, includePrivateVM: Boolean): VM? {
        vms.forEach {
            if (preferVMs.contains(it.name)) {
                return getVM(it.name, includePrivateVM) ?: return@forEach
            }
        }

        return null
    }

    private fun getVM(vmName: String, includePrivateVM: Boolean): VM? {
        val vm = queryVMByName(vmName)
        if (vm == null) {
            logger.warn("The vm($vmName) is not exist")
            return null
        }
        if (vm.inMaintain) {
            return null
        }
        if (includePrivateVM) {
            return vm
        }
        val typeRecord = vmTypeDao.findVMTypeById(dslContext, vm.typeId) ?: return null
        if (typeRecord.typeName != "public") {
            logger.warn("The vm(${vm.name}) is not public(${typeRecord.typeName})")
            return null
        }
        return vm
    }
}
