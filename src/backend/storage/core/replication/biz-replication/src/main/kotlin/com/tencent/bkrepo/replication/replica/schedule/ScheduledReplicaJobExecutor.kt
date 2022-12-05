/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.replication.replica.schedule

import com.tencent.bkrepo.replication.manager.LocalDataManager
import com.tencent.bkrepo.replication.pojo.record.ExecutionStatus
import com.tencent.bkrepo.replication.pojo.task.ReplicaTaskInfo
import com.tencent.bkrepo.replication.replica.base.AbstractReplicaJobExecutor
import com.tencent.bkrepo.replication.service.ClusterNodeService
import com.tencent.bkrepo.replication.service.ReplicaRecordService
import com.tencent.bkrepo.replication.service.ReplicaTaskService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 调度类型同步任务逻辑实现类
 * 任务由线程池执行
 */
@Suppress("TooGenericExceptionCaught")
@Component
class ScheduledReplicaJobExecutor(
    clusterNodeService: ClusterNodeService,
    localDataManager: LocalDataManager,
    replicaService: ScheduledReplicaService,
    private val replicaTaskService: ReplicaTaskService,
    private val replicaRecordService: ReplicaRecordService,
    private val replicaTaskScheduler: ReplicaTaskScheduler
) : AbstractReplicaJobExecutor(clusterNodeService, localDataManager, replicaService) {

    /**
     * 执行同步任务
     * @param taskId 任务id
     * 该任务只能由一个节点执行，已经成功抢占到锁才能执行到此处
     */
    fun execute(taskId: String) {
        logger.info("Start to execute replication task[$taskId].")
        val task = findAndCheckTask(taskId) ?: return
        var status = ExecutionStatus.SUCCESS
        var errorReason: String? = null
        var recordId: String? = null
        try {
            // 查询同步对象
            val taskDetail = replicaTaskService.getDetailByTaskKey(task.key)
            // 开启新的同步记录
            val taskRecord = replicaRecordService.startNewRecord(task.key).apply { recordId = id }
            val result = task.remoteClusters.map { submit(taskDetail, taskRecord, it) }.map { it.get() }
            result.forEach {
                if (it.status == ExecutionStatus.FAILED) {
                    status = ExecutionStatus.FAILED
                    errorReason = "部分数据同步失败"
                }
            }
        } catch (exception: Exception) {
            // 记录异常
            status = ExecutionStatus.FAILED
            errorReason = exception.message.orEmpty()
        } finally {
            // 保存结果
            replicaRecordService.completeRecord(recordId!!, status, errorReason)
            logger.info("Replica task[$taskId], record[$recordId] finished")
        }
    }

    /**
     * 查找并检查任务状态
     * @return 如果任务不存在或不能被执行，返回null，否则返回任务信息
     */
    private fun findAndCheckTask(taskId: String): ReplicaTaskInfo? {
        // 任务不存在，删除任务
        val task = replicaTaskService.getByTaskId(taskId) ?: run {
            logger.warn("Task[$taskId] does not exist, delete job and trigger.")
            replicaTaskScheduler.deleteJob(taskId)
            return null
        }
        // 任务未开启，跳过
        if (!task.enabled) {
            logger.info("Task[$taskId] status is paused, ignore executing.")
            return null
        }
        // 任务正在执行，跳过
        if (task.lastExecutionStatus == ExecutionStatus.RUNNING) {
            logger.warn("Task[$taskId] status is running, ignore executing.")
            return null
        }
        return task
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ScheduledReplicaJobExecutor::class.java)
    }
}
