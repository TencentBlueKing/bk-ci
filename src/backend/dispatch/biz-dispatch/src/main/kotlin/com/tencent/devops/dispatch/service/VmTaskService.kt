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

import com.tencent.devops.dispatch.dao.VMDao
import com.tencent.devops.dispatch.dao.VMTaskDao
import com.tencent.devops.dispatch.service.vm.VMCache
import org.jooq.DSLContext
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class VmTaskService @Autowired constructor(
    private val vmTaskDao: VMTaskDao,
    private val vmDao: VMDao,
    private val vmCache: VMCache,
    private val rabbitTemplate: RabbitTemplate,
    private val dslContext: DSLContext
) {

//    companion object {
//        private val logger = LoggerFactory.getLogger(VmTaskService::class.java)
//    }
//
//
//    fun addTask(task: TaskCreate) {
//        val taskId = vmTaskDao.addTask(dslContext, task)
//        task.vmList.forEach { vmId ->
//            val message = TaskMessage(taskId, vmId, task.script,"",TaskPhase.QUEUE,0)
//            sendMsgToMq(message, 0, ROUTE_TASK_BEGIN)
//            updateTaskStatus(taskId,TaskPhase.QUEUE)
//            addTaskDetail(taskId, vmId, TaskPhase.QUEUE,"task in queue")
//
//            //timeout message
//            sendMsgToMq(message, 3600*1000*2, ROUTE_TASK_NEDD_END)
//        }
//    }
//
//    fun finishTask(vmIP: String, vmTaskResult: VMTaskResult): Boolean {
//        val vmId = vmDao.findVMByIp(dslContext, vmIP)?.vmId ?: throw VMNotExistException("vm not exist [$vmIP]")
//        val message = TaskMessage(vmTaskResult.taskId, vmId, "",vmTaskResult.message,if (vmTaskResult.success) TaskPhase.SUCCESS else TaskPhase.FAIL,0)
//        rabbitTemplate.convertAndSend(EXCHANGE_TASK, ROUTE_TASK_END, message)
//        return true
//    }
//
//    fun listTasks(userid: String?, page: Int, pageSize: Int): TaskWithPage {
//        val count = vmTaskDao.getTasksCount(dslContext, userid)
//        val tasks = vmTaskDao.listTasks(dslContext, userid, page, pageSize)
//        val parseTasks = mutableListOf<Task>()
//        tasks.forEach { t ->
//            parseTasks.add(vmTaskDao.parseTask(t))
//        }
//        return TaskWithPage(count, parseTasks)
//    }
//
//    fun updateTaskStatus(taskId: Int, status: TaskPhase) {
//        vmTaskDao.updateTaskStatus(dslContext, taskId, status)
//    }
//
//    fun getTaskDetails(taskId: Int): List<TaskDetail> {
//        val taskDetails = vmTaskDao.getTaskDetails(dslContext, taskId)
//        val parseTaskDetails = mutableListOf<TaskDetail>()
//        taskDetails.forEach { t ->
//            val tDetail = vmTaskDao.parseTaskDetail(t)
//            tDetail.vmName = vmDao.findVMById(dslContext, tDetail.vmId)?.vmName ?: ""
//            parseTaskDetails.add(tDetail)
//        }
//        return parseTaskDetails
//
//    }
//
//    fun getVmScript(vmIp: String): Task? {
//        val vmId = vmDao.findVMByIp(dslContext, vmIp)?.vmId
//        if (vmId == null) throw VMNotExistException("vm not exist [$vmIp]")
//        val task = vmTaskDao.getLastestVmScript(dslContext, vmId)
//        return task
//    }
//
//    private fun delAllSnapshots(vm: VirtualMachine, tree: Array<VirtualMachineSnapshotTree>?) {
//        tree?.forEach {
//            delAllSnapshots(vm, it.childSnapshotList)
//            val task = VirtualMachineSnapshot(vm.serverConnection, it.snapshot).removeSnapshot_Task(true)
//            if (task.waitForTask() != com.vmware.vim25.mo.Task.SUCCESS) {
//                VmTaskService.logger.error("Removing Snapshot for VM - [${vm.config}] Failure")
//            }
//        }
//
//    }
//
//    fun delAndInitSnapshot(taskMessage: TaskMessage) {
//        val vmId = taskMessage.vmId
//        val vm = vmCache.getVM(vmId)
//        if (vm == null) {
//            logger.error("ShutdownVM: Cannot find vm $vmId")
//            return
//        }
//        val snapInfo = vm.snapshot
//        if (vm.snapshot != null) {
//            val snapRootTree = snapInfo.getRootSnapshotList()
//            //删除所有的snapshot
//            delAllSnapshots(vm, snapRootTree)
//        }
//        //创建init的快照
//        vm.createSnapshot_Task("init", "", false, false).waitForTask()
//    }
//
//    fun retry(taskMessage: TaskMessage) {
//        with(taskMessage){
//            retryCount++
//            if (retryCount == 60) {
//                status = TaskPhase.QUEUE_TIMEOUT
//                logger.error("task queue timeout [$taskId]")
//                sendMsgToMq(taskMessage, 0, ROUTE_TASK_END)
//                return
//            }
//            sendMsgToMq(taskMessage, 60*1000, ROUTE_TASK_BEGIN)
//        }
//
//    }
//
//    fun isTaskFinish(taskId: Int): Boolean {
//        return vmTaskDao.parseTask(vmTaskDao.getTaskById(dslContext, taskId)).status >= TaskPhase.SUCCESS.value
//    }
//
//    fun updateTaskEndTime(taskId: Int) {
//        vmTaskDao.updateTaskEndTime(dslContext, taskId)
//    }
//
//    fun addTaskDetail(taskId: Int, vmId: Int, phase: TaskPhase, msg: String) {
//        vmTaskDao.addTaskDetail(dslContext, taskId, vmId, phase,msg)
//    }
//
//    fun sendMsgToMq(taskMessage: TaskMessage, delay: Int, destRoute: String) {
//        rabbitTemplate.convertAndSend(EXCHANGE_TASK, destRoute, taskMessage) { message ->
//            message.messageProperties.setHeader("x-delay", delay)
//            message
//        }
//    }
}