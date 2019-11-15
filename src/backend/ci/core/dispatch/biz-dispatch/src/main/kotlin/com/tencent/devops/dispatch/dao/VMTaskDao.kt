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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

/**
 * Created by ddlin on 2017/12/18.
 */
@Repository
class VMTaskDao @Autowired constructor() {
//    fun addTask(dslContext: DSLContext, task: TaskCreate):Int {
//        val now = LocalDateTime.now()
//        var taskResult: TDispatchTaskRecord? = null
//        dslContext.transaction { configuration ->
//            val transactionContext = DSL.using(configuration)
//            with(TDispatchTask.T_DISPATCH_TASK) {
//                taskResult = transactionContext.insertInto(this,
//                        TASK_USER_ID,
//                        TASK_NAME,
//                        TASK_BEGIN_TIME,
//                        TASK_SCRIPT,
//                        TASK_STATUS)
//                        .values(task.userid, task.name, now, task.script,TaskPhase.READY.value)
//                        .returning(TASK_ID)
//                        .fetchOne()
//            }
//
//            //添加任务对应的主机
//            with(TDispatchTaskVm.T_DISPATCH_TASK_VM) {
//                val batch = transactionContext.batch(
//                        dslContext.insertInto(this,
//                                TASK_ID,
//                                VM_ID,
//                                STATUS).values(null as Int?, null, null)
//                )
//                task.vmList.forEach { vm ->
//                    batch.bind(taskResult!!.getValue(TDispatchTask.T_DISPATCH_TASK.TASK_ID), vm, 0)
//                }
//                batch.execute()
//            }
//        }
//        return taskResult?.taskId?:0
//    }
//
//    fun listTasks(dslContext: DSLContext, userid: String?, page: Int, pageSize: Int): List<TDispatchTaskRecord> {
//        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
//        with(TDispatchTask.T_DISPATCH_TASK) {
//            return dslContext.selectFrom(this)
//                    .where(if (userid != null) listOf<Condition>(TASK_USER_ID.eq(userid)) else listOf<Condition>())
//                    .orderBy(TASK_BEGIN_TIME.desc())
//                    .limit(sqlLimit.offset, sqlLimit.limit)
//                    .fetch()
//        }
//
//    }
//
//    fun getTasksCount(dslContext: DSLContext, userid: String?): Int {
//        with(TDispatchTask.T_DISPATCH_TASK) {
//            return dslContext.selectCount()
//                    .from(this)
//                    .where(if (userid != null) listOf<Condition>(TASK_USER_ID.eq(userid)) else listOf<Condition>())
//                    .fetchOne(0, Int::class.java)
//        }
//    }
//
//    fun getTaskDetails(dslContext: DSLContext, taskId: Int): List<TDispatchTaskDetailRecord> {
//        with(TDispatchTaskDetail.T_DISPATCH_TASK_DETAIL) {
//            return dslContext.selectFrom(this)
//                    .where(TASK_ID.eq(taskId))
//                    .orderBy(TASK_DETAIL_TIME.asc()).fetch()
//        }
//    }
//
//    fun getLastestVmScript(dslContext: DSLContext, vmId: Int): Task? {
//        var taskIds :List<String>?= null;
//        var task :Task? = null;
//        with(TDispatchTaskVm.T_DISPATCH_TASK_VM){
//            taskIds=dslContext.select(TASK_ID)
//                    .from(this)
//                    .where(VM_ID.eq(vmId).and(STATUS.eq(TaskPhase.READY.value)))
//                    .fetch(0,String::class.java)
//        }
//        with(TDispatchTask.T_DISPATCH_TASK){
//            val result=dslContext.selectFrom(this)
//                    .where(TASK_ID.`in`(taskIds))
//                    .orderBy(TASK_BEGIN_TIME)
//                    .fetch()
//            if (result.size!=0){
//                return parseTask(result[0])
//            }
//        }
//        return task
//    }
//
//    fun updateTaskStatus(dslContext: DSLContext, taskId: Int, status: TaskPhase) {
//        with(TDispatchTask.T_DISPATCH_TASK) {
//            dslContext.update(this)
//                    .set(TASK_STATUS, status.value)
//                    .where(TASK_ID.eq(taskId))
//                    .execute()
//        }
//    }
//
//    fun updateTaskEndTime(dslContext: DSLContext, taskId: Int) {
//        with(TDispatchTask.T_DISPATCH_TASK) {
//            dslContext.update(this)
//                    .set(TASK_END_TIME, LocalDateTime.now())
//                    .where(TASK_ID.eq(taskId))
//                    .execute()
//        }
//    }
//
//
//    fun getTaskById(dslContext: DSLContext, taskId: Int): TDispatchTaskRecord {
//        with(TDispatchTask.T_DISPATCH_TASK) {
//           return dslContext.selectFrom(this)
//                    .where(TASK_ID.eq(taskId))
//                    .fetchOne()
//        }
//    }
//
//
//    fun parseTask(record: TDispatchTaskRecord): Task {
//        return Task(record.taskId, record.taskName, record.taskScript, record.taskBeginTime.timestamp(), record.taskEndTime?.timestamp(), record.taskStatus)
//    }
//
//    fun parseTaskDetail(record: TDispatchTaskDetailRecord): TaskDetail {
//        return TaskDetail(record.id, record.taskId, record.vmId, "", record.taskMessage, record.taskDetailTime.timestamp())
//    }
//
//    fun addTaskDetail(dslContext: DSLContext, taskId: Int, vmId: Int, phase: TaskPhase, msg: String) {
//        with(TDispatchTaskDetail.T_DISPATCH_TASK_DETAIL){
//            dslContext.insertInto(this,TASK_ID,VM_ID,PHASE_ID,TASK_MESSAGE,TASK_DETAIL_TIME)
//                    .values(taskId,vmId,phase.value,msg, LocalDateTime.now())
//                    .execute()
//        }
//    }
}