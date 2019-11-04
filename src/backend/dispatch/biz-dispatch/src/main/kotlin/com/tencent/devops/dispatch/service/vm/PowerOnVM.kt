package com.tencent.devops.dispatch.service.vm

import com.tencent.devops.dispatch.service.ProjectSnapshotService
import com.vmware.vim25.VirtualMachinePowerState
import com.vmware.vim25.VirtualMachineSnapshotTree
import com.vmware.vim25.mo.Task
import com.vmware.vim25.mo.VirtualMachineSnapshot
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PowerOnVM(
    private val vmCache: VMCache,
    private val projectSnapshotService: ProjectSnapshotService
) {

    fun powerOn(vmId: Int): Boolean {
        val vm = vmCache.getVM(vmId)
        if (vm == null) {
            logger.error("ShutdownVM: Cannot find vm $vmId")
            return false
        }
        val result = vm.powerOnVM_Task(null).waitForTask()

        if (result == Task.SUCCESS)
            return true
        return false
    }

    fun powerOn(projectId: String, vmId: Int, snapshotKey: String): Boolean {
        val vm = vmCache.getVM(vmId)
        if (vm == null) {
            logger.error("PowerOn: Cannot find vm $vmId")
            return false
        }

        if (vm.runtime?.powerState != VirtualMachinePowerState.poweredOff) {
            logger.warn("The vm($vmId) is not power off - ${vm.runtime?.powerState}")
            return false
        }

        // 首先从快照树中间删除原来的快照
        try {
            val snapInfo = vm.snapshot
            if (snapInfo == null) {
                val result = vm.powerOnVM_Task(null).waitForTask()

                return result == Task.SUCCESS
            }

            val snapRootTree = snapInfo.getRootSnapshotList()

            // 先找匹配工程的快照
            val startupSnapshot = projectSnapshotService.getProjectStartupSnapshot(projectId)
            var snapshot = getMatchedSnapShot(projectId, snapRootTree, snapshotKey, null)

            if (snapshot == null) {
                // Trying to find the back up snap key
                snapshot = getMatchedSnapShot(projectId, snapRootTree, snapshotKey + ".bak", null)
                if (snapshot == null) {
                    snapshot = getMatchedSnapShot(projectId, snapRootTree, null, startupSnapshot)
                    if (snapshot == null && startupSnapshot != null) {
                        // Try to get the 'init' snapshot
                        snapshot = getMatchedSnapShot(projectId, snapRootTree, null, null)
                    }
                } else {
                    logger.info("Get the backup snapshot of $snapshotKey")
                }
            }

            if (snapshot == null) {
                logger.info("Can't find any snapshot")
                return vm.powerOnVM_Task(null).waitForTask() == Task.SUCCESS
            }

            var result = VirtualMachineSnapshot(vm.serverConnection, snapshot.snapshot).revertToSnapshot_Task(null).waitForTask()
            if (result != Task.SUCCESS) {
                return false
            }

            result = vm.powerOnVM_Task(null).waitForTask()

            logger.info("Revert the snapshot(${snapshot.name}) and start vm for snapshot($snapshotKey)")
            if (result == Task.SUCCESS)
                return true
            // Wait 10 seconds to check its status is power on
            for (i in 1..10) {
                logger.warn("Fail revert snapshot and the vm status ${vm.runtime.powerState}")
                if (vm.runtime.powerState == VirtualMachinePowerState.poweredOn) {
                    return true
                }
                Thread.sleep(1000)
            }
        } catch (e: Exception) {
            logger.warn("Fail to power on vm - $vmId", e)
        }
        return false
    }

    private fun getMatchedSnapShot(projectId: String, tree: Array<VirtualMachineSnapshotTree>, snapshotKey: String?, startupSnapshot: String?): VirtualMachineSnapshotTree? {
        tree.forEach {
            val snapshotName = it.getName()
            val matched = when (snapshotKey) {
                null -> {
                    snapshotName == startupSnapshot ?: "init"
                }
                else -> snapshotName == "p_$snapshotKey"
            }
            if (matched) {
                logger.info("Get the match snapshot($snapshotName) for project($projectId)")
                return it
            } else {
                val children = it.getChildSnapshotList() ?: return@forEach
                if (children.isEmpty()) {
                    return@forEach
                }
                val snap = getMatchedSnapShot(projectId, children, snapshotKey, startupSnapshot)
                if (snap != null) {
                    return snap
                }
            }
        }
        return null
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

    companion object {
        private val logger = LoggerFactory.getLogger(PowerOnVM::class.java)
    }
}