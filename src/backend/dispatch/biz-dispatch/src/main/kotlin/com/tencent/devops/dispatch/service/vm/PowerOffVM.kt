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

import com.tencent.devops.common.web.mq.alert.AlertLevel
import com.tencent.devops.common.web.mq.alert.AlertUtils
import com.vmware.vim25.ManagedObjectReference
import com.vmware.vim25.VirtualMachineSnapshotTree
import com.vmware.vim25.mo.Task
import com.vmware.vim25.mo.VirtualMachine
import com.vmware.vim25.mo.VirtualMachineSnapshot
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PowerOffVM(private val vmCache: VMCache) {

    fun shutdown(vmId: Int): Boolean {
        val vm = vmCache.getVM(vmId)
        return if (vm == null) {
            logger.error("ShutdownVM: Cannot find vm $vmId")
            false
        } else {
            return shutdownVM(vm)
        }
    }

    fun shutdown(vmId: Int, snapshotKey: String): Boolean {
        // 首先查询该VM名称对应的VM对象
        val vm = vmCache.getVM(vmId)
        return if (vm == null) {
            logger.error("ShutdownVM: Cannot find vm $vmId")
            false
        } else {
            // 首先从快照树中间删除原来的快照
            // 改成shutdownGuest，要先建快照再关机
            try {
                val snapInfo = vm.snapshot
                return if (snapInfo == null) {
                    logger.info("Create the first snapshot for $snapshotKey")
                    createSnapshot(vm, snapshotKey) && shutdownVM(vm)
                } else {

                    val snapRootTree = snapInfo.getRootSnapshotList()
                    // 先找匹配工程的快照
                    val snapshot = getMatchedSnapShot(snapRootTree, snapshotKey)
                    if (snapshot != null) {
                        // Create the backup snapshot before remove it
                        logger.info("Backup the snapshot $snapshotKey")
                        if (!createSnapshot(vm, snapshotKey + ".bak")) {
                            logger.error("Fail to create the backup snapshot($snapshotKey)")
                            AlertUtils.doAlert(AlertLevel.HIGH, "DevOps Alert Notify", "Fail to create the backup snapshot $snapshotKey of vm $vmId")
                            return false
                        }

                        logger.info("Remove the old snapshot ${snapshot.name}")
                        if (!removeSnapshot(vm, snapshot.snapshot, vmId)) {
                            logger.error("ShutdownVM: Remove vm $vmId project $snapshotKey old snapshot fail")
                            AlertUtils.doAlert(AlertLevel.HIGH, "DevOps Alert Notify", "Fail to remove the backup snapshot $snapshotKey of vm $vmId")
                            return false
                        }
                    } else {
                        logger.info("Create the first snapshot for $snapshotKey")
                    }
                    val createSuccess = createSnapshot(vm, snapshotKey)
                    if (createSuccess) {
                        while (removeBackupSnapshot(vm, snapshotKey + ".bak", vmId))
                        return shutdownVM(vm)
                    }
                    logger.warn("Fail to create the snapshot $snapshotKey")
                    return false
                }
            } catch (e: Exception) {
                logger.error("Fail to shutdown vm - $vmId", e)
                return false
            }
        }
    }

    private fun removeBackupSnapshot(vm: VirtualMachine, snapshotKey: String, vmId: Int): Boolean {
        val snapInfo = vm.snapshot
        if (snapInfo == null) {
            logger.info("The snapshot is empty")
            return false
        }
        val snapRootTree = snapInfo.getRootSnapshotList()
        val snapshot = getMatchedSnapShot(snapRootTree, snapshotKey)
        if (snapshot == null) {
            logger.info("Fail to get the match backup snapshot $snapshotKey")
            return false
        }
        logger.info("Remove the backup snapshot $snapshotKey")
        removeSnapshot(vm, snapshot.snapshot, vmId, false)
        return true
    }

    /**
     * 不处理快照直接关机
     * @param vmId VM名称
     * @return 是否成功
     */
    fun directShutdown(vmId: Int): Boolean {
        // 首先查询该VM名称对应的VM对象
        val vm = vmCache.getVM(vmId)
        return vm != null && shutdownVM(vm)
    }

    private fun removeSnapshot(vm: VirtualMachine, snapshot: ManagedObjectReference, vmId: Int, removeChild: Boolean = true): Boolean {
        val task = VirtualMachineSnapshot(vm.serverConnection, snapshot).removeSnapshot_Task(removeChild)
        if (task.waitForTask() == Task.SUCCESS) {
            return true
        }
        logger.error("Removing Snapshot for VM - [$vmId] Failure")
        AlertUtils.doAlert(AlertLevel.HIGH, "DevOps Alert Notify", "Fail to remove the snapshot ${snapshot.`val`} of vm $vmId")
        return false
    }

    private fun createSnapshot(vm: VirtualMachine, snapshotKey: String): Boolean {
        val result = vm.createSnapshot_Task("p_$snapshotKey", "", false, false).waitForTask()
        return result == Task.SUCCESS
    }

    private fun getMatchedSnapShot(tree: Array<VirtualMachineSnapshotTree>, snapshotKey: String): VirtualMachineSnapshotTree? {
        tree.forEach foreach@{
            val snapshotName = it.getName()
            val matched = snapshotName == "p_$snapshotKey"
            if (matched) {
                return it
            } else {
                val children = it.getChildSnapshotList() ?: return@foreach
                val snap = getMatchedSnapShot(children, snapshotKey)
                if (snap != null) {
                    return snap
                }
            }
        }
        return null
    }

    private fun shutdownVM(vm: VirtualMachine): Boolean {
        try {
//            val result = vm.powerOffVM_Task().waitForTask()
//            logger.info("Shutting down the vm($result)")
//            return result == Task.SUCCESS
            vm.shutdownGuest()
        } catch (e: Exception) {
            logger.error("Fail to shutdown vm - " + vm.name, e)
        }
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PowerOffVM::class.java)
    }
}
