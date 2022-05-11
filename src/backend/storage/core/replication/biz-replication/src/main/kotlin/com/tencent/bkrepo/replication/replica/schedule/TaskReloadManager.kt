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

import com.tencent.bkrepo.replication.pojo.task.ReplicaTaskInfo
import com.tencent.bkrepo.replication.pojo.task.setting.ExecutionStrategy
import com.tencent.bkrepo.replication.replica.schedule.ReplicaTaskScheduler.Companion.JOB_DATA_TASK_KEY
import com.tencent.bkrepo.replication.replica.schedule.ReplicaTaskScheduler.Companion.REPLICA_JOB_GROUP
import com.tencent.bkrepo.replication.service.ReplicaTaskService
import org.quartz.CronScheduleBuilder
import org.quartz.JobBuilder
import org.quartz.JobDetail
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.util.Date

/**
 * 同步任务加载管理类
 * 负责定时加载任务信息到调度器中
 */
@Service
class TaskReloadManager(
    private val replicaTaskService: ReplicaTaskService,
    private val taskScheduler: ReplicaTaskScheduler
) {

    /**
     * 定时从数据库中重新加载任务列表
     * quartz scheduler中的job关联的是task id
     * 已存在的任务: 根据id判断是否存在，存在则跳过
     * 新增的任务: 加入到scheduler
     * 修改的任务: 每次修改任务会删除旧任务，因此id会变更，可以当做新任务加入
     * 删除的任务: 当job执行时，判断数据库中是否存在对应的task，如果不存在表示该任务过期了，跳过执行即可
     */
    @Scheduled(initialDelay = RELOAD_INITIAL_DELAY, fixedDelay = RELOAD_FIXED_DELAY)
    fun reloadTask() {
        val scheduledTasks = replicaTaskService.listUndoScheduledTasks()
        val taskIds = scheduledTasks.map { it.id }
        val jobKeys = taskScheduler.listJobKeys().map { it.name }
        val expiredTaskId = jobKeys subtract taskIds

        var newTaskCount = 0
        var expiredTaskCount = 0
        val totalCount = scheduledTasks.size

        // 移除过期job
        expiredTaskId.forEach {
            taskScheduler.deleteJob(it)
            expiredTaskCount += 1
        }

        // 创建新job
        scheduledTasks.forEach {
            if (!taskScheduler.exist(it.id)) {
                val jobDetail = createJobDetail(it)
                val trigger = createTrigger(it)
                taskScheduler.scheduleJob(jobDetail, trigger)
                newTaskCount += 1
            }
        }
        if (logger.isDebugEnabled) {
            logger.debug(
                "Success to reload replication task, " +
                    "total: $totalCount, new: $newTaskCount, expired: $expiredTaskCount"
            )
        }
    }

    /**
     * 根据任务信息创建job detail
     */
    private fun createJobDetail(task: ReplicaTaskInfo): JobDetail {
        return JobBuilder.newJob(ScheduledReplicaJob::class.java)
            .withIdentity(task.id, REPLICA_JOB_GROUP)
            .usingJobData(JOB_DATA_TASK_KEY, task.key)
            .requestRecovery()
            .build()
    }

    /**
     * 根据任务信息创建job trigger
     */
    private fun createTrigger(task: ReplicaTaskInfo): Trigger {
        with(task.setting) {
            val builder = TriggerBuilder.newTrigger().withIdentity(task.id, REPLICA_JOB_GROUP)
            when (executionStrategy) {
                ExecutionStrategy.IMMEDIATELY -> {
                    builder.startNow()
                }
                ExecutionStrategy.SPECIFIED_TIME -> {
                    builder.startAt(Date.from(executionPlan.executeTime!!.atZone(ZoneId.systemDefault()).toInstant()))
                }
                ExecutionStrategy.CRON_EXPRESSION -> {
                    builder.withSchedule(CronScheduleBuilder.cronSchedule(executionPlan.cronExpression))
                }
            }
            return builder.build()
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(TaskReloadManager::class.java)

        /**
         * 进程启动后加载任务延迟时间
         */
        private const val RELOAD_INITIAL_DELAY = 10 * 1000L

        /**
         * 重新加载任务固定延迟时间
         */
        private const val RELOAD_FIXED_DELAY = 10 * 1000L
    }
}
